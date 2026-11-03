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
 * @property queryKey 보이는 결과의 조건·정렬. 바뀌면 화면이 그리드를 맨 위로 올린다(새 결과가 옛 스크롤 위치에서
 *   시작해 자동 다음 페이지를 부르지 않게). 프로세스 종료 뒤에도 같도록 `toString()`으로 만든다
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
    val footer: ListFooter = ListFooter.None,
    val fullScreen: FullScreenState? = null,
    val message: UiMessage? = null,
    val queryKey: String = "",
)

/** 요약 칩 하나. 누르면 [target]. */
@Immutable
data class SummaryChip(val label: UiText, val target: ChipTarget)

/** 칩을 누르면: 필터 화면 / 최신순으로. */
enum class ChipTarget { FILTER, SORT_LATEST }

/** 새로고침 표시: 당김 표시(PULL) / 상단 선(BAR). */
enum class RefreshIndicator { PULL, BAR }

/** 그리드 끝 행: 없음 / 다음 페이지 받는 중 / 마지막 / 다음 페이지 실패. */
@Immutable
sealed interface ListFooter {
    data object None : ListFooter

    data object Loading : ListFooter

    data object End : ListFooter

    /**
     * 다음 페이지 실패(중간 검토 F7). 다시 시도할 수 있으면 `list_load_more_failed` + 버튼, 없으면(하루 한도 등)
     * 버튼 없이 그 오류 문구([text], P5).
     */
    data class Failed(val text: UiText, val canRetry: Boolean) : ListFooter
}

/** 결과가 비었을 때의 한 화면. */
@Immutable
sealed interface FullScreenState {
    /** 오류 없이 0건 → 조건 바꾸기. */
    data object Empty : FullScreenState

    /** 오류 → 문구 + (가능하면) 다시 시도 + 북마크 보기. */
    data class Error(val text: UiText, val canRetry: Boolean) : FullScreenState
}
