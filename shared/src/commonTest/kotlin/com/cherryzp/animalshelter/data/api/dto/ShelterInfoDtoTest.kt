package com.cherryzp.animalshelter.data.api.dto

import com.cherryzp.animalshelter.data.api.ApiJson
import com.cherryzp.animalshelter.data.api.fixture.ApiFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** `shelterInfo_v2` DTO와 관대한 숫자 직렬화기(D7). */
class ShelterInfoDtoTest {
    private fun decode(fields: String): ShelterInfoDto =
        ApiJson.decodeFromString("""{"careRegNo":"1",$fields}""")

    @Test
    fun `실제 응답 3건을 읽는다`() {
        val items = ApiJson.decodeFromString<ApiEnvelope<ShelterInfoDto>>(ApiFixtures.shelterInfoV2)
            .response.body!!.items

        assertEquals(3, items.size)
        assertEquals(35.922276, items[0].lat)
        assertEquals(0, items[0].vetPersonCnt)
        assertEquals(6, items[1].specsPersonCnt)
        assertEquals(3, items[1].transCarCnt)
        assertNull(items[0].medicalCnt)
    }

    @Test
    fun `정수 칸은 숫자·숫자 문자열·빈 문자열·null·없음을 모두 받는다`() {
        assertEquals(3, decode(""""vetPersonCnt":3""").vetPersonCnt)
        assertEquals(3, decode(""""vetPersonCnt":"3"""").vetPersonCnt)
        assertEquals(3, decode(""""vetPersonCnt":" 3 """").vetPersonCnt)
        assertEquals(3, decode(""""vetPersonCnt":3.0""").vetPersonCnt)
        assertNull(decode(""""vetPersonCnt":""""").vetPersonCnt)
        assertNull(decode(""""vetPersonCnt":null""").vetPersonCnt)
        assertNull(decode(""""vetPersonCnt":"abc"""").vetPersonCnt)
        assertNull(decode(""""vetPersonCnt":2.5""").vetPersonCnt)
        assertNull(decode(""""careNm":"x"""").vetPersonCnt)
    }

    @Test
    fun `실수 칸은 숫자·숫자 문자열·빈 문자열·null·없음을 모두 받는다`() {
        assertEquals(35.9, decode(""""lat":35.9""").lat)
        assertEquals(35.9, decode(""""lat":"35.9"""").lat)
        assertEquals(128.0, decode(""""lng":128""").lng)
        assertNull(decode(""""lat":""""").lat)
        assertNull(decode(""""lat":null""").lat)
        assertNull(decode(""""lat":"-"""").lat)
        assertNull(decode(""""careNm":"x"""").lat)
    }

    @Test
    fun `이상한 값 하나가 목록 전체를 깨지 않는다`() {
        val json = """
            {"response":{"header":{"resultCode":"00"},"body":{"items":{"item":[
              {"careRegNo":"1","lat":"","vetPersonCnt":"없음"},
              {"careRegNo":"2","lat":35.1,"vetPersonCnt":2}
            ]},"numOfRows":1000,"pageNo":1,"totalCount":2}}}
        """.trimIndent()

        val items = ApiJson.decodeFromString<ApiEnvelope<ShelterInfoDto>>(
            json
        ).response.body!!.items

        assertEquals(2, items.size)
        assertNull(items[0].lat)
        assertNull(items[0].vetPersonCnt)
        assertEquals(2, items[1].vetPersonCnt)
    }
}
