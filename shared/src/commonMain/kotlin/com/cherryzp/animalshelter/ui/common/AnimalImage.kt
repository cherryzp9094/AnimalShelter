package com.cherryzp.animalshelter.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.decode.DataSource
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.ic_pets
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

/** 세로 사진은 머리가 위쪽에 있어 위로 치우쳐 자른다(06 4.1). 중계 `sq480`의 gravity `y: 0.3`과 같다. */
private val PortraitAlignment = BiasAlignment(horizontalBias = 0f, verticalBias = -0.4f)

/** 위 겹(본 사진)의 단계(5c). */
private enum class PhotoStage { LOADING, FADING, DONE, FAILED }

/**
 * 동물 사진(브리핑 P6, 5c). 뒤에 placeholder(`surfaceContainerHighest` + `ic_pets`)를 깔고 위에 사진을 그린다 —
 * 받는 중·실패·사진 없음이 모두 같은 모습이다. 크롭이면 세로 사진만 위쪽 치우침(`painter.intrinsicSize`로 판별).
 * 설명은 카드 문장·상세 `photo_a11y`가 맡으므로 사진 겹은 `contentDescription = null`.
 *
 * 사진 주소는 [LocalPhotoVariants]가 [frame]과 표시 폭(px)으로 바꾼다(웹 — 중계 `?size=`, Android는 그대로).
 * 자리표시 주소(웹 — 작은 사진 `_s`)가 있으면 **두 겹**이다(5c 1.5 (a)): 아래 겹 = 자리표시, 위 겹 = 본 사진, 같이 시작한다.
 * 위 겹은 crossfade(150ms)로 불투명한 아래 겹 위에 나타나고, 성공해 crossfade가 끝나면(메모리 캐시 적중·모션 줄이기면 바로)
 * 아래 겹을 뺀다 — `Fit`에서 비율 반올림으로 1px 테두리가 남지 않게. 위 겹이 실패하면 아래 겹도 빼서 지금의 오류 모습이다.
 * 흐림 효과는 넣지 않는다(120px를 키운 것만으로 흐리고, 카드마다 blur 층은 웹 스크롤 비용이 든다).
 */
@Composable
fun AnimalImage(
    url: String?,
    frame: PhotoFrame,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    background: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
) {
    BoxWithConstraints(
        modifier = modifier.background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_pets),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp)
        )
        if (url != null) {
            val widthPx = if (constraints.hasBoundedWidth) constraints.maxWidth else 0
            val variants = LocalPhotoVariants.current
            val main =
                remember(variants, url, frame, widthPx) { variants.main(url, frame, widthPx) }
            val placeholder = remember(variants, url) { variants.placeholder(url) }
            val reducedMotion = LocalReducedMotion.current
            var stage by remember(main) { mutableStateOf(PhotoStage.LOADING) }
            if (stage == PhotoStage.FADING) {
                LaunchedEffect(main) {
                    delay(PHOTO_CROSSFADE_MILLIS.toLong())
                    stage = PhotoStage.DONE
                }
            }
            if (placeholder != null &&
                (stage == PhotoStage.LOADING || stage == PhotoStage.FADING)
            ) {
                PhotoLayer(url = placeholder, contentScale = contentScale)
            }
            PhotoLayer(
                url = main,
                contentScale = contentScale,
                onSuccess = { success ->
                    stage =
                        if (reducedMotion || success.result.dataSource == DataSource.MEMORY_CACHE) {
                            PhotoStage.DONE
                        } else {
                            PhotoStage.FADING
                        }
                },
                onError = { stage = PhotoStage.FAILED }
            )
        }
    }
}

/** 사진 한 겹: Coil `AsyncImage`, 세로 사진 치우침(크롭일 때), 모션 줄이기면 crossfade 끔. */
@Composable
private fun PhotoLayer(
    url: String,
    contentScale: ContentScale,
    onSuccess: (AsyncImagePainter.State.Success) -> Unit = {},
    onError: () -> Unit = {},
) {
    var alignment by remember(url) { mutableStateOf<Alignment>(Alignment.Center) }
    AsyncImage(
        model = rememberImageModel(url),
        contentDescription = null,
        contentScale = contentScale,
        alignment = alignment,
        onSuccess = { success ->
            val size = success.painter.intrinsicSize
            if (contentScale == ContentScale.Crop && size.height > size.width) {
                alignment = PortraitAlignment
            }
            onSuccess(success)
        },
        onError = { onError() },
        modifier = Modifier.fillMaxSize()
    )
}
