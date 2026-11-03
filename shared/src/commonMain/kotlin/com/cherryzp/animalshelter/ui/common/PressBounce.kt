package com.cherryzp.animalshelter.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** 북마크 누름 모션(06 5절, 브리핑 B8): scale 1 → 0.9 → 1, 모두 120ms. */
private const val BOUNCE_HALF_MILLIS = 60
private const val BOUNCE_SCALE = 0.9f

/** 누른 아이콘을 한 번 작게 했다 돌린다. [play]를 누를 때 부르고 [bounce]로 그린다. */
@Stable
class PressBounce internal constructor(
    private val scale: Animatable<Float, AnimationVector1D>,
    private val scope: CoroutineScope,
) {
    val value: Float get() = scale.value

    fun play() {
        scope.launch {
            scale.snapTo(1f)
            scale.animateTo(BOUNCE_SCALE, tween(BOUNCE_HALF_MILLIS))
            scale.animateTo(1f, tween(BOUNCE_HALF_MILLIS))
        }
    }
}

@Composable
fun rememberPressBounce(): PressBounce {
    val scope = rememberCoroutineScope()
    return remember(scope) { PressBounce(Animatable(1f), scope) }
}

/** 그리기 단계에서만 읽는다(재구성 없음). */
fun Modifier.bounce(state: PressBounce): Modifier = graphicsLayer {
    scaleX = state.value
    scaleY = state.value
}
