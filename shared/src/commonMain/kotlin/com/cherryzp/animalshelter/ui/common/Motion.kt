package com.cherryzp.animalshelter.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * 모션 줄이기(4a M2). 참이면 앱이 넣은 움직임을 끈다 — Nav3 전환, 사진 crossfade, 북마크 누름, 상세 바 제목 페이드,
 * 사진 두 번 탭 확대. M3 컴포넌트 안쪽 움직임(물결·스낵바·메뉴)은 그대로다. 웹은 `prefers-reduced-motion`을 따르고
 * (`:webApp`이 `PlatformHooks.reducedMotion`으로), Android는 Compose가 시스템 애니메이터 배율을 이미 따르므로 늘 거짓.
 */
val LocalReducedMotion = compositionLocalOf { false }

/** 사진 crossfade(06 5절). 로더 기본값(`App`)과 두 겹 사진의 아래 겹 빼기(`AnimalImage`, 5c)가 같이 쓴다. */
internal const val PHOTO_CROSSFADE_MILLIS = 150

/**
 * 사진 요청 모델(사진 세 곳 공용). 모션 줄이기면 crossfade를 끈 요청, 아니면 주소 그대로(로더 기본 crossfade 150ms).
 * 데이터는 같은 주소라 메모리 캐시·주소 매퍼는 그대로 쓴다.
 */
@Composable
fun rememberImageModel(url: String): Any {
    if (!LocalReducedMotion.current) return url
    val context = LocalPlatformContext.current
    return remember(url, context) {
        ImageRequest.Builder(context).data(url).crossfade(false).build()
    }
}
