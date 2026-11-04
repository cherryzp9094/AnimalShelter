// AnimalShelter 중계 Worker: 정적 웹 앱 + /api(공공데이터) + /img(사진) + /a/{id}(공유 미리보기) + assetlinks.
// 규약: docs/design/02-data.md 7절. 순수 함수는 lib.js.
// 주의: 업스트림 URL에는 serviceKey가 들어 있다 — console.log에 URL·env·업스트림 응답을 쓰지 않는다.
import assetLinks from "./assetlinks.json";
import * as lib from "./lib.js";

const TIMEOUT_MS = 20000;
const JSON_TYPE = "application/json; charset=utf-8";

export default {
  async fetch(request, env, ctx) {
    try {
      return await route(request, env, ctx);
    } catch {
      // 예외 메시지에는 URL이 들어 있을 수 있어 버린다.
      return jsonResponse(502, { relayError: "upstream" });
    }
  },
};

async function route(request, env, ctx) {
  const url = new URL(request.url);
  const path = url.pathname;
  const corsPath = path.startsWith("/api/") || path.startsWith("/img/");

  if (request.method === "OPTIONS" && corsPath) return preflight(request, env);
  if (request.method !== "GET" && request.method !== "HEAD") {
    return new Response(null, { status: 405, headers: { Allow: corsPath ? "GET, HEAD, OPTIONS" : "GET, HEAD" } });
  }

  if (corsPath) {
    const origin = request.headers.get("Origin");
    if (lib.corsDecision(origin, request.url, env.ALLOWED_ORIGINS) === "deny") {
      return jsonResponse(403, { relayError: "origin" });
    }
    const head = request.method === "HEAD";
    const response = path.startsWith("/api/") ? await handleApi(url, env, ctx, head) : await handleImage(url);
    return withCors(response, origin);
  }
  if (path.startsWith("/a/")) return handleShare(request, url, env, ctx);
  if (path === "/.well-known/assetlinks.json") {
    return new Response(JSON.stringify(assetLinks), {
      headers: { "Content-Type": "application/json", "Cache-Control": "public, max-age=3600" },
    });
  }
  if (path.startsWith("/.well-known/")) return new Response("Not Found", { status: 404 });
  return env.ASSETS.fetch(request);
}

async function handleApi(url, env, ctx, head) {
  const route = lib.parseApiPath(url.pathname);
  if (!route) return jsonResponse(404, { relayError: "route" });
  const query = lib.validateQuery(route.service, route.operation, url.searchParams);
  if (query.error) return jsonResponse(400, { relayError: query.error });
  if (!env.SERVICE_KEY) return jsonResponse(500, { relayError: "config" });
  // HEAD는 업스트림·캐시를 보지 않는다(R1 — 미리보기 봇·점검 도구가 한도를 쓰지 않게)
  if (head) return new Response(null, { headers: lib.headHeaders("api") });

  const ttl = lib.CACHE_TTL_SECONDS[route.operation];
  const cacheKey = ttl ? new Request(lib.cacheKeyUrl(url.origin, route.service, route.operation, query.params)) : null;
  if (cacheKey) {
    const hit = await caches.default.match(cacheKey);
    if (hit) return apiResponse(hit.status, await hit.text(), "HIT");
  }

  let upstream;
  try {
    upstream = await fetch(lib.buildUpstreamUrl(route.service, route.operation, query.params, env.SERVICE_KEY), {
      headers: { Accept: "application/json" },
      signal: AbortSignal.timeout(TIMEOUT_MS),
    });
  } catch {
    return jsonResponse(502, { relayError: "upstream" });
  }

  if (!cacheKey) return apiResponse(upstream.status, upstream.body, "BYPASS");

  const text = await upstream.text();
  if (lib.shouldCache(upstream.status, text)) {
    const stored = new Response(text, {
      headers: { "Content-Type": JSON_TYPE, "Cache-Control": `public, max-age=${ttl}` },
    });
    ctx.waitUntil(caches.default.put(cacheKey, stored));
  }
  return apiResponse(upstream.status, text, "MISS");
}

async function handleImage(url) {
  const image = lib.parseImagePath(url.pathname.slice("/img/".length));
  if (!image) return new Response("Not Found", { status: 404 });
  let upstream;
  try {
    upstream = await fetch(lib.imageUpstreamUrl(image.path), {
      cf: { cacheTtl: 86400, cacheEverything: true },
      signal: AbortSignal.timeout(TIMEOUT_MS),
    });
  } catch {
    return jsonResponse(502, { relayError: "upstream" });
  }
  if (!upstream.ok) return new Response(null, { status: upstream.status === 404 ? 404 : 502 });
  // 원본의 Content-Type(octet-stream)·Content-Disposition(attachment)은 옮기지 않는다.
  return new Response(upstream.body, {
    headers: { "Content-Type": image.contentType, "Cache-Control": "public, max-age=86400" },
  });
}

async function handleShare(request, url, env, ctx) {
  const desertionNo = lib.parseShareId(url.pathname);
  if (!desertionNo) return new Response("Not Found", { status: 404 });
  // HEAD는 조회하지 않는다(R1)
  if (request.method === "HEAD") return new Response(null, { headers: lib.headHeaders("share") });

  // 조회 결과(요약 JSON)만 1시간 캐시한다. HTML은 요청마다 조립한다(D14).
  const cacheKey = new Request(`${url.origin}/__share/${desertionNo}`);
  let summary = null;
  let cacheState = "MISS";
  const hit = await caches.default.match(cacheKey);
  if (hit) {
    summary = await hit.json();
    cacheState = "HIT";
  } else {
    const result = await lookupAnimal(desertionNo, env);
    if (!result.error) {
      summary = result.item ? lib.summarize(result.item) : { found: false };
      const stored = new Response(JSON.stringify(summary), {
        headers: { "Content-Type": JSON_TYPE, "Cache-Control": "public, max-age=3600" },
      });
      ctx.waitUntil(caches.default.put(cacheKey, stored));
    }
  }

  const index = await env.ASSETS.fetch(new Request(new URL("/", request.url)));
  const html = await index.text();
  const todayIso = lib.kstToday(new Date());
  const meta = lib.shareMeta(summary, { origin: url.origin, desertionNo, todayIso });
  return new Response(lib.injectOg(html, meta, desertionNo), {
    headers: {
      "Content-Type": "text/html; charset=utf-8",
      "Cache-Control": "public, max-age=3600",
      "X-Relay-Cache": cacheState,
    },
  });
}

/** 재조회 창 규칙으로 최대 3회. { item } | { item: null }(0건) | { error: true }(업스트림 실패, 캐시 안 함). */
async function lookupAnimal(desertionNo, env) {
  if (!env.SERVICE_KEY) return { error: true };
  const thisYear = Number(lib.kstToday(new Date()).slice(0, 4));
  for (const window of lib.lookupWindows(desertionNo, thisYear)) {
    const params = [["desertion_no", desertionNo], ["numOfRows", "1"], ["pageNo", "1"]];
    if (window) params.push(["bgnde", window[0]], ["endde", window[1]]);
    let json;
    try {
      const upstream = await fetch(
        lib.buildUpstreamUrl("abandonmentPublicService_v2", "abandonmentPublic_v2", params, env.SERVICE_KEY),
        { headers: { Accept: "application/json" }, signal: AbortSignal.timeout(TIMEOUT_MS) },
      );
      if (upstream.status !== 200) return { error: true };
      json = await upstream.json();
    } catch {
      return { error: true };
    }
    if (json?.response?.header?.resultCode !== "00") return { error: true };
    const item = lib.itemsOf(json).find((it) => it?.desertionNo === desertionNo);
    if (item) return { item };
  }
  return { item: null };
}

function apiResponse(status, body, cacheState) {
  return new Response(body, {
    status,
    headers: { "Content-Type": JSON_TYPE, "Cache-Control": "no-store", "X-Relay-Cache": cacheState },
  });
}

function jsonResponse(status, value) {
  return new Response(JSON.stringify(value), {
    status,
    headers: { "Content-Type": JSON_TYPE, "Cache-Control": "no-store" },
  });
}

function withCors(response, origin) {
  if (!origin) return response;
  const headers = new Headers(response.headers);
  headers.set("Access-Control-Allow-Origin", origin);
  headers.set("Access-Control-Expose-Headers", "X-Relay-Cache");
  headers.append("Vary", "Origin");
  return new Response(response.body, { status: response.status, headers });
}

function preflight(request, env) {
  const origin = request.headers.get("Origin");
  if (!origin || lib.corsDecision(origin, request.url, env.ALLOWED_ORIGINS) !== "allow") {
    return jsonResponse(403, { relayError: "origin" });
  }
  const headers = {
    "Access-Control-Allow-Origin": origin,
    "Access-Control-Allow-Methods": "GET, OPTIONS",
    "Access-Control-Max-Age": "3600",
    Vary: "Origin",
  };
  const requested = request.headers.get("Access-Control-Request-Headers");
  if (requested) headers["Access-Control-Allow-Headers"] = requested;
  return new Response(null, { status: 204, headers });
}
