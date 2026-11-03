# docs/api — 공공데이터 API 공식 명세와 실제 응답

- `official/swagger_15098931.json` — 농림축산식품부 농림축산검역본부_국가동물보호정보시스템 구조동물 조회 서비스(abandonmentPublicService_v2) Swagger 2.0 명세. 출처 https://www.data.go.kr/data/15098931/openapi.do 상세 페이지의 `swaggerJson`, 2026-11-03 저장.
- `official/swagger_15098915.json` — 동물보호센터 정보 조회서비스(animalShelterSrvc_v2) Swagger 2.0 명세. 출처 https://www.data.go.kr/data/15098915/openapi.do, 2026-11-03 저장.
- `samples/live-2026-11-03/` — 2026-11-03 실제 호출 응답(`_type=json`, 일부 xml). 인증키는 들어 있지 않다. `sigungu_v2_seoul.json`은 `upr_cd=6110000`, `kind_v2_dog.json`은 `up_kind_cd=417000`, `abandonmentPublic_v2_empty.json`은 결과 0건(`"items": {}`).
- `samples/live-2026-11-04/` — 2026-11-04 Phase 2 실호출(`LiveApiProbeTest`, `encodedParameters` 키) 응답. `abandonmentPublic_v2_single.json`은 조건 없이 `numOfRows=1`(totalCount 7,566), `abandonmentPublic_v2_desertion_no.json`은 그 1건을 `desertion_no` + `bgnde=endde=happenDt`로 다시 조회한 것(totalCount 1). **결과가 1건이어도 `items.item`은 배열이다.** `careOwnerNm`은 `(가림)`, 키 0건(인코딩형·디코딩형)·`serviceKey=` 0건을 설계 담당이 확인했다. 호출 장부 요약은 `docs/design/reports/phase2.md` 3절.
- `samples/gateway_error_30.json` — 미등록 키로 호출했을 때의 HTTP 403 본문(`docs/rebuild/00-requirements.md` 3절에서 옮김).
- `survey-2026-11-03.md` — 2026-11-03 실측 집계(구조동물 2,612건·보호소 338건의 필드 채움률, 값 형식, API 동작). 원본 응답은 저장소에 없고 집계 결과만 있다. 화면 결정의 근거(`docs/design/05-service.md` 1절).
- 테스트 픽스처는 이 파일들을 `tools/gen_fixtures.py`로 Kotlin 상수(Phase 2부터 `shared/src/commonMain/.../data/api/fixture/ApiFixtures.kt` — 테스트와 키 없는 빌드의 Fixture 엔드포인트가 같이 쓴다. Phase 1에는 `commonTest/.../fixtures/`)로 변환해 쓴다(`docs/design/02-data.md` 11절). 이 폴더의 파일은 손으로 고치지 않는다(새 응답은 날짜 폴더를 추가). `live-2026-11-04/` 두 파일은 Phase 3a 첫 커밋에서 다시 생성해 `ApiFixtures.abandonmentPublicV2Single`·`abandonmentPublicV2DesertionNo`로 들어간다(Fixture 엔진 규칙은 그대로 `abandonmentPublicV2P1`을 쓴다).

- 예시 응답의 `careOwnerNm`(보호소 대표자, 개인 이름일 수 있음)은 `(가림)`으로 바꿔 두었다(2026-11-03, 2026-11-04). 앱 모델은 이 필드를 쓰지 않는다.
