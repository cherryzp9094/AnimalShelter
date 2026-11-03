package com.cherryzp.animalshelter.ui.shelter

import com.cherryzp.animalshelter.fake.testShelterInfo
import com.cherryzp.animalshelter.model.TimeRange
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.hours_all_day
import com.cherryzp.animalshelter.resources.hours_range
import com.cherryzp.animalshelter.resources.today_adoption
import com.cherryzp.animalshelter.resources.today_closed
import com.cherryzp.animalshelter.resources.today_hours
import com.cherryzp.animalshelter.ui.common.uiText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

/** 오늘 운영시간 부분(브리핑 B7). 2026-11-04는 수요일, 11-07은 토요일. */
class ShelterHoursTest {
    private val wednesday = LocalDate(2026, 11, 4)
    private val saturday = LocalDate(2026, 11, 7)
    private val info = testShelterInfo().copy(
        weekdayHours = TimeRange("09:00", "18:00"),
        weekdayAdoptionHours = TimeRange("13:00", "15:00"),
        weekendHours = TimeRange("10:00", "17:00"),
        weekendAdoptionHours = null,
        closedDay = "일요일"
    )

    private fun range(start: String, end: String) = uiText(Res.string.hours_range, start, end)

    @Test
    fun `평일은 평일 값, 운영·분양·휴무 순`() {
        assertEquals(
            listOf(
                uiText(Res.string.today_hours, range("09:00", "18:00")),
                uiText(Res.string.today_adoption, range("13:00", "15:00")),
                uiText(Res.string.today_closed, "일요일")
            ),
            info.todayParts(wednesday)
        )
    }

    @Test
    fun `주말은 주말 값, 빠진 분양 시간은 뺀다`() {
        assertEquals(
            listOf(
                uiText(Res.string.today_hours, range("10:00", "17:00")),
                uiText(Res.string.today_closed, "일요일")
            ),
            info.todayParts(saturday)
        )
    }

    @Test
    fun `00시~24시는 24시간`() {
        val allDay = info.copy(weekdayHours = TimeRange("00:00", "24:00"))
        assertEquals(
            uiText(Res.string.today_hours, uiText(Res.string.hours_all_day)),
            allDay.todayParts(wednesday).first()
        )
    }

    @Test
    fun `값이 하나도 없으면 빈 목록`() {
        val empty = info.copy(
            weekdayHours = null,
            weekdayAdoptionHours = null,
            weekendHours = null,
            closedDay = "  "
        )
        assertTrue(empty.todayParts(wednesday).isEmpty())
        assertTrue(empty.todayParts(saturday).isEmpty())
    }
}
