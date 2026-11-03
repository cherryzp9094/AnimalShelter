package com.cherryzp.animalshelter.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation3.LocalListDetailSceneScope
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass

// 칸 규칙(03 0.4, 브리핑 B14).

/** 목록 칸이 지금 펼쳐져 보이는가(두 칸·세 칸). 목록-상세 장면 밖이면 false. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun isListPaneExpanded(): Boolean {
    val scope = LocalListDetailSceneScope.current ?: return false
    val value = scope.scaffoldTransitionScope.scaffoldStateTransition.currentState
    return value[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded
}

/**
 * 목록 칸 폭(03 0.4): 창 폭 클래스가 expanded(840~1199) 400dp · large(1200~1599) 440dp ·
 * extra-large(≥ 1600) 480dp. 한 칸(< 840)에서는 쓰이지 않는다.
 */
fun listPaneWidthDp(windowWidthDp: Int): Int = when {
    windowWidthDp >= WindowSizeClass.WIDTH_DP_EXTRA_LARGE_LOWER_BOUND -> 480
    windowWidthDp >= WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND -> 440
    else -> 400
}
