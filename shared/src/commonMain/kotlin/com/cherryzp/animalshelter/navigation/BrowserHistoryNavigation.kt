package com.cherryzp.animalshelter.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * 브라우저 기록 API(브리핑 W1 — 라이브러리 없이 다섯 개: `history.state`·`pushState`·`replaceState`·`go`, `popstate`).
 * 웹 구현은 `:webApp`의 `@JsFun`, 테스트는 가짜. 문자열만 오간다.
 */
interface BrowserHistory {
    /** 지금 기록의 `state`(문자열이 아니면 null). */
    fun state(): String?

    /** `location.hash`(`#` 포함, 없으면 빈 문자열). */
    fun hash(): String

    fun pushState(state: String, url: String)

    fun replaceState(state: String, url: String)

    fun go(delta: Int)

    /** `popstate`를 듣는다(인자: 그 기록의 `state` 또는 null, `location.hash`). 돌려준 함수를 부르면 그만 듣는다. */
    fun listenPopState(listener: (state: String?, hash: String) -> Unit): () -> Unit

    /** [block]을 다음 차례에 부른다(`setTimeout(…, 0)` — popstate 처리 안에서 기록을 바로 고치지 않으려고). */
    fun nextTurn(block: () -> Unit)
}

/**
 * 브라우저 기록 연결(브리핑 W3·W4). 규칙은 [WebHistorySync]가 정하고 여기서는 [history]를 부르기만 한다.
 * 만들 때 시작 스택을 정하고(기록의 state → 새로고침, 없으면 해시) 주소를 정규화한다 — Compose보다 먼저 만든다.
 */
class BrowserHistoryNavigation(private val history: BrowserHistory) : NavigationBinding {
    private val sync: WebHistorySync

    init {
        val (started, normalize) = WebHistorySync.start(history.state(), history.hash())
        sync = started
        normalize?.let(::execute)
    }

    override val initialStack: List<NavKey> = sync.lastSynced

    @Composable
    override fun Bind(backStack: NavBackStack<NavKey>) {
        DisposableEffect(backStack) {
            val stop = history.listenPopState { state, hash ->
                val outcome = sync.onPopState(state, hash)
                outcome.apply?.let { backStack.replaceWith(it) }
                outcome.deferred?.let { command -> history.nextTurn { execute(command) } }
            }
            onDispose { stop() }
        }
        LaunchedEffect(backStack) {
            snapshotFlow { backStack.toList() }.collect { stack ->
                sync.onAppChanged(stack)?.let(::execute)
            }
        }
    }

    private fun execute(command: HistoryCommand) {
        when (command) {
            is HistoryCommand.Push -> history.pushState(command.state, command.url)
            is HistoryCommand.Replace -> history.replaceState(command.state, command.url)
            is HistoryCommand.Go -> history.go(command.delta)
        }
    }
}

/**
 * back stack을 [target]으로 맞춘다: 같은 앞부분은 두고 다른 꼬리만 빼고 더한다(`clear` 후 다시 넣으면 entry 상태가
 * 사라진다). 한 스냅샷 안에서 바꿔 중간 상태(짧은 스택)가 관찰되지 않게 한다 — 관찰되면 그것을 앱의 뒤로로 읽는다.
 */
internal fun MutableList<NavKey>.replaceWith(target: List<NavKey>) {
    Snapshot.withMutableSnapshot {
        val common = zip(target).takeWhile { (a, b) -> a == b }.size
        while (size > common) removeAt(lastIndex)
        addAll(target.drop(common))
    }
}
