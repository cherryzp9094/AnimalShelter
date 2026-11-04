package com.cherryzp.animalshelter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.cherryzp.animalshelter.ui.common.SystemBarsHook

/** 단일 Activity. 화면은 전부 :shared의 [App]이 그린다. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge()는 시스템 테마로만 아이콘 색을 정한다 — 앱 설정 테마·사진 뷰어에 맞춘다(C4)
        val systemBars = SystemBarsHook { lightIcons ->
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !lightIcons
                isAppearanceLightNavigationBars = !lightIcons
            }
        }
        setContent { App(PlatformHooks(systemBars = systemBars)) }
    }
}
