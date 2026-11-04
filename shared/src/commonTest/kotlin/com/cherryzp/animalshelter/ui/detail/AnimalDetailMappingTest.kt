package com.cherryzp.animalshelter.ui.detail

import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.fake.testAnimal
import com.cherryzp.animalshelter.model.Animal
import com.cherryzp.animalshelter.model.Neutered
import com.cherryzp.animalshelter.model.Promotion
import com.cherryzp.animalshelter.model.PromotionType
import com.cherryzp.animalshelter.model.Sex
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.choice_no
import com.cherryzp.animalshelter.resources.choice_unknown
import com.cherryzp.animalshelter.resources.choice_yes
import com.cherryzp.animalshelter.resources.label_happen_place
import com.cherryzp.animalshelter.resources.label_neuter
import com.cherryzp.animalshelter.resources.label_sex
import com.cherryzp.animalshelter.resources.label_weight
import com.cherryzp.animalshelter.resources.promo_period
import com.cherryzp.animalshelter.resources.sex_female
import com.cherryzp.animalshelter.resources.sex_male
import com.cherryzp.animalshelter.resources.weight_kg
import com.cherryzp.animalshelter.resources.weight_kg_range
import com.cherryzp.animalshelter.ui.common.UiText
import com.cherryzp.animalshelter.ui.common.uiText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate

/** 상세 매핑(중간 검토 F2·F8·F9). */
class AnimalDetailMappingTest {
    private val today = LocalDate(2026, 11, 4)

    private fun summaryValue(sex: Sex?, neutered: Neutered?, label: UiText): UiText? =
        testAnimal(sex = sex).copy(neutered = neutered).toDetailUi(today, ApiEndpoint.Fixture)
            .summary.firstOrNull { it.label == label }?.values?.single()

    @Test
    fun `상세 성별 값은 라벨 낱말을 되풀이하지 않는다`() {
        val label = uiText(Res.string.label_sex)
        assertEquals(uiText(Res.string.sex_male), summaryValue(Sex.MALE, null, label))
        assertEquals(uiText(Res.string.sex_female), summaryValue(Sex.FEMALE, null, label))
        assertEquals(uiText(Res.string.choice_unknown), summaryValue(Sex.UNKNOWN, null, label))
    }

    @Test
    fun `상세 중성화 값은 예·아니요·미상`() {
        val label = uiText(Res.string.label_neuter)
        assertEquals(uiText(Res.string.choice_yes), summaryValue(null, Neutered.YES, label))
        assertEquals(uiText(Res.string.choice_no), summaryValue(null, Neutered.NO, label))
        assertEquals(uiText(Res.string.choice_unknown), summaryValue(null, Neutered.UNKNOWN, label))
    }

    @Test
    fun `상세의 보호소 주소·발견 장소는 연속 공백을 하나로(F9)`() {
        val animal = testAnimal().let {
            it.copy(
                happenPlace = "세종  전동면",
                shelter = it.shelter.copy(address = " 세종특별자치시  전동면   미곡리 ")
            )
        }
        val ui = animal.toDetailUi(today, ApiEndpoint.Fixture)
        assertEquals("세종특별자치시 전동면 미곡리", ui.shelter?.address)
        assertEquals(
            listOf(UiText.Raw("세종 전동면")),
            ui.found.single { it.label == uiText(Res.string.label_happen_place) }.values
        )
    }

    private fun weightRow(animal: Animal) = animal.toDetailUi(today, ApiEndpoint.Fixture).summary
        .firstOrNull { it.label == uiText(Res.string.label_weight) }

    @Test
    fun `읽을 수 없는 체중은 상세에서도 행이 없다(L1)`() {
        assertNull(weightRow(testAnimal().copy(weightText = "400(Kg)", weightKg = null)))
        assertNull(weightRow(testAnimal().copy(weightText = "0(Kg)", weightKg = null)))
    }

    @Test
    fun `상세 체중 범위형은 앞~뒤(L1)`() {
        assertEquals(
            listOf(uiText(Res.string.weight_kg_range, "2", "2.5")),
            weightRow(
                testAnimal().copy(weightText = "2~2.5(Kg)", weightKg = 2.0, weightMaxKg = 2.5)
            )
                ?.values
        )
        assertEquals(
            listOf(uiText(Res.string.weight_kg, "5")),
            weightRow(testAnimal())?.values
        )
    }

    private fun promoPeriod(start: String?, end: String?): UiText? = testAnimal().copy(
        promotions = listOf(Promotion(PromotionType.ADOPTION_PROCESS, start = start, end = end))
    ).toDetailUi(today, ApiEndpoint.Fixture).promotions.single().period

    @Test
    fun `홍보 기간 YYYYMMDD는 날짜 형식으로(L3)`() {
        assertEquals(
            uiText(Res.string.promo_period, "2026-01-01", "2027-03-15"),
            promoPeriod("20260101", "20270315")
        )
        assertEquals(UiText.Raw("2026-01-01"), promoPeriod("20260101", null))
        assertEquals(UiText.Raw("상시"), promoPeriod("상시", null))
        assertEquals(
            uiText(Res.string.promo_period, "2026-01-01", "2026-12-31"),
            promoPeriod("2026-01-01", "20261231")
        )
    }
}
