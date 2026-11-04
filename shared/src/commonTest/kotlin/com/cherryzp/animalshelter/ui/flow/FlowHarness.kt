package com.cherryzp.animalshelter.ui.flow

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.SoftwareKeyboardController
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
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.cherryzp.animalshelter.App
import com.cherryzp.animalshelter.PlatformHooks
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
const val PHONE_WIDTH_DP = 400
const val PHONE_HEIGHT_DP = 760

/** 두 칸 흐름 창(large 1280 — 목록 440dp + 상세, 03 0.4). */
const val WIDE_WIDTH_DP = 1280
const val WIDE_HEIGHT_DP = 900

/** 두 칸 창의 낮은 높이 — 2단 상세 본문이 스크롤된다(높이 compact는 아니다, 3e E2). */
const val WIDE_SHORT_HEIGHT_DP = 600

/** 폰 가로 흐름 창(폭 expanded·높이 compact — 3e E3). */
const val LANDSCAPE_WIDTH_DP = 900
const val LANDSCAPE_HEIGHT_DP = 400

/** 기다림 상한(밀리초). 화면 전환·ViewModel 수집은 실제 시간으로 돈다. */
const val FLOW_TIMEOUT_MILLIS = 10_000L

/**
 * 흐름 테스트의 창 크기(dp). 흐름 중에 [resizeWindow]로 바꾸면 창 크기 클래스·칸 수와 배치 크기가 따라간다 — 웹에서
 * 창을 끌어 줄이는 것과 같다(Activity 재생성 없음, 4a-fixes2 G1 T12).
 */
@Stable
class FlowWindow(widthDp: Int = PHONE_WIDTH_DP, heightDp: Int = PHONE_HEIGHT_DP) {
    var widthDp by mutableIntStateOf(widthDp)
        internal set
    var heightDp by mutableIntStateOf(heightDp)
        internal set
}

/**
 * 가짜 키보드 컨트롤러(5a-live-fixes L5 T17). 하네스가 `LocalSoftwareKeyboardController`로 앱에 주고, `hide()`
 * 횟수를 센다 — 품종을 메뉴에서 고르면 키보드를 닫는지 본다. `show()`는 아무것도 하지 않는다.
 */
class FakeKeyboardController : SoftwareKeyboardController {
    var hideCount: Int = 0
        private set

    override fun show() = Unit

    override fun hide() {
        hideCount++
    }
}

/**
 * 흐름 하나의 가짜 Repository들(네트워크 없음 — 테스트 동물은 사진이 없다)과 창 크기([window]),
 * 키보드 컨트롤러([keyboard]), 시스템 뒤로 입력([systemBack] — [pressSystemBack]이 쓴다).
 */
class FlowFakes(
    val animals: FakeAnimalRepository = FakeAnimalRepository(),
    val bookmarks: FakeBookmarkRepository = FakeBookmarkRepository(),
    val settings: FakeSettingsRepository = FakeSettingsRepository(),
    val codeTables: FakeCodeTableRepository = FakeCodeTableRepository(),
    val shelters: FakeShelterRepository = FakeShelterRepository(),
    val clock: FakeClock = FakeClock(),
    val window: FlowWindow = FlowWindow(),
    val keyboard: FakeKeyboardController = FakeKeyboardController(),
    val systemBack: DirectNavigationEventInput = DirectNavigationEventInput(),
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

/**
 * 창 크기를 [FlowWindow]에서 읽는다(창 크기 클래스·칸 규칙이 이 값을 본다). `containerSize`를 읽을 때마다 상태를
 * 읽으므로 창 크기가 바뀌면 그 값을 읽은 곳이 다시 그려진다.
 */
private class FlowWindowInfo(private val size: () -> IntSize) : WindowInfo {
    override val isWindowFocused: Boolean = true
    override val containerSize: IntSize get() = size()
}

/**
 * [input]을 지금 뒤로 이벤트 디스패처에 붙인다 — Android 시스템 뒤로·웹 Esc가 들어오는 길과 같다(4b-fixes B1 T19).
 * 앱 안 ←(`action_back`)과 달리 NavDisplay·칸 장면의 뒤로 처리기를 거친다.
 */
@Composable
private fun SystemBackInput(input: DirectNavigationEventInput) {
    val dispatcher = checkNotNull(LocalNavigationEventDispatcherOwner.current) {
        "테스트 환경에 NavigationEventDispatcherOwner가 없다"
    }.navigationEventDispatcher
    DisposableEffect(dispatcher, input) {
        dispatcher.addInput(input)
        onDispose { dispatcher.removeInput(input) }
    }
}

@Composable
private fun SizedApp(fakes: FlowFakes, hooks: PlatformHooks) {
    val window = fakes.window
    SystemBackInput(fakes.systemBack)
    val density = LocalDensity.current
    val windowInfo = remember(window, density) {
        FlowWindowInfo {
            with(density) { IntSize(window.widthDp.dp.roundToPx(), window.heightDp.dp.roundToPx()) }
        }
    }
    CompositionLocalProvider(
        LocalWindowInfo provides windowInfo,
        LocalSoftwareKeyboardController provides fakes.keyboard
    ) {
        // 테스트 화면(브라우저 캔버스 1024px 폭, 기기 세로 411dp 폭)보다 커도 지정한 크기로 배치한다(왼쪽 위 기준) —
        // 줄이면 창 크기 클래스(1280)와 실제 칸 폭이 어긋나 두 칸 상세가 1단이 된다(3e). 넘친 부분은 보이지 않을 뿐이라
        // 그쪽 노드는 위치만 재고(`getUnclippedBoundsInRoot`) 누르지 않는다.
        Box(
            Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .size(window.widthDp.dp, window.heightDp.dp)
        ) { App(hooks) }
    }
}

/**
 * UI 흐름 하나(브리핑 C6, P14): Fake Repository + 실제 ViewModel(`viewModelModule`)을 Koin에 올리고 `App()`을 띄운다.
 * 흐름 본문은 `commonTest`에, `@Test` 래퍼는 `wasmJsTest`·`androidDeviceTest`에만 둔다(호스트 JVM에서는 돌리지 않는다).
 * 창은 기본 폰 세로(400×760), 두 칸 흐름은 [WIDE_WIDTH_DP]×[WIDE_HEIGHT_DP] — 흐름 중에 [resizeWindow]로 바꿀 수
 * 있다(T12). [hooks]는 `App(hooks)`에 그대로 넘긴다(모션 줄이기 T11 — 4a 검토 F4).
 */
@OptIn(ExperimentalTestApi::class)
fun runFlowTest(
    fakes: FlowFakes = FlowFakes(),
    widthDp: Int = PHONE_WIDTH_DP,
    heightDp: Int = PHONE_HEIGHT_DP,
    hooks: PlatformHooks = PlatformHooks(),
    block: suspend ComposeUiTest.(FlowFakes) -> Unit,
): TestResult = runComposeUiTest {
    fakes.window.widthDp = widthDp
    fakes.window.heightDp = heightDp
    startKoin { modules(flowModule(fakes), viewModelModule) }
    try {
        setContent { SizedApp(fakes, hooks) }
        block(fakes)
    } finally {
        stopKoin()
    }
}

/**
 * 흐름 중에 창 크기를 바꾼다(4a-fixes2 G1 T12 — 웹에서 창을 끌어 줄이는 것과 같다, Activity 재생성 없음). 창 크기
 * 클래스·칸 수가 따라가 두 칸 ↔ 한 칸 장면이 바뀐다. UI 스레드에서 쓰고 화면이 가라앉을 때까지 기다린다.
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.resizeWindow(fakes: FlowFakes, widthDp: Int, heightDp: Int) {
    runOnIdle {
        fakes.window.widthDp = widthDp
        fakes.window.heightDp = heightDp
    }
    awaitIdle()
}

/**
 * 시스템 뒤로 한 번(Android 뒤로 제스처·단추, 웹 Esc — 4b-fixes B1 T19). 하네스가 붙인 [FlowFakes.systemBack]으로
 * 끝난 뒤로를 넣고 화면이 가라앉을 때까지 기다린다.
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.pressSystemBack(fakes: FlowFakes) {
    runOnIdle { fakes.systemBack.backCompleted() }
    awaitIdle()
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
