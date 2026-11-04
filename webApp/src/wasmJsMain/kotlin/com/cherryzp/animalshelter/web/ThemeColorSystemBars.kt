@file:OptIn(ExperimentalWasmJsInterop::class)

package com.cherryzp.animalshelter.web

import com.cherryzp.animalshelter.ui.common.SystemBarsHook

// 브라우저 테마 색 메타를 바꾼다. 없으면 만든다(셸 index.html에는 로딩용 하나가 있다).
@JsFun(
    "(color) => { " +
        "let meta = document.querySelector('meta[name=\"theme-color\"]'); " +
        "if (!meta) { meta = document.createElement('meta'); meta.name = 'theme-color'; " +
        "document.head.appendChild(meta); } " +
        "meta.content = color; }"
)
private external fun setThemeColorMeta(color: String)

/** DESIGN.md `background`(라이트). */
private const val LIGHT_BACKGROUND = "#F6FAFE"

/** DESIGN.md `dark-background` — 검은 사진 뷰어와도 가깝다. */
private const val DARK_BACKGROUND = "#0F1417"

/**
 * 웹의 [SystemBarsHook](4a 검토 F5): 시스템 바 대신 브라우저 테마 색(`<meta name="theme-color">`)을 앱 바탕에 맞춘다.
 * 로딩 중에는 셸이 브랜드 원색(#0A81AB)을 두고, 앱이 뜨면 밝은 아이콘(다크 테마·사진 뷰어)이면 다크 바탕, 아니면 라이트 바탕.
 */
object ThemeColorSystemBars : SystemBarsHook {
    override fun setLightIcons(lightIcons: Boolean) =
        setThemeColorMeta(if (lightIcons) DARK_BACKGROUND else LIGHT_BACKGROUND)
}
