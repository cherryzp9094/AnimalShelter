// 웹 진입점 파일 이름은 Kotlin/Wasm 관례대로 main.kt로 둔다.
@file:Suppress("ktlint:standard:filename")

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.cherryzp.animalshelter.App
import com.cherryzp.animalshelter.PlatformHooks
import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.di.AppConfig
import com.cherryzp.animalshelter.di.initKoin
import com.cherryzp.animalshelter.navigation.BrowserHistoryNavigation
import com.cherryzp.animalshelter.ui.theme.AppFontFiles
import com.cherryzp.animalshelter.web.JsBrowserHistory
import com.cherryzp.animalshelter.web.WebConfig
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.preloadFont

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => location.origin")
private external fun locationOrigin(): String

// index.html의 로딩 화면을 없앤다(H2). 셸이 없으면(테스트 페이지 등) 아무것도 안 한다.
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(reduced) => { if (window.animalShelterReady) window.animalShelterReady(reduced); }")
private external fun notifyAppReady(reducedMotion: Boolean)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => window.matchMedia('(prefers-reduced-motion: reduce)').matches")
private external fun prefersReducedMotion(): Boolean

// 시스템 설정이 바뀌면 알린다(M2)
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    "(callback) => window.matchMedia('(prefers-reduced-motion: reduce)')" +
        ".addEventListener('change', (e) => callback(e.matches))"
)
private external fun listenReducedMotion(callback: (Boolean) -> Unit)

/** 웹 진입점. 웹은 키 없이 중계 서버만 부른다(01-overview 7절). */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class)
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
    val hooks = PlatformHooks(
        navigation = navigation,
        // 로딩 화면(index.html)을 없앤다 — 모션 줄이기면 페이드 없이(H2)
        onFirstFrame = { notifyAppReady(prefersReducedMotion()) }
    )
    // 모션 줄이기(M2): 시작할 때 읽고 바뀌면 따라간다
    val reducedMotion = mutableStateOf(prefersReducedMotion())
    listenReducedMotion { reducedMotion.value = it }
    ComposeViewport(viewportContainerId = "root") {
        // 글꼴 둘을 받은 뒤 그린다 — 대체 글꼴이 먼저 보였다 바뀌지 않게(H2). 그동안은 로딩 화면이 덮고 있다
        val fonts = AppFontFiles.map { (font, weight) -> preloadFont(font, weight) }
        if (fonts.all { it.value != null }) App(hooks.copy(reducedMotion = reducedMotion.value))
    }
}
