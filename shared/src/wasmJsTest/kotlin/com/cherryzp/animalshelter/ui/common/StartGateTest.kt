package com.cherryzp.animalshelter.ui.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.cherryzp.animalshelter.ui.flow.awaitCondition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 테스트에서 기다리는 상한(실제 앱은 2초). */
private const val WAIT_MILLIS = 300L

/**
 * 웹 시작 문(WS1, `reports/phase5.md` 5.3): 글꼴·문구(필수)가 되면 아이콘(선택)은 [WAIT_MILLIS]까지만 기다리고 열며,
 * 한 번 열면 조건이 다시 거짓이 돼도 닫지 않는다(App을 다시 만들지 않는다). 아이콘 파일을 못 받으면
 * `preloadImageVector`가 null로 남아 예전 문은 끝내 열리지 않았다.
 */
@OptIn(ExperimentalTestApi::class)
class StartGateTest {
    /** T25 필수만 되고 선택이 끝내 안 오면(아이콘 404) 기다린 뒤 연다 — 기다리기 전에는 닫혀 있다. */
    @Test
    fun t25OpensAfterWaitWhenOptionalNeverArrives() = runComposeUiTest {
        var opened = false
        setContent { if (rememberStartGate(true, false, WAIT_MILLIS)) opened = true }
        mainClock.advanceTimeBy(WAIT_MILLIS / 2)
        assertFalse(opened, "기다리지 않고 열었다")
        mainClock.advanceTimeBy(WAIT_MILLIS)
        assertTrue(opened, "기다린 뒤에도 열지 않았다")
    }

    /** T25 다 되면 바로(첫 구성에서) 연다. */
    @Test
    fun t25OpensAtOnceWhenAllReady() = runComposeUiTest {
        var firstValue: Boolean? = null
        setContent {
            val open = rememberStartGate(true, true, WAIT_MILLIS)
            remember { firstValue = open }
        }
        awaitCondition("첫 구성") { firstValue != null }
        assertEquals(true, firstValue)
    }

    /** T25 필수(글꼴·문구)가 안 되면 기다린 뒤에도 열지 않는다(글꼴은 그대로 — N33). */
    @Test
    fun t25StaysClosedWithoutRequired() = runComposeUiTest {
        var opened = false
        setContent { if (rememberStartGate(false, true, WAIT_MILLIS)) opened = true }
        mainClock.advanceTimeBy(WAIT_MILLIS * 3)
        assertFalse(opened)
    }

    /** T25 한 번 연 문은 조건이 다시 거짓이 돼도 닫지 않는다 — 안쪽을 다시 만들지 않는다. */
    @Test
    fun t25StaysOpenOnceOpened() = runComposeUiTest {
        var required by mutableStateOf(true)
        var optional by mutableStateOf(true)
        var created = 0
        var shown = false
        setContent {
            shown = rememberStartGate(required, optional, WAIT_MILLIS)
            if (shown) remember { created++ }
        }
        mainClock.advanceTimeByFrame()
        assertTrue(shown)
        required = false
        optional = false
        mainClock.advanceTimeBy(WAIT_MILLIS * 2)
        assertTrue(shown, "조건이 거짓이 되자 닫혔다")
        assertEquals(1, created, "안쪽을 다시 만들었다")
    }
}
