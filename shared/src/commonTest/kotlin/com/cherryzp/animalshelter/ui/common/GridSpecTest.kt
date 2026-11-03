package com.cherryzp.animalshelter.ui.common

import com.cherryzp.animalshelter.model.ListLayout
import kotlin.test.Test
import kotlin.test.assertEquals

/** 그리드 열 규칙(03 0.4, 브리핑 A9) — 칸 폭 기준. */
class GridSpecTest {
    @Test
    fun `폰 폭은 2열·1열 고정, 가장자리 16dp`() {
        assertEquals(
            GridSpec(GridColumns.Fixed(2), 16),
            gridSpecFor(360f, 1f, false, ListLayout.GRID)
        )
        assertEquals(
            GridSpec(GridColumns.Fixed(1), 16),
            gridSpecFor(599f, 1f, false, ListLayout.LARGE)
        )
    }

    @Test
    fun `600dp 이상은 최소 폭 Adaptive, 가장자리 24dp`() {
        assertEquals(
            GridSpec(GridColumns.Adaptive(156), 24),
            gridSpecFor(600f, 1f, false, ListLayout.GRID)
        )
        assertEquals(
            GridSpec(GridColumns.Adaptive(320), 24),
            gridSpecFor(840f, 1f, false, ListLayout.LARGE)
        )
    }

    @Test
    fun `글자 1_5배 이상이면 폰은 1열, 넓으면 최소 폭 2배`() {
        assertEquals(GridColumns.Fixed(1), gridSpecFor(411f, 1.5f, false, ListLayout.GRID).columns)
        assertEquals(GridColumns.Fixed(2), gridSpecFor(411f, 1.3f, false, ListLayout.GRID).columns)
        assertEquals(
            GridColumns.Adaptive(312),
            gridSpecFor(700f, 2f, false, ListLayout.GRID).columns
        )
    }

    @Test
    fun `창 높이가 compact면 보기 옵션과 무관하게 Adaptive 156`() {
        assertEquals(
            GridColumns.Adaptive(156),
            gridSpecFor(800f, 1f, true, ListLayout.LARGE).columns
        )
        assertEquals(
            GridColumns.Adaptive(156),
            gridSpecFor(560f, 1f, true, ListLayout.GRID).columns
        )
    }
}
