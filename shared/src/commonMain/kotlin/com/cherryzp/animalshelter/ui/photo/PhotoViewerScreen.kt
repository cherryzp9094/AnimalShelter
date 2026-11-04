package com.cherryzp.animalshelter.ui.photo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.a11y_loading
import com.cherryzp.animalshelter.resources.action_close
import com.cherryzp.animalshelter.resources.action_next_photo
import com.cherryzp.animalshelter.resources.action_previous_photo
import com.cherryzp.animalshelter.resources.action_retry
import com.cherryzp.animalshelter.resources.detail_not_found
import com.cherryzp.animalshelter.resources.photo_a11y
import com.cherryzp.animalshelter.resources.photo_counter
import com.cherryzp.animalshelter.ui.common.DarkSystemBarsEffect
import com.cherryzp.animalshelter.ui.common.LocalReducedMotion
import com.cherryzp.animalshelter.ui.common.progressName
import com.cherryzp.animalshelter.ui.common.resolve
import com.cherryzp.animalshelter.ui.common.statusMessage
import com.cherryzp.animalshelter.ui.common.uiText
import com.cherryzp.animalshelter.ui.theme.AnimalShelterTheme
import com.cherryzp.animalshelter.ui.theme.TABULAR_NUMBERS
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** 사진 뷰어 배경과 글자(테마와 무관 — 사진을 보는 화면). */
private val ViewerBackground = Color.Black
private val ViewerContent = Color.White

/**
 * 위 바 배경의 검정 불투명도. 사진이 바 아래로 들어가도(넓은 창·확대) 흰 글자·아이콘이 대비를 지키게 —
 * 가장 밝은(흰) 사진 위에서 0.4면 2.85:1, 0.6이면 5.74:1(3d 점검 A3).
 */
private const val VIEWER_BAR_SCRIM_ALPHA = 0.6f

/** 사진 뷰어(ViewModel을 받아 상태를 모은다, P2). 뒤로 = 닫기(NavDisplay 기본). */
@Composable
fun PhotoViewerScreen(
    viewModel: PhotoViewerViewModel,
    initialIndex: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PhotoViewerScreen(
        state = state,
        initialIndex = initialIndex,
        onClose = onClose,
        onRetry = viewModel::onRetry,
        modifier = modifier
    )
}

/**
 * 사진 뷰어(상태만, 03 2.4·브리핑 B9): 검은 배경, `HorizontalPager`(시작 = [initialIndex], 쪽 번호는
 * `rememberPagerState`가 저장), 쪽마다 [ZoomableImage]. 확대 중에는 넘기기를 막는다. 위에 닫기 + "1 / 2", 2장
 * 이상이고 확대 중이 아니면 양옆에 이전·다음 단추([ViewerPageButtons]).
 */
@Composable
fun PhotoViewerScreen(
    state: PhotoViewerUiState,
    initialIndex: Int,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 검은 배경 — 테마가 라이트여도 상태 바 아이콘을 밝게(C4)
    DarkSystemBarsEffect()
    Box(modifier = modifier.fillMaxSize().background(ViewerBackground)) {
        val photos = state.photos
        val error = state.error
        when {
            photos.isNotEmpty() -> {
                val pagerState = rememberPagerState(
                    initialPage = initialIndex.coerceIn(0, photos.lastIndex)
                ) { photos.size }
                var zoomed by remember { mutableStateOf(false) }
                HorizontalPager(
                    state = pagerState,
                    key = { photos[it] },
                    userScrollEnabled = !zoomed,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    ZoomableImage(
                        url = photos[page],
                        description = stringResource(
                            Res.string.photo_a11y,
                            page + 1,
                            photos.size,
                            state.title.orEmpty()
                        ),
                        onZoomChanged = { if (page == pagerState.currentPage) zoomed = it }
                    )
                }
                if (photos.size >= 2 && !zoomed) {
                    ViewerPageButtons(pagerState = pagerState, count = photos.size)
                }
                ViewerTopBar(
                    counter = stringResource(
                        Res.string.photo_counter,
                        pagerState.currentPage + 1,
                        photos.size
                    ),
                    onClose = onClose
                )
            }

            error != null -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                ) {
                    Text(
                        text = error.resolve(),
                        color = ViewerContent,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.statusMessage()
                    )
                    if (state.canRetry) {
                        OutlinedButton(onClick = onRetry) {
                            Text(stringResource(Res.string.action_retry), color = ViewerContent)
                        }
                    }
                }
                ViewerTopBar(counter = null, onClose = onClose)
            }

            else -> {
                CircularProgressIndicator(
                    color = ViewerContent,
                    modifier = Modifier.align(Alignment.Center)
                        .progressName(stringResource(Res.string.a11y_loading))
                )
                ViewerTopBar(counter = null, onClose = onClose)
            }
        }
    }
}

/**
 * 이전·다음 사진 단추(5a U2): 화면 왼쪽·오른쪽 세로 가운데, 지름 48dp 원(검정 [VIEWER_BAR_SCRIM_ALPHA] — 흰 사진 위에서도
 * 흰 아이콘이 대비를 지킨다) + 흰 아이콘 24dp, 가장자리에서 8dp + `safeDrawing`의 가로 inset. 첫 쪽에서는 이전, 마지막
 * 쪽에서는 다음을 숨긴다(사진 위의 비활성 단추는 군더더기). 확대 중에는 부르지 않는다 — 확대 중 끌기는 사진 이동이고
 * 넘기기를 막는 규칙(03 2.4)과 같다. 누르면 한 쪽(모션 줄이기면 바로).
 */
@Composable
private fun BoxScope.ViewerPageButtons(pagerState: PagerState, count: Int) {
    val scope = rememberCoroutineScope()
    val reducedMotion = LocalReducedMotion.current
    val current = pagerState.currentPage
    fun goTo(page: Int) {
        scope.launch {
            if (reducedMotion) {
                pagerState.scrollToPage(
                    page
                )
            } else {
                pagerState.animateScrollToPage(page)
            }
        }
    }
    val colors = IconButtonDefaults.iconButtonColors(
        containerColor = ViewerBackground.copy(alpha = VIEWER_BAR_SCRIM_ALPHA),
        contentColor = ViewerContent
    )
    val edge = Modifier
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        .padding(horizontal = 8.dp)
    if (current > 0) {
        IconButton(
            onClick = { goTo(current - 1) },
            colors = colors,
            modifier = Modifier.align(Alignment.CenterStart).then(edge).size(48.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(Res.string.action_previous_photo),
                modifier = Modifier.size(24.dp)
            )
        }
    }
    if (current < count - 1) {
        IconButton(
            onClick = { goTo(current + 1) },
            colors = colors,
            modifier = Modifier.align(Alignment.CenterEnd).then(edge).size(48.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(Res.string.action_next_photo),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ViewerTopBar(counter: String?, onClose: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(ViewerBackground.copy(alpha = VIEWER_BAR_SCRIM_ALPHA))
            // 위·옆만 — 전체 safeDrawing이면 아래 내비게이션 바 높이만큼 바가 두꺼워진다
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
            .padding(4.dp)
    ) {
        IconButton(onClick = onClose) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(Res.string.action_close),
                tint = ViewerContent
            )
        }
        counter?.let {
            Text(
                text = it,
                color = ViewerContent,
                style = MaterialTheme.typography.titleMedium
                    .copy(fontFeatureSettings = TABULAR_NUMBERS),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Preview
@Composable
private fun PhotoViewerErrorPreview() {
    AnimalShelterTheme {
        PhotoViewerScreen(
            state = PhotoViewerUiState(error = uiText(Res.string.detail_not_found)),
            initialIndex = 0,
            onClose = {},
            onRetry = {}
        )
    }
}
