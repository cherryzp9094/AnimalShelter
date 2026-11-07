package com.cherryzp.animalshelter.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.allDrawableResources
import com.cherryzp.animalshelter.resources.allPluralStringResources
import com.cherryzp.animalshelter.resources.allStringResources
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.rememberResourceEnvironment

/**
 * 앱의 벡터 아이콘(`drawable/ic_*.xml`, 이름 앞자리 `ic_`). `painterResource`의 벡터도 웹에서는 비동기라 아직 못 읽은
 * 아이콘은 첫 구성에서 빈 그림이다(북마크 표시·발바닥) — 웹 진입점이 글꼴처럼 `preloadImageVector`로 먼저 읽는다.
 * 그림(`logo_white.png` 등)은 넣지 않는다.
 */
val AppVectorIcons: List<DrawableResource> by lazy {
    Res.allDrawableResources.filterKeys { it.startsWith("ic_") }.values.toList()
}

/**
 * [environment]의 모든 문구·복수 문구를 한 번씩 읽어 CMP 문구 캐시를 채운다.
 *
 * CMP 웹(wasmJs)의 `stringResource`·`pluralStringResource`는 문구를 비동기로 읽는다 — 아직 읽지 않은 문구는 첫 구성에서
 * ""로 그려지고 다음 프레임에 채워진다(components-resources 1.12.2 `ResourceState.web.kt`). 캐시는 문구 파일 안의 위치
 * (경로·오프셋·크기)로 잡히고 읽은 값을 계속 들고 있어서, 여기서 한 번 읽어 두면 그 뒤 구성에서는 같은 프레임에 바로
 * 나온다. Android는 구성 안에서 동기로 읽으므로 부르지 않는다. 문구 하나를 못 읽어도 나머지는 계속 읽는다(그 문구만
 * 예전처럼 늦게 나온다).
 */
suspend fun preloadStringResources(environment: ResourceEnvironment) {
    coroutineScope {
        Res.allStringResources.values.forEach { resource ->
            launch { readQuietly { getString(environment, resource) } }
        }
        Res.allPluralStringResources.values.forEach { resource ->
            // 캐시는 수량과 상관없이 문구 항목 하나 — 아무 수량으로 한 번 읽으면 된다
            launch { readQuietly { getPluralString(environment, resource, 1) } }
        }
    }
}

private inline fun readQuietly(read: () -> Unit) {
    try {
        read()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        // 못 읽은 문구는 stringResource가 다시 읽는다 — 화면을 막지 않는다
    }
}

/**
 * 웹 진입점용: 지금 리소스 환경(언어)의 문구를 [preloadStringResources]로 다 읽었으면 true(웹 첫 프레임 빈 문구,
 * 2026-11-07). 처음 한 번 true가 된 뒤에는 다시 false가 되지 않는다 — 언어가 바뀌면(웹 `languagechange` 뒤 이 함수를
 * 부르는 범위가 다시 그려질 때) 화면을 내리지 않고 새 언어를 뒤에서 미리 읽는다. 다 읽을 때마다 [onPreloaded]를 그
 * 환경으로 부른다.
 */
@Composable
fun rememberStringResourcesReady(onPreloaded: (ResourceEnvironment) -> Unit = {}): Boolean {
    val environment = rememberResourceEnvironment()
    val callback by rememberUpdatedState(onPreloaded)
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(environment) {
        preloadStringResources(environment)
        ready = true
        callback(environment)
    }
    return ready
}
