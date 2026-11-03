package com.cherryzp.animalshelter.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 시스템 바 아이콘 색 훅(C4). 플랫폼 API가 필요해 expect를 늘리지 않고 `App(systemBars = …)` 인자로 받는다 —
 * Android `MainActivity`가 `WindowInsetsControllerCompat`로 구현하고, 웹·테스트는 [NoOp].
 * `ComponentActivity.enableEdgeToEdge()`는 시스템 테마만 보므로 설정의 테마(라이트/다크)와 검은 사진 뷰어를 따로 맞춘다.
 */
fun interface SystemBarsHook {
    /** [lightIcons]가 true면 밝은 아이콘(어두운 배경 위), false면 어두운 아이콘. */
    fun setLightIcons(lightIcons: Boolean)

    companion object {
        val NoOp: SystemBarsHook = SystemBarsHook { }
    }
}

/** 어두운 바탕을 시스템 바 뒤에 그리는 화면 수(사진 뷰어). 0보다 크면 테마와 무관하게 밝은 아이콘. */
@Stable
class DarkSystemBarRequests {
    var count by mutableIntStateOf(0)
        internal set
}

val LocalDarkSystemBarRequests = staticCompositionLocalOf { DarkSystemBarRequests() }

/** 이 화면이 보이는 동안 시스템 바 아이콘을 밝게(검은 배경의 사진 뷰어, C4). */
@Composable
fun DarkSystemBarsEffect() {
    val requests = LocalDarkSystemBarRequests.current
    DisposableEffect(requests) {
        requests.count++
        onDispose { requests.count-- }
    }
}
