package com.cherryzp.animalshelter.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.cherryzp.animalshelter.navigation.PhotoViewerKey
import com.cherryzp.animalshelter.navigation.ShelterInfoKey
import com.cherryzp.animalshelter.platform.ExternalActions
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.action_back
import com.cherryzp.animalshelter.resources.action_call_shelter
import com.cherryzp.animalshelter.resources.action_go_to_list
import com.cherryzp.animalshelter.resources.action_retry
import com.cherryzp.animalshelter.resources.detail_not_found
import com.cherryzp.animalshelter.resources.detail_notice_gone
import com.cherryzp.animalshelter.resources.error_network
import com.cherryzp.animalshelter.ui.common.BookmarkToggleButton
import com.cherryzp.animalshelter.ui.common.BottomBar
import com.cherryzp.animalshelter.ui.common.BottomBarButtonHeight
import com.cherryzp.animalshelter.ui.common.CenteredLoading
import com.cherryzp.animalshelter.ui.common.FullScreenMessage
import com.cherryzp.animalshelter.ui.common.MessageAction
import com.cherryzp.animalshelter.ui.common.MessageSnackbarEffect
import com.cherryzp.animalshelter.ui.common.PART_SEPARATOR
import com.cherryzp.animalshelter.ui.common.TestTags
import com.cherryzp.animalshelter.ui.common.resolve
import com.cherryzp.animalshelter.ui.common.uiText
import com.cherryzp.animalshelter.ui.preview.PreviewData
import com.cherryzp.animalshelter.ui.theme.AnimalShelterTheme
import org.jetbrains.compose.resources.stringResource

/** 이 폭부터 2단(브리핑 B4 — 칸이 받은 폭). */
private val TwoColumnMinWidth = 600.dp

/** 상단 바 제목(D6)의 나타남·사라짐(emil 점검 — 사라짐이 더 빠르게). */
private const val TITLE_FADE_IN_MILLIS = 150
private const val TITLE_FADE_OUT_MILLIS = 100

/** 2단에서 바 제목을 보이는 스크롤 겹침 — `TopAppBar`가 `scrolledContainerColor`로 바꾸는 조건과 같다(R9). */
private const val TITLE_OVERLAP_THRESHOLD = 0.01f

/** 본문 최대 폭(06 4.2). */
private val ContentMaxWidth = 840.dp

/** 사진 틀 최대 한 변. 창 높이가 compact(폰 가로)면 칸 높이의 55%. */
private val PhotoMaxSide = 480.dp
private const val COMPACT_HEIGHT_PHOTO_FRACTION = 0.55f

/** 2단 왼쪽 열: 40%, 280~420dp. */
private const val LEFT_COLUMN_FRACTION = 0.4f
private val LeftColumnMin = 280.dp
private val LeftColumnMax = 420.dp

/**
 * 상세 화면(ViewModel을 받아 상태를 모은다, P2). 전화·지도·복사는 여기서 부르고 결과만 ViewModel에 알린다(B3).
 * @param showBack 목록 칸이 같이 보이면 false(B5 — 두 칸에서 뒤로 화살표 금지)
 */
@Composable
fun AnimalDetailScreen(
    viewModel: AnimalDetailViewModel,
    showBack: Boolean,
    onBack: () -> Unit,
    onOpenPhoto: (PhotoViewerKey) -> Unit,
    onOpenShelter: (ShelterInfoKey) -> Unit,
    onGoToList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val detail = state.detail
    val title = detail?.titleParts?.map { it.resolve() }?.joinToString(PART_SEPARATOR)
    AnimalDetailScreen(
        state = state,
        showBack = showBack,
        onBack = onBack,
        onToggleBookmark = viewModel::onToggleBookmark,
        onRetry = viewModel::onRetry,
        onGoToList = onGoToList,
        onOpenPhoto = { index ->
            if (detail != null) {
                onOpenPhoto(PhotoViewerKey(detail.desertionNo, index, detail.photoOriginals, title))
            }
        },
        onOpenShelter = {
            val shelter = detail?.shelter
            val regNo = shelter?.regNo
            if (regNo != null) {
                onOpenShelter(ShelterInfoKey(regNo, shelter.name, shelter.tel, shelter.address))
            }
        },
        onDial = { viewModel.onExternalActionResult(ExternalActions.dial(it)) },
        onOpenMap = {
            viewModel.onExternalActionResult(ExternalActions.openMap(it.lat, it.lng, it.label))
        },
        onCopyAddress = { viewModel.onCopyResult(ExternalActions.copyText(it)) },
        onMessageShown = viewModel::onMessageShown,
        onMessageAction = viewModel::onMessageAction,
        modifier = modifier
    )
}

/** 상세 화면(상태만 — `@Preview`·UI 테스트). 03 2.3, 브리핑 B4~B6. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalDetailScreen(
    state: AnimalDetailUiState,
    showBack: Boolean,
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit,
    onRetry: () -> Unit,
    onGoToList: () -> Unit,
    onOpenPhoto: (index: Int) -> Unit,
    onOpenShelter: () -> Unit,
    onDial: (String) -> Unit,
    onOpenMap: (MapTarget) -> Unit,
    onCopyAddress: (String) -> Unit,
    onMessageShown: (id: Long) -> Unit,
    onMessageAction: (action: MessageAction, id: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val nameLine = remember { NameLineTracker() }
    // 1단: 품종명 줄이 바 아래로 지나간 뒤(D6). 2단: 바가 스크롤 색으로 바뀌는 순간(R9) — 본문이 짧아 줄이 바 아래로
    // 못 지나가기도 한다. 조건은 TopAppBar가 색을 바꾸는 것과 같다(overlappedFraction > 0.01).
    val showTitle by remember(scrollBehavior) {
        derivedStateOf {
            if (nameLine.twoColumn) {
                scrollBehavior.state.overlappedFraction > TITLE_OVERLAP_THRESHOLD
            } else {
                nameLine.isUnderBar
            }
        }
    }
    MessageSnackbarEffect(
        message = state.message,
        snackbarHostState = snackbarHostState,
        onShown = onMessageShown,
        onAction = onMessageAction
    )
    val detail = state.detail
    val tel = detail?.shelter?.tel

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    // D6: 품종명 줄이 바 아래로 지나간 뒤에만(사진 아래 제목과 겹치지 않게). 2단은 스크롤하면(R9).
                    // 불투명도만 짧게(emil 점검): 나타날 때 150ms ease-out, 사라질 때 100ms — 움직임은 없다.
                    AnimatedVisibility(
                        visible = showTitle && detail != null,
                        enter = fadeIn(tween(TITLE_FADE_IN_MILLIS, easing = LinearOutSlowInEasing)),
                        exit = fadeOut(tween(TITLE_FADE_OUT_MILLIS))
                    ) {
                        Text(
                            text = detail?.titleParts?.map { it.resolve() }
                                ?.joinToString(PART_SEPARATOR).orEmpty(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag(TestTags.DETAIL_TITLE)
                        )
                    }
                },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(Res.string.action_back)
                            )
                        }
                    }
                },
                actions = {
                    if (detail != null) {
                        BookmarkToggleButton(
                            bookmarked = state.isBookmarked,
                            onClick = onToggleBookmark,
                            modifier = Modifier.testTag(TestTags.DETAIL_BOOKMARK)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            if (tel != null) CallBar(onClick = { onDial(tel) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            val error = state.error
            when {
                detail != null -> DetailContent(
                    state = state,
                    detail = detail,
                    nameLine = nameLine,
                    onOpenPhoto = onOpenPhoto,
                    onOpenShelter = onOpenShelter,
                    onDial = onDial,
                    onOpenMap = onOpenMap,
                    onCopyAddress = onCopyAddress
                )

                state.notFound -> FullScreenMessage(
                    text = stringResource(Res.string.detail_not_found),
                    primaryLabel = stringResource(Res.string.action_go_to_list),
                    onPrimary = onGoToList
                )

                error != null -> FullScreenMessage(
                    text = error.resolve(),
                    primaryLabel = stringResource(Res.string.action_retry)
                        .takeIf { state.canRetry },
                    onPrimary = onRetry
                )

                else -> CenteredLoading()
            }
            if (state.isLoading && detail != null) {
                LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
    }
}

/**
 * 상세 바 제목(3d D6)을 보일지 정하는 두 위치(루트 좌표, px): 본문 스크롤 영역의 위 끝(= 상단 바 아래 끝)과
 * 요약 품종명 줄의 아래 끝. 스크롤하면 줄의 위치만 바뀐다. 줄이 아직 배치되지 않았으면 무한대라 제목이 없다.
 * [twoColumn]은 본문이 2단인지(본문의 `BoxWithConstraints`가 `SideEffect`로 쓴다 — 2단은 다른 규칙, R9).
 */
@Stable
private class NameLineTracker {
    var viewportTop by mutableFloatStateOf(0f)
    var lineBottom by mutableFloatStateOf(Float.POSITIVE_INFINITY)
    var twoColumn by mutableStateOf(false)

    /** 품종명 줄의 아래 끝이 바 아래로 지나갔는가. */
    val isUnderBar: Boolean get() = lineBottom <= viewportTop
}

/** 하단 고정 바: 보호소에 전화하기(filled, 칸 폭 전체, 높이 48dp). 공유는 Phase 4a(같은 바). */
@Composable
private fun CallBar(onClick: () -> Unit) {
    BottomBar {
        Button(
            onClick = onClick,
            modifier = Modifier.weight(1f).heightIn(min = BottomBarButtonHeight)
        ) {
            Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = stringResource(Res.string.action_call_shelter),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

/**
 * 본문(B4): 칸이 받은 폭 < 600dp → 1단(사진 → 섹션), ≥ 600dp → 2단(왼쪽 40%·280~420dp에 사진 + 요약,
 * 오른쪽에 섹션 2~8). 전체가 한 `verticalScroll`, 본문 최대 840dp 가운데.
 */
@Composable
private fun DetailContent(
    state: AnimalDetailUiState,
    detail: AnimalDetailUi,
    nameLine: NameLineTracker,
    onOpenPhoto: (Int) -> Unit,
    onOpenShelter: () -> Unit,
    onDial: (String) -> Unit,
    onOpenMap: (MapTarget) -> Unit,
    onCopyAddress: (String) -> Unit,
) {
    val heightCompact = !currentWindowAdaptiveInfoV2().windowSizeClass
        .isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
    val photoTitle = detail.titleParts.map { it.resolve() }.joinToString(PART_SEPARATOR)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val photoMax: Dp =
            if (heightCompact) maxHeight * COMPACT_HEIGHT_PHOTO_FRACTION else PhotoMaxSide
        val paneWidth = maxWidth
        val twoColumn = paneWidth >= TwoColumnMinWidth
        // 컴포지션 중에 바깥 상태를 쓰지 않는다 — 적용된 뒤에 바 제목 규칙에 알린다(R9).
        SideEffect { nameLine.twoColumn = twoColumn }
        val edge = if (twoColumn) 24.dp else 16.dp
        val body: @Composable (Modifier) -> Unit = { mod ->
            DetailSectionsBody(
                detail = detail,
                shelterToday = state.shelterToday,
                hasMap = state.map != null,
                onDial = onDial,
                onCopyAddress = onCopyAddress,
                onOpenMap = { state.map?.let(onOpenMap) },
                onOpenShelter = onOpenShelter,
                modifier = mod
            )
        }
        val banner: @Composable () -> Unit = {
            if (state.isStale) NoticeBanner(stringResource(Res.string.detail_notice_gone))
        }
        val nameLineModifier = Modifier.onGloballyPositioned {
            nameLine.lineBottom = it.positionInRoot().y + it.size.height
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
                .onGloballyPositioned { nameLine.viewportTop = it.positionInRoot().y }
                .verticalScroll(rememberScrollState())
                .testTag(TestTags.DETAIL_SCROLL)
        ) {
            if (twoColumn) {
                val contentWidth = min(paneWidth, ContentMaxWidth) - edge * 2
                val leftWidth = (contentWidth * LEFT_COLUMN_FRACTION)
                    .coerceIn(LeftColumnMin, LeftColumnMax)
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth()
                        .padding(horizontal = edge, vertical = 16.dp)
                ) {
                    banner()
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.width(leftWidth)
                        ) {
                            PhotoPager(detail.photoUrls, photoTitle, photoMax, onOpenPhoto)
                            SummarySection(detail, nameLineModifier = nameLineModifier)
                        }
                        body(Modifier.weight(1f))
                    }
                }
            } else {
                PhotoPager(detail.photoUrls, photoTitle, photoMax, onOpenPhoto)
                Column(
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth()
                        .padding(horizontal = edge, vertical = 16.dp)
                ) {
                    banner()
                    SummarySection(detail, nameLineModifier = nameLineModifier)
                    body(Modifier)
                }
            }
        }
    }
}

@Composable
private fun PreviewDetail(
    state: AnimalDetailUiState,
    modifier: Modifier = Modifier,
    showBack: Boolean = true,
) {
    AnimalShelterTheme {
        AnimalDetailScreen(
            state = state,
            showBack = showBack,
            onBack = {},
            onToggleBookmark = {},
            onRetry = {},
            onGoToList = {},
            onOpenPhoto = {},
            onOpenShelter = {},
            onDial = {},
            onOpenMap = {},
            onCopyAddress = {},
            onMessageShown = {},
            onMessageAction = { _, _ -> },
            modifier = modifier
        )
    }
}

@Preview
@Composable
private fun DetailOneColumnRichPreview() {
    PreviewDetail(PreviewData.detailState(PreviewData.richAnimal, bookmarked = true))
}

@Preview
@Composable
private fun DetailOneColumnSparsePreview() {
    PreviewDetail(PreviewData.detailState(PreviewData.sparseAnimal))
}

@Preview
@Composable
private fun DetailTwoColumnPreview() {
    PreviewDetail(
        PreviewData.detailState(PreviewData.richAnimal),
        modifier = Modifier.width(760.dp),
        showBack = false
    )
}

@Preview
@Composable
private fun DetailTwoColumnSparsePreview() {
    PreviewDetail(
        PreviewData.detailState(PreviewData.sparseAnimal),
        modifier = Modifier.width(760.dp)
    )
}

@Preview
@Composable
private fun DetailStalePreview() {
    PreviewDetail(
        PreviewData.detailState(PreviewData.richAnimal, bookmarked = true).copy(isStale = true)
    )
}

@Preview
@Composable
private fun DetailNotFoundPreview() {
    PreviewDetail(AnimalDetailUiState(notFound = true))
}

@Preview
@Composable
private fun DetailErrorPreview() {
    PreviewDetail(AnimalDetailUiState(error = uiText(Res.string.error_network), canRetry = true))
}
