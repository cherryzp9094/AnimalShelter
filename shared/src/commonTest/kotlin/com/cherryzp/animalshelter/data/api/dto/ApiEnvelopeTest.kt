package com.cherryzp.animalshelter.data.api.dto

import com.cherryzp.animalshelter.data.api.ApiJson
import com.cherryzp.animalshelter.data.api.fixture.ApiFixtures
import com.cherryzp.animalshelter.data.repository.toAnimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** 실제 응답 픽스처로 봉투·items 처리를 확인한다. */
class ApiEnvelopeTest {
    @Test
    fun `sido_v2 실제 응답은 16건이고 resultCode가 00이다`() {
        val envelope = ApiJson.decodeFromString<ApiEnvelope<SidoDto>>(ApiFixtures.sidoV2)

        assertEquals("00", envelope.response.header.resultCode)
        val body = envelope.response.body!!
        assertEquals(16, body.items.size)
        assertEquals(16, body.totalCount)
        assertEquals("6110000", body.items.first().orgCd)
        assertEquals("서울특별시", body.items.first().orgdownNm)
    }

    @Test
    fun `0건 응답의 빈 items 객체는 빈 목록이 된다`() {
        val envelope = ApiJson.decodeFromString<ApiEnvelope<AbandonmentItemDto>>(
            ApiFixtures.abandonmentPublicV2Empty
        )

        assertEquals("00", envelope.response.header.resultCode)
        val body = envelope.response.body!!
        assertTrue(body.items.isEmpty())
        assertEquals(0, body.totalCount)
    }

    @Test
    fun `목록 실제 응답 5건을 읽는다`() {
        val body = ApiJson.decodeFromString<ApiEnvelope<AbandonmentItemDto>>(
            ApiFixtures.abandonmentPublicV2P1
        ).response.body!!

        assertEquals(5, body.items.size)
        assertEquals(7544, body.totalCount)
        assertEquals("450650202602738", body.items.first().desertionNo)
    }

    @Test
    fun `item이 객체 하나여도 1건 목록이 된다`() {
        val json = """
            {"response":{"header":{"resultCode":"00"},
             "body":{"items":{"item":{"kindCd":"000054","kindNm":"골든 리트리버"}},"numOfRows":1,"pageNo":1,"totalCount":1}}}
        """.trimIndent()

        val body = ApiJson.decodeFromString<ApiEnvelope<KindDto>>(json).response.body!!

        assertEquals(listOf(KindDto("000054", "골든 리트리버")), body.items)
    }

    @Test
    fun `numOfRows·pageNo·totalCount·reqNo는 숫자와 문자열 둘 다 받는다`() {
        val asNumbers = """
            {"response":{"header":{"reqNo":123,"resultCode":"00"},
             "body":{"items":{},"numOfRows":30,"pageNo":2,"totalCount":7544}}}
        """.trimIndent()
        val asStrings = """
            {"response":{"header":{"reqNo":"123","resultCode":"00"},
             "body":{"items":{},"numOfRows":"30","pageNo":"2","totalCount":"7544"}}}
        """.trimIndent()

        for (json in listOf(asNumbers, asStrings)) {
            val response = ApiJson.decodeFromString<ApiEnvelope<KindDto>>(json).response
            assertEquals("123", response.header.reqNo)
            val body = response.body!!
            assertEquals(30, body.numOfRows)
            assertEquals(2, body.pageNo)
            assertEquals(7544, body.totalCount)
        }
    }

    @Test
    fun `게이트웨이 오류 본문을 읽는다`() {
        val error = ApiJson.decodeFromString<GatewayErrorEnvelope>(ApiFixtures.gatewayError30)

        val header = error.response.cmmMsgHeader!!
        assertEquals("30", header.returnReasonCode)
        assertEquals("SERVICE_KEY_IS_NOT_REGISTERED_ERROR", header.errMsg)
        assertEquals("등록되지 않은 서비스키", header.returnAuthMsg)
    }

    @Test
    fun `코드표 실제 응답을 읽는다`() {
        // 표본은 numOfRows=10으로 받은 것이라 28건 중 10건만 들어 있다.
        val sigunguBody = ApiJson.decodeFromString<ApiEnvelope<SigunguDto>>(
            ApiFixtures.sigunguV2Seoul
        )
            .response.body!!
        val sigungu = sigunguBody.items
        val kinds = ApiJson.decodeFromString<ApiEnvelope<KindDto>>(ApiFixtures.kindV2Dog)
            .response.body!!.items

        assertEquals(10, sigungu.size)
        assertEquals(28, sigunguBody.totalCount)
        assertEquals(SigunguDto("6110000", "6119999", "가정보호"), sigungu.first())
        assertTrue(kinds.isNotEmpty())
        assertTrue(kinds.all { !it.kindCd.isNullOrEmpty() && !it.kindNm.isNullOrEmpty() })
    }

    @Test
    fun `단건 실측 표본 둘은 item이 배열 1건이고 대표자 이름은 모델에 없다(R5)`() {
        for (raw in listOf(
            ApiFixtures.abandonmentPublicV2Single,
            ApiFixtures.abandonmentPublicV2DesertionNo
        )) {
            // 원문: 1건이어도 item은 배열이다(2026-11-04 실측)
            val items = ApiJson.parseToJsonElement(
                raw
            ).jsonObject["response"]!!.jsonObject["body"]!!
                .jsonObject["items"]!!.jsonObject["item"]
            assertIs<JsonArray>(items)
            assertEquals(1, items.size)
            val owner = (items.single() as JsonObject)["careOwnerNm"]?.jsonPrimitive?.content
            assertNotNull(owner)

            val body = ApiJson.decodeFromString<ApiEnvelope<AbandonmentItemDto>>(
                raw
            ).response.body!!
            assertEquals(1, body.items.size)
            val animal = assertNotNull(body.items.single().toAnimal())
            assertEquals("413587202600490", animal.desertionNo)
            assertFalse(owner in animal.toString(), "Animal에 careOwnerNm 값이 들어갔다")
        }
    }
}
