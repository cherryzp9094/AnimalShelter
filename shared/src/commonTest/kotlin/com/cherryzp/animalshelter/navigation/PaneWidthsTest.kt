package com.cherryzp.animalshelter.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

/** 목록 칸 폭(브리핑 B14, 03 0.4). */
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
}
