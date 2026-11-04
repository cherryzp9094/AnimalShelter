package com.cherryzp.animalshelter.ui.common

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.window.core.layout.WindowSizeClass

/**
 * 목록·북마크 목록 상단 바의 스크롤 동작(3e 검토 N4, Android skills `adaptive` Step 5).
 *
 * 창 높이가 compact(폰 가로 — 그리드의 `Adaptive(156)` 규칙과 같은 판정)면 `enterAlways`(내리면 숨고 올리면 바로
 * 나온다), 아니면 지금처럼 pinned. 두 갈래는 각자 기본 `rememberTopAppBarState()`를 써서 상태를 같이 쓰지 않는다 —
 * 가로에서 접힌 높이가 세로의 pinned 바에 남지 않는다. 그리드가 어느 쪽으로도 스크롤할 수 없으면(결과가 적을 때)
 * 바를 움직이지 않는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun listTopBarScrollBehavior(gridState: LazyGridState): TopAppBarScrollBehavior {
    val heightCompact = !currentWindowAdaptiveInfoV2().windowSizeClass
        .isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
    return if (heightCompact) {
        val canScroll = remember(gridState) {
            { gridState.canScrollForward || gridState.canScrollBackward }
        }
        TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = canScroll)
    } else {
        TopAppBarDefaults.pinnedScrollBehavior()
    }
}
