# AnimalShelter 중계 Worker (`relay/`)

Cloudflare Worker 하나가 웹 앱(정적 자산)과 함께 다음을 맡는다. 규약은 `docs/design/02-data.md` 7절.

- `/api/{service}/{operation}` — 공공데이터 API 중계. 키(`SERVICE_KEY`)는 중계만 갖고, 웹 번들에는 들어가지 않는다.
- `/img/{path}` — 보호소 사진 중계(원본 서버에 CORS가 없고 `Content-Type`이 `application/octet-stream`이다).
- `/a/{desertionNo}` — 공유 링크 미리보기(OG 태그를 넣은 `index.html`). 사람 브라우저는 그대로 앱이 열린다.
- `/.well-known/assetlinks.json` — Android App Links 검증 파일(`assetlinks.json`, Phase 4·5에 서명 SHA-256을 채운다).

## 파일

| 파일 | 내용 |
|---|---|
| `worker.js` | fetch 핸들러: 라우팅·업스트림 호출·캐시·CORS(I/O) |
| `lib.js` | 순수 함수: 경로·파라미터 검사, 업스트림 주소, 사진 경로, 재조회 창, KST 날짜, 상태 판정, OG 문구, HTML 이스케이프·주입, Origin 판정 |
| `test/lib.test.js` | `node --test`(네트워크 없음, 입력은 인라인 또는 `../docs/api/samples`) |
| `wrangler.toml` | 이름·정적 자산·`run_worker_first`·observability 끔·로컬 포트 8788 |
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
```

- 8787은 이 맥의 다른 프로젝트 서버가 쓴다 — 그대로 둔다.
- 로그인 없이 뜬다. `login`·`whoami`·`deploy`·`secret`·`--remote` 같은 계정 명령은 쓰지 않는다(배포는 아래 절차로 사용자가).
- `/cdn-cgi/local/explorer`(관측 API)와 `relay/.wrangler/`에는 업스트림 URL(=키)이 남을 수 있으니 열어 보지 않는다.
- 끝낼 때: `wrangler dev`를 띄운 그 작업을 멈춘다 — 터미널 앞에서 띄웠으면 그 창에서 `Ctrl+C`, 에이전트가 백그라운드 작업으로 띄웠으면 그 작업을 멈춘다. **프로세스 이름 패턴으로 kill하지 않는다**(`pkill`·`pgrep -f …` 금지 — 이 맥의 8787 python 서버 같은 다른 프로세스를 건드릴 수 있다). 멈춘 뒤 `lsof -nP -iTCP:8788 -sTCP:LISTEN`이 비었는지 확인한다(남아 있으면 PID를 확인해 사람에게 알린다). 지울 때: `rm relay/.dev.vars`.
- 에뮬레이터(Android 17)에서 `http://10.0.2.2:8788`을 쓰려면 앱을 설치할 때마다 `adb shell pm grant com.cherryzp.animalshelter android.permission.ACCESS_LOCAL_NETWORK`(debug 빌드만 이 권한을 선언한다).

## 경로 규약

| 요청 | 동작 |
|---|---|
| `GET /api/{service}/{operation}?…` | `service` 2개·`operation` 6개만(그 밖 404). 오퍼레이션별 허용 파라미터만(모르는 이름·중복·50자 초과 → 400 `{"relayError":"param"}`), `numOfRows` 1~1000. 클라이언트의 `serviceKey`·`_type`은 버리고 중계가 `_type=json&serviceKey=…`를 **문자열로** 붙인다(키는 이미 퍼센트 인코딩형). 타임아웃 20초. 응답은 업스트림 상태·본문 그대로, `Content-Type: application/json; charset=utf-8`, `Cache-Control: no-store` |
| `GET /img/shelter/YYYY/MM/<파일>.(jpg\|jpeg\|png)` | 퍼센트 디코딩 뒤 형식 검사(`..` 거부, 대괄호 허용). `image/jpeg`·`image/png`, `public, max-age=86400`, Cloudflare 캐시 1일 |
| `GET /a/{15자리}` | 재조회 창(날짜 없이 → 구조번호의 연도 → 그 전 해, 최대 3회)으로 찾아 요약 JSON을 1시간 캐시(0건도 캐시, 업스트림 오류는 캐시하지 않고 일반 OG). `index.html`(`/`로 읽는다 — `/index.html`은 307)에 `<base href="/">`·OG·`<title>`을 넣고 `<body>` 첫 줄에 `history.replaceState(null, "", "/#/animal/{id}")`. `text/html`, `public, max-age=3600` |
| `GET /.well-known/assetlinks.json` | `assetlinks.json` 그대로(`application/json`, 리다이렉트 없음). 그 밖 `/.well-known/*`는 404 |
| 그 밖 | 정적 자산(SPA: 없는 경로도 `index.html`) |

- 메서드: GET·HEAD만(그 밖 405). `/api`·`/img`의 `OPTIONS`는 사전 요청.
- CORS(`/api`·`/img`만, 로컬 개발용): `ALLOWED_ORIGINS` 안의 Origin이면 `Access-Control-Allow-Origin`·`Vary: Origin`·`Access-Control-Expose-Headers: X-Relay-Cache`, 사전 요청은 204. 목록 밖 Origin은 403. Origin 없는 요청(curl·Android)은 CORS 헤더 없이 허용. 배포(A안)는 같은 origin이라 CORS가 필요 없다.
- 업스트림 실패·타임아웃: 502 `{"relayError":"upstream"}`.

## 캐시

- `caches.default`. 확인용 헤더 `X-Relay-Cache: HIT | MISS | BYPASS`.
- 코드표 4종(`sido_v2`·`sigungu_v2`·`shelter_v2`·`kind_v2`)·`shelterInfo_v2`: 1일. 키는 중계 자신의 주소(허용 파라미터만 이름순, 키 없음). 저장 조건: 업스트림 200 + 본문 앞 1KB의 `"resultCode":"00"` + `totalCount` > 0(오류·0건을 하루 동안 돌려주지 않게).
- 목록(`abandonmentPublic_v2`): 캐시하지 않는다(`BYPASS`, 본문을 그대로 흘려보낸다).
- `/a` 조회 결과: 1시간. 사진: 1일(Cloudflare 캐시).

## 한도

Workers 무료: 하루 100,000 요청(정적 자산 요청은 제외), 요청당 CPU 10ms, 서브리퀘스트 50. 업스트림 개발계정: 서비스마다 하루 10,000건. 계산은 `docs/design/05-service.md` 5.3.

## 배포(사용자가 설계 담당과 함께)

1. Cloudflare 계정 → `npx wrangler login`
2. `npx wrangler secret put SERVICE_KEY` — 값은 프롬프트에 직접 입력(화면·기록에 남기지 않는다)
3. 웹 앱을 같은 origin으로 빌드: `./gradlew :webApp:wasmJsBrowserDistribution -PrelayBaseUrl=`
4. `npx wrangler deploy`

배포 뒤 확인:
- `/api/abandonmentPublicService_v2/sido_v2?numOfRows=100`을 두 번 불러 두 번째가 `X-Relay-Cache: HIT`인지(Cache API가 `workers.dev`에서 되는지). 안 되면 KV로 바꾼다(02-data 7절).
- `/.well-known/assetlinks.json`이 리다이렉트 없이 200 `application/json`인지.

## 보안 주의

- 업스트림 주소에는 키가 있다. `console.log`에 URL·`env`·업스트림 응답을 쓰지 않는다. 예외 메시지도 버린다.
- `[observability] enabled = false` — 추적에 업스트림 URL이 남지 않게.
- 중계는 인증이 없다. Origin 허용 목록은 브라우저에만 통한다 — 남용이 보이면 Cloudflare 대시보드의 Rate Limiting 규칙을 켠다.
