package com.cherryzp.animalshelter.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** 북마크 누름 모션(06 5절, 브리핑 B8): scale 1 → 0.9 → 1, 모두 120ms. */
private const val BOUNCE_HALF_MILLIS = 60
private const val BOUNCE_SCALE = 0.9f

/**
 * 강한 ease-out(emil-design-eng 점검, 3d). 기본 `FastOutSlowInEasing`은 처음이 느려 60ms 안에서 누름이 늦게 보인다 —
 * 줄어들 때·돌아올 때 모두 바로 움직이기 시작해야 "들었다"가 전해진다.
 */
private val BounceEasing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

/** 누른 아이콘을 한 번 작게 했다 돌린다. [play]를 누를 때 부르고 [bounce]로 그린다. */
@Stable
class PressBounce internal constructor(
    private val scale: Animatable<Float, AnimationVector1D>,
    private val scope: CoroutineScope,
    private val reducedMotion: () -> Boolean,
) {
    val value: Float get() = scale.value

    /** 모션 줄이기(M2)면 움직이지 않는다. */
    fun play() {
        if (reducedMotion()) return
        scope.launch {
            scale.snapTo(1f)
            scale.animateTo(BOUNCE_SCALE, tween(BOUNCE_HALF_MILLIS, easing = BounceEasing))
            scale.animateTo(1f, tween(BOUNCE_HALF_MILLIS, easing = BounceEasing))
        }
    }
}

@Composable
fun rememberPressBounce(): PressBounce {
    val scope = rememberCoroutineScope()
    val reduced by rememberUpdatedState(LocalReducedMotion.current)
    return remember(scope) { PressBounce(Animatable(1f), scope) { reduced } }
}

/** 그리기 단계에서만 읽는다(재구성 없음). */
fun Modifier.bounce(state: PressBounce): Modifier = graphicsLayer {
    scaleX = state.value
    scaleY = state.value
}
