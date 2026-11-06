// /img 흐름 테스트(5c — docs/design/briefs/phase5c-web-images.md 1.2·I2). node --test, 네트워크 없음:
// 가짜 fetch·cache·env.IMAGES, Node의 Response·Headers·ReadableStream.
import { test } from "node:test";
import assert from "node:assert/strict";
import { handleImage } from "../image.js";
import * as lib from "../lib.js";

const ORIGIN = "https://relay.example";
const PATH = "shelter/2026/10/202611041511187.jpg";
const BRACKET = "shelter/2026/10/202611041011648[1].jpg";
const ORIGINAL = new Uint8Array(1000).fill(7);
const WEBP = new Uint8Array(100).fill(9);

/** 업스트림 가짜. respond(url, init) → Response. 부른 주소·init을 남긴다. */
function fakeFetch(respond = () => original()) {
  const calls = [];
  const fn = async (url, init) => {
    calls.push({ url: String(url), init });
    return respond(String(url), init);
  };
  fn.calls = calls;
  return fn;
}

function original(status = 200, extra = {}) {
  return new Response(status === 200 ? ORIGINAL : null, {
    status,
    headers: { "Content-Type": "application/octet-stream", "cf-cache-status": "HIT", ...extra },
  });
}

/** Cache API 가짜(Map). put은 본문을 다 읽어 둔다. */
function fakeCache() {
  const store = new Map();
  return {
    store,
    matches: 0,
    async match(request) {
      this.matches += 1;
      const hit = store.get(request.url);
      return hit ? new Response(hit.body, { headers: hit.headers }) : undefined;
    },
    async put(request, response) {
      store.set(request.url, { body: new Uint8Array(await response.arrayBuffer()), headers: [...response.headers] });
    },
  };
}

/** Images 바인딩 가짜. 결과(바이트)·예외·지연을 고른다. 받은 transform·output을 남긴다. */
function fakeImages({ result = WEBP, error = null, delayMs = 0 } = {}) {
  const log = { inputs: 0, transforms: [], outputs: [] };
  const transformer = {
    transform(options) {
      log.transforms.push(options);
      return transformer;
    },
    async output(options) {
      log.outputs.push(options);
      if (delayMs) await new Promise((resolve) => setTimeout(resolve, delayMs));
      if (error) throw error;
      return {
        contentType: () => "image/webp",
        response: () => new Response(result, { headers: { "Content-Type": "image/webp" } }),
      };
    },
  };
  return {
    log,
    input(stream) {
      assert.ok(stream instanceof ReadableStream);
      log.inputs += 1;
      return transformer;
    },
  };
}

function imagesError(code, message = "secret https://upstream.example/?serviceKey=x") {
  const error = new Error(message);
  if (code !== undefined) error.code = code;
  return error;
}

function setup({ fetch = fakeFetch(), images = fakeImages(), engine, timeoutMs = 10000, now = 1_000_000 } = {}) {
  const pending = [];
  const env = { IMAGE_ENGINE: engine };
  if (images) env.IMAGES = images;
  const io = { fetch, cache: fakeCache(), state: {}, now: () => io.clock, clock: now, transformTimeoutMs: timeoutMs };
  const ctx = { waitUntil: (promise) => pending.push(promise) };
  const call = async (pathAndQuery, method = "GET") => {
    const response = await handleImage(new URL(`${ORIGIN}/img/${pathAndQuery}`), method, env, ctx, io);
    await Promise.all(pending.splice(0));
    return response;
  };
  return { env, io, ctx, call, fetch, images };
}

const bytesOf = async (response) => new Uint8Array(await response.arrayBuffer());

test("R1 parseImageSize: 없음·sq480·fit1080, 둘 이상·모르는 값·빈 값·대문자·_s + 토큰은 param", () => {
  const p = (query, path = PATH) => lib.parseImageSize(new URLSearchParams(query), path);
  assert.deepEqual(p(""), { token: null });
  assert.deepEqual(p("w=1"), { token: null });
  assert.deepEqual(p("size=sq480"), { token: "sq480" });
  assert.deepEqual(p("size=fit1080"), { token: "fit1080" });
  for (const bad of ["size=sq480&size=fit1080", "size=sq480&size=sq480", "size=sq481", "size=", "size=SQ480", "size=constructor"]) {
    assert.deepEqual(p(bad), { error: "param" }, bad);
  }
  assert.deepEqual(p("size=sq480", "shelter/2026/10/202611041511187_s.jpg"), { error: "param" });
  assert.deepEqual(p("size=fit1080", "shelter/2026/10/a_S.JPEG"), { error: "param" });
  assert.deepEqual(p("", "shelter/2026/10/202611041511187_s.jpg"), { token: null });
});

test("R3 토큰 없음: 원본을 그대로 흘린다 — BYPASS·original·업스트림 cf-cache-status", async () => {
  const { call, fetch, images } = setup();
  const response = await call(PATH);
  assert.equal(response.status, 200);
  assert.equal(response.headers.get("Content-Type"), "image/jpeg");
  assert.equal(response.headers.get("Cache-Control"), "public, max-age=86400");
  assert.equal(response.headers.get("X-Relay-Cache"), "BYPASS");
  assert.equal(response.headers.get("X-Relay-Image"), "original");
  assert.equal(response.headers.get("X-Relay-Upstream-Cache"), "HIT");
  assert.deepEqual(await bytesOf(response), ORIGINAL);
  assert.equal(fetch.calls.length, 1);
  assert.equal(fetch.calls[0].url, lib.imageUpstreamUrl(PATH));
  assert.deepEqual(fetch.calls[0].init.cf, { cacheTtl: 86400, cacheEverything: true });
  assert.equal(images.log.inputs, 0);
});

test("R3 _s 주소(토큰 없음)도 그대로, 업스트림 cf-cache-status가 없으면 -", async () => {
  const fetch = fakeFetch(() => new Response(ORIGINAL, { headers: {} }));
  const { call } = setup({ fetch });
  const response = await call("shelter/2026/10/202611041511187_s.jpg");
  assert.equal(response.status, 200);
  assert.equal(response.headers.get("X-Relay-Upstream-Cache"), "-");
  assert.equal(response.headers.get("X-Relay-Image"), "original");
});

test("R4 sq480 MISS: 바인딩 transform·output이 1.2 (b)와 같고 200 image/webp", async () => {
  const { call, images, fetch } = setup();
  const response = await call(`${PATH}?size=sq480`);
  assert.equal(response.status, 200);
  assert.equal(response.headers.get("Content-Type"), "image/webp");
  assert.equal(response.headers.get("X-Relay-Cache"), "MISS");
  assert.equal(response.headers.get("X-Relay-Image"), "sq480; engine=binding");
  assert.equal(response.headers.get("X-Relay-Upstream-Cache"), "HIT");
  assert.equal(response.headers.get("Cache-Control"), "public, max-age=86400");
  assert.deepEqual(await bytesOf(response), WEBP);
  assert.deepEqual(images.log.transforms, [
    { width: 480, height: 480, fit: "crop", gravity: { x: 0.5, y: 0.3, mode: "remainder" } },
  ]);
  assert.deepEqual(images.log.outputs, [{ format: "image/webp", quality: 75 }]);
  assert.deepEqual(fetch.calls[0].init.cf, { cacheTtl: 86400, cacheEverything: true });
});

test("R4 fit1080: scale-down 1080 틀", async () => {
  const { call, images } = setup();
  const response = await call(`${PATH}?size=fit1080`);
  assert.equal(response.headers.get("X-Relay-Image"), "fit1080; engine=binding");
  assert.deepEqual(images.log.transforms, [{ width: 1080, height: 1080, fit: "scale-down" }]);
});

test("R5 저장: 키 origin/__img/v1/sq480/<경로>, 30일, 본문 = 결과", async () => {
  const { call, io } = setup();
  await call(`${PATH}?size=sq480`);
  const key = `${ORIGIN}/__img/v1/sq480/${PATH}`;
  assert.deepEqual([...io.cache.store.keys()], [key]);
  const stored = io.cache.store.get(key);
  const headers = new Headers(stored.headers);
  assert.equal(headers.get("Cache-Control"), "public, max-age=2592000");
  assert.equal(headers.get("Content-Type"), "image/webp");
  assert.equal(headers.get("X-Relay-Image"), "sq480; engine=binding");
  assert.deepEqual(stored.body, WEBP);
});

test("R6 HIT: 저장된 본문·X-Relay-Image, 업스트림·바인딩 호출 0", async () => {
  const first = setup();
  await first.call(`${PATH}?size=sq480`);
  const fetch = fakeFetch();
  const images = fakeImages();
  const second = setup({ fetch, images });
  second.io.cache = first.io.cache;
  const response = await second.call(`${PATH}?size=sq480`);
  assert.equal(response.status, 200);
  assert.equal(response.headers.get("X-Relay-Cache"), "HIT");
  assert.equal(response.headers.get("X-Relay-Image"), "sq480; engine=binding");
  assert.equal(response.headers.get("X-Relay-Upstream-Cache"), "-");
  assert.equal(response.headers.get("Cache-Control"), "public, max-age=86400");
  assert.equal(response.headers.get("Content-Type"), "image/webp");
  assert.deepEqual(await bytesOf(response), WEBP);
  assert.equal(fetch.calls.length, 0);
  assert.equal(images.log.inputs, 0);
});

test("R6 캐시 확인은 엔진보다 먼저 — off여도 저장된 변형을 낸다", async () => {
  const first = setup();
  await first.call(`${PATH}?size=sq480`);
  const second = setup({ engine: "off" });
  second.io.cache = first.io.cache;
  const response = await second.call(`${PATH}?size=sq480`);
  assert.equal(response.headers.get("X-Relay-Cache"), "HIT");
  assert.equal(second.fetch.calls.length, 0);
});

test("R7 결과 ≥ 원본: 원본·reason=larger, 원본 바이트를 변형 키에 저장", async () => {
  const { call, io } = setup({ images: fakeImages({ result: new Uint8Array(1000).fill(1) }) });
  const response = await call(`${PATH}?size=sq480`);
  assert.equal(response.status, 200);
  assert.equal(response.headers.get("Content-Type"), "image/jpeg");
  assert.equal(response.headers.get("X-Relay-Image"), "original; reason=larger");
  assert.equal(response.headers.get("X-Relay-Cache"), "MISS");
  assert.equal(response.headers.get("Cache-Control"), "public, max-age=86400");
  assert.deepEqual(await bytesOf(response), ORIGINAL);
  const stored = io.cache.store.get(`${ORIGIN}/__img/v1/sq480/${PATH}`);
  assert.deepEqual(stored.body, ORIGINAL);
  assert.equal(new Headers(stored.headers).get("X-Relay-Image"), "original; reason=larger");
  assert.equal(new Headers(stored.headers).get("Content-Type"), "image/jpeg");
});

test("R8 9422: 원본·reason=quota·저장 0, 10분 안의 다음 요청은 바인딩 호출 0, 10분 뒤 다시 시도", async () => {
  const images = fakeImages({ error: imagesError(9422) });
  const { call, io } = setup({ images });
  const first = await call(`${PATH}?size=sq480`);
  assert.equal(first.headers.get("X-Relay-Image"), "original; reason=quota");
  assert.equal(first.headers.get("Cache-Control"), "public, max-age=3600");
  assert.equal(first.headers.get("X-Relay-Cache"), "MISS");
  assert.deepEqual(await bytesOf(first), ORIGINAL);
  assert.equal(io.cache.store.size, 0);
  assert.equal(images.log.inputs, 1);

  io.clock += 9 * 60 * 1000;
  const paused = await call(`${PATH}?size=fit1080`);
  assert.equal(paused.headers.get("X-Relay-Image"), "original; reason=quota");
  assert.equal(paused.headers.get("Cache-Control"), "public, max-age=3600");
  assert.deepEqual(await bytesOf(paused), ORIGINAL);
  assert.equal(images.log.inputs, 1);
  assert.equal(io.cache.store.size, 0);

  io.clock += 2 * 60 * 1000;
  await call(`${PATH}?size=fit1080`);
  assert.equal(images.log.inputs, 2);
});

test("R9 다른 코드 → error-<n>, 코드 없는 예외 → error, 시간 초과 → timeout(모두 원본·1시간·저장 0)", async () => {
  for (const [images, reason, timeoutMs] of [
    [fakeImages({ error: imagesError(9412) }), "error-9412", 10000],
    [fakeImages({ error: imagesError(undefined) }), "error", 10000],
    [fakeImages({ delayMs: 200 }), "timeout", 20],
  ]) {
    const { call, io } = setup({ images, timeoutMs });
    const response = await call(`${PATH}?size=sq480`);
    assert.equal(response.status, 200, reason);
    assert.equal(response.headers.get("X-Relay-Image"), `original; reason=${reason}`);
    assert.equal(response.headers.get("Cache-Control"), "public, max-age=3600");
    assert.equal(response.headers.get("Content-Type"), "image/jpeg");
    assert.deepEqual(await bytesOf(response), ORIGINAL);
    assert.equal(io.cache.store.size, 0);
  }
});

test("R9 원본 > 20MB → too-big(바인딩 호출 0)", async () => {
  const big = new Uint8Array(lib.IMAGE_INPUT_MAX_BYTES + 1);
  const fetch = fakeFetch(() => new Response(big));
  const { call, images } = setup({ fetch });
  const response = await call(`${PATH}?size=sq480`);
  assert.equal(response.headers.get("X-Relay-Image"), "original; reason=too-big");
  assert.equal(images.log.inputs, 0);
  assert.equal((await bytesOf(response)).byteLength, big.byteLength);
});

test("R10 env.IMAGES 없음 → no-binding, off → off(바인딩 호출 0), 모르는 값 → off", async () => {
  const none = setup({ images: null });
  const a = await none.call(`${PATH}?size=sq480`);
  assert.equal(a.headers.get("X-Relay-Image"), "original; reason=no-binding");
  assert.equal(a.headers.get("Cache-Control"), "public, max-age=3600");
  assert.deepEqual(await bytesOf(a), ORIGINAL);

  for (const engine of ["off", "Binding", "zz"]) {
    const off = setup({ engine });
    const b = await off.call(`${PATH}?size=sq480`);
    assert.equal(b.headers.get("X-Relay-Image"), "original; reason=off", engine);
    assert.equal(b.headers.get("X-Relay-Cache"), "MISS");
    assert.equal(b.headers.get("Cache-Control"), "public, max-age=3600");
    assert.equal(off.images.log.inputs, 0);
    assert.equal(off.io.cache.store.size, 0);
  }
});

test("R11 원본 404 → 404 + 1시간, 500 → 502, fetch 예외 → 502 upstream", async () => {
  for (const query of ["", "?size=sq480"]) {
    const notFound = setup({ fetch: fakeFetch(() => original(404)) });
    const a = await notFound.call(`${PATH}${query}`);
    assert.equal(a.status, 404);
    assert.equal(a.headers.get("Cache-Control"), "public, max-age=3600");
    assert.equal((await bytesOf(a)).byteLength, 0);
    assert.equal(notFound.images.log.inputs, 0);

    const broken = setup({ fetch: fakeFetch(() => original(500)) });
    const b = await broken.call(`${PATH}${query}`);
    assert.equal(b.status, 502);

    const thrown = setup({ fetch: fakeFetch(() => { throw new Error("https://upstream/?serviceKey=x"); }) });
    const c = await thrown.call(`${PATH}${query}`);
    assert.equal(c.status, 502);
    assert.equal(c.headers.get("Cache-Control"), "no-store");
    assert.deepEqual(JSON.parse(new TextDecoder().decode(await bytesOf(c))), { relayError: "upstream" });
  }
});

test("R11 잘못된 경로 404, 잘못된 size 400 param", async () => {
  const { call, fetch } = setup();
  assert.equal((await call("shelter/../x.jpg")).status, 404);
  for (const query of ["size=bogus", "size=sq480&size=fit1080"]) {
    const response = await call(`${PATH}?${query}`);
    assert.equal(response.status, 400);
    assert.deepEqual(JSON.parse(new TextDecoder().decode(await bytesOf(response))), { relayError: "param" });
  }
  const thumb = await call("shelter/2026/10/202611041511187_s.jpg?size=sq480");
  assert.equal(thumb.status, 400);
  assert.equal(fetch.calls.length, 0);
});

test("R12 HEAD + 토큰: 바인딩 호출 0, reason=head, BYPASS", async () => {
  const { call, images, io } = setup();
  const response = await call(`${PATH}?size=sq480`, "HEAD");
  assert.equal(response.status, 200);
  assert.equal(response.headers.get("X-Relay-Image"), "original; reason=head");
  assert.equal(response.headers.get("X-Relay-Cache"), "BYPASS");
  assert.equal(response.headers.get("Content-Type"), "image/jpeg");
  assert.equal(images.log.inputs, 0);
  assert.equal(io.cache.matches, 0);
});

test("R13 fetch 엔진 성공: engine=fetch, 넘긴 cf.image = cfImageOptions, 저장", async () => {
  const fetch = fakeFetch((url, init) =>
    init?.cf?.image
      ? new Response(WEBP, { headers: { "Content-Type": "image/webp", "cf-resized": "internal=ok/m", "cf-cache-status": "MISS" } })
      : original());
  const { call, images, io } = setup({ fetch, engine: "fetch" });
  const response = await call(`${PATH}?size=sq480`);
  assert.equal(response.status, 200);
  assert.equal(response.headers.get("Content-Type"), "image/webp");
  assert.equal(response.headers.get("X-Relay-Image"), "sq480; engine=fetch");
  assert.equal(response.headers.get("X-Relay-Upstream-Cache"), "MISS");
  assert.equal(response.headers.get("Cache-Control"), "public, max-age=86400");
  assert.deepEqual(await bytesOf(response), WEBP);
  assert.equal(fetch.calls.length, 1);
  assert.equal(fetch.calls[0].url, lib.imageUpstreamUrl(PATH));
  assert.deepEqual(fetch.calls[0].init.cf, { cacheTtl: 86400, cacheEverything: true, image: lib.cfImageOptions("sq480") });
  assert.deepEqual(lib.cfImageOptions("sq480"), {
    width: 480, height: 480, fit: "crop", gravity: { x: 0.5, y: 0.3 }, format: "webp", quality: 75,
  });
  assert.deepEqual(lib.cfImageOptions("fit1080"), { width: 1080, height: 1080, fit: "scale-down", format: "webp", quality: 75 });
  assert.equal(images.log.inputs, 0);
  assert.equal(io.cache.store.size, 1);
});

test("R14 fetch 엔진: 200 JPEG → not-resized(받은 본문), err=9422 → quota + 원본 다시 받기, err=9401 → error-9401, 500 → error-500", async () => {
  const jpeg = setup({
    engine: "fetch",
    fetch: fakeFetch(() => new Response(ORIGINAL, { headers: { "Content-Type": "image/jpeg" } })),
  });
  const a = await jpeg.call(`${PATH}?size=sq480`);
  assert.equal(a.headers.get("X-Relay-Image"), "original; reason=not-resized");
  assert.equal(a.headers.get("Content-Type"), "image/jpeg");
  assert.equal(a.headers.get("Cache-Control"), "public, max-age=3600");
  assert.deepEqual(await bytesOf(a), ORIGINAL);
  assert.equal(jpeg.fetch.calls.length, 1);
  assert.equal(jpeg.io.cache.store.size, 0);

  for (const [status, resized, reason] of [[400, "err=9422", "quota"], [415, "err=9401", "error-9401"], [500, null, "error-500"]]) {
    const failing = setup({
      engine: "fetch",
      fetch: fakeFetch((url, init) =>
        init?.cf?.image
          ? new Response("bad", { status, headers: resized ? { "cf-resized": resized } : {} })
          : original()),
    });
    const b = await failing.call(`${PATH}?size=fit1080`);
    assert.equal(b.status, 200, reason);
    assert.equal(b.headers.get("X-Relay-Image"), `original; reason=${reason}`);
    assert.equal(b.headers.get("Cache-Control"), "public, max-age=3600");
    assert.deepEqual(await bytesOf(b), ORIGINAL);
    assert.equal(failing.fetch.calls.length, 2);
    assert.equal(failing.fetch.calls[1].init.cf.image, undefined);
    assert.equal(failing.io.cache.store.size, 0);
  }
});

test("R14 fetch 엔진 9422 뒤 10분은 cf.image 없이 원본만", async () => {
  const fetch = fakeFetch((url, init) =>
    init?.cf?.image ? new Response("bad", { status: 400, headers: { "cf-resized": "err=9422" } }) : original());
  const { call } = setup({ engine: "fetch", fetch });
  await call(`${PATH}?size=sq480`);
  assert.equal(fetch.calls.length, 2);
  const paused = await call(`${PATH}?size=sq480`);
  assert.equal(paused.headers.get("X-Relay-Image"), "original; reason=quota");
  assert.equal(fetch.calls.length, 3);
  assert.equal(fetch.calls[2].init.cf.image, undefined);
});

test("R15 대괄호 경로: 업스트림 주소·캐시 키가 %5B·%5D", async () => {
  const { call, fetch, io } = setup();
  const response = await call(`${BRACKET.replace("[", "%5B").replace("]", "%5D")}?size=sq480`);
  assert.equal(response.status, 200);
  assert.equal(fetch.calls[0].url, "https://openapi.animal.go.kr/openapi/service/rest/fileDownloadSrvc/files/shelter/2026/10/202611041011648%5B1%5D.jpg");
  assert.deepEqual([...io.cache.store.keys()], [`${ORIGIN}/__img/v1/sq480/shelter/2026/10/202611041011648%5B1%5D.jpg`]);
  assert.equal(lib.imageCacheKeyUrl(ORIGIN, "fit1080", BRACKET), `${ORIGIN}/__img/v1/fit1080/shelter/2026/10/202611041011648%5B1%5D.jpg`);
});

test("R16 EXPOSE_HEADERS에 셋, /img 응답 헤더에 URL·오류 메시지가 없다", async () => {
  assert.equal(lib.EXPOSE_HEADERS, "X-Relay-Cache, X-Relay-Image, X-Relay-Upstream-Cache");
  const cases = [
    setup({ images: fakeImages({ error: imagesError(9412) }) }),
    setup({ images: fakeImages({ error: imagesError(undefined) }) }),
    setup({ engine: "fetch", fetch: fakeFetch((u, init) => init?.cf?.image ? new Response("x", { status: 400, headers: { "cf-resized": "err=9500" } }) : original()) }),
    setup({ fetch: fakeFetch(() => { throw new Error("https://upstream.example/?serviceKey=x"); }) }),
  ];
  for (const { call } of cases) {
    for (const query of ["", "?size=sq480"]) {
      const response = await call(`${PATH}${query}`);
      for (const [name, value] of response.headers) {
        assert.ok(!/https?:|serviceKey|secret|upstream\.example/i.test(value), `${name}: ${value}`);
      }
    }
  }
});

test("transformFailureReason·cfResizedErrorCode·imageEngine", () => {
  assert.equal(lib.transformFailureReason(imagesError(9422)), "quota");
  assert.equal(lib.transformFailureReason(imagesError(9412)), "error-9412");
  assert.equal(lib.transformFailureReason(imagesError("9523")), "error-9523");
  assert.equal(lib.transformFailureReason(imagesError(undefined)), "error");
  assert.equal(lib.transformFailureReason(imagesError("bad")), "error");
  assert.equal(lib.transformFailureReason(null), "error");
  assert.equal(lib.cfResizedErrorCode("err=9422"), 9422);
  assert.equal(lib.cfResizedErrorCode("internal=ok/m"), null);
  assert.equal(lib.cfResizedErrorCode(null), null);
  assert.equal(lib.imageEngine(undefined), "binding");
  assert.equal(lib.imageEngine(""), "binding");
  assert.equal(lib.imageEngine("binding"), "binding");
  assert.equal(lib.imageEngine("fetch"), "fetch");
  assert.equal(lib.imageEngine("off"), "off");
  assert.equal(lib.imageEngine("FETCH"), "off");
  assert.equal(lib.isThumbnailPath("shelter/2026/10/a_s.jpg"), true);
  assert.equal(lib.isThumbnailPath("shelter/2026/10/a_s.jpeg"), true);
  assert.equal(lib.isThumbnailPath("shelter/2026/10/a_sx.jpg"), false);
  assert.equal(lib.isThumbnailPath("shelter/2026/10/a.png"), false);
});
