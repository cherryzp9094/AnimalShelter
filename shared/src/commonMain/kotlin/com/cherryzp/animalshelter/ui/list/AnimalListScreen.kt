package com.cherryzp.animalshelter.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cherryzp.animalshelter.model.SortOrder
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.action_retry
import com.cherryzp.animalshelter.resources.action_view_bookmarks
import com.cherryzp.animalshelter.resources.empty_change_filter
import com.cherryzp.animalshelter.resources.empty_results
import com.cherryzp.animalshelter.resources.error_network
import com.cherryzp.animalshelter.resources.list_end
import com.cherryzp.animalshelter.resources.list_total_count
import com.cherryzp.animalshelter.ui.common.AnimalCardUi
import com.cherryzp.animalshelter.ui.common.AnimalGrid
import com.cherryzp.animalshelter.ui.common.CenteredLoading
import com.cherryzp.animalshelter.ui.common.FullScreenMessage
import com.cherryzp.animalshelter.ui.common.MessageAction
import com.cherryzp.animalshelter.ui.common.MessageSnackbarEffect
import com.cherryzp.animalshelter.ui.common.resolve
import com.cherryzp.animalshelter.ui.common.uiText
import com.cherryzp.animalshelter.ui.preview.PreviewData
import com.cherryzp.animalshelter.ui.theme.AnimalShelterTheme
import com.cherryzp.animalshelter.ui.theme.TABULAR_NUMBERS
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import org.jetbrains.compose.resources.stringResource

/** 마지막에서 이 번째 항목이 보이면 다음 페이지(브리핑 A12). */
private const val NEAR_END_OFFSET = 6

/** 목록 화면(ViewModel을 받아 상태를 모은다). 탐색은 콜백으로 — nav entry가 한다(P2). */
@Composable
fun AnimalListScreen(
    viewModel: AnimalListViewModel,
    onOpenFilter: () -> Unit,
    onOpenAnimal: (AnimalCardUi) -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AnimalListScreen(
        state = state,
        onOpenFilter = onOpenFilter,
        onOpenAnimal = onOpenAnimal,
        onOpenBookmarks = onOpenBookmarks,
        onOpenSettings = onOpenSettings,
        onRefresh = viewModel::onRefresh,
        onRetry = viewModel::onRetry,
        onNearEnd = viewModel::onNearEnd,
        onSortSelected = viewModel::onSortSelected,
        onSortChipClicked = viewModel::onSortChipClicked,
        onToggleLayout = viewModel::onToggleLayout,
        onToggleBookmark = viewModel::onToggleBookmark,
        onMessageShown = viewModel::onMessageShown,
        onMessageAction = viewModel::onMessageAction,
        modifier = modifier
    )
}

/** 목록 화면(상태만 — `@Preview`·UI 테스트). 03 2.1, 브리핑 A6~A15. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalListScreen(
    state: AnimalListUiState,
    onOpenFilter: () -> Unit,
    onOpenAnimal: (AnimalCardUi) -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: (byPull: Boolean) -> Unit,
    onRetry: () -> Unit,
    onNearEnd: () -> Unit,
    onSortSelected: (SortOrder) -> Unit,
    onSortChipClicked: () -> Unit,
    onToggleLayout: () -> Unit,
    onToggleBookmark: (desertionNo: String) -> Unit,
    onMessageShown: (id: Long) -> Unit,
    onMessageAction: (action: MessageAction, id: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val gridState = rememberLazyGridState()

    MessageSnackbarEffect(
        message = state.message,
        snackbarHostState = snackbarHostState,
        onShown = onMessageShown,
        onAction = { action, id ->
            onMessageAction(action, id)
            if (action == MessageAction.VIEW_BOOKMARKS) onOpenBookmarks()
        }
    )

    // 조건·정렬이 바뀌면 맨 위로(처음 그릴 때·다른 화면에서 돌아올 때는 위치를 지킨다)
    var shownQueryKey by rememberSaveable { mutableStateOf(state.queryKey) }
    if (state.queryKey != shownQueryKey) {
        shownQueryKey = state.queryKey
        gridState.requestScrollToItem(0)
    }

    val currentOnNearEnd by rememberUpdatedState(onNearEnd)
    LaunchedEffect(gridState) {
        snapshotFlow {
            val info = gridState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: -1) to info.totalItemsCount
        }
            .distinctUntilChanged()
            .filter { (last, total) -> total > 0 && last >= total - 1 - NEAR_END_OFFSET }
            .collect { currentOnNearEnd() }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ListTopBar(
                layout = state.layout,
                checkedSort = state.pendingSort ?: state.sort,
                onOpenFilter = onOpenFilter,
                onSortSelected = onSortSelected,
                onToggleLayout = onToggleLayout,
                onOpenBookmarks = onOpenBookmarks,
                onRefresh = { onRefresh(false) },
                onOpenSettings = onOpenSettings,
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
            val fullScreen = state.fullScreen
            when {
                state.isInitialLoading -> CenteredLoading()

                fullScreen is FullScreenState.Empty -> FullScreenMessage(
                    text = stringResource(Res.string.empty_results),
                    primaryLabel = stringResource(Res.string.empty_change_filter),
                    onPrimary = onOpenFilter,
                    modifier = Modifier.padding(outer)
                )

                fullScreen is FullScreenState.Error -> FullScreenMessage(
                    text = fullScreen.text.resolve(),
                    primaryLabel = stringResource(Res.string.action_retry).takeIf {
                        fullScreen.canRetry
                    },
                    onPrimary = onRetry,
                    secondaryLabel = stringResource(Res.string.action_view_bookmarks),
                    onSecondary = onOpenBookmarks,
                    modifier = Modifier.padding(outer)
                )

                else -> PullToRefreshBox(
                    isRefreshing = state.refreshIndicator == RefreshIndicator.PULL,
                    onRefresh = { onRefresh(true) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    AnimalGrid(
                        items = state.items,
                        layout = state.layout,
                        onCardClick = onOpenAnimal,
                        onBookmarkClick = { onToggleBookmark(it.desertionNo) },
                        state = gridState,
                        contentPadding = outer,
                        header = {
                            listHeader(
                                state = state,
                                onChipClick = { chip ->
                                    if (chip.target ==
                                        ChipTarget.SORT_LATEST
                                    ) {
                                        onSortChipClicked()
                                    } else {
                                        onOpenFilter()
                                    }
                                }
                            )
                        },
                        footer = { listFooter(state.footer, onRetry) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            if (state.refreshIndicator == RefreshIndicator.BAR) {
                LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
    }
}

/** 그리드 머리: 요약 칩(목록과 함께 스크롤, A7) + 결과 헤더(A8). 둘 다 전체 폭. */
private fun LazyGridScope.listHeader(state: AnimalListUiState, onChipClick: (SummaryChip) -> Unit) {
    item(key = "chips", span = { GridItemSpan(maxLineSpan) }, contentType = "chips") {
        SummaryChipRow(chips = state.chips, onChipClick = onChipClick)
    }
    state.header?.let { header ->
        item(key = "header", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
            Text(
                text = header.resolve(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFeatureSettings = TABULAR_NUMBERS
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

/** 그리드 끝 행(A11, F7): 받는 중 / 마지막 / 다음 페이지 실패(+ 다시 시도할 수 있으면 버튼). */
private fun LazyGridScope.listFooter(footer: ListFooter, onRetry: () -> Unit) {
    if (footer == ListFooter.None) return
    item(key = "footer", span = { GridItemSpan(maxLineSpan) }, contentType = "footer") {
        Box(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (footer) {
                ListFooter.Loading -> CircularProgressIndicator(
                    Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )

                ListFooter.End -> Text(
                    text = stringResource(Res.string.list_end),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                is ListFooter.Failed -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = footer.text.resolve(),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    if (footer.canRetry) {
                        TextButton(onClick = onRetry) {
                            Text(stringResource(Res.string.action_retry))
                        }
                    }
                }

                ListFooter.None -> Unit
            }
        }
    }
}

@Composable
private fun PreviewList(state: AnimalListUiState) {
    AnimalShelterTheme {
        AnimalListScreen(
            state = state,
            onOpenFilter = {},
            onOpenAnimal = {},
            onOpenBookmarks = {},
            onOpenSettings = {},
            onRefresh = {},
            onRetry = {},
            onNearEnd = {},
            onSortSelected = {},
            onSortChipClicked = {},
            onToggleLayout = {},
            onToggleBookmark = {},
            onMessageShown = {},
            onMessageAction = { _, _ -> }
        )
    }
}

@Preview
@Composable
private fun AnimalListDataPreview() {
    PreviewList(
        AnimalListUiState(
            chips = summaryChips(
                com.cherryzp.animalshelter.model.SearchFilter(),
                SortOrder.LATEST,
                PreviewData.today
            ),
            header = uiText(Res.string.list_total_count, "7,544"),
            items = PreviewData.cards,
            footer = ListFooter.End
        )
    )
}

@Preview
@Composable
private fun AnimalListEmptyPreview() {
    PreviewList(AnimalListUiState(fullScreen = FullScreenState.Empty))
}

@Preview
@Composable
private fun AnimalListErrorPreview() {
    PreviewList(
        AnimalListUiState(
            fullScreen = FullScreenState.Error(uiText(Res.string.error_network), canRetry = true)
        )
    )
}
