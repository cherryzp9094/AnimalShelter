// 웹 진입점 파일 이름은 Kotlin/Wasm 관례대로 main.kt로 둔다.
@file:Suppress("ktlint:standard:filename")

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.cherryzp.animalshelter.App
import com.cherryzp.animalshelter.PlatformHooks
import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.di.AppConfig
import com.cherryzp.animalshelter.di.initKoin
import com.cherryzp.animalshelter.navigation.BrowserHistoryNavigation
import com.cherryzp.animalshelter.web.JsBrowserHistory
import com.cherryzp.animalshelter.web.WebConfig

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => location.origin")
private external fun locationOrigin(): String

/** 웹 진입점. 웹은 키 없이 중계 서버만 부른다(01-overview 7절). */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin(
        AppConfig(
            endpoint = ApiEndpoint.Relay(WebConfig.RELAY_BASE_URL),
            isDebug = false,
            versionName = WebConfig.VERSION_NAME,
            // 웹은 디버그 항목이 없어 화면에 나오지 않는다(브리핑 B2)
            relayBaseUrl = WebConfig.RELAY_BASE_URL,
            // 공유 링크(4a S4): 같은 origin 배포면 그 origin, 로컬 개발은 /a가 있는 중계 주소
            shareBaseUrl = WebConfig.RELAY_BASE_URL.ifEmpty { locationOrigin() }
        )
    )
    // 시작 스택은 Compose보다 먼저 정한다(기록의 state 또는 해시, 브리핑 W3)
    val navigation = BrowserHistoryNavigation(JsBrowserHistory)
    ComposeViewport(viewportContainerId = "root") {
        App(PlatformHooks(navigation = navigation))
    }
}
