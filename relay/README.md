# AnimalShelter 중계 Worker (`relay/`)

Cloudflare Worker 하나가 웹 앱(정적 자산)과 함께 다음을 맡는다. 규약은 `docs/design/02-data.md` 7절.

- `/api/{service}/{operation}` — 공공데이터 API 중계. 키(`SERVICE_KEY`)는 중계만 갖고, 웹 번들에는 들어가지 않는다.
- `/img/{path}` — 보호소 사진 중계(원본 서버에 CORS가 없고 `Content-Type`이 `application/octet-stream`이다). `?size=sq480|fit1080`이면 Cloudflare Images로 줄인 WebP(5c).
- `/a/{desertionNo}` — 공유 링크 미리보기(OG 태그를 넣은 `index.html`). 사람 브라우저는 그대로 앱이 열린다.
- `/.well-known/assetlinks.json` — Android App Links 검증 파일(`assetlinks.json`). 지금은 **디버그** 서명 SHA-256 하나(4a), 릴리즈 지문은 Phase 5.

## 파일

| 파일 | 내용 |
|---|---|
| `worker.js` | fetch 핸들러: 라우팅·업스트림 호출·캐시·CORS(I/O) |
| `image.js` | `/img` 흐름(5c): 변환 토큰·Images 바인딩/`cf.image`/off·원본 되돌아가기·변형 캐시·진단 헤더. I/O(`fetch`·`cache`·상태·시계·제한 시간)는 `worker.js`가 넘긴다 |
| `lib.js` | 순수 함수: 경로·파라미터 검사, 업스트림 주소, 사진 경로·변환 토큰·캐시 키·엔진·오류 이유, 재조회 창, KST 날짜, 상태 판정, OG 문구, HTML 이스케이프·주입, Origin 판정 |
| `test/lib.test.js` | `node --test`(네트워크 없음, 입력은 인라인 또는 `../docs/api/samples`) |
| `test/image.test.js` | `/img` 흐름(5c) — 가짜 `fetch`·`cache`·`env.IMAGES`(네트워크 없음) |
| `wrangler.toml` | 이름 `animalshelter`·`workers_dev = true`(4b — 주소 `https://animalshelter.mulddae.workers.dev`)·정적 자산·`run_worker_first`·observability 끔·`preview_urls = false`·로컬 포트 8788. `[vars] IMAGE_ENGINE`(5c — `binding`·`fetch`·`off`)·`[images] binding = "IMAGES"`(5c). `account_id`·`routes`는 없다(이 맥의 wrangler 로그인 계정) |
| `assetlinks.json` | App Links 검증 파일(패키지 `com.cherryzp.animalshelter`, 디버그 SHA-256 하나) |
| `package.json`, `package-lock.json` | `wrangler` 4.156.2 고정(올릴 때는 설계 담당 승인) |
| `.dev.vars.example` | 로컬 비밀값 자리(값 없음). 실제 `.dev.vars`는 git 제외 |

## 로컬 실행

```sh
# 저장소 루트에서
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :webApp:wasmJsBrowserDistribution && ./gradlew --stop   # [assets] 폴더가 있어야 뜬다

# .dev.vars: 값을 화면에 내지 않고 local.properties에서 옮긴다(권한 600)
( umask 077 && sed -n 's/^SERVICE_KEY=/SERVICE_KEY=/p' local.properties > relay/.dev.vars )
grep -c '^SERVICE_KEY=.' relay/.dev.vars    # 1 (값은 보지 않는다)

cd relay && npm ci && npm test
npx wrangler dev        # http://127.0.0.1:8788 (에뮬레이터에서는 http://10.0.2.2:8788)
npx wrangler dev --var IMAGE_ENGINE:fetch   # 사진 변환 엔진을 바꿔 띄울 때(fetch·off)
```

- 사진 변환(5c): 시작 출력의 바인딩 표에 `env.IMAGES … local`이 있다(로그인 없음). 로컬 Images는 sharp로 만든 **오프라인 모조**다 — 바인딩 갈래는 quality를 쓰지 않고(WebP 기본 q80 — 배포보다 크다) 좌표 gravity를 무시한다(세로 사진도 가운데로 자른다). 그림이 아닌 입력의 오류 코드는 9523(로컬). `fetch` 엔진(`cf.image`)도 로컬 모조가 받는다(quality 반영, `cf-resized: internal=ok/m`). 실제 결과(q75·위 30% 자르기)는 배포에서 본다. 로컬 변환은 고유 변환 수에 들지 않는다.

- 8787은 이 맥의 다른 프로젝트 서버가 쓴다 — 그대로 둔다.
- 로그인 없이 뜬다. `login`·`whoami`·`deploy`·`secret`·`--remote` 같은 계정 명령은 쓰지 않는다(배포는 아래 절 — 사용자 앞에서만).
- `/cdn-cgi/local/explorer`(관측 API)와 `relay/.wrangler/`에는 업스트림 URL(=키)이 남을 수 있으니 열어 보지 않는다.
- 끝낼 때: `wrangler dev`를 띄운 그 작업을 멈춘다 — 터미널 앞에서 띄웠으면 그 창에서 `Ctrl+C`, 에이전트가 백그라운드 작업으로 띄웠으면 그 작업을 멈춘다. **프로세스 이름 패턴으로 kill하지 않는다**(`pkill`·`pgrep -f …` 금지 — 이 맥의 8787 python 서버 같은 다른 프로세스를 건드릴 수 있다). 멈춘 뒤 `lsof -nP -iTCP:8788 -sTCP:LISTEN`이 비었는지 확인한다(남아 있으면 PID를 확인해 사람에게 알린다). 지울 때: `rm relay/.dev.vars`.
- 에뮬레이터(Android 17)에서 `http://10.0.2.2:8788`을 쓰려면 앱을 설치할 때마다 `adb shell pm grant com.cherryzp.animalshelter android.permission.ACCESS_LOCAL_NETWORK`(debug 빌드만 이 권한을 선언한다).

## 경로 규약

| 요청 | 동작 |
|---|---|
| `GET /api/{service}/{operation}?…` | `service` 2개·`operation` 6개만(그 밖 404). 오퍼레이션별 허용 파라미터만(모르는 이름·중복·50자 초과 → 400 `{"relayError":"param"}`), `numOfRows` 1~1000. 클라이언트의 `serviceKey`·`_type`은 버리고 중계가 `_type=json&serviceKey=…`를 **문자열로** 붙인다(키는 이미 퍼센트 인코딩형). 타임아웃 20초. 응답은 업스트림 상태·본문 그대로, `Content-Type: application/json; charset=utf-8`, `Cache-Control: no-store` |
| `GET /img/shelter/YYYY/MM/<파일>.(jpg\|jpeg\|png)` | 퍼센트 디코딩 뒤 형식 검사(`..` 거부, 대괄호 허용 — 작은 사진 `<이름>_s.jpg`도 같은 규칙). `image/jpeg`·`image/png`(스트림), `public, max-age=86400`, Cloudflare 캐시 1일. 원본 404 → 404 본문 없음·`public, max-age=3600`(없는 `_s`를 브라우저가 거듭 부르지 않게) |
| `GET /img/…?size=sq480\|fit1080` | **사진 변환(5c)**. `sq480` = 정사각 480 자르기(gravity 위 30% — 앱의 세로 사진 치우침과 같다, 키우지 않음, 카드), `fit1080` = 1080 틀 안(줄이기만, 상세·넓은 카드). 출력 WebP q75. `size`가 둘 이상·표에 없는 값·`_s` 경로에 붙은 것 → 400 `{"relayError":"param"}`. 엔진은 `IMAGE_ENGINE`: `binding`(기본 — `env.IMAGES`)·`fetch`(`cf.image`)·`off`(변환 없음), 모르는 값은 `off`. 흐름: 변형 캐시(엔진보다 먼저 — `off`여도 저장된 변형은 낸다) → 원본 받기(지금처럼) → 변환(10초) → 결과가 원본보다 작으면 WebP, 크거나 같으면 원본(`larger` — 정해진 결과라 저장). **어떤 실패든 원본**: `X-Relay-Image: original; reason=<이유>` — `off`·`no-binding`(바인딩 없음)·`quota`(9422 이달 무료 할당량 — 그 isolate는 10분 쉰다)·`error-<코드>`·`error`(코드 없음)·`timeout`·`too-big`(> 20MB)·`not-resized`(`fetch` 200인데 WebP 아님)·`error-<상태>`(`fetch` 4xx·5xx)·`head`(HEAD는 변환하지 않는다). 원본 404·그 밖 실패는 토큰 없을 때와 같다(404·502) |
| `GET /a/{15자리}` | 재조회 창(날짜 없이 → 구조번호의 연도 → 그 전 해, 최대 3회)으로 찾아 요약 JSON을 1시간 캐시(0건도 캐시, 업스트림 오류는 캐시하지 않고 일반 OG). `index.html`(`/`로 읽는다 — `/index.html`은 307)의 `<meta charset>` 바로 뒤에(처음 1024바이트 안에 남게) `<base href="/">`·`og:type`(website)·`og:locale`(ko_KR)·`og:title`·`og:description`·`og:image`(첫 사진 — `popfile1`~`popfile8` 중 비지 않은 첫 값, 있을 때)·`og:url`·`og:site_name`·`twitter:card`(사진이 있으면 `summary_large_image`, 없으면 `summary`)·`<meta name="description">`(동물 설명)·`robots`(noindex)·`<title>`을 넣는다 — `index.html`의 `<title>`·`description`·`robots`는 빼서 같은 이름이 하나씩만 남는다. `<body>` 첫 줄에 `history.replaceState(null, "", "/#/animal/{id}")`. `text/html`, `public, max-age=3600` |
| `GET /.well-known/assetlinks.json` | `assetlinks.json` 그대로(`application/json`, 리다이렉트 없음). 그 밖 `/.well-known/*`는 404 |
| 그 밖 | 정적 자산(SPA: 없는 경로도 `index.html`) |

- 메서드: GET·HEAD만(그 밖 405). `/api`·`/img`의 `OPTIONS`는 사전 요청.
- **HEAD**(4a): `/api`·`/a`의 HEAD는 업스트림을 부르지 않는다(캐시도 보지 않는다). 경로·파라미터 검사는 GET과 같아 잘못되면 같은 404/400, 맞으면 200 + GET의 헤더만(`/api`: `application/json; charset=utf-8`·`no-store`·`X-Relay-Cache: BYPASS`, `/a`: `text/html; charset=utf-8`·`public, max-age=3600`), 본문 없음. `/img`의 HEAD는 GET처럼 사진 서버를 부른다(한도 밖, 미리보기 봇이 형식을 본다) — `?size=`가 있어도 변환하지 않는다(`X-Relay-Image: original; reason=head`, 5c). 정적 자산의 HEAD는 Static Assets가 처리한다.
- CORS(`/api`·`/img`만, 판정은 `lib.corsDecision`): Origin 없는 요청(curl·Android·같은 origin GET)은 CORS 헤더 없이 허용. Worker 자신의 origin이면 허용. `ALLOWED_ORIGINS`(로컬 개발 8080)는 **Worker가 루프백(`127.0.0.1`·`localhost`·`[::1]`)에서 돌 때만** 쓴다 — 배포에서는 그 목록이 남아 있어도 `localhost`가 열리지 않는다. 허용이면 `Access-Control-Allow-Origin`·`Vary: Origin`·`Access-Control-Expose-Headers: X-Relay-Cache, X-Relay-Image, X-Relay-Upstream-Cache`, 사전 요청은 204. 그 밖 Origin은 403.
- 업스트림 실패·타임아웃: 502 `{"relayError":"upstream"}`.
- `/img` 진단 헤더(5c): `X-Relay-Cache`(`HIT` 저장된 변형 · `MISS` 토큰 있음 · `BYPASS` 토큰 없음·HEAD), `X-Relay-Image`(`sq480; engine=binding`·`fit1080; engine=fetch`·`original`·`original; reason=…`), `X-Relay-Upstream-Cache`(원본 응답의 `cf-cache-status`, 없거나 HIT 변형이면 `-`). CORS `Access-Control-Expose-Headers`에 셋 다(`lib.EXPOSE_HEADERS`). 오류 메시지·URL은 헤더·로그에 쓰지 않는다(코드 숫자만).
- 공유 미리보기 `og:image`는 원본 `/img/<경로>` 그대로다(봇의 WebP 지원이 제각각, 공유마다 변환 수를 쓰지 않는다).

## 캐시

- `caches.default`. 확인용 헤더 `X-Relay-Cache: HIT | MISS | BYPASS`. `workers.dev`에서도 된다(2026-11-05 배포 뒤 `sido_v2` 두 번이 MISS → HIT). 캐시는 **데이터센터마다 따로**다 — 같은 요청도 다른 데이터센터(이 맥은 HKG·NRT·KIX — 응답의 `cf-ray` 끝)에서는 MISS.
- 코드표 4종(`sido_v2`·`sigungu_v2`·`shelter_v2`·`kind_v2`)·`shelterInfo_v2`: 1일. 키는 중계 자신의 주소(허용 파라미터만 이름순, 키 없음). 저장 조건: 업스트림 200 + 본문 앞 1KB의 `"resultCode":"00"` + `totalCount` > 0(오류·0건을 하루 동안 돌려주지 않게).
- 목록(`abandonmentPublic_v2`): 캐시하지 않는다(`BYPASS`, 본문을 그대로 흘려보낸다).
- `/a` 조회 결과: 1시간. 사진: 1일(Cloudflare 캐시).
- 사진 변형(5c): `caches.default`에 30일(`public, max-age=2592000`), 키 `<origin>/__img/v1/<토큰>/<경로>`(대괄호 `%5B`·`%5D`). 저장하는 것은 WebP 결과와 `larger`(원본 바이트)뿐 — 일시적인 되돌아가기(quota·error·timeout·off·no-binding·not-resized)는 저장하지 않는다. 브라우저에는 변형·`larger` 1일(`max-age=86400`), 되돌아가기 1시간(`max-age=3600`). `v1`은 인코딩 판 — 품질·형식·gravity를 바꾸거나 잘못된 변형을 거두려면 `v2`로 올린다(Cache API는 배포를 넘어 남는다).

## 한도

Workers 무료: 하루 100,000 요청(정적 자산 요청은 제외), 요청당 CPU 10ms, 서브리퀘스트 50. 업스트림 개발계정: 서비스마다 하루 10,000건. 계산은 `docs/design/05-service.md` 5.3.

Cloudflare Images(5c): 무료 **5,000 고유 변환/달**(원본 사진 × 변환 값의 서로 다른 조합을 달마다 한 번 — format·quality는 셈 밖). 넘으면 캐시된 변형은 그대로, 새 변환은 9422(과금 없음) → 중계는 원본으로 되돌아간다. 바인딩 입력 20MB. 이 규약에서 사진 한 장은 많아야 둘(`sq480`·`fit1080`) — 예상 월 2,000~4,300(브리핑 1.3). 남용(사진 경로를 모아 두 토큰으로 부르기)이 보이면 `IMAGE_ENGINE = "off"`로 다시 배포하거나 대시보드 Rate Limiting.

## 정적 자산(웹 셸, `webApp/src/wasmJsMain/resources/`)

- `robots.txt`: `User-agent: *` / `Disallow: /api/` — `/a/`·`/img/`는 미리보기 봇이 읽어야 해서 막지 않는다. `index.html`에도 `<meta name="robots" content="noindex">`(개인용 — 색인 봇이 중계 한도를 쓰지 않게).
- `.assetsignore`: `*.map` — 소스맵은 올리지 않는다(요청하면 SPA `index.html` — 4b Q6, 2026-11-05). 올리려면 그 줄만 지운다. wrangler는 `.assetsignore`·`_headers`·`_redirects` 자체도 자산으로 올리지 않는다(`_headers`는 헤더 규칙으로 읽는다).
- `_headers`: 정적 자산 응답에 `X-Content-Type-Options: nosniff`(Worker가 내는 응답은 `worker.js`가 같은 값을 붙인다 — `lib.SECURITY_HEADERS`).
- 셋 다 배포 폴더(`webApp/build/dist/wasmJs/productionExecutable/`) 맨 위에 생긴다.

## 배포

**배포됨 — `https://animalshelter.mulddae.workers.dev`**(2026-11-05, Worker `animalshelter`, 첫 버전 `d8180b4a-7277-424d-9486-a85f0d5be661`, 자산 26개, secret `SERVICE_KEY` 하나 — 지금 버전은 아래 표의 마지막 줄). 배포 뒤 확인 D1~D11은 `docs/design/reports/phase4b.md`.

| 때 | 버전 ID | 무엇 | 웹 번들(앱 wasm · Skiko wasm) |
|---|---|---|---|
| 2026-11-05 첫 배포 | `d8180b4a-7277-424d-9486-a85f0d5be661` | 4b — 주소·secret·자산 26개 | 코드 `ef933cf` |
| 2026-11-05 다시 배포 | `82f8b88c-f7b5-417f-99e1-4af467083d9a` | 4b-fixes B1(넓은 창에서도 Esc·시스템 뒤로 = 맨 위 칸 닫기), 메인 세션이 사용자 승인 범위 안에서 같은 명령으로. Worker 코드·secret 그대로 | 코드 `627126b` — `8369ce85609f0388a2ee.wasm` · `658820f72b4617efb380.wasm`(그대로), 자산 26개 |
| 2026-11-05 다시 배포 | `040481c4-a8d6-4436-b13e-7c61765a812d` | 5b-prep 웹 고지(Emscripten·musl 원문, Kotlin·kotlinx 저작권자), 메인 세션이 같은 명령으로. Worker 코드·secret 그대로. 확인은 `docs/design/reports/phase5.md` 1.4 | 코드 `4a67af4` — `0418fc47762f9a61ad57.wasm` · `658820f72b4617efb380.wasm`(그대로), 자산 26개 |

- 다시 배포한 뒤 확인(계정 명령 없이): `curl -sI https://animalshelter.mulddae.workers.dev/`가 200, 배포된 `animalshelter.js`가 가리키는 wasm 이름 = 로컬 dist, 바뀐 화면은 Playwright(`/api`는 표본 — 실호출 0). B1은 `docs/design/reports/phase4b.md` 10.2.

- 계정 명령(`whoami`·`secret`·`deploy`·`deployments`·`rollback`·`delete`·`tail`)은 메인 세션·설계 담당이 **사용자 앞에서만** 한다(`docs/design/briefs/phase4b-deploy.md` 3절). 이 맥의 wrangler 로그인을 쓴다(`account_id` 없음).
- 인증키: 값을 화면에 내지 않고 `local.properties`에서 파이프로 — `sed -n 's/^SERVICE_KEY=//p' ../local.properties | npx wrangler secret put SERVICE_KEY`(stdin이 터미널이 아니면 끝 공백·줄바꿈을 지우고 읽는다). 확인은 `npx wrangler secret list` — 이름만. `relay/.dev.vars`는 로컬 `wrangler dev`만 읽고 배포에는 올라가지 않는다.
- 다시 배포: 저장소 루트에서 `./gradlew :webApp:wasmJsBrowserDistribution -PrelayBaseUrl=` → `./gradlew --stop`(기본 번들은 `localhost:8788`을 부른다 — 올리기 전 `grep -rlF localhost:8788 ../webApp/build/dist/wasmJs/productionExecutable | wc -l`이 0) → `cd relay && env -u CLAUDECODE npx wrangler deploy`(`env -u CLAUDECODE`: 에이전트 환경이면 wrangler가 `workers.dev` 하위 도메인을 묻지 않고 등록하는 갈래가 있다 — 4b 2절 결과 7). 출력의 버전 ID를 적는다.
- 사진 변환 갈래(5c — 브리핑 6절): 배포 출력의 바인딩 표에 `env.IMAGES`(Images)·`env.IMAGE_ENGINE`("binding")이 있어야 한다.
  - 갈래 B — 바인딩이 `workers.dev`에서 안 되면(`reason=no-binding`이거나 `sq480`·`fit1080`이 같은 `error-<n>`, 또는 배포가 `[images]` 때문에 실패): `wrangler.toml`의 `IMAGE_ENGINE = "fetch"`(배포가 실패했으면 `[images]` 두 줄도 주석) → 다시 배포. 기대는 `engine=fetch`.
  - 갈래 C — `fetch`도 `not-resized`·`error`면 `IMAGE_ENGINE = "off"` → 다시 배포. 카드·상세는 `_s` 자리표시 뒤에 원본이다.
- 되돌리기: `npx wrangler deployments list` → `npx wrangler rollback [버전 ID]`. 지우기(`npx wrangler delete`)는 보낸 공유 링크·App Links가 모두 죽는다.
- 주소를 바꾸지 않는다: 공유 링크 `/a/…`·App Links(`gradle.properties` `shareHost`)·`assetlinks.json`이 이 호스트에 묶여 있다.
- 배포에서 본 것(로컬과 다른 점): 정적 HTML의 Content-Type은 `text/html`(charset 없음 — `<meta charset>`가 44바이트째라 괜찮다), Compose 문구 `.cvr`은 Content-Type 없음, 압축은 요청에 따라 zstd·br·gzip, 정적 자산은 `public, max-age=0, must-revalidate` + ETag(재방문 304).

## 보안 주의

- 업스트림 주소에는 키가 있다. `console.log`에 URL·`env`·업스트림 응답을 쓰지 않는다. 예외 메시지도 버린다.
- `[observability] enabled = false` — 추적에 업스트림 URL이 남지 않게.
- 중계는 인증이 없다. Origin 허용 목록은 브라우저에만 통한다 — 남용이 보이면 Cloudflare 대시보드의 Rate Limiting 규칙을 켠다.
