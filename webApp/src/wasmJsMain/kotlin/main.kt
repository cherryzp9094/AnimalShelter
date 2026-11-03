// 웹 진입점 파일 이름은 Kotlin/Wasm 관례대로 main.kt로 둔다.
@file:Suppress("ktlint:standard:filename")

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.cherryzp.animalshelter.App
import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.di.AppConfig
import com.cherryzp.animalshelter.di.initKoin
import com.cherryzp.animalshelter.web.WebConfig

/** 웹 진입점. 웹은 키 없이 중계 서버만 부른다(01-overview 7절). */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin(
        AppConfig(
            endpoint = ApiEndpoint.Relay(WebConfig.RELAY_BASE_URL),
            isDebug = false,
            versionName = WebConfig.VERSION_NAME
        )
    )
    ComposeViewport(viewportContainerId = "root") {
        App()
    }
}
