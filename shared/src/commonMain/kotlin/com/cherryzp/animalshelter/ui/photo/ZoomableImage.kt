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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import coil3.compose.AsyncImage
import com.cherryzp.animalshelter.ui.common.LocalReducedMotion
import com.cherryzp.animalshelter.ui.common.rememberImageModel
import kotlin.math.pow
import kotlinx.coroutines.launch

/** 확대 범위와 두 번 탭 배율(브리핑 B9). */
private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f
private const val DOUBLE_TAP_MILLIS = 200

/** 마우스 휠 한 칸의 배율(4a M3). 터치패드처럼 잘게 오는 값은 그만큼만(한 번에 한 칸까지). */
private const val WHEEL_STEP = 1.1f

/**
 * 한 쪽의 사진: `Fit` + 핀치 줌 1~5배 · 두 번 탭 1배 ↔ 2.5배(누른 곳을 중심으로) · 확대 중 드래그(경계 안) ·
 * 마우스 휠 한 칸 1.1배(포인터 위치 기준, 1~5배 — 4a M3; 1배에서 줄이는 쪽은 아무것도 안 하고 이벤트도 남긴다).
 * 1배에서는 한 손가락 드래그를 받지 않는다 — 페이저가 넘긴다. 확대 여부는 [onZoomChanged]로 알린다
 * (확대 중에는 페이저 넘기기를 막는다). 모션 줄이기(M2)면 두 번 탭 확대가 움직임 없이 바로 바뀐다.
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
    val reducedMotion by rememberUpdatedState(LocalReducedMotion.current)
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
                        if (reducedMotion) {
                            apply(target, targetOffset)
                        } else {
                            scope.launch {
                                animate(0f, 1f, animationSpec = tween(DOUBLE_TAP_MILLIS)) { t, _ ->
                                    apply(
                                        startScale + (target - startScale) * t,
                                        startOffset + (targetOffset - startOffset) * t
                                    )
                                }
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                // 마우스 휠 줌(M3): 위로(음수) = 확대. 포인터 아래의 점이 그 자리에 남도록 옮긴다
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type != PointerEventType.Scroll) continue
                        val change = event.changes.firstOrNull() ?: continue
                        val notches = change.scrollDelta.y.coerceIn(-1f, 1f)
                        if (notches == 0f) continue
                        val newScale = (
                            scale * WHEEL_STEP.pow(
                                -notches
                            )
                            ).coerceIn(MIN_SCALE, MAX_SCALE)
                        if (newScale == scale) continue // 1배에서 줄이기·5배에서 키우기는 아무것도 안 한다
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val anchor = change.position - center
                        apply(newScale, anchor - (anchor - offset) * (newScale / scale))
                        change.consume()
                    }
                }
            }
            .transformable(state = transformState, canPan = { scale > MIN_SCALE })
    ) {
        AsyncImage(
            model = rememberImageModel(url),
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
