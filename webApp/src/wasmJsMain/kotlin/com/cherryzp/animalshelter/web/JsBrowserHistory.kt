@file:OptIn(ExperimentalWasmJsInterop::class)

package com.cherryzp.animalshelter.web

import com.cherryzp.animalshelter.navigation.BrowserHistory

// 기록의 state는 문자열(JSON)만 다룬다 — 다른 값이면 빈 문자열로 넘겨 "없음"으로 본다.
@JsFun("() => (typeof history.state === 'string' ? history.state : '')")
private external fun historyState(): String

@JsFun("() => location.hash")
private external fun locationHash(): String

@JsFun("(state, url) => history.pushState(state, '', url)")
private external fun historyPushState(state: String, url: String)

@JsFun("(state, url) => history.replaceState(state, '', url)")
private external fun historyReplaceState(state: String, url: String)

@JsFun("(delta) => history.go(delta)")
private external fun historyGo(delta: Int)

@JsFun(
    "(callback) => { " +
        "const handler = (e) => " +
        "callback(typeof e.state === 'string' ? e.state : '', location.hash); " +
        "window.addEventListener('popstate', handler); return handler; }"
)
private external fun addPopStateListener(callback: (String, String) -> Unit): JsAny

@JsFun("(handler) => window.removeEventListener('popstate', handler)")
private external fun removePopStateListener(handler: JsAny)

@JsFun("(callback) => { setTimeout(callback, 0); }")
private external fun setTimeoutZero(callback: () -> Unit)

/** [BrowserHistory]의 브라우저 구현(브리핑 W4 — `@JsFun`, `kotlinx-browser` 없음). */
object JsBrowserHistory : BrowserHistory {
    override fun state(): String? = historyState().ifEmpty { null }

    override fun hash(): String = locationHash()

    override fun pushState(state: String, url: String) = historyPushState(state, url)

    override fun replaceState(state: String, url: String) = historyReplaceState(state, url)

    override fun go(delta: Int) = historyGo(delta)

    override fun listenPopState(listener: (state: String?, hash: String) -> Unit): () -> Unit {
        val handler = addPopStateListener { state, hash -> listener(state.ifEmpty { null }, hash) }
        return { removePopStateListener(handler) }
    }

    override fun nextTurn(block: () -> Unit) = setTimeoutZero(block)
}
