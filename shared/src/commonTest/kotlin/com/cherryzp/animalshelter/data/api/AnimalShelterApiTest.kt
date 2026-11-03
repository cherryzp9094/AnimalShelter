package com.cherryzp.animalshelter.data.api

import com.cherryzp.animalshelter.data.api.fixture.ApiFixtures
import com.cherryzp.animalshelter.model.AnimalKind
import com.cherryzp.animalshelter.model.Neutered
import com.cherryzp.animalshelter.model.NoticeState
import com.cherryzp.animalshelter.model.SearchFilter
import com.cherryzp.animalshelter.model.Sex
import com.cherryzp.animalshelter.testing.ApiTestResponses
import io.ktor.client.engine.mock.respond
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

/** URL 조립·키 처리·오류 분류(02-data 6·11절, D5·D6). 실제 서버는 치지 않는다(MockEngine). */
class AnimalShelterApiTest {
    @Test
    fun `Direct는 serviceKey를 인코딩된 그대로 붙인다`() = runTest {
        val harness = ApiTestHarness(handler = jsonOk(ApiFixtures.sidoV2))

        harness.api.getSido()

        val url = harness.urls.single()
        val text = url.toString()
        assertTrue(
            text.startsWith("https://apis.data.go.kr/1543061/abandonmentPublicService_v2/sido_v2?")
        )
        assertTrue("serviceKey=$FAKE_KEY" in text)
        assertFalse("%252B" in text)
        assertEquals("json", url.parameters["_type"])
        assertEquals("100", url.parameters["numOfRows"])
    }

    @Test
    fun `Relay는 키 없이 api 경로로 부른다`() = runTest {
        val harness = ApiTestHarness(
            endpoint = ApiEndpoint.Relay("http://localhost:8788/"),
            handler = jsonOk(ApiFixtures.sidoV2)
        )

        harness.api.getSido()

        val url = harness.urls.single()
        assertEquals("localhost", url.host)
        assertEquals(8788, url.port)
        assertEquals("/api/abandonmentPublicService_v2/sido_v2", url.encodedPath)
        assertNull(url.parameters["serviceKey"])
        assertFalse("serviceKey" in url.toString())
    }

    @Test
    fun `Relay 주소가 비면 같은 origin의 상대 경로`() = runTest {
        val harness =
            ApiTestHarness(
                endpoint = ApiEndpoint.Relay(""),
                handler = jsonOk(ApiFixtures.shelterInfoV2)
            )

        harness.api.getAllShelterInfo()

        assertEquals("/api/animalShelterSrvc_v2/shelterInfo_v2", harness.urls.single().encodedPath)
        assertEquals("1000", harness.urls.single().parameters["numOfRows"])
    }

    @Test
    fun `빈 조건은 파라미터를 붙이지 않는다`() = runTest {
        val harness = ApiTestHarness(handler = jsonOk(ApiFixtures.abandonmentPublicV2P1))

        harness.api.searchAnimals(SearchFilter(kindCode = " ", sidoCode = ""), pageNo = 1)

        val names = harness.urls.single().parameters.names()
        assertEquals(setOf("pageNo", "numOfRows", "_type", "serviceKey"), names)
        assertEquals("30", harness.urls.single().parameters["numOfRows"])
    }

    @Test
    fun `조건은 명세 이름으로 붙는다 - 목록은 upkind`() = runTest {
        val harness = ApiTestHarness(handler = jsonOk(ApiFixtures.abandonmentPublicV2P1))
        val filter = SearchFilter(
            kind = AnimalKind.DOG,
            kindCode = "000054",
            dateFrom = LocalDate(2026, 10, 1),
            dateTo = LocalDate(2026, 11, 4),
            sidoCode = "6110000",
            sigunguCode = "3220000",
            shelterRegNo = "311322200900001",
            state = NoticeState.NOTICE,
            neutered = Neutered.YES,
            sex = Sex.FEMALE
        )

        harness.api.searchAnimals(filter, pageNo = 2)

        val p = harness.urls.single().parameters
        assertEquals("417000", p["upkind"])
        assertNull(p["up_kind_cd"])
        assertEquals("000054", p["kind"])
        assertEquals("20261001", p["bgnde"])
        assertEquals("20261104", p["endde"])
        assertEquals("6110000", p["upr_cd"])
        assertEquals("3220000", p["org_cd"])
        assertEquals("311322200900001", p["care_reg_no"])
        assertEquals("notice", p["state"])
        assertEquals("Y", p["neuter_yn"])
        assertEquals("F", p["sex_cd"])
        assertEquals("2", p["pageNo"])
    }

    @Test
    fun `품종 조회는 up_kind_cd`() = runTest {
        val harness = ApiTestHarness(handler = jsonOk(ApiFixtures.kindV2Dog))

        val result = harness.api.getKinds(AnimalKind.DOG)

        val p = harness.urls.single().parameters
        assertEquals("417000", p["up_kind_cd"])
        assertNull(p["upkind"])
        assertEquals("500", p["numOfRows"])
        assertEquals(10, assertIs<ApiResult.Success<*>>(result).let { (it.value as List<*>).size })
    }

    @Test
    fun `게이트웨이 403 본문은 Gateway(30)`() = runTest {
        val harness = ApiTestHarness(handler = {
            respond(ApiFixtures.gatewayError30, HttpStatusCode.Forbidden)
        })

        val result = harness.api.getSido()

        assertEquals(
            ApiResult.Failure(ApiError.Gateway("30", "등록되지 않은 서비스키")),
            result
        )
    }

    @Test
    fun `200과 XML 게이트웨이 본문도 Gateway`() = runTest {
        val xml = "<OpenAPI_ServiceResponse><cmmMsgHeader><errMsg>SERVICE ERROR</errMsg>" +
            "<returnAuthMsg>LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR</returnAuthMsg>" +
            "<returnReasonCode>22</returnReasonCode></cmmMsgHeader></OpenAPI_ServiceResponse>"
        val harness = ApiTestHarness(handler = {
            respond(xml, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/xml"))
        })

        val result = harness.api.getSido()

        assertEquals(
            ApiResult.Failure(
                ApiError.Gateway("22", "LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR")
            ),
            result
        )
    }

    @Test
    fun `XML resultCode는 Service, 둘 다 없으면 Parse`() = runTest {
        val service = ApiTestHarness(handler = {
            respond(
                "<response><header><resultCode>03</resultCode><resultMsg>NODATA</resultMsg></header></response>"
            )
        })
        val html = ApiTestHarness(handler = { respond("<html>error</html>", HttpStatusCode.OK) })

        assertEquals(ApiResult.Failure(ApiError.Service("03", "NODATA")), service.api.getSido())
        assertEquals(ApiResult.Failure(ApiError.Parse("<html>error</html>")), html.api.getSido())
    }

    @Test
    fun `resultCode 99는 Service`() = runTest {
        val harness =
            ApiTestHarness(
                handler = jsonOk(ApiTestResponses.envelope(emptyList(), resultCode = "99"))
            )

        assertEquals(ApiResult.Failure(ApiError.Service("99", "ERROR")), harness.api.getSido())
    }

    @Test
    fun `Relay의 502 relayError는 RelayUnavailable, 그 밖 상태는 Http`() = runTest {
        val relay =
            ApiTestHarness(endpoint = ApiEndpoint.Relay("http://localhost:8788"), handler = {
                respond("""{"relayError":"upstream"}""", HttpStatusCode.BadGateway)
            })
        val direct500 =
            ApiTestHarness(handler = { respond("oops", HttpStatusCode.InternalServerError) })
        val relay400 =
            ApiTestHarness(endpoint = ApiEndpoint.Relay("http://localhost:8788"), handler = {
                respond("""{"relayError":"param"}""", HttpStatusCode.BadRequest)
            })

        assertEquals(ApiResult.Failure(ApiError.RelayUnavailable), relay.api.getSido())
        assertEquals(ApiResult.Failure(ApiError.Http(500)), direct500.api.getSido())
        assertEquals(ApiResult.Failure(ApiError.Http(400)), relay400.api.getSido())
    }

    @Test
    fun `봉투가 아니면 Parse이고 detail은 200자까지`() = runTest {
        val body = "{\"unexpected\":\"" + "x".repeat(300) + "\"}"
        val harness = ApiTestHarness(handler = jsonOk(body))

        val error = assertIs<ApiError.Parse>(harness.api.getSido().errorOrNull())

        assertEquals(200, error.detail.length)
    }

    @Test
    fun `타임아웃·연결 실패·파싱 실패의 결과에 키가 없다`() = runTest {
        val timeout = ApiTestHarness(handler = { request ->
            throw HttpRequestTimeoutException(request.url.toString(), 20_000)
        })
        val connect = ApiTestHarness(handler = { request ->
            throw ConnectTimeoutException("Connect timeout ${request.url}", null)
        })
        val generic = ApiTestHarness(handler = { request ->
            throw IllegalStateException("failed ${request.url}")
        })
        val parse = ApiTestHarness(handler = {
            respond(
                "<html>bad request serviceKey=$FAKE_KEY&x=1 TEST+KEY/VAL==</html>",
                HttpStatusCode.OK
            )
        })
        val relay =
            ApiTestHarness(
                endpoint = ApiEndpoint.Relay(
                    "http://localhost:8788"
                ),
                handler = { request ->
                    throw ConnectTimeoutException("Connect timeout ${request.url}", null)
                }
            )

        val results = listOf(
            timeout.api.getSido(),
            connect.api.getSido(),
            generic.api.searchAnimals(SearchFilter(), 1),
            parse.api.getSido(),
            relay.api.getSido()
        )

        assertEquals(ApiResult.Failure(ApiError.Network), results[0])
        assertEquals(ApiResult.Failure(ApiError.Network), results[1])
        assertEquals(ApiResult.Failure(ApiError.Network), results[2])
        assertIs<ApiError.Parse>(results[3].errorOrNull())
        assertEquals(ApiResult.Failure(ApiError.RelayUnavailable), results[4])
        for (result in results) {
            val text = result.toString()
            assertFalse("TEST" in text, text)
            assertFalse(FAKE_KEY in text, text)
        }
    }

    @Test
    fun `getAnimal은 happenDate가 있으면 그날 하루로 1회`() = runTest {
        val harness = ApiTestHarness(handler = jsonOk(ApiTestResponses.animals("450650202602738")))

        val result = harness.api.getAnimal("450650202602738", LocalDate(2026, 11, 3))

        assertEquals("450650202602738", result.getOrNull()?.desertionNo)
        val p = harness.urls.single().parameters
        assertEquals("450650202602738", p["desertion_no"])
        assertEquals("20261103", p["bgnde"])
        assertEquals("20261103", p["endde"])
        assertEquals("1", p["numOfRows"])
    }

    @Test
    fun `getAnimal은 desertion_no 실측 응답(item 배열 1건)을 한 마리로 읽는다(R5)`() = runTest {
        val harness = ApiTestHarness(handler = jsonOk(ApiFixtures.abandonmentPublicV2DesertionNo))

        val animal = harness.api.getAnimal("413587202600490", LocalDate(2026, 11, 3)).getOrNull()

        assertEquals("413587202600490", animal?.desertionNo)
        assertEquals(1, harness.urls.size)
    }

    @Test
    fun `getAnimal은 날짜가 없으면 기본 창 → 그 해 → 전 해, 최대 3회`() = runTest {
        val harness = ApiTestHarness(handler = jsonOk(ApiTestResponses.empty))

        val result = harness.api.getAnimal("450650202602738")

        assertEquals(ApiResult.Success(null), result)
        val windows = harness.urls.map { it.parameters["bgnde"] to it.parameters["endde"] }
        assertEquals(
            listOf(null to null, "20260101" to "20261231", "20250101" to "20251231"),
            windows
        )
    }

    @Test
    fun `getAnimal은 찾으면 멈춘다`() = runTest {
        var calls = 0
        val harness = ApiTestHarness(handler = {
            calls++
            val body = if (calls ==
                2
            ) {
                ApiTestResponses.animals("450650202502738")
            } else {
                ApiTestResponses.empty
            }
            respond(body, HttpStatusCode.OK)
        })

        val result = harness.api.getAnimal("450650202502738")

        assertEquals("450650202502738", result.getOrNull()?.desertionNo)
        assertEquals(2, harness.urls.size)
        assertEquals("20250101", harness.urls[1].parameters["bgnde"])
    }

    @Test
    fun `getAnimal 연도가 2000~올해 밖이면 연도 창을 건너뛴다`() = runTest {
        val past = ApiTestHarness(handler = jsonOk(ApiTestResponses.empty))
        val future = ApiTestHarness(handler = jsonOk(ApiTestResponses.empty))

        past.api.getAnimal("448567199901800")
        future.api.getAnimal("448567203001800")

        assertEquals(1, past.urls.size)
        assertEquals(1, future.urls.size)
        assertEquals(listOf(null), lookupWindows("12345", null, 2026))
        assertEquals(2, lookupWindows("448567200001800", null, 2026).size)
    }

    @Test
    fun `getAnimal 도중 실패는 그 오류로 멈춘다`() = runTest {
        val harness = ApiTestHarness(handler = { respond("x", HttpStatusCode.ServiceUnavailable) })

        assertEquals(
            ApiResult.Failure(ApiError.Http(503)),
            harness.api.getAnimal("450650202602738")
        )
        assertEquals(1, harness.urls.size)
    }

    @Test
    fun `searchAll은 페이지를 이어 붙이고 중복은 앞의 것만`() = runTest {
        val harness = ApiTestHarness(handler = { request ->
            val body = when (request.url.parameters["pageNo"]) {
                "1" -> ApiTestResponses.animals("A", "B", "C", totalCount = 1500)
                else -> ApiTestResponses.animals("C", "D", totalCount = 1500)
            }
            respond(body, HttpStatusCode.OK)
        })

        val result = harness.api.searchAll(SearchFilter(state = NoticeState.NOTICE))

        assertEquals(listOf("A", "B", "C", "D"), result.getOrNull()?.map { it.desertionNo })
        assertEquals(listOf("1", "2"), harness.urls.map { it.parameters["pageNo"] })
        assertTrue(
            harness.urls.all {
                it.parameters["numOfRows"] == "1000" &&
                    it.parameters["state"] == "notice"
            }
        )
    }

    @Test
    fun `searchAll은 5000건을 넘으면 첫 페이지만 부르고 TooManyResults`() = runTest {
        val harness =
            ApiTestHarness(handler = jsonOk(ApiTestResponses.animals("A", totalCount = 5001)))

        assertEquals(
            ApiResult.Failure(ApiError.TooManyResults(5001)),
            harness.api.searchAll(SearchFilter())
        )
        assertEquals(1, harness.urls.size)
    }

    @Test
    fun `searchAll은 빈 페이지에서 끝난다`() = runTest {
        val harness = ApiTestHarness(handler = { request ->
            val body = if (request.url.parameters["pageNo"] == "1") {
                ApiTestResponses.animals("A", totalCount = 3000)
            } else {
                ApiTestResponses.envelope(emptyList(), totalCount = 3000)
            }
            respond(body, HttpStatusCode.OK)
        })

        assertEquals(
            listOf("A"),
            harness.api.searchAll(SearchFilter()).getOrNull()?.map {
                it.desertionNo
            }
        )
        assertEquals(2, harness.urls.size)
    }

    @Test
    fun `목록 페이지는 빈 페이지면 hasMore가 거짓(D9)`() = runTest {
        val harness =
            ApiTestHarness(
                handler = jsonOk(ApiTestResponses.envelope(emptyList(), totalCount = 100))
            )

        val page = harness.api.searchAnimals(SearchFilter(), pageNo = 2).getOrNull()!!

        assertTrue(page.items.isEmpty())
        assertEquals(100, page.totalCount)
        assertFalse(page.hasMore)
    }
}
