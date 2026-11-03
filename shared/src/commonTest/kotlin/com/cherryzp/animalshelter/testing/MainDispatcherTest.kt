package com.cherryzp.animalshelter.testing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * ViewModel 테스트(브리핑 P14): `Dispatchers.Main`(= `viewModelScope`)을 이 테스트의 스케줄러에 붙이고 끝나면 되돌린다.
 * `advanceUntilIdle()`로 ViewModel 안의 코루틴까지 진행된다.
 *
 * 끝낼 때 `backgroundScope`의 구독을 끊고 [viewModels]의 `viewModelScope`를 취소한 뒤 진행시킨다 — 그러지 않으면
 * 남은 ViewModel 코루틴이 되돌린 Main(테스트 밖)으로 가려다 실패한다(JVM 호스트). `uiState` 구독은 ViewModel을 만든 직후에 시작한다
 * (가상 시간에서 구독 전에 `advanceUntilIdle()`이 `WhileSubscribed`를 먼저 돌리면 늦은 구독이 시작되지 않았다).
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun runMainTest(
    viewModels: () -> List<ViewModel> = { emptyList() },
    block: suspend TestScope.() -> Unit,
): TestResult = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    try {
        block()
    } finally {
        backgroundScope.coroutineContext.job.cancelChildren()
        viewModels().forEach { it.viewModelScope.cancel() }
        advanceUntilIdle()
        Dispatchers.resetMain()
    }
}
