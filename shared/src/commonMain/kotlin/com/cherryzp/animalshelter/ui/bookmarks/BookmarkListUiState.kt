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
 * 되살린 카드([restoredKey])가 화면 밖이면 스크롤할 위치(F4). `LazyVerticalGrid`는 첫 보이는 항목의 key로 위치를
 * 지키므로 그보다 앞에 끼운 카드는 화면 위로 숨는다. [firstVisibleKey]·[lastVisibleKey]는 마지막 배치의 보이는 처음·끝
 * 항목 key. 되살린 카드가 그 사이(새 목록 기준)면 null — 이미 보인다. 목록에 아직 없으면 null.
 */
fun restoreScrollTarget(
    keys: List<String>,
    restoredKey: String,
    firstVisibleKey: Any?,
    lastVisibleKey: Any?,
): Int? {
    val index = keys.indexOf(restoredKey)
    if (index < 0) return null
    val first = keys.indexOf(firstVisibleKey)
    val last = keys.indexOf(lastVisibleKey)
    return when {
        first < 0 || last < 0 -> index
        index < first || index > last -> index
        else -> null
    }
}
