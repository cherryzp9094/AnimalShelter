package com.cherryzp.animalshelter.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.photo_a11y
import com.cherryzp.animalshelter.resources.title_photos
import com.cherryzp.animalshelter.ui.common.AnimalImage
import org.jetbrains.compose.resources.stringResource

/**
 * 상세 사진(브리핑 B4, 06 4.2): 정사각 틀 · `Fit`(잘리지 않게) · `surfaceContainerHigh` 배경, 한 변은 받은 폭과
 * [maxSide] 중 작은 것. 2장 이상이면 아래 점 표시. 탭하면 [onOpen](그 쪽 번호). 사진 위에 글자를 얹지 않는다.
 * [pagerState]는 부르는 쪽(`DetailContent`)이 `rememberPanePagerState`로 한 번 만들어 넘긴다 — 1단 ↔ 2단·두 칸 ↔ 한 칸
 * 장면·재생성 뒤에도 같은 쪽이다(5a K1). 사진이 없으면 쓰지 않는다.
 */
@Composable
fun PhotoPager(
    urls: List<String>,
    title: String,
    maxSide: Dp,
    pagerState: PagerState,
    onOpen: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = MaterialTheme.colorScheme.surfaceContainerHigh
    BoxWithConstraints(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        val side = min(maxWidth, maxSide)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (urls.isEmpty()) {
                AnimalImage(
                    url = null,
                    contentScale = ContentScale.Fit,
                    background = background,
                    modifier = Modifier.size(side).clearAndSetSemantics {}
                )
                return@Column
            }
            val openLabel = stringResource(Res.string.title_photos)
            HorizontalPager(
                state = pagerState,
                key = { urls[it] },
                modifier = Modifier.size(side).background(background)
            ) { page ->
                val description = stringResource(Res.string.photo_a11y, page + 1, urls.size, title)
                AnimalImage(
                    url = urls[page],
                    contentScale = ContentScale.Fit,
                    background = background,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClickLabel = openLabel) { onOpen(page) }
                        .semantics { contentDescription = description }
                )
            }
            if (urls.size >= 2) {
                PagerDots(
                    count = urls.size,
                    current = pagerState.currentPage,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

/**
 * 쪽 점(장식 — 사진 설명이 "사진 n/m"을 읽는다). 지금 점 `primary`, 나머지 `outline`(바탕과 비글자 3:1 이상 —
 * `outlineVariant`는 1.6:1이라 점이 보이지 않았다, 3d 점검 A2). 간격 8dp(A6).
 */
@Composable
private fun PagerDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.clearAndSetSemantics {}
    ) {
        repeat(count) { index ->
            Box(
                Modifier
                    .size(8.dp)
                    .background(
                        if (index == current) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        CircleShape
                    )
            )
        }
    }
}
