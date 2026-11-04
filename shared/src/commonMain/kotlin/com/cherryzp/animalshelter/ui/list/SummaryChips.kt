package com.cherryzp.animalshelter.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cherryzp.animalshelter.model.AnimalKind
import com.cherryzp.animalshelter.model.NoticeState
import com.cherryzp.animalshelter.model.SearchFilter
import com.cherryzp.animalshelter.model.SortOrder
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.a11y_sort_to_latest
import com.cherryzp.animalshelter.resources.date_default
import com.cherryzp.animalshelter.resources.date_last_30
import com.cherryzp.animalshelter.resources.date_last_7
import com.cherryzp.animalshelter.resources.date_range
import com.cherryzp.animalshelter.resources.empty_change_filter
import com.cherryzp.animalshelter.resources.filter_none
import com.cherryzp.animalshelter.resources.kind_cat
import com.cherryzp.animalshelter.resources.kind_dog
import com.cherryzp.animalshelter.resources.kind_other
import com.cherryzp.animalshelter.resources.sort_closing_soon
import com.cherryzp.animalshelter.resources.state_notice
import com.cherryzp.animalshelter.resources.state_protect
import com.cherryzp.animalshelter.ui.common.QuickDate
import com.cherryzp.animalshelter.ui.common.TestTags
import com.cherryzp.animalshelter.ui.common.UiText
import com.cherryzp.animalshelter.ui.common.formatShortDate
import com.cherryzp.animalshelter.ui.common.quickDateOf
import com.cherryzp.animalshelter.ui.common.resolve
import com.cherryzp.animalshelter.ui.common.toUiText
import com.cherryzp.animalshelter.ui.common.uiText
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** 축종 문구(칩). */
fun AnimalKind.chipText(): UiText = when (this) {
    AnimalKind.DOG -> uiText(Res.string.kind_dog)
    AnimalKind.CAT -> uiText(Res.string.kind_cat)
    AnimalKind.OTHER -> uiText(Res.string.kind_other)
}

/**
 * 요약 칩(브리핑 A7): 있는 조건만, 순서 축종 · 품종 · 지역 · 상태 · 구조 날짜 · 성별 · 중성화 · 정렬.
 * 날짜 칩은 늘 있다(비면 "최근 1개월"). 축종·품종·지역·상태·성별·중성화가 하나도 없으면 맨 앞에 "전체".
 * 마감 임박순이면 상태 칩은 "공고중"(조회가 공고중으로 고정된다)이고 끝에 "마감 임박순"(누르면 최신순).
 * 이름은 [SearchFilter]의 표시 이름만 쓴다 — 코드표를 부르지 않는다(03 4절 1번).
 */
fun summaryChips(filter: SearchFilter, sort: SortOrder, today: LocalDate): List<SummaryChip> {
    val closingSoon = sort == SortOrder.CLOSING_SOON
    val conditions = buildList {
        filter.kind?.let { add(it.chipText()) }
        if (filter.kindCode != null) filter.kindName?.let { add(UiText.Raw(it)) }
        regionText(filter)?.let { add(UiText.Raw(it)) }
        val state = if (closingSoon) NoticeState.NOTICE else filter.state
        when (state) {
            NoticeState.NOTICE -> add(uiText(Res.string.state_notice))
            NoticeState.PROTECT -> add(uiText(Res.string.state_protect))
            NoticeState.ALL -> Unit
        }
    }
    val tail = buildList {
        filter.sex?.let { add(it.toUiText()) }
        filter.neutered?.let { add(it.toUiText()) }
    }
    return buildList {
        if (conditions.isEmpty() &&
            tail.isEmpty()
        ) {
            add(SummaryChip(uiText(Res.string.filter_none), ChipTarget.FILTER))
        }
        conditions.forEach { add(SummaryChip(it, ChipTarget.FILTER)) }
        add(SummaryChip(dateText(filter, today), ChipTarget.FILTER))
        tail.forEach { add(SummaryChip(it, ChipTarget.FILTER)) }
        if (closingSoon) {
            add(
                SummaryChip(uiText(Res.string.sort_closing_soon), ChipTarget.SORT_LATEST)
            )
        }
    }
}

/** 지역 칩: 보호소를 골랐으면 그 이름, 아니면 "시도 시군구". */
private fun regionText(filter: SearchFilter): String? {
    if (filter.shelterRegNo != null) filter.shelterName?.let { return it }
    val names = listOfNotNull(
        filter.sidoName.takeIf { filter.sidoCode != null },
        filter.sigunguName.takeIf { filter.sigunguCode != null }
    )
    return names.joinToString(" ").ifEmpty { null }
}

private fun dateText(filter: SearchFilter, today: LocalDate): UiText {
    val from = filter.dateFrom
    val to = filter.dateTo
    if (from == null && to == null) return uiText(Res.string.date_default)
    return when (quickDateOf(from, to, today)) {
        QuickDate.LAST_7 -> uiText(Res.string.date_last_7)

        QuickDate.LAST_30 -> uiText(Res.string.date_last_30)

        else -> uiText(
            Res.string.date_range,
            from?.let { formatShortDate(it, today) }.orEmpty(),
            to?.let { formatShortDate(it, today) }.orEmpty()
        )
    }
}

/** 칩을 누르면 되는 일 — 접근성 동작 이름(R4). */
fun ChipTarget.actionLabel(): StringResource = when (this) {
    ChipTarget.FILTER -> Res.string.empty_change_filter
    ChipTarget.SORT_LATEST -> Res.string.a11y_sort_to_latest
}

/** 요약 칩 줄(`FlowRow`, `AssistChip`). 칩마다 클릭 동작 이름([actionLabel]). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SummaryChipRow(
    chips: List<SummaryChip>,
    onChipClick: (SummaryChip) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.testTag(TestTags.SUMMARY_CHIPS),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        chips.forEach { chip ->
            // 클릭 동작 이름(R4): "고양이, 버튼"만으로는 누르면 무엇이 되는지 모른다. AssistChip은 이름 인자가 없어
            // 의미로 준다 — 같은 노드의 OnClick을 이름 있는 같은 동작으로 덮어써 동작은 하나다.
            val actionLabel = stringResource(chip.target.actionLabel())
            AssistChip(
                onClick = { onChipClick(chip) },
                label = { Text(chip.label.resolve()) },
                modifier = Modifier.semantics {
                    onClick(label = actionLabel) {
                        onChipClick(chip)
                        true
                    }
                }
            )
        }
    }
}
