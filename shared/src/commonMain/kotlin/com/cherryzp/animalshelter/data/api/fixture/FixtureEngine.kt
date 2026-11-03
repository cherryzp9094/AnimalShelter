package com.cherryzp.animalshelter.data.api.fixture

import com.cherryzp.animalshelter.data.api.ApiJson
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Fixture 엔드포인트(D4): 네트워크 없이 [ApiFixtures]로 답하는 MockEngine.
 * 키 없는 빌드와 디버그 강제 픽스처가 쓴다. 오퍼레이션별 규칙은 Phase 2 브리핑 4.4.
 */
internal fun createFixtureEngine(): HttpClientEngine = MockEngine { request ->
    fixtureResponse(request.url)
}

private fun MockRequestHandleScope.fixtureResponse(url: Url): HttpResponseData {
    val params = url.parameters
    val body = when (url.encodedPath.substringAfterLast('/')) {
        "sido_v2" -> ApiFixtures.sidoV2

        "sigungu_v2" -> if (params["upr_cd"] == SEOUL) ApiFixtures.sigunguV2Seoul else EMPTY

        "shelter_v2" -> EMPTY

        "kind_v2" -> if (params["up_kind_cd"] == DOG) ApiFixtures.kindV2Dog else EMPTY

        "abandonmentPublic_v2" -> {
            val desertionNo = params["desertion_no"]
            val pageNo = params["pageNo"]?.toIntOrNull() ?: 1
            when {
                desertionNo != null -> singleAnimal(desertionNo)
                pageNo <= 1 -> ApiFixtures.abandonmentPublicV2P1
                else -> EMPTY
            }
        }

        "shelterInfo_v2" -> ApiFixtures.shelterInfoV2

        else -> return respond("", HttpStatusCode.NotFound)
    }
    return respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
}

private const val SEOUL = "6110000"
private const val DOG = "417000"
private val EMPTY: String get() = ApiFixtures.abandonmentPublicV2Empty

/** 목록 표본에서 그 구조번호 1건만 배열 모양으로. 없으면 0건. */
private fun singleAnimal(desertionNo: String): String {
    val root = ApiJson.parseToJsonElement(ApiFixtures.abandonmentPublicV2P1).jsonObject
    val response = root.getValue("response").jsonObject
    val items = response.getValue("body").jsonObject.getValue("items").jsonObject
        .getValue("item").jsonArray
    val match = items.firstOrNull {
        it.jsonObject["desertionNo"]?.jsonPrimitive?.content == desertionNo
    } ?: return EMPTY
    val body = buildJsonObject {
        put("items", buildJsonObject { put("item", JsonArray(listOf(match))) })
        put("numOfRows", 1)
        put("pageNo", 1)
        put("totalCount", 1)
    }
    return JsonObject(mapOf("response" to JsonObject(response + ("body" to body)))).toString()
}
