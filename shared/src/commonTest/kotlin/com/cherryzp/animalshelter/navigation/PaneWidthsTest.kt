package com.cherryzp.animalshelter.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

/** 목록 칸 폭·칸 수 상한(브리핑 B14, 03 0.4, Phase 3 검토 N1). */
class PaneWidthsTest {
    @Test
    fun `창 폭 클래스마다 목록 칸 폭`() {
        assertEquals(400, listPaneWidthDp(840))
        assertEquals(400, listPaneWidthDp(1199))
        assertEquals(440, listPaneWidthDp(1200))
        assertEquals(440, listPaneWidthDp(1599))
        assertEquals(480, listPaneWidthDp(1600))
        assertEquals(480, listPaneWidthDp(2560))
    }

    @Test
    fun `창 높이가 compact면 폭과 무관하게 한 칸`() {
        assertEquals(1, maxPaneCount(914, 411)) // 폰 가로(N1)
        assertEquals(1, maxPaneCount(1600, 479))
        assertEquals(1, maxPaneCount(800, 360))
    }

    @Test
    fun `높이가 되면 폭 클래스대로 1칸·2칸·3칸`() {
        assertEquals(1, maxPaneCount(839, 1000))
        assertEquals(2, maxPaneCount(840, 480))
        assertEquals(2, maxPaneCount(1199, 900))
        assertEquals(3, maxPaneCount(1200, 1000))
        assertEquals(3, maxPaneCount(1600, 900))
    }
}
