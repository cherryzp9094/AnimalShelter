// 중계 Worker의 순수 함수(네트워크·env 없음). 규약: docs/design/02-data.md 7절, 05-service.md 4.1·5.1.
// worker.js가 I/O를 맡고, 여기 함수는 test/lib.test.js가 node --test로 확인한다.

export const UPSTREAM_BASE = "https://apis.data.go.kr/1543061";
export const FILES_BASE = "https://openapi.animal.go.kr/openapi/service/rest/fileDownloadSrvc/files/";
export const MAX_ROWS = 1000;
export const MAX_VALUE_LENGTH = 50;
export const SITE_NAME = "애니멀 쉘터";

const PAGING = ["pageNo", "numOfRows"];

/** 서비스 → 오퍼레이션 → 허용 파라미터(02-data 1.1 + pageNo·numOfRows). */
export const ROUTES = {
  abandonmentPublicService_v2: {
    abandonmentPublic_v2: [
      "bgnde", "endde", "upkind", "kind", "upr_cd", "org_cd", "care_reg_no", "state",
      "neuter_yn", "bgupd", "enupd", "sex_cd", "rfid_cd", "desertion_no", "notice_no", ...PAGING,
    ],
    sido_v2: [...PAGING],
    sigungu_v2: ["upr_cd", ...PAGING],
    shelter_v2: ["upr_cd", "org_cd", ...PAGING],
    kind_v2: ["up_kind_cd", ...PAGING],
  },
  animalShelterSrvc_v2: {
    shelterInfo_v2: ["care_reg_no", "care_nm", "upr_cd", "org_cd", ...PAGING],
  },
};

/** 1일 캐시하는 오퍼레이션(초). 목록(abandonmentPublic_v2)은 캐시하지 않는다. */
export const CACHE_TTL_SECONDS = {
  sido_v2: 86400,
  sigungu_v2: 86400,
  shelter_v2: 86400,
  kind_v2: 86400,
  shelterInfo_v2: 86400,
};

/** 클라이언트가 보내도 버리는 파라미터(중계가 붙인다). */
const DROPPED = new Set(["serviceKey", "_type"]);

/** `/api/{service}/{operation}` → { service, operation } 또는 null. */
export function parseApiPath(pathname) {
  const match = /^\/api\/([A-Za-z0-9_]+)\/([A-Za-z0-9_]+)\/?$/.exec(pathname);
  if (!match) return null;
  const [, service, operation] = match;
  if (!Object.hasOwn(ROUTES, service) || !Object.hasOwn(ROUTES[service], operation)) return null;
  return { service, operation };
}

/**
 * 쿼리 검사. 허용 밖 이름·중복·50자 초과·잘못된 페이지 값이면 { error: "param" }.
 * 통과하면 { params: [[name, value], …] }(이름순, serviceKey·_type 제외).
 */
export function validateQuery(service, operation, searchParams) {
  const allowed = new Set(ROUTES[service][operation]);
  const seen = new Map();
  for (const [name, value] of searchParams) {
    if (DROPPED.has(name)) continue;
    if (!allowed.has(name) || seen.has(name) || value.length > MAX_VALUE_LENGTH) return { error: "param" };
    seen.set(name, value);
  }
  if (seen.has("numOfRows")) {
    const rows = seen.get("numOfRows");
    if (!/^\d+$/.test(rows) || Number(rows) < 1 || Number(rows) > MAX_ROWS) return { error: "param" };
  }
  if (seen.has("pageNo")) {
    const page = seen.get("pageNo");
    if (!/^\d+$/.test(page) || Number(page) < 1) return { error: "param" };
  }
  const params = [...seen.entries()].sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0));
  return { params };
}

/**
 * 업스트림 주소. 키는 이미 퍼센트 인코딩된 형태라 **문자열로 이어 붙인다**
 * (URLSearchParams·searchParams.set에 넣으면 `%`가 `%25`로 다시 인코딩된다).
 */
export function buildUpstreamUrl(service, operation, params, serviceKey) {
  const query = new URLSearchParams(params).toString();
  return `${UPSTREAM_BASE}/${service}/${operation}?${query ? `${query}&` : ""}_type=json&serviceKey=${serviceKey}`;
}

/** 캐시 키: 중계 자신의 주소(허용 파라미터만, 이름순, 키 없음). */
export function cacheKeyUrl(origin, service, operation, params) {
  const query = new URLSearchParams(params).toString();
  return `${origin}/api/${service}/${operation}${query ? `?${query}` : ""}`;
}

/** 저장 조건: 업스트림 200 + 본문 앞 1KB의 resultCode "00" + totalCount > 0. */
export function shouldCache(status, bodyText) {
  if (status !== 200) return false;
  if (!/"resultCode"\s*:\s*"00"/.test(bodyText.slice(0, 1024))) return false;
  const counts = [...bodyText.matchAll(/"totalCount"\s*:\s*"?(\d+)"?/g)];
  return counts.length > 0 && Number(counts[counts.length - 1][1]) > 0;
}

const IMAGE_PATH = /^shelter\/\d{4}\/\d{2}\/[A-Za-z0-9_.[\]-]+\.(jpe?g|png)$/i;

/** `/img/` 뒤 경로 → { path, contentType } 또는 null(퍼센트 디코딩 뒤 검사, `..` 거부). */
export function parseImagePath(rawPath) {
  let path;
  try {
    path = decodeURIComponent(rawPath);
  } catch {
    return null;
  }
  if (path.includes("..")) return null;
  const match = IMAGE_PATH.exec(path);
  if (!match) return null;
  const contentType = match[1].toLowerCase() === "png" ? "image/png" : "image/jpeg";
  return { path, contentType };
}

/** 사진 원본 주소. 대괄호는 `%5B`·`%5D`로. */
export function imageUpstreamUrl(path) {
  return FILES_BASE + path.replaceAll("[", "%5B").replaceAll("]", "%5D");
}

/**
 * 사진 변환 크기(5c — 브리핑 1.2 (b)). 토큰은 둘뿐이다 — 사진 한 장의 고유 변환이 많아야 둘(무료 5,000/달, 1.3).
 * `sq480`: 카드(표시 폭 ≤ 680px). 정사각 480 자르기 — `crop`은 키우지 않는다. gravity `y: 0.3`은 앱의 세로 사진 치우침
 * `BiasAlignment(0f, -0.4f)`와 같다(넘치는 몫의 30%를 위에서 자른다). `mode`는 workerd `ImageTransform.gravity`가 요구한다.
 * `fit1080`: 상세·넓은 카드. 1080 틀 안으로 줄이기만 한다.
 */
export const IMAGE_SIZES = {
  sq480: { width: 480, height: 480, fit: "crop", gravity: { x: 0.5, y: 0.3, mode: "remainder" } },
  fit1080: { width: 1080, height: 1080, fit: "scale-down" },
};

/** 변환 출력(두 토큰 공통). 웹 앱은 Skia로 디코딩한다 — WebP 됨, AVIF 안 됨(브리핑 1.1). */
export const IMAGE_OUTPUT = { format: "image/webp", quality: 75 };

/** 인코딩 판. 품질·형식·gravity를 바꾸면 올린다(Cache API는 배포를 넘어 남는다). */
export const IMAGE_CACHE_VERSION = "v1";

/** 바인딩 입력 한도(20MB — 보수적으로 10진). 넘으면 변환하지 않고 원본. */
export const IMAGE_INPUT_MAX_BYTES = 20_000_000;

/** 무료 할당량(9422) 뒤 그 isolate가 변환을 쉬는 시간. */
export const IMAGE_QUOTA_PAUSE_MS = 10 * 60 * 1000;

/** CORS로 브라우저에 보여 줄 진단 헤더(`/api`·`/img`). */
export const EXPOSE_HEADERS = "X-Relay-Cache, X-Relay-Image, X-Relay-Upstream-Cache";

/** 작은 사진(`<이름>_s.jpg`) 경로인지 — 사진 서버의 비공식 규칙(survey 7절). 변환 토큰을 붙이지 않는다. */
export function isThumbnailPath(path) {
  return /_s\.jpe?g$/i.test(path);
}

/**
 * `/img` 쿼리의 `size`. 없으면 { token: null }(지금과 같다), 표의 값 하나면 { token },
 * 둘 이상·표에 없는 값(빈 값·대문자 포함)·작은 사진 경로에 붙은 것은 { error: "param" }. 다른 쿼리 이름은 무시한다.
 */
export function parseImageSize(searchParams, path) {
  const values = searchParams.getAll("size");
  if (values.length === 0) return { token: null };
  if (values.length > 1 || !Object.hasOwn(IMAGE_SIZES, values[0]) || isThumbnailPath(path)) return { error: "param" };
  return { token: values[0] };
}

/** 변형 저장 키(중계 자신의 주소 아래 `__img/v1/<토큰>/<경로>`, 대괄호는 `%5B`·`%5D`). */
export function imageCacheKeyUrl(origin, token, path) {
  return `${origin}/__img/${IMAGE_CACHE_VERSION}/${token}/${path.replaceAll("[", "%5B").replaceAll("]", "%5D")}`;
}

/** `fetch` 엔진의 `cf.image`(바인딩과 같은 값, gravity는 좌표만). */
export function cfImageOptions(token) {
  const { width, height, fit, gravity } = IMAGE_SIZES[token];
  const options = { width, height, fit };
  if (gravity) options.gravity = { x: gravity.x, y: gravity.y };
  return { ...options, format: "webp", quality: IMAGE_OUTPUT.quality };
}

/** `[vars] IMAGE_ENGINE`: 없거나 비면 binding, binding·fetch·off는 그대로, 모르는 값은 off. */
export function imageEngine(value) {
  if (value === undefined || value === null || value === "") return "binding";
  return ["binding", "fetch", "off"].includes(value) ? value : "off";
}

/** 바인딩 예외 → 이유: 9422(이달 무료 할당량) → quota, 숫자 코드 → error-<n>, 그 밖 → error. 메시지는 보지 않는다. */
export function transformFailureReason(error) {
  const code = Number(error?.code);
  if (!Number.isInteger(code) || code <= 0) return "error";
  return code === 9422 ? "quota" : `error-${code}`;
}

/** `cf-resized` 헤더의 `err=<n>` → 숫자, 없으면 null. */
export function cfResizedErrorCode(header) {
  const match = /err=(\d+)/.exec(header ?? "");
  return match ? Number(match[1]) : null;
}

/** `/a/{15자리}`(끝 `/` 허용) → 구조번호 또는 null. */
export function parseShareId(pathname) {
  const match = /^\/a\/(\d{15})\/?$/.exec(pathname);
  return match ? match[1] : null;
}

/**
 * `desertion_no` 재조회 창(02-data 6절): ① 날짜 없이 → ② 구조번호 7~10째 자리 연도 → ③ 그 전 해.
 * 연도가 2000~올해 밖이면 ②③ 생략. null = 날짜 없이, 아니면 [bgnde, endde].
 */
export function lookupWindows(desertionNo, thisYear) {
  const year = Number(desertionNo.slice(6, 10));
  const windows = [null];
  if (!Number.isInteger(year) || year < 2000 || year > thisYear) return windows;
  windows.push([`${year}0101`, `${year}1231`]);
  if (year - 1 >= 2000) windows.push([`${year - 1}0101`, `${year - 1}1231`]);
  return windows;
}

/** 서울 오늘(UTC+9 고정) "YYYY-MM-DD". */
export function kstToday(now) {
  return new Date(now.getTime() + 9 * 3600 * 1000).toISOString().slice(0, 10);
}

/** "YYYYMMDD" → "YYYY-MM-DD" 또는 null. */
export function basicToIso(text) {
  const match = /^(\d{4})(\d{2})(\d{2})$/.exec((text ?? "").trim());
  return match ? `${match[1]}-${match[2]}-${match[3]}` : null;
}

function daysBetween(fromIso, toIso) {
  return Math.round((Date.parse(`${toIso}T00:00:00Z`) - Date.parse(`${fromIso}T00:00:00Z`)) / 86400000);
}

/** 상태 판정(02-data 5.2, 앱과 같은 규칙). */
export function animalStatus(processState, noticeEdt, todayIso) {
  const state = (processState ?? "").trim();
  const end = basicToIso(noticeEdt);
  if (state === "보호중" && end && end >= todayIso) return { type: "noticing", daysLeft: daysBetween(todayIso, end) };
  if (state === "보호중") return { type: "protecting" };
  if (state === "종료") return { type: "ended", reason: null };
  if (state.startsWith("종료(")) {
    const reason = state.slice(3).replace(/\)$/, "").trim();
    return { type: "ended", reason: reason || null };
  }
  return { type: "unknown", raw: state || null };
}

/**
 * 응답 item에서 미리보기에 필요한 값만(1시간 캐시하는 요약). 대표자 이름은 넣지 않는다.
 * `photo`는 `popfile1`~`popfile8` 중 비지 않은 첫 값 — `popfile1`이 비고 `popfile2`만 있는 공고가 있다(5a-live M3).
 */
export function summarize(item) {
  const pick = (key) => {
    const value = item?.[key];
    return typeof value === "string" && value.trim() ? value.trim() : null;
  };
  return {
    found: true,
    desertionNo: pick("desertionNo"),
    kindNm: pick("kindNm"),
    sexCd: pick("sexCd"),
    age: pick("age"),
    orgNm: pick("orgNm"),
    careNm: pick("careNm"),
    noticeSdt: pick("noticeSdt"),
    noticeEdt: pick("noticeEdt"),
    processState: pick("processState"),
    colorCd: pick("colorCd"),
    weight: pick("weight"),
    specialMark: pick("specialMark"),
    photo: [1, 2, 3, 4, 5, 6, 7, 8].map((n) => pick(`popfile${n}`)).find(Boolean) ?? null,
  };
}

/** 응답 본문 → item 배열(`{}`·배열·객체 하나). */
export function itemsOf(json) {
  const item = json?.response?.body?.items?.item;
  if (Array.isArray(item)) return item;
  if (item && typeof item === "object") return [item];
  return [];
}

const SEX_LABEL = { M: "수컷", F: "암컷", Q: "성별 미상" };

/** 카드 제목 = 미리보기 제목(05-service 4.1): "한국 고양이 · 수컷 · 2024년생"(+ "· 생후 2개월 미만"). */
export function ogTitle(summary) {
  const parts = [];
  if (summary.kindNm) parts.push(summary.kindNm);
  if (summary.sexCd && SEX_LABEL[summary.sexCd]) parts.push(SEX_LABEL[summary.sexCd]);
  const year = /^(\d{4})/.exec(summary.age ?? "");
  if (year) parts.push(`${year[1]}년생`);
  if ((summary.age ?? "").includes("60일미만")) parts.push("생후 2개월 미만");
  return parts.length ? parts.join(" · ") : SITE_NAME;
}

/** 상태 문구: 공고 2026-11-03 ~ 11-13(D-10) / 오늘 마감 / 보호중 / 종료 · 사유. */
export function statusText(summary, todayIso) {
  const status = animalStatus(summary.processState, summary.noticeEdt, todayIso);
  if (status.type === "noticing") {
    const start = basicToIso(summary.noticeSdt);
    const end = basicToIso(summary.noticeEdt);
    const endText = start && start.slice(0, 4) === end.slice(0, 4) ? end.slice(5) : end;
    const day = status.daysLeft === 0 ? "오늘 마감" : `D-${status.daysLeft}`;
    return `공고 ${start ? `${start} ~ ` : "~ "}${endText}(${day})`;
  }
  if (status.type === "protecting") return "보호중";
  if (status.type === "ended") return status.reason ? `종료 · ${status.reason}` : "종료";
  return status.raw;
}

/**
 * 체중 원문 "5(Kg)" → "5kg", 범위형 "2~2.5(Kg)" → "2~2.5kg"(앱 `parseWeight`와 같은 규칙 — 02-data 5.1).
 * 숫자마다 0 초과 100 이하, 범위는 앞 < 뒤(같으면 하나), 역순·한쪽 무효·읽을 수 없으면 null.
 */
export function weightText(weight) {
  const match = /^([\d.,]+)\s*(?:~\s*([\d.,]+))?/.exec((weight ?? "").trim());
  if (!match) return null;
  const number = (text) => {
    const value = Number(text.replace(/[.,]+/g, ".").replace(/^\.|\.$/g, ""));
    return Number.isFinite(value) && value > 0 && value <= 100 ? value : null;
  };
  const first = number(match[1]);
  if (first === null) return null;
  if (match[2] === undefined) return `${first}kg`;
  const second = number(match[2]);
  if (second === null || second < first) return null;
  return second === first ? `${first}kg` : `${first}~${second}kg`;
}

/** 설명(05-service 5.1): 관할·보호소 · 상태 · 색 · 체중 · 특징 — 최신 정보는 보호소에 확인해 주세요. */
export function ogDescription(summary, todayIso) {
  const place = [summary.orgNm, summary.careNm].filter(Boolean).join(" ");
  const parts = [place, statusText(summary, todayIso), summary.colorCd, weightText(summary.weight), summary.specialMark]
    .filter(Boolean);
  return `${parts.join(" · ")} — 최신 정보는 보호소에 확인해 주세요`;
}

/** 미리보기 메타. summary가 null이면 일반 안내(업스트림 오류), found=false면 "찾을 수 없어요". */
export function shareMeta(summary, { origin, desertionNo, todayIso }) {
  const url = `${origin}/a/${desertionNo}`;
  if (!summary) {
    return { title: SITE_NAME, description: "보호소에 들어온 유기동물 공고를 확인하세요. 최신 정보는 보호소에 확인해 주세요", image: null, url };
  }
  if (!summary.found) {
    return { title: "공고를 찾을 수 없어요", description: "공고가 지났거나 주소가 잘못됐을 수 있어요.", image: null, url };
  }
  const photo = summary.photo ?? "";
  const filesAt = photo.indexOf("/fileDownloadSrvc/files/");
  const rest = filesAt >= 0 ? photo.slice(filesAt + "/fileDownloadSrvc/files/".length) : null;
  const image = rest && parseImagePath(rest)
    ? `${origin}/img/${rest.replaceAll("[", "%5B").replaceAll("]", "%5D")}`
    : null;
  return { title: ogTitle(summary), description: ogDescription(summary, todayIso), image, url };
}

/** HTML 이스케이프(& < > " '). API에서 온 값은 전부 이것을 거친다. */
export function escapeHtml(text) {
  return String(text)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

/**
 * index.html에 미리보기를 넣는다(D14, 4a R2): `<head>`의 `<meta charset>` 바로 뒤(없으면 맨 앞)에 `<base href="/">`와 OG 메타·`<title>`·
 * `<meta name="description">`(동물 설명)·`robots`. `twitter:card`는 사진이 있으면 `summary_large_image`, 없으면 `summary`
 * (5a-live M12 — 큰 이미지 카드에 이미지가 없으면 봇마다 빈 틀이 된다). index.html에 있던 `<title>`·`description`·`robots`는 뺀다 —
 * 같은 이름 메타가 둘이 되지 않게. `<body>` 첫 줄에 해시 라우팅으로 상세를 여는 `replaceState` 스크립트.
 */
export function injectOg(html, meta, desertionNo) {
  const e = escapeHtml;
  const tags = [
    `<base href="/">`,
    `<meta property="og:type" content="website">`,
    `<meta property="og:locale" content="ko_KR">`,
    `<meta property="og:title" content="${e(meta.title)}">`,
    `<meta property="og:description" content="${e(meta.description)}">`,
    meta.image ? `<meta property="og:image" content="${e(meta.image)}">` : null,
    `<meta property="og:url" content="${e(meta.url)}">`,
    `<meta property="og:site_name" content="${e(SITE_NAME)}">`,
    `<meta name="twitter:card" content="${meta.image ? "summary_large_image" : "summary"}">`,
    `<meta name="description" content="${e(meta.description)}">`,
    `<meta name="robots" content="noindex">`,
    `<title>${e(meta.title)}</title>`,
  ].filter(Boolean).join("\n");
  const id = /^\d{15}$/.test(desertionNo) ? desertionNo : "";
  const script = `<script>history.replaceState(null, "", "/#/animal/${id}")</script>`;
  return html
    .replace(/<title>[\s\S]*?<\/title>\s*/i, "")
    .replace(/<meta\s+name="(description|robots)"[^>]*>\s*/gi, "")
    .replace(/<head([^>]*)>(\s*<meta\s+charset=[^>]*>)?/i, (tag) => `${tag}\n${tags}`)
    .replace(/<body([^>]*)>/i, (tag) => `${tag}\n${script}`);
}

/** 허용 Origin인지(쉼표 구분 목록). */
export function isAllowedOrigin(origin, allowedCsv) {
  return (allowedCsv ?? "").split(",").map((s) => s.trim()).filter(Boolean).includes(origin);
}

const LOOPBACK_HOSTS = new Set(["127.0.0.1", "localhost", "[::1]"]);

/** Worker가 루프백 주소(로컬 `wrangler dev`)에서 도는지 — 요청 주소의 호스트로 본다. */
export function isLoopbackHost(hostname) {
  return LOOPBACK_HOSTS.has(hostname);
}

/**
 * `/api`·`/img`의 Origin 판정(4a R3): "none"(Origin 없음 — curl·Android·같은 origin GET, CORS 헤더 없이 허용) ·
 * "allow" · "deny"(403). 허용 목록(`ALLOWED_ORIGINS`, 로컬 개발 8080 → 8788)은 **Worker가 루프백에서 돌 때만** 쓴다.
 * 그 밖(배포)은 Worker 자신의 origin만 허용 — 배포에 localhost 허용이 남지 않고 설정을 바꿀 일이 없다.
 */
export function corsDecision(origin, requestUrl, allowedCsv) {
  if (!origin) return "none";
  const self = new URL(requestUrl);
  if (origin === self.origin) return "allow";
  if (isLoopbackHost(self.hostname) && isAllowedOrigin(origin, allowedCsv)) return "allow";
  return "deny";
}

const JSON_CONTENT_TYPE = "application/json; charset=utf-8";

/**
 * HEAD 응답 헤더(4a R1 — 업스트림을 부르지 않는다). 경로·파라미터 검사는 GET과 같게 한 뒤 맞으면 이 헤더로 200, 본문 없음.
 * `/api`는 GET과 같은 `Content-Type`·`Cache-Control: no-store`에 `X-Relay-Cache: BYPASS`, `/a`는 `text/html`·1시간.
 */
export function headHeaders(kind) {
  if (kind === "api") {
    return { "Content-Type": JSON_CONTENT_TYPE, "Cache-Control": "no-store", "X-Relay-Cache": "BYPASS" };
  }
  return { "Content-Type": "text/html; charset=utf-8", "Cache-Control": "public, max-age=3600" };
}

/** 모든 Worker 응답에 붙이는 헤더(4a H5 — 형식 추측 막기). 정적 자산은 웹 셸의 `_headers`가 같은 값을 붙인다. */
export const SECURITY_HEADERS = { "X-Content-Type-Options": "nosniff" };
