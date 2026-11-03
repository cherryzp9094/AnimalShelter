package com.cherryzp.animalshelter.ui.photo

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

/** 확대 범위와 두 번 탭 배율(브리핑 B9). */
private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f
private const val DOUBLE_TAP_MILLIS = 200

/**
 * 한 쪽의 사진: `Fit` + 핀치 줌 1~5배 · 두 번 탭 1배 ↔ 2.5배(누른 곳을 중심으로) · 확대 중 드래그(경계 안).
 * 1배에서는 한 손가락 드래그를 받지 않는다 — 페이저가 넘긴다. 확대 여부는 [onZoomChanged]로 알린다
 * (확대 중에는 페이저 넘기기를 막는다).
 */
@Composable
fun ZoomableImage(
    url: String,
    description: String,
    onZoomChanged: (zoomed: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(MIN_SCALE) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val currentOnZoomChanged by rememberUpdatedState(onZoomChanged)
    val scope = rememberCoroutineScope()

    fun clamp(value: Offset, atScale: Float): Offset {
        val maxX = size.width * (atScale - 1f) / 2f
        val maxY = size.height * (atScale - 1f) / 2f
        return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
    }

    fun apply(newScale: Float, newOffset: Offset) {
        val wasZoomed = scale > MIN_SCALE
        scale = newScale
        offset = clamp(newOffset, newScale)
        val zoomed = newScale > MIN_SCALE
        if (zoomed != wasZoomed) currentOnZoomChanged(zoomed)
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        apply((scale * zoomChange).coerceIn(MIN_SCALE, MAX_SCALE), offset + panChange)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tap ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val target = if (scale > MIN_SCALE) MIN_SCALE else DOUBLE_TAP_SCALE
                        val startScale = scale
                        val startOffset = offset
                        // 누른 곳이 그 자리에 남도록 옮긴다(1배로 돌아갈 때는 가운데로)
                        val targetOffset = if (target > MIN_SCALE) {
                            (center - tap) * (target - 1f)
                        } else {
                            Offset.Zero
                        }
                        scope.launch {
                            animate(0f, 1f, animationSpec = tween(DOUBLE_TAP_MILLIS)) { t, _ ->
                                apply(
                                    startScale + (target - startScale) * t,
                                    startOffset + (targetOffset - startOffset) * t
                                )
                            }
                        }
                    }
                )
            }
            .transformable(state = transformState, canPan = { scale > MIN_SCALE })
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = description }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
    }
}
