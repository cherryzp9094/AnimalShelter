package com.cherryzp.animalshelter

import com.cherryzp.animalshelter.navigation.NavigationBinding
import com.cherryzp.animalshelter.ui.common.SystemBarsHook

/**
 * 앱 모듈(:androidApp·:webApp)이 [App]에 넘기는 플랫폼 연결 묶음(브리핑 W4). 플랫폼 API가 필요한 것을 expect를
 * 늘리지 않고 인자로 받는다.
 *
 * @property systemBars 시스템 바 아이콘 색(C4). 웹·테스트는 NoOp
 * @property navigation 처음 back stack과 플랫폼 탐색 연결(웹 브라우저 기록 W3, Android App Links L3)
 */
data class PlatformHooks(
    val systemBars: SystemBarsHook = SystemBarsHook.NoOp,
    val navigation: NavigationBinding = NavigationBinding.None,
)
