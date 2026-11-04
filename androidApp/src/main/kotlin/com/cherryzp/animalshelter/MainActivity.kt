package com.cherryzp.animalshelter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.cherryzp.animalshelter.navigation.shareLinkNavigation
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
        // 공유 링크(App Links)로 열렸으면 [목록, 상세]에서 시작한다(L3). 저장된 back stack이 있으면(회전·프로세스
        // 복원) 그것이 이긴다 — 링크를 다시 적용하지 않는다. onNewIntent는 쓰지 않는다(링크마다 새 작업 위에 열린다).
        val navigation = shareLinkNavigation(intent?.dataString, BuildConfig.SHARE_HOST)
        setContent { App(PlatformHooks(systemBars = systemBars, navigation = navigation)) }
    }
}
