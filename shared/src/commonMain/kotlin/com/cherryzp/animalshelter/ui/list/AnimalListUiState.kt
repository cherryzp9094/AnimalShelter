package com.cherryzp.animalshelter.ui.list

import androidx.compose.runtime.Immutable
import com.cherryzp.animalshelter.model.ListLayout
import com.cherryzp.animalshelter.model.SortOrder
import com.cherryzp.animalshelter.ui.common.AnimalCardUi
import com.cherryzp.animalshelter.ui.common.UiMessage
import com.cherryzp.animalshelter.ui.common.UiText

/**
 * 목록 화면 상태(브리핑 3a 5절).
 * @property sort 보이는 정렬
 * @property pendingSort 전환 중인 목표(정렬 메뉴의 체크) — 없으면 null
 * @property header "전체 n마리"/"공고중 n마리". 모르면 null
 * @property fullScreen 결과가 비었을 때만(빈 결과·오류)
 * @property message 스낵바 한 번(보여 준 뒤 `onMessageShown()`)
 */
@Immutable
data class AnimalListUiState(
    val chips: List<SummaryChip> = emptyList(),
    val sort: SortOrder = SortOrder.LATEST,
    val pendingSort: SortOrder? = null,
    val layout: ListLayout = ListLayout.GRID,
    val header: UiText? = null,
    val items: List<AnimalCardUi> = emptyList(),
    val isInitialLoading: Boolean = false,
    val refreshIndicator: RefreshIndicator? = null,
    val footer: ListFooter = ListFooter.NONE,
    val fullScreen: FullScreenState? = null,
    val message: UiMessage? = null,
)

/** 요약 칩 하나. 누르면 [target]. */
@Immutable
data class SummaryChip(val label: UiText, val target: ChipTarget)

/** 칩을 누르면: 필터 화면 / 최신순으로. */
enum class ChipTarget { FILTER, SORT_LATEST }

/** 새로고침 표시: 당김 표시(PULL) / 상단 선(BAR). */
enum class RefreshIndicator { PULL, BAR }

/** 그리드 끝 행: 없음 / 다음 페이지 받는 중 / 마지막 / 다음 페이지 실패(다시 시도). */
enum class ListFooter { NONE, LOADING, END, RETRY }

/** 결과가 비었을 때의 한 화면. */
@Immutable
sealed interface FullScreenState {
    /** 오류 없이 0건 → 조건 바꾸기. */
    data object Empty : FullScreenState

    /** 오류 → 문구 + (가능하면) 다시 시도 + 북마크 보기. */
    data class Error(val text: UiText, val canRetry: Boolean) : FullScreenState
}
