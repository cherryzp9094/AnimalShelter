package com.cherryzp.animalshelter.platform

// `noopener`를 창 기능 문자열로 주면 window.open이 늘 null을 돌려줘 성공을 알 수 없다 — 열고 나서 opener를 끊는다.
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    "(url) => { const w = window.open(url, '_blank'); if (!w) return false; w.opener = null; return true; }"
)
private external fun openInNewTab(url: String): Boolean

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(url) => { window.location.href = url; }")
private external fun navigateTo(url: String)

// 쓰기는 비동기(Promise)라 결과를 기다리지 않는다. 클립보드 API가 없으면(비보안 문맥) false.
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    "(text) => { if (!navigator.clipboard || !navigator.clipboard.writeText) return false; " +
        "navigator.clipboard.writeText(text).catch(() => {}); return true; }"
)
private external fun writeClipboard(text: String): Boolean

actual object ExternalActions {
    // 휴대폰 브라우저는 전화 앱을, 데스크톱은 연결된 앱(있으면)을 연다. 브라우저는 결과를 알려 주지 않는다.
    actual fun dial(tel: String): Boolean {
        navigateTo("tel:$tel")
        return true
    }

    actual fun openUrl(url: String): Boolean = openInNewTab(url)

    actual fun openMap(lat: Double, lng: Double, label: String): Boolean =
        openInNewTab("https://www.google.com/maps?q=$lat,$lng")

    actual fun copyText(text: String): CopyFeedback =
        if (writeClipboard(text)) CopyFeedback.NEEDS_APP_MESSAGE else CopyFeedback.FAILED
}
