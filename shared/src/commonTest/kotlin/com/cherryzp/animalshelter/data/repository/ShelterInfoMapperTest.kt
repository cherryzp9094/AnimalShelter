package com.cherryzp.animalshelter.data.repository

import com.cherryzp.animalshelter.data.api.ApiJson
import com.cherryzp.animalshelter.data.api.dto.ApiEnvelope
import com.cherryzp.animalshelter.data.api.dto.ShelterInfoDto
import com.cherryzp.animalshelter.data.api.fixture.ApiFixtures
import com.cherryzp.animalshelter.model.RoomCounts
import com.cherryzp.animalshelter.model.TimeRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 보호소 정보 정리 규칙(02-data 5.1). */
class ShelterInfoMapperTest {
    private fun dto(block: ShelterInfoDto.() -> ShelterInfoDto = { this }) =
        ShelterInfoDto(careRegNo = "311322200900001", careNm = "보호소").block()

    @Test
    fun `실제 응답 3건을 매핑한다`() {
        val items = ApiJson.decodeFromString<ApiEnvelope<ShelterInfoDto>>(ApiFixtures.shelterInfoV2)
            .response.body!!.items.mapNotNull { it.toShelterInfo() }

        assertEquals(3, items.size)
        val first = items[0]
        assertEquals(TimeRange("00:00", "24:00"), first.weekdayHours)
        assertTrue(first.weekdayHours!!.isAllDay)
        assertNull(first.closedDay) // "0"
        assertEquals("6층", first.jibunAddress)
        assertEquals(35.922276, first.latitude)
        val second = items[1]
        assertNull(second.jibunAddress) // 공백만
        assertEquals("토요일+일요일", second.closedDay)
        assertEquals(RoomCounts(1, 1, 1, 1), second.roomCounts)
        assertEquals(TimeRange("13:00", "15:00"), second.weekdayAdoptionHours)
    }

    @Test
    fun `가려진 전화는 null`() {
        assertNull(dto { copy(careTel = "***********") }.toShelterInfo()!!.tel)
        assertEquals("02-123-4567", dto { copy(careTel = "02-123-4567") }.toShelterInfo()!!.tel)
    }

    @Test
    fun `좌표가 0이거나 범위 밖이면 둘 다 null`() {
        val zero = dto { copy(lat = 0.0, lng = 127.0) }.toShelterInfo()!!
        val outside = dto { copy(lat = 37.5, lng = 140.0) }.toShelterInfo()!!
        val ok = dto { copy(lat = 37.5, lng = 127.0) }.toShelterInfo()!!

        assertNull(zero.latitude)
        assertNull(zero.longitude)
        assertNull(outside.latitude)
        assertNull(outside.longitude)
        assertEquals(37.5, ok.latitude)
        assertEquals(127.0, ok.longitude)
    }

    @Test
    fun `숫자만 있는 휴무일은 null`() {
        assertNull(dto { copy(closeDay = "1+15+10") }.toShelterInfo()!!.closedDay)
        assertNull(dto { copy(closeDay = "2") }.toShelterInfo()!!.closedDay)
        assertEquals("연중무휴", dto { copy(closeDay = "연중무휴") }.toShelterInfo()!!.closedDay)
    }

    @Test
    fun `콜론만 있거나 형식이 다른 시간은 null`() {
        assertNull(timeRange(":", ":"))
        assertNull(timeRange("09:00", ":"))
        assertNull(timeRange("9시", "18:00"))
        assertNull(timeRange("24:30", "25:00"))
        assertEquals(TimeRange("09:00", "18:00"), timeRange(" 09:00", "18:00 "))
    }

    @Test
    fun `시설 수가 하나도 없으면 roomCounts는 null, 번호나 이름이 없으면 버린다`() {
        assertNull(dto().toShelterInfo()!!.roomCounts)
        assertNull(ShelterInfoDto(careRegNo = "1").toShelterInfo())
        assertNull(ShelterInfoDto(careNm = "x").toShelterInfo())
    }
}
