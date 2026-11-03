package com.cherryzp.animalshelter.platform

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(url) => { window.open(url, '_blank', 'noopener'); }")
private external fun openInNewTab(url: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(url) => { window.location.href = url; }")
private external fun navigateTo(url: String)

actual object ExternalActions {
    // 휴대폰 브라우저는 전화 앱을, 데스크톱은 연결된 앱(있으면)을 연다.
    actual fun dial(tel: String) = navigateTo("tel:$tel")

    actual fun openUrl(url: String) = openInNewTab(url)
}
