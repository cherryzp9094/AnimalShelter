# 애니멀 쉘터 (AnimalShelter)

## 무엇

보호소에 들어온 유기동물을 빨리, 정확하게 찾게 돕는 앱이다. 공공데이터포털의 구조동물 조회 서비스(15098931)와 동물보호센터 정보(15098915)를 사진 중심으로 보여 주고, 보호소 전화·지도·운영 시간으로 이어 준다. 목록(사진 그리드)·필터·정렬(최신순 / 마감 임박순)·상세·사진 뷰어·북마크·보호소 정보·설정이 있다. Android 앱과 웹 앱(Kotlin/Wasm)이 같은 Compose Multiplatform UI를 쓴다. 개인용이며 스토어 배포는 없다. 자세한 것은 [`PRODUCT.md`](PRODUCT.md).

## 구조

```
:androidApp ──▶ :shared ◀── :webApp
                  │
relay/  (Cloudflare Worker — Gradle 밖, HTTP 규약만 공유)
```

- [`shared/`](shared/) — 모든 UI·ViewModel·Repository·API·저장소 로직(`commonMain`). 플랫폼 소스셋에는 `expect`의 `actual`만 둔다.
- [`androidApp/`](androidApp/) — Android 앱 셸. [`webApp/`](webApp/) — 웹 앱 셸(로딩 화면·안내·메타는 `webApp/src/wasmJsMain/resources/`).
- [`relay/`](relay/) — Worker 하나가 웹 앱 정적 자산과 함께 `/api`(공공데이터 중계)·`/img`(사진 중계)·`/a/…`(공유 링크 미리보기)·`/.well-known/assetlinks.json`(App Links)을 낸다.
- [`docs/`](docs/) — 설계·API 명세·실제 응답 표본·옛 앱 분석. [`tools/`](tools/) — 키 검사·픽스처 생성·테마 생성 스크립트.
- 레이어: UI(Compose + ViewModel + `StateFlow`) → Repository 인터페이스 → 데이터 소스(Ktor API, DataStore). 도메인 레이어는 없다([`docs/design/01-overview.md`](docs/design/01-overview.md) 5절).
- 주요 라이브러리: Kotlin Multiplatform, Compose Multiplatform(Material 3·적응형), Navigation 3, Lifecycle ViewModel, Koin, Ktor, kotlinx.serialization·coroutines·datetime, DataStore, Coil. 버전은 [`gradle/libs.versions.toml`](gradle/libs.versions.toml)에만 있다.

## 준비

- Android Studio Quail 4의 JBR을 `JAVA_HOME`으로(시스템 java는 쓰지 않는다).
- Android SDK(Android Studio가 설치한 것).
- Node ≥ 22(중계 Worker 테스트·로컬 실행).
- Google Chrome(웹 브라우저 테스트가 Chrome headless로 돈다).
- 클론한 뒤 키 차단 훅을 켠다(`gitleaks`가 필요하다):

```sh
git config core.hooksPath .githooks
```

## 키 넣는 법

1. 공공데이터포털에서 15098931(구조동물 조회)·15098915(동물보호센터 정보)를 활용신청한다.
2. 마이페이지의 **일반 인증키(Encoding — 퍼센트 인코딩형)**를 저장소 루트 `local.properties`에 넣는다:

```properties
SERVICE_KEY=<인증키>
```

- `local.properties`는 커밋되지 않는다(`.gitignore`, pre-commit 훅이 키를 막는다). 키를 문서·로그·커밋에 쓰지 않는다.
- 키가 없으면 앱은 Fixture(저장소 안의 응답 표본)로 뜬다. 키가 있어도 `-Pfixture=true`로 만들면 Fixture로 뜬다.
- 중계용 `relay/.dev.vars`는 값을 화면에 내지 않고 만든다(권한 600):

```sh
( umask 077 && sed -n 's/^SERVICE_KEY=/SERVICE_KEY=/p' local.properties > relay/.dev.vars )
```

- 키가 새었다고 의심되면 먼저 공공데이터포털에서 재발급하고 모든 자리를 바꾼다([`docs/design/briefs/phase4b-deploy.md`](docs/design/briefs/phase4b-deploy.md) 6절).

## 빌드

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"   # 시스템 java 금지
./gradlew spotlessApply                               # 커밋 전 포맷(ktlint)
./gradlew :androidApp:assembleDebug                   # Android 디버그 APK
./gradlew :androidApp:assembleDebug -Pfixture=true    # 키가 있어도 Fixture로 뜨는 디버그 APK(캡처·수동 확인의 기본, 실호출 0)
./gradlew :androidApp:assembleDebug -PshareHost=animalshelter.invalid -Pfixture=true   # App Links 로컬 확인용(공유 버튼도 보인다). shareHost는 호스트 이름만(소문자 — 아니면 설정 단계에서 멈춘다)
./gradlew :shared:allTests                            # 공통 테스트: Android 호스트(JVM) + wasmJs 브라우저(Chrome headless)
./gradlew :webApp:wasmJsBrowserDistribution           # 웹 배포 번들 → webApp/build/dist/wasmJs/productionExecutable
./gradlew :webApp:wasmJsBrowserDistribution -PrelayBaseUrl=   # 같은 origin 번들(배포·wrangler dev 8788 확인 — /api·/img를 상대 경로로, 공유 주소는 location.origin)
./gradlew :webApp:wasmJsBrowserDevelopmentRun         # 웹 개발 서버(기본 http://localhost:8080)
./gradlew :androidApp:lintDebug spotlessCheck
./gradlew :androidApp:assembleRelease                 # R8. 서명은 signing/ 이 있을 때만
./gradlew :shared:assembleAndroidDeviceTest           # 기기 UI 테스트 APK(Phase 3부터). 실행은 데몬을 내린 뒤 adb install -t + am instrument(briefs/phase3-common.md 6절)
(cd relay && npm ci && npm test)                      # 중계 Worker 순수 함수 테스트(Phase 2부터, Node ≥ 22)
```

- 검증 순서: `spotlessApply → :androidApp:assembleDebug → :shared:allTests → :webApp:wasmJsBrowserDistribution → :androidApp:lintDebug → spotlessCheck`. 그 밖의 규칙은 [`CLAUDE.md`](CLAUDE.md).

## 테스트

- `./gradlew :shared:allTests` — 공통 테스트를 Android 호스트(JVM)와 wasmJs 브라우저(Karma + Chrome headless) 두 타깃에서 돌린다. 테스트는 Fake·Ktor `MockEngine`·[`docs/api/samples`](docs/api/samples/) 픽스처만 쓴다 — 실제 서버를 치는 테스트는 없다(`LiveApiProbeTest`는 평소 건너뛴다).
- 기기 UI 테스트: `./gradlew :shared:assembleAndroidDeviceTest`로 APK를 만들고 `./gradlew --stop`으로 Gradle을 내린 뒤 에뮬레이터에서 `adb install -t` + `adb shell am instrument`로 돌린다([`docs/design/briefs/phase3-common.md`](docs/design/briefs/phase3-common.md) 4절).
- 중계 Worker: `(cd relay && npm ci && npm test)`(네트워크 없음).

## 중계 서버

규약·파일·캐시·보안 주의는 [`relay/README.md`](relay/README.md). 로컬 실행만 줄이면:

```sh
./gradlew :webApp:wasmJsBrowserDistribution -PrelayBaseUrl= && ./gradlew --stop
cd relay && npm ci && npm test
npx wrangler dev        # http://127.0.0.1:8788 (에뮬레이터에서는 http://10.0.2.2:8788)
```

`wrangler`는 로컬 `dev`만 쓴다. 계정 명령(`login`·`deploy`·`secret` 등)은 배포 절차에서만.

## 웹 배포

Worker 하나가 웹 앱과 중계를 함께 낸다(같은 origin — 번들은 `-PrelayBaseUrl=`로 만든다). 절차는 [`relay/README.md`](relay/README.md)의 배포 절과 [`docs/design/briefs/phase4b-deploy.md`](docs/design/briefs/phase4b-deploy.md).

- 배포 주소: **https://animalshelter.mulddae.workers.dev**(Cloudflare Worker `animalshelter`, 2026-11-05 — 확인 기록 [`docs/design/reports/phase4b.md`](docs/design/reports/phase4b.md)). 공유 링크는 `https://animalshelter.mulddae.workers.dev/a/<구조번호>`이고, Android 앱이 있으면 App Links로 앱 상세가 열린다.

## 알려진 한계

2026-11-05 기준이다. `v2.0.0`도 이대로 낸다(5b — [`docs/design/briefs/phase5b-release.md`](docs/design/briefs/phase5b-release.md) 0.1).

- **웹 화면 읽기**: 웹에서 메뉴·대화상자(필터의 지역 드롭다운 같은 메뉴, 정렬·더 보기 메뉴, 날짜 대화상자)를 한 번 열고 닫으면, 새로고침 전까지 화면 읽기 프로그램(VoiceOver 등)이 본 화면을 읽지 못한다. 보이는 화면·마우스·키보드 조작과 Android 앱은 정상이다. Compose Multiplatform 1.12.2 웹의 결함이고 1.13.0에서 고쳐졌다 — 안정판이 나오면 올린다(2026-11-05 현재 `1.13.0-beta02`). 웹의 Tab·읽기 순서가 본문 → 하단 바 → 상단 바인 것도 같은 웹 한계다([`03-screens.md`](docs/design/03-screens.md) 0.2·4절 21, [`reports/phase5a.md`](docs/design/reports/phase5a.md) 14.4).
- **영어 화면의 카드**: 영어(보조 언어)로 보면 2열 카드의 둘째 줄(성별 · 나이 · 체중)이 폰 폭에서 자주 잘려 "…"로 끝난다(360dp 약 83%, 411dp 약 16%). 상세 화면에는 모두 나온다(카드의 화면 읽기 문장에도 성별·나이는 있다). 짧은 영어 문구는 이후 단계다([`reports/phase5a.md`](docs/design/reports/phase5a.md) N15).
- **웹 개인 정보 보호 창의 북마크**: 브라우저 저장소(OPFS)를 쓸 수 없는 창(Safari 개인 정보 보호 창 등)에서는 북마크를 저장하지 못하고 "북마크를 저장하지 못했어요."가 뜬다. 필터의 지역 목록·보호소 정보는 그 창이 열려 있는 동안 기억해 그대로 뜬다.
- **옛 Safari(18.2~18.x)**: OPFS 파일 쓰기(`createWritable`)가 Safari 26부터로 알려져 있어, 그 전 판에서는 북마크가 저장되지 않고 같은 안내가 뜬다. Chrome에서 그 메서드를 지운 흉내로만 확인했다(실제 기기는 5b에서 — 있으면).
- **웹 브라우저 요건**: WasmGC가 되는 브라우저만 — Chrome·Edge 119+, Firefox 120+, Safari 18.2+. 그 밖에서는 "이 브라우저에서는 열 수 없어요." 안내가 뜬다([`01-overview.md`](docs/design/01-overview.md) 2절).

## 설계 문서

- [`docs/design/`](docs/design/) — [`01-overview.md`](docs/design/01-overview.md)(정체성·모듈·아키텍처) · [`02-data.md`](docs/design/02-data.md)(API·모델·저장소·중계) · [`03-screens.md`](docs/design/03-screens.md)(화면·문구) · [`04-plan.md`](docs/design/04-plan.md)(단계) · [`05-service.md`](docs/design/05-service.md)(사용자·정보 구조·공유) · [`06-design-direction.md`](docs/design/06-design-direction.md)(디자인 방향).
- 단계별 브리핑 [`docs/design/briefs/`](docs/design/briefs/), 결과 [`docs/design/reports/`](docs/design/reports/).
- [`PRODUCT.md`](PRODUCT.md)(서비스 요약), [`DESIGN.md`](DESIGN.md)(디자인 시스템), 작업 규칙 [`CLAUDE.md`](CLAUDE.md).

## 출처

- 데이터: 농림축산식품부 농림축산검역본부 국가동물보호정보시스템 · 공공데이터포털 15098931·15098915(이용허락범위 제한 없음 — [`docs/design/05-service.md`](docs/design/05-service.md) 5.4).
- 글꼴: Pretendard(Kil Hyung-jin, SIL Open Font License 1.1).
- 아이콘: Material Symbols(Google, Apache-2.0 — `google/material-design-icons@10ecc14`, [`shared/src/commonMain/composeResources/drawable/`](shared/src/commonMain/composeResources/drawable/)의 XML 주석).
- 색: `@material/material-color-utilities` 0.4.0(Google, Apache-2.0 — [`tools/gen_theme.mjs`](tools/gen_theme.mjs)가 만든 정적 값).
- 에이전트 스킬: [`.claude/skills/adaptive`](.claude/skills/adaptive/)·[`edge-to-edge`](.claude/skills/edge-to-edge/) = Google `android/skills`(https://github.com/android/skills, Apache-2.0). 2026-11-04 android CLI 1.0.16261425로 설치한 그대로이고, 설치본에는 프런트매터가 가리키는 `LICENSE.txt`가 없다([`docs/design/reports/phase3.md`](docs/design/reports/phase3.md) 6절).
- 그 밖의 라이브러리: 앱의 설정 > 정보 > 오픈소스 라이선스와 [`shared/src/commonMain/composeResources/files/licenses/`](shared/src/commonMain/composeResources/files/licenses/).

## 라이선스

이 저장소의 코드에는 아직 라이선스를 정하지 않았다(비공개 저장소 — 공개할 때 정한다). 제3자 라이선스는 위 출처 절.
