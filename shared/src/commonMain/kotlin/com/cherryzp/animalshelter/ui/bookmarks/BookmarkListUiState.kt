package com.cherryzp.animalshelter.ui.bookmarks

import androidx.compose.runtime.Immutable
import com.cherryzp.animalshelter.model.ListLayout
import com.cherryzp.animalshelter.ui.common.AnimalCardUi
import com.cherryzp.animalshelter.ui.common.UiMessage

/**
 * 북마크 목록 상태(브리핑 3b 3절·B10).
 * @property items 추가 역순, 배지는 오늘 기준으로 다시 판정
 * @property isEmpty 받은 뒤 0건(빈 상태 문구). 받기 전에는 false
 */
@Immutable
data class BookmarkListUiState(
    val items: List<AnimalCardUi> = emptyList(),
    val layout: ListLayout = ListLayout.GRID,
    val isEmpty: Boolean = false,
    val message: UiMessage? = null,
)
