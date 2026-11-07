package com.cherryzp.animalshelter.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.allDrawableResources
import com.cherryzp.animalshelter.resources.allPluralStringResources
import com.cherryzp.animalshelter.resources.allStringResources
import com.cherryzp.animalshelter.resources.sex_male
import com.cherryzp.animalshelter.ui.flow.awaitCondition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestResult
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.preloadImageVector
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => navigator.languages[0]")
private external fun navigatorLanguage(): String

// 브라우저 언어를 바꾼 것처럼 한다 — Compose(Locale.current)는 navigator.languages[0], CMP 리소스 비구성
// 경로(getString)는 navigator.language를 읽는다. 인스턴스에 덮어쓴 속성을 지우면 원래 값으로 돌아간다.
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    "(tag) => {" +
        " Object.defineProperty(navigator, 'language', { configurable: true, get: () => tag });" +
        " Object.defineProperty(navigator, 'languages', { configurable: true, get: () => [tag] }); }"
)
private external fun overrideNavigatorLanguage(tag: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => { delete navigator.language; delete navigator.languages; }")
private external fun restoreNavigatorLanguage()

/** 지금 브라우저 언어가 아닌 쪽(ko ↔ en) — 다른 테스트가 읽지 않아 CMP 문구 캐시가 비어 있는 언어. */
private fun otherLanguageTag(): String =
    if (navigatorLanguage().startsWith("ko")) "en-US" else "ko-KR"

/** CMP가 아직 못 읽은 벡터 대신 내는 자리표시의 이름(components-resources `ImageResources.kt`). */
private const val EMPTY_VECTOR_NAME = "emptyImageVector"

/** 첫 구성에서 읽은 값(키 → 문구). */
private class FirstComposition {
    var values: List<Pair<String, String>>? = null
}

/**
 * 모든 문구·복수 문구·벡터 아이콘(`ic_*`)을 읽고 첫 구성의 값만 남긴다 — 웹에서 아직 못 읽은 문구는 첫 구성에서 "",
 * 아이콘은 빈 자리표시 벡터(이름 [EMPTY_VECTOR_NAME])다. 아이콘은 읽었으면 "icon", 아니면 ""로 남긴다.
 */
@Composable
private fun FirstCompositionProbe(probe: FirstComposition) {
    val strings = Res.allStringResources.entries.map { (key, res) -> key to stringResource(res) }
    val plurals = Res.allPluralStringResources.entries.map { (key, res) ->
        key to pluralStringResource(res, 1)
    }
    val icons = Res.allDrawableResources.entries.filter {
        it.key.startsWith("ic_")
    }.map { (key, res) ->
        key to if (vectorResource(res).name == EMPTY_VECTOR_NAME) "" else "icon"
    }
    val first = remember { strings + plurals + icons }
    LaunchedEffect(Unit) { probe.values = first }
}

/** 웹 진입점(webApp `main.kt`)의 문 — 글꼴만 빼고 같다(글꼴은 문구·아이콘 빈칸과 무관하다). */
@OptIn(ExperimentalResourceApi::class)
@Composable
private fun rememberWebResourcesReady(): Boolean {
    val icons = AppVectorIcons.map { preloadImageVector(it) }
    val strings = rememberStringResourcesReady()
    return icons.all { it.value != null } && strings
}

@OptIn(ExperimentalTestApi::class)
private suspend fun ComposeUiTest.awaitProbe(probe: FirstComposition): List<Pair<String, String>> {
    awaitCondition("첫 구성 문구") { probe.values != null }
    val values = checkNotNull(probe.values)
    val blanks = values.filter { it.second.isEmpty() }.map { it.first }
    assertEquals(emptyList(), blanks, "첫 구성에서 빈 문구 ${blanks.size}/${values.size}")
    return values
}

/**
 * 웹 첫 프레임 빈 문구(2026-11-07): CMP 웹의 `stringResource`는 문구를 비동기로 읽어 아직 못 읽은 문구를 첫 구성에서
 * ""로 그린다 — 배지 "보호중"·"마감 임박 · D-3"이 빈 상자, 카드의 "수컷"이 빠진 채로 로딩 화면이 걷혔다.
 * 웹 진입점(webApp `main.kt`)과 같은 문(문구 [rememberStringResourcesReady] + 아이콘
 * [AppVectorIcons]의 `preloadImageVector`)이 열린 뒤에는 모든 문구·아이콘이 첫 구성에서 채워져야 한다.
 */
@OptIn(ExperimentalTestApi::class)
class ResourcesPreloadTest {
    /** T24 브라우저 언어 그대로: 준비된 뒤 첫 구성에서 모든 문구·복수 문구가 채워져 있다. */
    @Test
    fun t24FirstCompositionFilledAfterReady() = runComposeUiTest {
        val probe = FirstComposition()
        setContent { if (rememberWebResourcesReady()) FirstCompositionProbe(probe) }
        awaitProbe(probe)
    }

    /** T24 다른 언어로 열어도(문구 캐시가 비어 있다) 준비된 뒤 첫 구성이 채워져 있다. */
    @Test
    fun t24OtherLanguageFirstCompositionFilled(): TestResult {
        overrideNavigatorLanguage(otherLanguageTag())
        return runComposeUiTest {
            try {
                val probe = FirstComposition()
                setContent { if (rememberWebResourcesReady()) FirstCompositionProbe(probe) }
                val values = awaitProbe(probe).toMap()
                assertEquals(getString(Res.string.sex_male), values["sex_male"])
            } finally {
                restoreNavigatorLanguage()
            }
        }
    }

    /**
     * T24 열린 채 언어가 바뀌면(웹 `languagechange`) 화면을 내리지 않고(상태 유지) 새 언어 문구를 미리 읽는다 — 그 뒤
     * 새로 그리는 곳은 첫 구성부터 새 언어로 채워져 있다.
     */
    @Test
    fun t24LanguageChangeKeepsContentAndPreloads() = runComposeUiTest {
        val startTag = navigatorLanguage()
        try {
            var languageChanges by mutableIntStateOf(0)
            var showProbe by mutableStateOf(false)
            var preloaded = 0
            var contentCreated = 0
            val probe = FirstComposition()
            setContent {
                // 웹 진입점이 languagechange마다 바꾸는 값 — 이 범위를 다시 그려 리소스 환경을 다시 읽게 한다
                languageChanges
                if (rememberStringResourcesReady(onPreloaded = { preloaded++ })) {
                    remember { contentCreated++ }
                    if (showProbe) FirstCompositionProbe(probe)
                }
            }
            awaitCondition("처음 미리 읽기") { preloaded == 1 }
            overrideNavigatorLanguage(if (startTag.startsWith("ko")) "en-US" else "ko-KR")
            runOnIdle { languageChanges++ }
            awaitCondition("새 언어 미리 읽기") { preloaded == 2 }
            runOnIdle { showProbe = true }
            val values = awaitProbe(probe).toMap()
            assertEquals(getString(Res.string.sex_male), values["sex_male"])
            assertEquals(1, contentCreated, "언어가 바뀌어도 화면을 다시 만들지 않는다")
            assertTrue(values.isNotEmpty())
        } finally {
            restoreNavigatorLanguage()
        }
    }
}
