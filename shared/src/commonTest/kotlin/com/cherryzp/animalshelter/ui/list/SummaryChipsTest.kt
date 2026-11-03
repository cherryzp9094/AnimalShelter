package com.cherryzp.animalshelter.ui.list

import com.cherryzp.animalshelter.model.AnimalKind
import com.cherryzp.animalshelter.model.Neutered
import com.cherryzp.animalshelter.model.NoticeState
import com.cherryzp.animalshelter.model.SearchFilter
import com.cherryzp.animalshelter.model.Sex
import com.cherryzp.animalshelter.model.SortOrder
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.date_last_7
import com.cherryzp.animalshelter.resources.date_range
import com.cherryzp.animalshelter.resources.kind_cat
import com.cherryzp.animalshelter.resources.neuter_yes
import com.cherryzp.animalshelter.resources.sex_female
import com.cherryzp.animalshelter.resources.sort_closing_soon
import com.cherryzp.animalshelter.resources.state_notice
import com.cherryzp.animalshelter.resources.state_protect
import com.cherryzp.animalshelter.ui.common.UiText
import com.cherryzp.animalshelter.ui.common.uiText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate

/** 요약 칩(브리핑 A7): 순서, 마감 임박순의 공고중 고정·정렬 칩. */
class SummaryChipsTest {
    private val today = LocalDate(2026, 11, 4)

    private val full = SearchFilter(
        kind = AnimalKind.CAT,
        kindCode = "000116",
        kindName = "한국 고양이",
        sidoCode = "6110000",
        sidoName = "서울특별시",
        sigunguCode = "3220000",
        sigunguName = "강남구",
        state = NoticeState.PROTECT,
        dateFrom = LocalDate(2026, 10, 29),
        dateTo = today,
        sex = Sex.FEMALE,
        neutered = Neutered.YES
    )

    @Test
    fun `있는 조건만 축종·품종·지역·상태·날짜·성별·중성화 순`() {
        val chips = summaryChips(full, SortOrder.LATEST, today)

        assertEquals(
            listOf(
                uiText(Res.string.kind_cat),
                UiText.Raw("한국 고양이"),
                UiText.Raw("서울특별시 강남구"),
                uiText(Res.string.state_protect),
                uiText(Res.string.date_last_7),
                uiText(Res.string.sex_female),
                uiText(Res.string.neuter_yes)
            ),
            chips.map { it.label }
        )
        assertEquals(setOf(ChipTarget.FILTER), chips.map { it.target }.toSet())
    }

    @Test
    fun `마감 임박순이면 상태는 공고중으로 보이고 끝에 정렬 칩`() {
        val chips = summaryChips(SearchFilter(), SortOrder.CLOSING_SOON, today)

        assertEquals(uiText(Res.string.state_notice), chips.first().label)
        assertEquals(
            SummaryChip(uiText(Res.string.sort_closing_soon), ChipTarget.SORT_LATEST),
            chips.last()
        )
        assertEquals(3, chips.size) // 공고중 · 최근 1개월 · 마감 임박순("전체" 없음)
    }

    @Test
    fun `보호소를 골랐으면 지역 칩은 보호소 이름, 그 밖의 날짜는 범위`() {
        val filter = SearchFilter(
            sidoCode = "6110000",
            sidoName = "서울특별시",
            shelterRegNo = "311322200900001",
            shelterName = "서울동물복지지원센터",
            dateFrom = LocalDate(2026, 10, 1),
            dateTo = LocalDate(2026, 10, 15)
        )
        val labels = summaryChips(filter, SortOrder.LATEST, today).map { it.label }

        assertEquals(
            listOf(UiText.Raw("서울동물복지지원센터"), uiText(Res.string.date_range, "10-01", "10-15")),
            labels
        )
    }
}
