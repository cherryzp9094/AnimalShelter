// 중계 순수 함수 테스트(node --test, 네트워크 없음). 입력은 인라인 또는 ../docs/api/samples.
import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import * as lib from "../lib.js";

const sample = JSON.parse(
  readFileSync(new URL("../../docs/api/samples/live-2026-11-03/abandonmentPublic_v2_p1.json", import.meta.url), "utf8"),
);
const items = lib.itemsOf(sample);
const FAKE_KEY = "TEST%2BKEY%2FVAL%3D%3D";

test("parseApiPath는 허용한 서비스·오퍼레이션만", () => {
  assert.deepEqual(lib.parseApiPath("/api/abandonmentPublicService_v2/sido_v2"), {
    service: "abandonmentPublicService_v2",
    operation: "sido_v2",
  });
  assert.deepEqual(lib.parseApiPath("/api/animalShelterSrvc_v2/shelterInfo_v2/"), {
    service: "animalShelterSrvc_v2",
    operation: "shelterInfo_v2",
  });
  assert.equal(lib.parseApiPath("/api/nope/sido_v2"), null);
  assert.equal(lib.parseApiPath("/api/animalShelterSrvc_v2/sido_v2"), null);
  assert.equal(lib.parseApiPath("/api/abandonmentPublicService_v2/constructor"), null);
  assert.equal(lib.parseApiPath("/api/abandonmentPublicService_v2"), null);
});

test("validateQuery: 허용 파라미터를 이름순으로, serviceKey·_type은 버린다", () => {
  const q = new URLSearchParams("upr_cd=6110000&numOfRows=30&serviceKey=x&_type=xml&pageNo=1");
  assert.deepEqual(lib.validateQuery("abandonmentPublicService_v2", "abandonmentPublic_v2", q), {
    params: [["numOfRows", "30"], ["pageNo", "1"], ["upr_cd", "6110000"]],
  });
});

test("validateQuery: 모르는 이름·중복·50자 초과·numOfRows 1001은 param 오류", () => {
  const check = (query, op = "abandonmentPublic_v2") =>
    lib.validateQuery("abandonmentPublicService_v2", op, new URLSearchParams(query));
  assert.deepEqual(check("foo=1"), { error: "param" });
  assert.deepEqual(check("upr_cd=1&upr_cd=2"), { error: "param" });
  assert.deepEqual(check(`kind=${"1".repeat(51)}`), { error: "param" });
  assert.deepEqual(check("numOfRows=1001"), { error: "param" });
  assert.deepEqual(check("numOfRows=0"), { error: "param" });
  assert.deepEqual(check("pageNo=-1"), { error: "param" });
  assert.deepEqual(check("upkind=417000", "kind_v2"), { error: "param" }); // 품종은 up_kind_cd
  assert.deepEqual(check("numOfRows=1000").params, [["numOfRows", "1000"]]);
});

test("buildUpstreamUrl은 키를 다시 인코딩하지 않고 문자열로 붙인다", () => {
  const url = lib.buildUpstreamUrl("abandonmentPublicService_v2", "sido_v2", [["numOfRows", "100"]], FAKE_KEY);
  assert.equal(
    url,
    `https://apis.data.go.kr/1543061/abandonmentPublicService_v2/sido_v2?numOfRows=100&_type=json&serviceKey=${FAKE_KEY}`,
  );
  assert.ok(!url.includes("%252B"));
  const bare = lib.buildUpstreamUrl("abandonmentPublicService_v2", "sido_v2", [], FAKE_KEY);
  assert.ok(bare.endsWith(`sido_v2?_type=json&serviceKey=${FAKE_KEY}`));
});

test("cacheKeyUrl에는 키가 없다", () => {
  const key = lib.cacheKeyUrl("http://127.0.0.1:8788", "abandonmentPublicService_v2", "kind_v2", [["up_kind_cd", "417000"]]);
  assert.equal(key, "http://127.0.0.1:8788/api/abandonmentPublicService_v2/kind_v2?up_kind_cd=417000");
  assert.ok(!key.includes("serviceKey"));
});

test("shouldCache: 200 + resultCode 00 + totalCount > 0만", () => {
  const ok = readFileSync(new URL("../../docs/api/samples/live-2026-11-03/sido_v2.json", import.meta.url), "utf8");
  const empty = readFileSync(
    new URL("../../docs/api/samples/live-2026-11-03/abandonmentPublic_v2_empty.json", import.meta.url),
    "utf8",
  );
  const gateway = readFileSync(new URL("../../docs/api/samples/gateway_error_30.json", import.meta.url), "utf8");
  assert.equal(lib.shouldCache(200, ok), true);
  assert.equal(lib.shouldCache(500, ok), false);
  assert.equal(lib.shouldCache(200, empty), false);
  assert.equal(lib.shouldCache(200, gateway), false);
  assert.equal(lib.shouldCache(200, '{"response":{"header":{"resultCode":"99"},"body":{"totalCount":3}}}'), false);
});

test("parseImagePath: 형식 검사·대괄호 허용·확장자 대소문자·경로 이탈 거부", () => {
  assert.deepEqual(lib.parseImagePath("shelter/2026/10/20261103141175.jpg"), {
    path: "shelter/2026/10/20261103141175.jpg",
    contentType: "image/jpeg",
  });
  assert.deepEqual(lib.parseImagePath("shelter/2026/10/abc%5B1%5D.PNG"), {
    path: "shelter/2026/10/abc[1].PNG",
    contentType: "image/png",
  });
  assert.equal(lib.parseImagePath("shelter/2026/10/x.jpeg").contentType, "image/jpeg");
  assert.equal(lib.parseImagePath("shelter/2026/10/x.gif"), null);
  assert.equal(lib.parseImagePath("..%2F..%2Fetc%2Fpasswd"), null);
  assert.equal(lib.parseImagePath("shelter/2026/10/..%2Fx.jpg"), null);
  assert.equal(lib.parseImagePath("shelter/2026/10/a%2Fb.jpg"), null);
  assert.equal(lib.parseImagePath("%E0%A4%A"), null);
});

test("imageUpstreamUrl은 대괄호를 인코딩한다", () => {
  assert.equal(
    lib.imageUpstreamUrl("shelter/2026/10/a[1].jpg"),
    "https://openapi.animal.go.kr/openapi/service/rest/fileDownloadSrvc/files/shelter/2026/10/a%5B1%5D.jpg",
  );
});

test("parseShareId는 15자리 숫자만", () => {
  assert.equal(lib.parseShareId("/a/450650202602738"), "450650202602738");
  assert.equal(lib.parseShareId("/a/450650202602738/"), "450650202602738");
  assert.equal(lib.parseShareId("/a/12345"), null);
  assert.equal(lib.parseShareId("/a/45065020260273x"), null);
  assert.equal(lib.parseShareId("/a/450650202602738/x"), null);
});

test("lookupWindows: 기본 → 그 해 → 전 해, 범위 밖 연도는 기본만", () => {
  assert.deepEqual(lib.lookupWindows("450650202602738", 2026), [
    null,
    ["20260101", "20261231"],
    ["20250101", "20251231"],
  ]);
  assert.deepEqual(lib.lookupWindows("448567199901800", 2026), [null]);
  assert.deepEqual(lib.lookupWindows("448567203001800", 2026), [null]);
  assert.deepEqual(lib.lookupWindows("448567200001800", 2026), [null, ["20000101", "20001231"]]);
});

test("kstToday는 UTC+9 기준", () => {
  assert.equal(lib.kstToday(new Date("2026-11-03T14:59:59Z")), "2026-11-03");
  assert.equal(lib.kstToday(new Date("2026-11-03T15:00:00Z")), "2026-11-04");
});

test("animalStatus 네 갈래(앱과 같은 규칙)", () => {
  assert.deepEqual(lib.animalStatus("보호중", "20261113", "2026-11-03"), { type: "noticing", daysLeft: 10 });
  assert.deepEqual(lib.animalStatus("보호중", "20261103", "2026-11-03"), { type: "noticing", daysLeft: 0 });
  assert.deepEqual(lib.animalStatus("보호중", "20261102", "2026-11-03"), { type: "protecting" });
  assert.deepEqual(lib.animalStatus("종료(반환)", "20261113", "2026-11-03"), { type: "ended", reason: "반환" });
  assert.deepEqual(lib.animalStatus("종료", null, "2026-11-03"), { type: "ended", reason: null });
  assert.deepEqual(lib.animalStatus("", null, "2026-11-03"), { type: "unknown", raw: null });
});

test("미리보기 제목·설명은 실제 응답으로 05-service 문구를 만든다", () => {
  const summary = lib.summarize(items[0]);
  assert.equal(summary.found, true);
  assert.equal(Object.hasOwn(summary, "careOwnerNm"), false);
  assert.equal(lib.ogTitle(summary), "한국 고양이 · 수컷 · 2024년생");
  assert.equal(
    lib.ogDescription(summary, "2026-11-03"),
    "제주특별자치도 제2동물보호센터 · 공고 2026-11-03 ~ 11-13(D-10) · 기타(고등어) · 5kg · " +
      "개체관리번호 26652 - 목에 플라스틱통 끼임 — 최신 정보는 보호소에 확인해 주세요",
  );
  assert.ok(lib.statusText(summary, "2026-11-13").endsWith("(오늘 마감)"));
  assert.equal(lib.statusText(lib.summarize(items[4]), "2026-11-03"), "종료 · 반환");
  const young = { ...summary, age: "2026(60일미만)(년생)" };
  assert.equal(lib.ogTitle(young), "한국 고양이 · 수컷 · 2026년생 · 생후 2개월 미만");
});

test("weightText는 오타를 정리하고 범위 밖은 버린다", () => {
  assert.equal(lib.weightText("5(Kg)"), "5kg");
  assert.equal(lib.weightText("0.7(Kg)"), "0.7kg");
  assert.equal(lib.weightText("2..00(Kg)"), "2kg");
  assert.equal(lib.weightText("3,5(Kg)"), "3.5kg");
  assert.equal(lib.weightText("400(Kg)"), null);
  assert.equal(lib.weightText(null), null);
});

test("shareMeta: 사진은 중계 /img 절대 주소, 0건·오류는 안내 문구", () => {
  const meta = lib.shareMeta(lib.summarize(items[0]), {
    origin: "https://relay.example",
    desertionNo: "450650202602738",
    todayIso: "2026-11-03",
  });
  assert.equal(meta.image, "https://relay.example/img/shelter/2026/10/20261103141175.jpg");
  assert.equal(meta.url, "https://relay.example/a/450650202602738");
  const missing = lib.shareMeta({ found: false }, { origin: "o", desertionNo: "1", todayIso: "2026-11-03" });
  assert.equal(missing.title, "공고를 찾을 수 없어요");
  assert.equal(missing.image, null);
  assert.equal(lib.shareMeta(null, { origin: "o", desertionNo: "1", todayIso: "x" }).title, "애니멀 쉘터");
});

test("itemsOf는 {}·배열·객체 하나를 모두 목록으로", () => {
  assert.equal(items.length, 5);
  assert.deepEqual(lib.itemsOf({ response: { body: { items: {} } } }), []);
  assert.deepEqual(lib.itemsOf({ response: { body: { items: { item: { a: 1 } } } } }), [{ a: 1 }]);
  assert.deepEqual(lib.itemsOf(null), []);
});

test("escapeHtml은 & < > \" '를 바꾼다", () => {
  assert.equal(lib.escapeHtml(`<a href="x">'&'</a>`), "&lt;a href=&quot;x&quot;&gt;&#39;&amp;&#39;&lt;/a&gt;");
});

test("injectOg: head 맨 앞 base, OG·title 교체, body 첫 줄 replaceState, 값은 이스케이프", () => {
  const html = `<!doctype html>\n<html lang="ko">\n<head>\n<meta charset="utf-8">\n<title>애니멀 쉘터</title>\n</head>\n<body>\n<div id="root"></div>\n<script src="animalshelter.js"></script>\n</body>\n</html>`;
  const meta = {
    title: `<script>alert("x")</script>`,
    description: "설명 & 'quote'",
    image: "https://relay.example/img/shelter/2026/10/a.jpg",
    url: "https://relay.example/a/450650202602738",
  };
  const out = lib.injectOg(html, meta, "450650202602738");
  assert.equal((out.match(/<base href="\/">/g) ?? []).length, 1);
  assert.equal((out.match(/history\.replaceState/g) ?? []).length, 1);
  assert.match(out, /<body>\n<script>history\.replaceState\(null, "", "\/#\/animal\/450650202602738"\)<\/script>/);
  assert.equal((out.match(/<meta property="og:[a-z_:]*"/g) ?? []).length, 7);
  assert.ok(out.includes(`<meta property="og:type" content="website">`));
  assert.ok(out.includes(`<meta property="og:locale" content="ko_KR">`));
  assert.equal((out.match(/<title>/g) ?? []).length, 1);
  assert.ok(!out.includes("<title>애니멀 쉘터</title>"));
  assert.ok(!out.includes(`<script>alert`));
  assert.ok(out.includes("&lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt;"));
  assert.ok(out.includes(`<meta name="robots" content="noindex">`));
  assert.ok(out.includes(`<meta name="twitter:card" content="summary_large_image">`));
  // charset은 처음 1024바이트 안에 — 머리 맨 앞에 두고 그 뒤에 넣는다(H5)
  assert.match(out, /<head>\n<meta charset="utf-8">\n<base href="\/">/);
  assert.ok(out.indexOf('<meta charset') < out.indexOf("<base"));
});

test("injectOg: charset이 없으면 head 맨 앞에", () => {
  const out = lib.injectOg("<html><head><title>x</title></head><body></body></html>", {
    title: "t", description: "d", image: null, url: "u",
  }, "450650202602738");
  assert.match(out, /<head>\n<base href="\/">/);
  assert.deepEqual(lib.SECURITY_HEADERS, { "X-Content-Type-Options": "nosniff" });
});

test("injectOg: index.html의 description·robots는 동물 것으로 바꿔 하나씩만", () => {
  const html = `<!doctype html>\n<html lang="ko">\n<head>\n<meta charset="utf-8">\n<title>애니멀 쉘터</title>\n` +
    `<meta name="description" content="앱 설명">\n<meta name="robots" content="noindex">\n` +
    `<meta name="theme-color" content="#F6FAFE">\n</head>\n<body>\n</body>\n</html>`;
  const meta = { title: "제목", description: "동물 설명 <b>", image: null, url: "https://relay.example/a/450650202602738" };
  const out = lib.injectOg(html, meta, "450650202602738");
  assert.equal((out.match(/<meta name="description"/g) ?? []).length, 1);
  assert.ok(out.includes(`<meta name="description" content="동물 설명 &lt;b&gt;">`));
  assert.ok(!out.includes("앱 설명"));
  assert.equal((out.match(/<meta name="robots"/g) ?? []).length, 1);
  assert.ok(out.includes(`<meta name="theme-color" content="#F6FAFE">`));
  assert.equal((out.match(/<meta property="og:image"/g) ?? []).length, 0);
  for (const name of ["og:type", "og:locale", "og:title", "og:description", "og:url", "og:site_name"]) {
    assert.equal((out.match(new RegExp(`<meta property="${name}"`, "g")) ?? []).length, 1, name);
  }
});

test("corsDecision: Origin 없음은 허용, 같은 origin 허용, 허용 목록은 루프백 Worker에서만", () => {
  const allowed = "http://localhost:8080,http://127.0.0.1:8080";
  const local = "http://127.0.0.1:8788/api/abandonmentPublicService_v2/sido_v2";
  const deployed = "https://animalshelter.example/api/abandonmentPublicService_v2/sido_v2";
  assert.equal(lib.corsDecision(null, local, allowed), "none");
  assert.equal(lib.corsDecision("", deployed, allowed), "none");
  assert.equal(lib.corsDecision("http://localhost:8080", local, allowed), "allow");
  assert.equal(lib.corsDecision("http://localhost:8080", "http://localhost:8788/img/x", allowed), "allow");
  assert.equal(lib.corsDecision("http://localhost:8080", "http://[::1]:8788/api/x", allowed), "allow");
  assert.equal(lib.corsDecision("https://evil.example", local, allowed), "deny");
  assert.equal(lib.corsDecision("http://localhost:8080", deployed, allowed), "deny");
  assert.equal(lib.corsDecision("https://animalshelter.example", deployed, allowed), "allow");
  assert.equal(lib.corsDecision("http://animalshelter.example", deployed, allowed), "deny");
  assert.equal(lib.isLoopbackHost("127.0.0.1"), true);
  assert.equal(lib.isLoopbackHost("localhost"), true);
  assert.equal(lib.isLoopbackHost("[::1]"), true);
  assert.equal(lib.isLoopbackHost("127.0.0.1.example"), false);
});

test("headHeaders: /api는 GET 헤더 + BYPASS, /a는 html 1시간", () => {
  assert.deepEqual(lib.headHeaders("api"), {
    "Content-Type": "application/json; charset=utf-8",
    "Cache-Control": "no-store",
    "X-Relay-Cache": "BYPASS",
  });
  assert.deepEqual(lib.headHeaders("share"), {
    "Content-Type": "text/html; charset=utf-8",
    "Cache-Control": "public, max-age=3600",
  });
});

test("assetlinks.json: 이 앱 패키지와 SHA-256 지문 하나(디버그 — 릴리즈는 Phase 5)", () => {
  const links = JSON.parse(readFileSync(new URL("../assetlinks.json", import.meta.url), "utf8"));
  assert.equal(links.length, 1);
  assert.deepEqual(links[0].relation, ["delegate_permission/common.handle_all_urls"]);
  assert.equal(links[0].target.namespace, "android_app");
  assert.equal(links[0].target.package_name, "com.cherryzp.animalshelter");
  assert.equal(links[0].target.sha256_cert_fingerprints.length, 1);
  assert.match(links[0].target.sha256_cert_fingerprints[0], /^([0-9A-F]{2}:){31}[0-9A-F]{2}$/);
});

test("isAllowedOrigin은 목록에 있는 것만", () => {
  const allowed = "http://localhost:8080, http://127.0.0.1:8080";
  assert.equal(lib.isAllowedOrigin("http://localhost:8080", allowed), true);
  assert.equal(lib.isAllowedOrigin("http://127.0.0.1:8080", allowed), true);
  assert.equal(lib.isAllowedOrigin("https://evil.example", allowed), false);
  assert.equal(lib.isAllowedOrigin("http://localhost:8080", undefined), false);
});

test("summarize: photo는 popfile1~8 중 비지 않은 첫 값, 다 비면 null(5a-live M3)", () => {
  const item = { ...items[0], popfile1: "  ", popfile2: " https://openapi.animal.go.kr/openapi/service/rest/fileDownloadSrvc/files/shelter/2026/11/b.jpg " };
  delete item.popfile3;
  const summary = lib.summarize(item);
  assert.equal(summary.photo, "https://openapi.animal.go.kr/openapi/service/rest/fileDownloadSrvc/files/shelter/2026/11/b.jpg");
  assert.equal("careOwnerNm" in summary, false);
  const none = lib.summarize({ desertionNo: "1", popfile1: "", popfile8: " " });
  assert.equal(none.photo, null);
});

test("shareMeta: popfile1이 비면 popfile2로 og:image(5a-live M3)", () => {
  const item = { ...items[0], popfile1: "", popfile2: "https://openapi.animal.go.kr/openapi/service/rest/fileDownloadSrvc/files/shelter/2026/11/b.jpg" };
  const meta = lib.shareMeta(lib.summarize(item), {
    origin: "https://relay.example",
    desertionNo: "450650202602738",
    todayIso: "2026-11-03",
  });
  assert.equal(meta.image, "https://relay.example/img/shelter/2026/11/b.jpg");
});

test("injectOg: twitter:card는 사진이 있으면 summary_large_image, 없으면 summary(5a-live M12)", () => {
  const html = "<html><head><meta charset=\"utf-8\"></head><body></body></html>";
  const base = { title: "t", description: "d", url: "u" };
  const withPhoto = lib.injectOg(html, { ...base, image: "https://relay.example/img/a.jpg" }, "450650202602738");
  assert.ok(withPhoto.includes(`<meta name="twitter:card" content="summary_large_image">`));
  const noPhoto = lib.injectOg(html, { ...base, image: null }, "450650202602738");
  assert.ok(noPhoto.includes(`<meta name="twitter:card" content="summary">`));
  assert.ok(!noPhoto.includes("summary_large_image"));
  const missing = lib.shareMeta({ found: false }, { origin: "o", desertionNo: "1", todayIso: "2026-11-03" });
  assert.ok(lib.injectOg(html, missing, "1").includes(`<meta name="twitter:card" content="summary">`));
  const noPhotoSummary = lib.shareMeta(lib.summarize({ ...items[0], popfile1: "", popfile2: "" }), {
    origin: "o", desertionNo: "450650202602738", todayIso: "2026-11-03",
  });
  assert.equal(noPhotoSummary.image, null);
});

test("weightText 범위형은 앞~뒤, 같으면 하나, 역순·무효는 null(5a-live M2, 앱 L1과 같은 규칙)", () => {
  assert.equal(lib.weightText("2~2.5(Kg)"), "2~2.5kg");
  assert.equal(lib.weightText("2 ~ 2.5(Kg)"), "2~2.5kg");
  assert.equal(lib.weightText("2~2(Kg)"), "2kg");
  assert.equal(lib.weightText("2.5~2(Kg)"), null);
  assert.equal(lib.weightText("0~1(Kg)"), null);
  assert.equal(lib.weightText("300~450(Kg)"), null);
  assert.equal(lib.weightText("0(Kg)"), null);
});

test("R2 parseImagePath는 작은 사진 _s 이름도 받는다(규칙 그대로, 5c)", () => {
  assert.deepEqual(lib.parseImagePath("shelter/2026/10/202611041011648%5B1%5D_s.jpg"), {
    path: "shelter/2026/10/202611041011648[1]_s.jpg",
    contentType: "image/jpeg",
  });
  assert.deepEqual(lib.parseImagePath("shelter/2026/10/202611041511187_s.jpg"), {
    path: "shelter/2026/10/202611041511187_s.jpg",
    contentType: "image/jpeg",
  });
});

test("isEndedByDeath: 앱과 같은 경계값 표(5d RL1)", () => {
  const hidden = ["종료(자연사)", "종료(안락사)", " 종료(자연사) ", "종료( 안락사 )", "종료(자연사"];
  const shown = [
    "종료(반환)", "종료(입양)", "종료(기증)", "종료(방사)", "종료", "종료()", "종료( )",
    "종료(폐사)", "종료(사망)", "자연사", "보호중", "", "  ", null, undefined,
  ];
  for (const value of hidden) assert.equal(lib.isEndedByDeath(value), true, `숨겨야 한다: [${value}]`);
  for (const value of shown) assert.equal(lib.isEndedByDeath(value), false, `보여야 한다: [${value}]`);
  // 배지(statusText)와 같은 해석 — 숨기는 값은 "종료 · 자연사/안락사"로 읽는 값이다
  for (const value of hidden) {
    assert.match(lib.statusText({ processState: value }, "2026-11-07"), /^종료 · (자연사|안락사)$/);
  }
});

const GENERIC_DESCRIPTION = lib.shareMeta(null, { origin: "o", desertionNo: "1", todayIso: "x" }).description;
const HIDDEN_TITLE = "이 공고는 더 이상 볼 수 없어요";
const shareArgs = { origin: "https://relay.example", desertionNo: "450650202602738", todayIso: "2026-11-07" };

test("shareMeta: 사망 종료는 사진·동물 설명 없이 숨김 제목·일반 설명(5d RL2)", () => {
  for (const processState of ["종료(자연사)", "종료(안락사)"]) {
    const meta = lib.shareMeta(lib.summarize({ ...items[0], processState }), shareArgs);
    assert.equal(meta.title, HIDDEN_TITLE, processState);
    assert.equal(meta.image, null, processState);
    assert.equal(meta.description, GENERIC_DESCRIPTION, processState);
    assert.equal(meta.url, "https://relay.example/a/450650202602738");
  }
});

test("injectOg: 사망 종료 메타는 og:image 없이 summary 카드, title 그 글, replaceState 그대로(5d RL3)", () => {
  const html = `<html><head><meta charset="utf-8"><title>애니멀 쉘터</title></head><body></body></html>`;
  const meta = lib.shareMeta(lib.summarize({ ...items[0], processState: "종료(자연사)" }), shareArgs);
  const out = lib.injectOg(html, meta, "450650202602738");
  assert.ok(!out.includes(`property="og:image"`));
  assert.ok(out.includes(`<meta name="twitter:card" content="summary">`));
  assert.ok(out.includes(`<title>${HIDDEN_TITLE}</title>`));
  assert.ok(out.includes(`<meta property="og:title" content="${HIDDEN_TITLE}">`));
  assert.ok(out.includes(`history.replaceState(null, "", "/#/animal/450650202602738")`));
});

test("shareMeta: 종료(입양)·보호중은 지금 그대로 동물 제목·사진(5d RL4)", () => {
  for (const processState of ["종료(입양)", "보호중"]) {
    const summary = lib.summarize({ ...items[0], processState });
    const meta = lib.shareMeta(summary, shareArgs);
    assert.equal(meta.title, lib.ogTitle(summary), processState);
    assert.notEqual(meta.title, HIDDEN_TITLE);
    assert.ok(meta.image?.startsWith("https://relay.example/img/"), processState);
  }
});
