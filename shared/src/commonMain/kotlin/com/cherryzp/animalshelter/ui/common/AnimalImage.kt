package com.cherryzp.animalshelter.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.ic_pets
import org.jetbrains.compose.resources.painterResource

/** 세로 사진은 머리가 위쪽에 있어 위로 치우쳐 자른다(06 4.1). */
private val PortraitAlignment = BiasAlignment(horizontalBias = 0f, verticalBias = -0.4f)

/**
 * 동물 사진(브리핑 P6). 뒤에 placeholder(`surfaceContainerHighest` + `ic_pets`)를 깔고 위에 사진을 그린다 —
 * 받는 중·실패·사진 없음이 모두 같은 모습이다. 크롭이면 세로 사진만 위쪽 치우침(`painter.intrinsicSize`로 판별).
 * 설명은 카드 문장에 합치므로 이미지 자체는 `contentDescription = null`.
 */
@Composable
fun AnimalImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    background: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
) {
    Box(modifier = modifier.background(background), contentAlignment = Alignment.Center) {
        Icon(
            painter = painterResource(Res.drawable.ic_pets),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp)
        )
        if (url != null) {
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
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
