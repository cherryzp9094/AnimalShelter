package com.cherryzp.animalshelter.ui.common

import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.fake.testAnimal
import com.cherryzp.animalshelter.model.ListLayout
import com.cherryzp.animalshelter.model.Sex
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

/** 카드 글자 구성(중간 검토 F1): 1행 품종 / 2행 성별 · 나이 · 체중 / 3행 보호소 / (1열) 4행 색상 · 특징 · 5행 발견 장소. */
class AnimalCardMappingTest {
    private val today = LocalDate(2026, 11, 4)

    private fun card(animal: com.cherryzp.animalshelter.model.Animal) =
        animal.toCardUi(today, ApiEndpoint.Fixture, isBookmarked = false)

    @Test
    fun `1행은 품종, 2행은 성별·나이·체중 순`() {
        val ui = card(testAnimal(kindName = " 한국 고양이 ", sex = Sex.MALE, birthYear = 2024))
        assertEquals("한국 고양이", ui.breed)
        assertEquals(
            listOf(
                uiText(Res.string.sex_male),
                uiText(Res.string.age_year, 2024),
                uiText(Res.string.weight_kg, "5")
            ),
            ui.facts
        )
        assertEquals("제주특별자치도 · 제2동물보호센터", ui.shelterLine)
    }

    @Test
    fun `2행은 60일 미만이면 나이 뒤에 덧붙이고 없는 부분은 뺀다`() {
        val baby = card(
            testAnimal(sex = Sex.FEMALE, birthYear = 2026, isUnderTwoMonths = true)
                .copy(weightKg = null)
        )
        assertEquals(
            listOf(
                uiText(Res.string.sex_female),
                uiText(Res.string.age_year, 2026),
                uiText(Res.string.age_under_two_months)
            ),
            baby.facts
        )
        val bare =
            card(testAnimal(kindName = null, sex = null, birthYear = null).copy(weightKg = null))
        assertNull(bare.breed)
        assertEquals(emptyList(), bare.facts)
    }

    @Test
    fun `2열은 품종·사실·보호소까지, 색상·특징·발견 장소 없음`() {
        val ui = card(testAnimal())
        assertEquals(
            listOf(CardRow.BREED, CardRow.FACTS, CardRow.SHELTER),
            ui.visibleRows(ListLayout.GRID)
        )
    }

    @Test
    fun `1열은 4행 색상·특징과 5행 발견 장소를 더한다`() {
        val ui = card(testAnimal())
        assertEquals(
            listOf(
                CardRow.BREED,
                CardRow.FACTS,
                CardRow.SHELTER,
                CardRow.APPEARANCE,
                CardRow.HAPPEN_PLACE
            ),
            ui.visibleRows(ListLayout.LARGE)
        )
        assertEquals(
            listOf(UiText.Raw("기타(고등어)"), UiText.Raw("목에 플라스틱통 끼임")),
            ui.appearance
        )
    }

    @Test
    fun `빈 행은 두 형태 모두 뺀다`() {
        val ui = card(
            testAnimal(kindName = null).copy(color = null, specialMark = ".", happenPlace = " ")
        )
        assertEquals(listOf(CardRow.FACTS, CardRow.SHELTER), ui.visibleRows(ListLayout.LARGE))
        assertEquals(listOf(CardRow.FACTS, CardRow.SHELTER), ui.visibleRows(ListLayout.GRID))
    }

    @Test
    fun `접근성 제목 부분은 품종·성별·나이 그대로(체중 없음)`() {
        val ui = card(testAnimal())
        assertEquals(testAnimal().titleParts(), ui.titleParts)
        assertEquals(3, ui.titleParts.size)
    }
}
