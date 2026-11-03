package com.cherryzp.animalshelter.ui.bookmarks

import androidx.compose.runtime.Immutable
import com.cherryzp.animalshelter.model.ListLayout
import com.cherryzp.animalshelter.ui.common.AnimalCardUi
import com.cherryzp.animalshelter.ui.common.UiMessage

/**
 * 북마크 목록 상태(브리핑 3b 3절·B10).
 * @property items 추가 역순, 배지는 오늘 기준으로 다시 판정
 * @property isEmpty 받은 뒤 0건(빈 상태 문구). 받기 전에는 false
 * @property restoredKey 되돌리기로 방금 되살린 카드(F4). 화면이 그 카드가 보이게 스크롤한 뒤 `onRestoreShown()`
 */
@Immutable
data class BookmarkListUiState(
    val items: List<AnimalCardUi> = emptyList(),
    val layout: ListLayout = ListLayout.GRID,
    val isEmpty: Boolean = false,
    val message: UiMessage? = null,
    val restoredKey: String? = null,
)

/**
 * 되살린 카드([restoredKey])가 보이게 스크롤할 위치(F4). `LazyVerticalGrid`는 첫 보이는 항목의 key로 위치를 지키므로
 * 그보다 앞에 끼운 카드는 화면 위로 숨는다(1열 첫 카드면 늘). [visibleTops]는 마지막 배치에서 보이는 항목 key → 위쪽
 * 오프셋(px, 0 = 위 여백 바로 아래). 위 여백(8dp) 안에 걸친 항목도 "보이는" 목록에 들어오므로 위쪽이 0보다 위면 숨은 것으로
 * 본다. 목록에 없거나 보이는 목록에 없거나 위가 잘렸으면 그 위치, 이미 보이면 null.
 */
fun restoreScrollTarget(keys: List<String>, restoredKey: String, visibleTops: Map<Any, Int>): Int? {
    val index = keys.indexOf(restoredKey)
    if (index < 0) return null
    val top = visibleTops[restoredKey] ?: return index
    return index.takeIf { top < 0 }
}
