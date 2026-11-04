package com.cherryzp.animalshelter

import com.cherryzp.animalshelter.navigation.NavigationBinding
import com.cherryzp.animalshelter.ui.common.SystemBarsHook

/**
 * 앱 모듈(:androidApp·:webApp)이 [App]에 넘기는 플랫폼 연결 묶음(브리핑 W4). 플랫폼 API가 필요한 것을 expect를
 * 늘리지 않고 인자로 받는다.
 *
 * @property systemBars 시스템 바 색(C4). Android는 시스템 바 아이콘 색, 웹은 브라우저 테마 색(`ThemeColorSystemBars` —
 *   4a 검토 F5), 테스트는 NoOp
 * @property navigation 처음 back stack과 플랫폼 탐색 연결(웹 브라우저 기록 W3, Android App Links L3)
 * @property reducedMotion 앱이 넣은 움직임을 끈다(웹 `prefers-reduced-motion`, M2 — `LocalReducedMotion`). Android는
 *   Compose가 시스템 애니메이터 배율을 이미 따르므로 늘 false
 * @property onFirstFrame 테마를 읽고 첫 화면을 그린 뒤 한 번(웹은 로딩 화면을 없앤다, H2). Android는 쓰지 않는다
 */
data class PlatformHooks(
    val systemBars: SystemBarsHook = SystemBarsHook.NoOp,
    val navigation: NavigationBinding = NavigationBinding.None,
    val reducedMotion: Boolean = false,
    val onFirstFrame: () -> Unit = {},
)
