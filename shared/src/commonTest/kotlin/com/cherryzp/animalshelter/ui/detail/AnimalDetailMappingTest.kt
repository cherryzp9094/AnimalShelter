package com.cherryzp.animalshelter.ui.detail

import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.fake.testAnimal
import com.cherryzp.animalshelter.model.Neutered
import com.cherryzp.animalshelter.model.Sex
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.choice_no
import com.cherryzp.animalshelter.resources.choice_unknown
import com.cherryzp.animalshelter.resources.choice_yes
import com.cherryzp.animalshelter.resources.label_neuter
import com.cherryzp.animalshelter.resources.label_sex
import com.cherryzp.animalshelter.resources.sex_female
import com.cherryzp.animalshelter.resources.sex_male
import com.cherryzp.animalshelter.ui.common.UiText
import com.cherryzp.animalshelter.ui.common.uiText
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
