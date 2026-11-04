package com.cherryzp.animalshelter.ui.flow

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.cherryzp.animalshelter.App
import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.data.api.EndpointSelector
import com.cherryzp.animalshelter.data.repository.AnimalRepository
import com.cherryzp.animalshelter.data.repository.BookmarkRepository
import com.cherryzp.animalshelter.data.repository.CodeTableRepository
import com.cherryzp.animalshelter.data.repository.SettingsRepository
import com.cherryzp.animalshelter.data.repository.ShelterRepository
import com.cherryzp.animalshelter.di.AppConfig
import com.cherryzp.animalshelter.di.viewModelModule
import com.cherryzp.animalshelter.fake.FakeAnimalRepository
import com.cherryzp.animalshelter.fake.FakeBookmarkRepository
import com.cherryzp.animalshelter.fake.FakeCodeTableRepository
import com.cherryzp.animalshelter.fake.FakeSettingsRepository
import com.cherryzp.animalshelter.fake.FakeShelterRepository
import com.cherryzp.animalshelter.platform.PlatformInfo
import com.cherryzp.animalshelter.testing.FakeClock
import com.cherryzp.animalshelter.ui.common.ImageDiskCacheProvider
import com.cherryzp.animalshelter.ui.common.ImageUrlResolver
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.withContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/** 흐름 테스트 창 크기(폰 세로, 03 0.4 compact — 2열/1열). 두 플랫폼에서 같게 고정한다. */
private const val PHONE_WIDTH_DP = 400
private const val PHONE_HEIGHT_DP = 760

/** 기다림 상한(밀리초). 화면 전환·ViewModel 수집은 실제 시간으로 돈다. */
const val FLOW_TIMEOUT_MILLIS = 10_000L

/** 흐름 하나의 가짜 Repository들(네트워크 없음 — 테스트 동물은 사진이 없다). */
class FlowFakes(
    val animals: FakeAnimalRepository = FakeAnimalRepository(),
    val bookmarks: FakeBookmarkRepository = FakeBookmarkRepository(),
    val settings: FakeSettingsRepository = FakeSettingsRepository(),
    val codeTables: FakeCodeTableRepository = FakeCodeTableRepository(),
    val shelters: FakeShelterRepository = FakeShelterRepository(),
    val clock: FakeClock = FakeClock(),
)

private fun flowModule(fakes: FlowFakes) = module {
    val config = AppConfig(ApiEndpoint.Fixture, isDebug = false, versionName = "test")
    single { config }
    single<ApiEndpoint> { config.endpoint }
    single { PlatformInfo(versionName = config.versionName) }
    single<Clock> { fakes.clock }
    single { EndpointSelector(config) { null } }
    single<AnimalRepository> { fakes.animals }
    single<BookmarkRepository> { fakes.bookmarks }
    single<SettingsRepository> { fakes.settings }
    single<CodeTableRepository> { fakes.codeTables }
    single<ShelterRepository> { fakes.shelters }
    single { ImageDiskCacheProvider { null } }
    single { ImageUrlResolver { it } }
}

/** 창 크기를 폰으로 고정한다(창 크기 클래스·칸 규칙이 이 값을 본다). */
private class FixedWindowInfo(override val containerSize: IntSize) : WindowInfo {
    override val isWindowFocused: Boolean = true
}

@Composable
private fun PhoneSizedApp() {
    val density = LocalDensity.current
    val size = with(density) {
        IntSize(PHONE_WIDTH_DP.dp.roundToPx(), PHONE_HEIGHT_DP.dp.roundToPx())
    }
    CompositionLocalProvider(LocalWindowInfo provides FixedWindowInfo(size)) {
        Box(Modifier.size(PHONE_WIDTH_DP.dp, PHONE_HEIGHT_DP.dp)) { App() }
    }
}

/**
 * UI 흐름 하나(브리핑 C6, P14): Fake Repository + 실제 ViewModel(`viewModelModule`)을 Koin에 올리고 `App()`을 띄운다.
 * 흐름 본문은 `commonTest`에, `@Test` 래퍼는 `wasmJsTest`·`androidDeviceTest`에만 둔다(호스트 JVM에서는 돌리지 않는다).
 */
@OptIn(ExperimentalTestApi::class)
fun runFlowTest(
    fakes: FlowFakes = FlowFakes(),
    block: suspend ComposeUiTest.(FlowFakes) -> Unit,
): TestResult = runComposeUiTest {
    startKoin { modules(flowModule(fakes), viewModelModule) }
    try {
        setContent { PhoneSizedApp() }
        block(fakes)
    } finally {
        stopKoin()
    }
}

/** 태그가 붙은 노드가 나타날 때까지 기다린 뒤 돌려준다. */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.awaitTag(tag: String): SemanticsNodeInteraction {
    awaitCondition("태그 $tag") { onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty() }
    return onNode(hasTestTag(tag))
}

/**
 * [condition]을 기다린다. `waitUntil`은 wasm에서 JS 이벤트 루프에 양보하지 않아 `Dispatchers.Main`의 ViewModel
 * 코루틴·웹 문구 로딩이 기다리는 동안 돌지 않는다 → `awaitIdle()` + 실제 시간 `delay`(Dispatchers.Default)로 양보하며
 * 다시 본다. 시간이 넘으면 무엇을 기다렸는지와 지금 화면의 의미 트리를 실패 문구에 싣는다.
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.awaitCondition(what: String, condition: () -> Boolean) {
    val deadline = TimeSource.Monotonic.markNow() + FLOW_TIMEOUT_MILLIS.milliseconds
    while (true) {
        awaitIdle()
        if (condition()) return
        if (deadline.hasPassedNow()) {
            val tree = runCatching { onRoot().printToString() }.getOrDefault("")
            throw AssertionError("기다리다 시간이 넘었다: $what\n$tree")
        }
        withContext(Dispatchers.Default) { delay(POLL_MILLIS) }
    }
}

private const val POLL_MILLIS = 50L
