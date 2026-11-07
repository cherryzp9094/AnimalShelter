package com.cherryzp.animalshelter.model

import com.cherryzp.animalshelter.fake.testAnimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

/** 상태 판정 네 갈래(02-data 5.2)와 서울 시간(D2). */
class AnimalStatusTest {
    private val today = LocalDate(2026, 11, 4)

    @Test
    fun `보호중이고 공고 종료일이 오늘 이후면 공고중과 남은 날`() {
        assertEquals(
            AnimalStatus.Noticing(9),
            animalStatusOf("보호중", LocalDate(2026, 11, 13), today)
        )
    }

    @Test
    fun `공고 종료일이 오늘이면 Noticing(0)`() {
        assertEquals(AnimalStatus.Noticing(0), animalStatusOf("보호중", today, today))
    }

    @Test
    fun `공고 기간이 지난 보호중은 Protecting`() {
        assertEquals(
            AnimalStatus.Protecting,
            animalStatusOf("보호중", LocalDate(2026, 11, 3), today)
        )
        assertEquals(AnimalStatus.Protecting, animalStatusOf("보호중", null, today))
    }

    @Test
    fun `종료는 괄호 안 원문을 사유로`() {
        assertEquals(AnimalStatus.Ended("반환"), animalStatusOf("종료(반환)", today, today))
        assertEquals(AnimalStatus.Ended("자연사"), animalStatusOf("종료(자연사)", null, today))
        assertEquals(AnimalStatus.Ended(null), animalStatusOf("종료", null, today))
    }

    @Test
    fun `빈 값과 모르는 값은 Unknown`() {
        assertEquals(AnimalStatus.Unknown(null), animalStatusOf("", today, today))
        assertEquals(AnimalStatus.Unknown(null), animalStatusOf(null, today, today))
        assertEquals(AnimalStatus.Unknown("공고중"), animalStatusOf("공고중", today, today))
    }

    @Test
    fun `사망 종료 판정 — 경계값 표(5d H1)`() {
        val hidden = listOf(
            "종료(자연사)",
            "종료(안락사)",
            " 종료(자연사) ",
            "종료( 안락사 )",
            "종료(자연사"
        )
        val shown = listOf(
            "종료(반환)",
            "종료(입양)",
            "종료(기증)",
            "종료(방사)",
            "종료",
            "종료()",
            "종료( )",
            "종료(폐사)",
            "종료(사망)",
            "자연사",
            "보호중",
            "",
            "  ",
            null
        )
        hidden.forEach { assertTrue(isEndedByDeath(it), "숨겨야 한다: [$it]") }
        shown.forEach { assertFalse(isEndedByDeath(it), "보여야 한다: [$it]") }
        // 배지와 같은 해석: 숨기는 값은 배지가 "종료 · 자연사/안락사"로 읽는 값이다
        hidden.forEach {
            val reason = (animalStatusOf(it, null, today) as AnimalStatus.Ended).reason
            assertTrue(reason in DEATH_END_REASONS, "배지 사유: [$it] → $reason")
        }
        assertTrue(testAnimal(processState = "종료(안락사)").isEndedByDeath())
        assertFalse(testAnimal(processState = "종료(입양)").isEndedByDeath())
    }

    @Test
    fun `서울 오늘은 UTC+9 기준이다`() {
        // 2026-11-03 15:30 UTC = 2026-11-04 00:30 KST
        val clock = object : Clock {
            override fun now(): Instant = Instant.parse("2026-11-03T15:30:00Z")
        }
        assertEquals(LocalDate(2026, 11, 4), clock.todayInSeoul())
    }
}
