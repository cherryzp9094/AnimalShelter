package com.cherryzp.animalshelter.ui.common

import com.cherryzp.animalshelter.model.AnimalStatus
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.a11y_days_left
import com.cherryzp.animalshelter.resources.a11y_ends_today
import com.cherryzp.animalshelter.resources.badge_closing_soon
import com.cherryzp.animalshelter.resources.dday
import com.cherryzp.animalshelter.resources.dday_today
import com.cherryzp.animalshelter.resources.status_ended
import com.cherryzp.animalshelter.resources.status_ended_reason
import com.cherryzp.animalshelter.resources.status_protecting
import com.cherryzp.animalshelter.resources.status_unknown
import kotlin.test.Test
import kotlin.test.assertEquals

/** 상태 배지(브리핑 P9): D-4 / D-3 / D-1 / 오늘 / 보호중 / 종료 사유 있음·없음 / 미상. */
class StatusUiTest {
    @Test
    fun `공고중 D-4 이상은 D-n과 공고중 색`() {
        val ui = AnimalStatus.Noticing(4).toStatusUi()
        assertEquals(StatusKind.NOTICING, ui.kind)
        assertEquals(uiText(Res.string.dday, 4), ui.label)
        assertEquals(uiText(Res.string.a11y_days_left, 4), ui.a11y)
    }

    @Test
    fun `D-3과 D-1은 마감 임박`() {
        for (days in listOf(3, 1)) {
            val ui = AnimalStatus.Noticing(days).toStatusUi()
            assertEquals(StatusKind.CLOSING_SOON, ui.kind)
            assertEquals(uiText(Res.string.badge_closing_soon, days), ui.label)
            assertEquals(uiText(Res.string.a11y_days_left, days), ui.a11y)
        }
    }

    @Test
    fun `오늘 마감은 마감 임박 색에 오늘 마감`() {
        val ui = AnimalStatus.Noticing(0).toStatusUi()
        assertEquals(StatusKind.CLOSING_SOON, ui.kind)
        assertEquals(uiText(Res.string.dday_today), ui.label)
        assertEquals(uiText(Res.string.a11y_ends_today), ui.a11y)
    }

    @Test
    fun `보호중`() {
        val ui = AnimalStatus.Protecting.toStatusUi()
        assertEquals(StatusKind.PROTECTING, ui.kind)
        assertEquals(uiText(Res.string.status_protecting), ui.label)
    }

    @Test
    fun `종료는 사유 원문, 없으면 종료`() {
        val withReason = AnimalStatus.Ended("반환").toStatusUi()
        assertEquals(StatusKind.ENDED, withReason.kind)
        assertEquals(uiText(Res.string.status_ended_reason, "반환"), withReason.label)
        assertEquals(uiText(Res.string.status_ended), AnimalStatus.Ended(null).toStatusUi().label)
    }

    @Test
    fun `미상은 원문 또는 상태 미상`() {
        assertEquals(UiText.Raw("임시보호"), AnimalStatus.Unknown("임시보호").toStatusUi().label)
        val unknown = AnimalStatus.Unknown(null).toStatusUi()
        assertEquals(StatusKind.UNKNOWN, unknown.kind)
        assertEquals(uiText(Res.string.status_unknown), unknown.label)
    }
}
