package com.cherryzp.animalshelter.ui.common

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.window.core.layout.WindowSizeClass
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * "맨 위 상태"(4a 검토 F2): 바를 다 펼치고(`heightOffset`) 스크롤 색도 끈다(`contentOffset`). 프로그램 스크롤은 바에
 * 전달되지 않으므로 맨 위로 보낼 때·그리드가 스크롤할 수 없게 될 때 이것으로 처음 상태를 맞춘다.
 */
@OptIn(ExperimentalMaterial3Api::class)
fun TopAppBarState.resetToTop() {
    heightOffset = 0f
    contentOffset = 0f
}

/**
 * 목록·북마크 목록 상단 바의 스크롤 동작(3e 검토 N4, Android skills `adaptive` Step 5).
 *
 * 창 높이가 compact(폰 가로 — 그리드의 `Adaptive(156)` 규칙과 같은 판정)면 `enterAlways`(내리면 숨고 올리면 바로
 * 나온다), 아니면 지금처럼 pinned. 두 갈래는 각자 기본 `rememberTopAppBarState()`를 써서 상태를 같이 쓰지 않는다 —
 * 가로에서 접힌 높이가 세로의 pinned 바에 남지 않는다. 그리드가 어느 쪽으로도 스크롤할 수 없으면(결과가 적을 때)
 * 바를 끌어 움직이지 않는다.
 *
 * 4a 검토 F2: 두 갈래 모두 그리드가 없거나([gridShown]이 false — 빈 결과·오류 전체 화면) 어느 쪽으로도 스크롤할 수
 * 없게 되면 [resetToTop] — 바가 접힌 채(또는 스크롤 색으로) 남지 않는다. 스크롤 가능 여부는 그리드가 한 번이라도
 * 배치된 뒤(`viewportSize.height > 0`)의 값만 본다 — 다른 화면에서 돌아올 때 복원된 접힌 바를 첫 배치 전 값으로
 * 펴지 않는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun listTopBarScrollBehavior(
    gridState: LazyGridState,
    gridShown: Boolean,
): TopAppBarScrollBehavior {
    val heightCompact = !currentWindowAdaptiveInfoV2().windowSizeClass
        .isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
    val behavior = if (heightCompact) {
        val canScroll = remember(gridState) {
            { gridState.canScrollForward || gridState.canScrollBackward }
        }
        TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = canScroll)
    } else {
        TopAppBarDefaults.pinnedScrollBehavior()
    }
    val currentGridShown by rememberUpdatedState(gridShown)
    val barState = behavior.state
    LaunchedEffect(gridState, barState) {
        snapshotFlow {
            if (!currentGridShown) {
                true
            } else {
                val laidOut = gridState.layoutInfo.viewportSize.height > 0
                laidOut && !gridState.canScrollForward && !gridState.canScrollBackward
            }
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { barState.resetToTop() }
    }
    return behavior
}
