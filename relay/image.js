// `/img` 흐름(5c — docs/design/briefs/phase5c-web-images.md 1.2). I/O는 주입받는다:
// io = { fetch, cache, state, now, transformTimeoutMs } — worker.js가 globalThis.fetch·caches.default·모듈 하나의 상태·
// Date.now·10000을 넘긴다. test/image.test.js는 가짜를 넘긴다.
// 주의: 오류 메시지·URL은 헤더·로그 어디에도 쓰지 않는다(코드 숫자만). 바이트를 JS로 도는 코드는 넣지 않는다(CPU 10ms).
import * as lib from "./lib.js";

const UPSTREAM_TIMEOUT_MS = 20000;
const UPSTREAM_CF = { cacheTtl: 86400, cacheEverything: true };
const BROWSER_OK = "public, max-age=86400";
const BROWSER_FALLBACK = "public, max-age=3600";
const STORED = "public, max-age=2592000";
const WEBP = "image/webp";

/** 원본 받기 실패를 나타내는 값(응답 대신). */
class UpstreamFailure {
  constructor(response) {
    this.response = response;
  }
}

export async function handleImage(url, method, env, ctx, io) {
  const image = lib.parseImagePath(url.pathname.slice("/img/".length));
  if (!image) return new Response("Not Found", { status: 404 });
  const size = lib.parseImageSize(url.searchParams, image.path);
  if (size.error) return jsonResponse(400, { relayError: size.error });

  const token = size.token;
  // 토큰 없음(원본·_s)과 HEAD(F13)는 지금처럼 원본을 흘린다
  if (!token || method === "HEAD") {
    return passOriginal(image, io, { cache: "BYPASS", image: token ? "original; reason=head" : "original", browser: BROWSER_OK });
  }

  // F1: 변형이 캐시에 있다 — 엔진보다 먼저(off여도 저장된 변형을 낸다)
  const cacheKey = new Request(lib.imageCacheKeyUrl(url.origin, token, image.path));
  const hit = await io.cache.match(cacheKey);
  if (hit) {
    return imageResponse(hit.body, {
      type: hit.headers.get("Content-Type") ?? image.contentType,
      browser: BROWSER_OK,
      cache: "HIT",
      image: hit.headers.get("X-Relay-Image") ?? "original",
      upstream: "-",
    });
  }

  const engine = lib.imageEngine(env.IMAGE_ENGINE);
  const fallback = (reason) => ({ cache: "MISS", image: `original; reason=${reason}`, browser: BROWSER_FALLBACK });
  if (engine === "off") return passOriginal(image, io, fallback("off"));
  if (engine === "binding" && !env.IMAGES) return passOriginal(image, io, fallback("no-binding"));
  // F7: 9422 뒤 쉬는 10분
  if ((io.state.quotaUntil ?? 0) > io.now()) return passOriginal(image, io, fallback("quota"));

  const store = (body, type, value) => {
    const stored = new Response(body, {
      headers: { "Content-Type": type, "Cache-Control": STORED, "X-Relay-Image": value },
    });
    ctx.waitUntil(io.cache.put(cacheKey, stored));
  };
  if (engine === "fetch") return viaCfImage(image, token, io, store, fallback);
  return viaBinding(image, token, env, io, store, fallback);
}

/** 바인딩 엔진: 원본을 버퍼로 받아 변환, 작으면 WebP(F2)·크면 원본(F3)을 저장하고 낸다. 실패는 원본(F6·F8·F9·F10). */
async function viaBinding(image, token, env, io, store, fallback) {
  const upstream = await fetchOriginal(image, io);
  if (upstream instanceof UpstreamFailure) return upstream.response;
  const upstreamCache = upstream.headers.get("cf-cache-status") ?? "-";
  let buffer;
  try {
    buffer = await upstream.arrayBuffer();
  } catch {
    return jsonResponse(502, { relayError: "upstream" });
  }
  const original = (reason) =>
    imageResponse(buffer, { type: image.contentType, upstream: upstreamCache, ...fallback(reason) });
  if (buffer.byteLength > lib.IMAGE_INPUT_MAX_BYTES) return original("too-big");

  let output;
  try {
    output = await withTimeout(io.transformTimeoutMs, async () => {
      const result = await env.IMAGES.input(new Response(buffer).body)
        .transform(lib.IMAGE_SIZES[token])
        .output(lib.IMAGE_OUTPUT);
      return result.response().arrayBuffer();
    });
  } catch (error) {
    if (error === TIMEOUT) return original("timeout");
    const reason = lib.transformFailureReason(error);
    if (reason === "quota") io.state.quotaUntil = io.now() + lib.IMAGE_QUOTA_PAUSE_MS;
    return original(reason);
  }

  // 크기 비교는 byteLength로만
  if (output.byteLength < buffer.byteLength) {
    const value = `${token}; engine=binding`;
    store(output, WEBP, value);
    return imageResponse(output, { type: WEBP, browser: BROWSER_OK, cache: "MISS", image: value, upstream: upstreamCache });
  }
  const value = "original; reason=larger";
  store(buffer, image.contentType, value);
  return imageResponse(buffer, { type: image.contentType, browser: BROWSER_OK, cache: "MISS", image: value, upstream: upstreamCache });
}

/** fetch 엔진(`cf.image`): 성공(WebP)만 저장한다. WebP가 아니면 받은 본문(F11), 실패면 원본을 따로 받는다(F12). */
async function viaCfImage(image, token, io, store, fallback) {
  let resized;
  try {
    resized = await io.fetch(lib.imageUpstreamUrl(image.path), {
      cf: { ...UPSTREAM_CF, image: lib.cfImageOptions(token) },
      signal: AbortSignal.timeout(UPSTREAM_TIMEOUT_MS),
    });
  } catch {
    return jsonResponse(502, { relayError: "upstream" });
  }
  const upstreamCache = resized.headers.get("cf-cache-status") ?? "-";
  if (resized.status === 404) return notFound(upstreamCache, "MISS");
  const errorCode = lib.cfResizedErrorCode(resized.headers.get("cf-resized"));
  if (resized.ok && errorCode === null) {
    const type = (resized.headers.get("Content-Type") ?? "").split(";")[0].trim().toLowerCase();
    if (type === WEBP) {
      const body = await resized.arrayBuffer();
      const value = `${token}; engine=fetch`;
      store(body, WEBP, value);
      return imageResponse(body, { type: WEBP, browser: BROWSER_OK, cache: "MISS", image: value, upstream: upstreamCache });
    }
    return imageResponse(resized.body, { type: image.contentType, upstream: upstreamCache, ...fallback("not-resized") });
  }
  // 받은 본문은 버린다(오류 본문). 원본을 변환 없이 다시 받는다.
  await resized.body?.cancel();
  let reason;
  if (errorCode === 9422) {
    reason = "quota";
    io.state.quotaUntil = io.now() + lib.IMAGE_QUOTA_PAUSE_MS;
  } else {
    reason = errorCode !== null ? `error-${errorCode}` : `error-${resized.status}`;
  }
  return passOriginal(image, io, fallback(reason));
}

/** 원본을 지금처럼 받아 그대로 흘린다(스트림). 404 → 404(F14), 그 밖 실패 → 502(F15). */
async function passOriginal(image, io, { cache, image: value, browser }) {
  const upstream = await fetchOriginal(image, io, cache);
  if (upstream instanceof UpstreamFailure) return upstream.response;
  return imageResponse(upstream.body, {
    type: image.contentType,
    browser,
    cache,
    image: value,
    upstream: upstream.headers.get("cf-cache-status") ?? "-",
  });
}

async function fetchOriginal(image, io, cacheState = "MISS") {
  let upstream;
  try {
    upstream = await io.fetch(lib.imageUpstreamUrl(image.path), {
      cf: UPSTREAM_CF,
      signal: AbortSignal.timeout(UPSTREAM_TIMEOUT_MS),
    });
  } catch {
    // 예외 메시지에는 URL이 들어 있을 수 있어 버린다
    return new UpstreamFailure(jsonResponse(502, { relayError: "upstream" }));
  }
  if (upstream.ok) return upstream;
  const upstreamCache = upstream.headers.get("cf-cache-status") ?? "-";
  await upstream.body?.cancel();
  if (upstream.status === 404) return new UpstreamFailure(notFound(upstreamCache, cacheState));
  return new UpstreamFailure(
    new Response(null, { status: 502, headers: { "X-Relay-Cache": cacheState, "X-Relay-Upstream-Cache": upstreamCache } }),
  );
}

/** F14: 본문 없음, 1시간 — 없는 `_s`를 브라우저가 거듭 부르지 않게. */
function notFound(upstreamCache, cacheState) {
  return new Response(null, {
    status: 404,
    headers: { "Cache-Control": BROWSER_FALLBACK, "X-Relay-Cache": cacheState, "X-Relay-Upstream-Cache": upstreamCache },
  });
}

// 원본의 Content-Type(octet-stream)·Content-Disposition(attachment)은 옮기지 않는다.
function imageResponse(body, { type, browser, cache, image, upstream }) {
  return new Response(body, {
    headers: {
      "Content-Type": type,
      "Cache-Control": browser,
      "X-Relay-Cache": cache,
      "X-Relay-Image": image,
      "X-Relay-Upstream-Cache": upstream,
    },
  });
}

function jsonResponse(status, value) {
  return new Response(JSON.stringify(value), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8", "Cache-Control": "no-store" },
  });
}

const TIMEOUT = Symbol("timeout");

/** [run]이 [ms] 안에 끝나지 않으면 TIMEOUT을 던진다(변환은 뒤에서 끝나도 버린다). */
async function withTimeout(ms, run) {
  let timer;
  const timeout = new Promise((_, reject) => {
    timer = setTimeout(() => reject(TIMEOUT), ms);
  });
  try {
    return await Promise.race([run(), timeout]);
  } finally {
    clearTimeout(timer);
  }
}
