package com.cherryzp.animalshelter.ui.common

import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.fake.testAnimal
import com.cherryzp.animalshelter.model.Sex
import com.cherryzp.animalshelter.model.ShelterSummary
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.age_under_two_months
import com.cherryzp.animalshelter.resources.age_year
import com.cherryzp.animalshelter.resources.sex_female
import com.cherryzp.animalshelter.resources.sex_male
import com.cherryzp.animalshelter.resources.weight_kg
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate

/** 카드 문구·숫자 서식(순수 함수). */
class AnimalTextTest {
    @Test
    fun `마리 수는 세 자리마다 쉼표`() {
        assertEquals("0", formatCount(0))
        assertEquals("999", formatCount(999))
        assertEquals("7,544", formatCount(7544))
        assertEquals("1,234,567", formatCount(1_234_567))
    }

    @Test
    fun `체중은 끝의 0을 뺀다`() {
        assertEquals("2.9", formatWeight(2.9))
        assertEquals("5", formatWeight(5.0))
        assertEquals("0.25", formatWeight(0.25))
        assertEquals("12.5", formatWeight(12.50))
        assertEquals(
            uiText(Res.string.weight_kg, "2.9"),
            testAnimal().copy(
                weightKg = 2.9,
                color = null,
                specialMark = null
            ).traitParts().single()
        )
    }

    @Test
    fun `날짜는 올해면 월-일, 아니면 연도까지`() {
        val today = LocalDate(2026, 11, 4)
        assertEquals("10-28", formatShortDate(LocalDate(2026, 10, 28), today))
        assertEquals("2025-12-31", formatShortDate(LocalDate(2025, 12, 31), today))
    }

    @Test
    fun `제목은 품종·성별·나이 순이고 60일 미만이면 덧붙인다`() {
        val title = testAnimal(kindName = "한국 고양이", sex = Sex.MALE, birthYear = 2024).titleParts()
        assertEquals(
            listOf(
                UiText.Raw("한국 고양이"),
                uiText(Res.string.sex_male),
                uiText(Res.string.age_year, 2024)
            ),
            title
        )

        val baby = testAnimal(
            sex = Sex.FEMALE,
            birthYear = 2026,
            isUnderTwoMonths = true
        ).titleParts()
        assertEquals(uiText(Res.string.sex_female), baby[1])
        assertEquals(uiText(Res.string.age_under_two_months), baby.last())
    }

    @Test
    fun `제목은 없는 부분을 빼고 빈 칸을 만들지 않는다`() {
        assertEquals(
            listOf(uiText(Res.string.age_year, 2024)),
            testAnimal(kindName = " ", sex = null, birthYear = 2024).titleParts()
        )
        assertEquals(
            emptyList(),
            testAnimal(kindName = null, sex = null, birthYear = null).titleParts()
        )
    }

    @Test
    fun `특징은 색상·체중·특징 중 있는 것만`() {
        val animal = testAnimal().copy(color = "흰색", weightKg = null, specialMark = "  ")
        assertEquals(listOf(UiText.Raw("흰색")), animal.traitParts())
    }

    @Test
    fun `보호소 줄은 관할 기관 · 보호소`() {
        assertEquals("제주특별자치도 · 제2동물보호센터", testAnimal().shelterLine())
        assertEquals("보호소", testAnimal(shelter = ShelterSummary(name = "보호소")).shelterLine())
        assertNull(testAnimal(shelter = ShelterSummary()).shelterLine())
    }

    @Test
    fun `접근성 문장은 쉼표로 잇고 빈 것은 뺀다`() {
        assertEquals(
            "한국 고양이, 수컷, 2024년생, 제주특별자치도 제2동물보호센터, 공고 3일 남음, 북마크됨",
            cardA11ySentence(
                title = listOf("한국 고양이", "수컷", "2024년생"),
                shelterLine = "제주특별자치도 · 제2동물보호센터",
                status = "공고 3일 남음",
                bookmarked = "북마크됨"
            )
        )
        assertEquals(
            "믹스견, 애월읍, 보호중",
            cardA11ySentence(listOf("믹스견", " "), null, "보호중", null, happenPlace = "애월읍")
        )
    }

    @Test
    fun `카드는 Relay면 사진을 중계 img로, Fixture·Direct면 원본`() {
        val animal = testAnimal()
        val today = LocalDate(2026, 11, 4)
        assertEquals(
            "/img/shelter/2026/10/a.jpg",
            animal.toCardUi(today, ApiEndpoint.Relay(""), isBookmarked = false).photoUrl
        )
        assertEquals(
            animal.photos.first(),
            animal.toCardUi(today, ApiEndpoint.Fixture, false).photoUrl
        )
        assertNull(
            animal.copy(photos = emptyList()).toCardUi(today, ApiEndpoint.Fixture, false).photoUrl
        )
    }
}
