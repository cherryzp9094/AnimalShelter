package com.cherryzp.animalshelter.ui.flow

import androidx.compose.ui.test.ExperimentalTestApi
import com.cherryzp.animalshelter.model.ListLayout
import kotlin.test.Test

/**
 * UI 흐름 테스트 래퍼(브리핑 C6). 본문은 commonTest의 `Flows.kt`. 이름에 공백을 쓰지 않는다 — 기기 테스트 APK의
 * D8이 DEX 040(API 30) 미만에서 공백 이름을 받지 않는다(minSdk 26).
 */
@OptIn(ExperimentalTestApi::class)
class UiFlowTest {
    /** T1 목록 → 상세 → 북마크 추가 → 북마크 목록에 그 카드. */
    @Test
    fun t1ListDetailBookmarkThenBookmarkList() = runFlowTest { listToBookmarksFlow(it) }

    /** T2 필터 고양이 + 공고중 → 목록 칩과 마지막 검색 조건. */
    @Test
    fun t2FilterCatNoticeThenChipsAndQuery() = runFlowTest { filterFlow(it) }

    /** T3 2열 가운데 카드 북마크 되돌리기는 같은 자리. */
    @Test
    fun t3GridUndoRestoresSamePlace() = runFlowTest { bookmarkUndoFlow(it, ListLayout.GRID) }

    /** T3 1열 첫 카드 되돌리기는 같은 자리(화면 밖 복원 회귀). */
    @Test
    fun t3LargeFirstCardUndoRestoresSamePlace() = runFlowTest {
        bookmarkUndoFlow(it, ListLayout.LARGE)
    }
}
