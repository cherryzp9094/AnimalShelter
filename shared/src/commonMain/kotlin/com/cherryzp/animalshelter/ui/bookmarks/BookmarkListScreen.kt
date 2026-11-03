package com.cherryzp.animalshelter.ui.bookmarks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cherryzp.animalshelter.model.ListLayout
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.action_back
import com.cherryzp.animalshelter.resources.action_layout_grid
import com.cherryzp.animalshelter.resources.action_layout_large
import com.cherryzp.animalshelter.resources.empty_bookmarks
import com.cherryzp.animalshelter.resources.ic_grid_view
import com.cherryzp.animalshelter.resources.ic_view_agenda
import com.cherryzp.animalshelter.resources.title_bookmarks
import com.cherryzp.animalshelter.ui.common.AnimalCardUi
import com.cherryzp.animalshelter.ui.common.AnimalGrid
import com.cherryzp.animalshelter.ui.common.FullScreenMessage
import com.cherryzp.animalshelter.ui.common.MessageAction
import com.cherryzp.animalshelter.ui.common.MessageSnackbarEffect
import com.cherryzp.animalshelter.ui.preview.PreviewData
import com.cherryzp.animalshelter.ui.theme.AnimalShelterTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** 북마크 목록(ViewModel을 받아 상태를 모은다, P2). */
@Composable
fun BookmarkListScreen(
    viewModel: BookmarkListViewModel,
    onBack: () -> Unit,
    onOpenAnimal: (AnimalCardUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BookmarkListScreen(
        state = state,
        onBack = onBack,
        onOpenAnimal = onOpenAnimal,
        onToggleBookmark = viewModel::onToggleBookmark,
        onToggleLayout = viewModel::onToggleLayout,
        onMessageShown = viewModel::onMessageShown,
        onMessageAction = viewModel::onMessageAction,
        onRestoreShown = viewModel::onRestoreShown,
        modifier = modifier
    )
}

/**
 * 북마크 목록(상태만, 03 2.5·브리핑 B10): 뒤로 + 제목 + 보기 전환(목록과 같은 설정값). 그리드·카드는 목록과 공용.
 * 비면 `ic_pets` + `empty_bookmarks`. 그리드 위치는 entry 안의 `rememberLazyGridState`가 지킨다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkListScreen(
    state: BookmarkListUiState,
    onBack: () -> Unit,
    onOpenAnimal: (AnimalCardUi) -> Unit,
    onToggleBookmark: (desertionNo: String) -> Unit,
    onToggleLayout: () -> Unit,
    onMessageShown: (id: Long) -> Unit,
    onMessageAction: (action: MessageAction, id: Long) -> Unit,
    modifier: Modifier = Modifier,
    onRestoreShown: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val gridState = rememberLazyGridState()
    MessageSnackbarEffect(
        message = state.message,
        snackbarHostState = snackbarHostState,
        onShown = onMessageShown,
        onAction = onMessageAction
    )
    // 되돌린 카드가 화면 밖(첫 보이는 카드보다 앞 — 1열 보기의 첫 카드 등)이면 보이게 한다(F4)
    val currentRestoreShown by rememberUpdatedState(onRestoreShown)
    val restoredKey = state.restoredKey
    LaunchedEffect(restoredKey, state.items) {
        if (restoredKey == null) return@LaunchedEffect
        if (state.items.none { it.desertionNo == restoredKey }) return@LaunchedEffect
        val visible = gridState.layoutInfo.visibleItemsInfo
        restoreScrollTarget(
            keys = state.items.map { it.desertionNo },
            restoredKey = restoredKey,
            firstVisibleKey = visible.firstOrNull()?.key,
            lastVisibleKey = visible.lastOrNull()?.key
        )?.let { gridState.requestScrollToItem(it) }
        currentRestoreShown()
    }
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.title_bookmarks)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back)
                        )
                    }
                },
                actions = {
                    // 지금과 반대 모드의 아이콘·문구(목록과 같다)
                    val toLarge = state.layout == ListLayout.GRID
                    val icon = if (toLarge) {
                        Res.drawable.ic_view_agenda
                    } else {
                        Res.drawable.ic_grid_view
                    }
                    val label = if (toLarge) {
                        Res.string.action_layout_large
                    } else {
                        Res.string.action_layout_grid
                    }
                    IconButton(onClick = onToggleLayout) {
                        Icon(painterResource(icon), contentDescription = stringResource(label))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        val direction = LocalLayoutDirection.current
        val outer = PaddingValues(
            start = innerPadding.calculateStartPadding(direction),
            end = innerPadding.calculateEndPadding(direction),
            bottom = innerPadding.calculateBottomPadding()
        )
        Box(Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
            if (state.isEmpty) {
                FullScreenMessage(
                    text = stringResource(Res.string.empty_bookmarks),
                    modifier = Modifier.padding(outer)
                )
            } else {
                AnimalGrid(
                    items = state.items,
                    layout = state.layout,
                    onCardClick = onOpenAnimal,
                    onBookmarkClick = { onToggleBookmark(it.desertionNo) },
                    state = gridState,
                    contentPadding = outer,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun PreviewBookmarks(state: BookmarkListUiState) {
    AnimalShelterTheme {
        BookmarkListScreen(
            state = state,
            onBack = {},
            onOpenAnimal = {},
            onToggleBookmark = {},
            onToggleLayout = {},
            onMessageShown = {},
            onMessageAction = { _, _ -> }
        )
    }
}

@Preview
@Composable
private fun BookmarkListPreview() {
    PreviewBookmarks(
        BookmarkListUiState(items = PreviewData.cards.map { it.copy(isBookmarked = true) })
    )
}

@Preview
@Composable
private fun BookmarkListEmptyPreview() {
    PreviewBookmarks(BookmarkListUiState(isEmpty = true))
}
