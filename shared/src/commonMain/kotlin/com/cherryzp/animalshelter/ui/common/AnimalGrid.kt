package com.cherryzp.animalshelter.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.cherryzp.animalshelter.model.ListLayout

/** 그리드 열 규칙의 결과(테스트하려고 Compose 타입과 따로 둔다). */
sealed interface GridColumns {
    data class Fixed(val count: Int) : GridColumns

    data class Adaptive(val minSizeDp: Int) : GridColumns
}

/** 열 규칙 + 가장자리 여백. */
data class GridSpec(val columns: GridColumns, val edgePaddingDp: Int)

/** 칸 폭이 이보다 좁으면 compact(가장자리 16dp, 열 고정). */
const val COMPACT_WIDTH_DP = 600

/** 이 글자 배율부터 카드 최소 폭을 2배로(03 0.4). */
const val LARGE_FONT_SCALE = 1.5f

private const val GRID_MIN_DP = 156
private const val LARGE_MIN_DP = 320

/**
 * 그리드 열 수 규칙(03 0.4, 브리핑 A9). 창 폭이 아니라 **칸이 받은 폭**으로 정한다.
 * - 칸 폭 < 600dp: 가장자리 16dp, `Fixed(GRID 2 / LARGE 1)`. 글자 배율 ≥ 1.5면 `Fixed(1)`.
 * - 그 이상: 24dp, `Adaptive(GRID 156 / LARGE 320)`. 글자 배율 ≥ 1.5면 최소 폭 2배.
 * - 창 높이가 compact(폰 가로)면 보기 옵션과 무관하게 `Adaptive(156)`(글자 배율 규칙은 그대로 적용).
 */
fun gridSpecFor(
    paneWidthDp: Float,
    fontScale: Float,
    heightCompact: Boolean,
    layout: ListLayout,
): GridSpec {
    val compact = paneWidthDp < COMPACT_WIDTH_DP
    val edge = if (compact) 16 else 24
    val scale = if (fontScale >= LARGE_FONT_SCALE) 2 else 1
    val columns = when {
        heightCompact -> GridColumns.Adaptive(GRID_MIN_DP * scale)

        compact -> GridColumns.Fixed(if (scale == 2 || layout == ListLayout.LARGE) 1 else 2)

        else -> GridColumns.Adaptive(
            (if (layout == ListLayout.LARGE) LARGE_MIN_DP else GRID_MIN_DP) * scale
        )
    }
    return GridSpec(columns, edge)
}

/**
 * 목록·북마크 공용 그리드(03 2.1·0.4). 머리 항목([header] — 요약 칩·결과 헤더)과 끝 항목([footer])은 전체 폭.
 * 열 수가 바뀌어도 [state]가 첫 보이는 항목을 지킨다. `key = desertionNo`, `contentType`(P16).
 *
 * @param contentPadding 가장자리 여백에 더할 바깥 여백(시스템 바 inset 등)
 * @param selectedDesertionNo 상세 칸에 열린 카드(두 칸 이상일 때만 — 3d D7). 한 칸이면 null
 */
@Composable
fun AnimalGrid(
    items: List<AnimalCardUi>,
    layout: ListLayout,
    onCardClick: (AnimalCardUi) -> Unit,
    onBookmarkClick: (AnimalCardUi) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(),
    header: LazyGridScope.() -> Unit = {},
    footer: LazyGridScope.() -> Unit = {},
    selectedDesertionNo: String? = null,
) {
    val heightCompact = !currentWindowAdaptiveInfoV2().windowSizeClass
        .isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier = modifier) {
        val spec = gridSpecFor(maxWidth.value, fontScale, heightCompact, layout)
        val edge = spec.edgePaddingDp.dp
        LazyVerticalGrid(
            columns = when (val columns = spec.columns) {
                is GridColumns.Fixed -> GridCells.Fixed(columns.count)
                is GridColumns.Adaptive -> GridCells.Adaptive(columns.minSizeDp.dp)
            },
            state = state,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = contentPadding.plusEdges(edge),
            modifier = Modifier.fillMaxSize()
        ) {
            header()
            items(items = items, key = { it.desertionNo }, contentType = { "animal" }) { ui ->
                AnimalCard(
                    ui = ui,
                    layout = layout,
                    onClick = { onCardClick(ui) },
                    onBookmarkClick = { onBookmarkClick(ui) },
                    selected = ui.desertionNo == selectedDesertionNo
                )
            }
            footer()
        }
    }
}

/** 바깥 여백에 가장자리 여백(좌우)과 위 8dp·아래 16dp를 더한다. */
@Composable
private fun PaddingValues.plusEdges(edge: Dp): PaddingValues {
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(direction) + edge,
        end = calculateEndPadding(direction) + edge,
        top = calculateTopPadding() + 8.dp,
        bottom = calculateBottomPadding() + 16.dp
    )
}
