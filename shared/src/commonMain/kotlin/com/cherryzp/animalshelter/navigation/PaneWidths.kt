package com.cherryzp.animalshelter.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation3.LocalListDetailSceneScope
import androidx.compose.runtime.Composable

// 칸 규칙(03 0.4, 브리핑 B14).

/** 목록 칸이 지금 펼쳐져 보이는가(두 칸·세 칸). 목록-상세 장면 밖이면 false. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun isListPaneExpanded(): Boolean {
    val scope = LocalListDetailSceneScope.current ?: return false
    val value = scope.scaffoldTransitionScope.scaffoldStateTransition.currentState
    return value[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded
}
