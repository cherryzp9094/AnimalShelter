package com.cherryzp.animalshelter.data.api.dto

import com.cherryzp.animalshelter.data.api.ApiJson
import com.cherryzp.animalshelter.fixtures.ApiFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 실제 응답 픽스처로 봉투·items 처리를 확인하는 스모크 테스트(Phase 1). */
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
}
