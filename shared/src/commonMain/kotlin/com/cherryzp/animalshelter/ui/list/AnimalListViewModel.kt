package com.cherryzp.animalshelter.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.data.api.ApiError
import com.cherryzp.animalshelter.data.api.EndpointSelector
import com.cherryzp.animalshelter.data.repository.AnimalRepository
import com.cherryzp.animalshelter.data.repository.BookmarkRepository
import com.cherryzp.animalshelter.data.repository.LoadState
import com.cherryzp.animalshelter.data.repository.SearchResults
import com.cherryzp.animalshelter.data.repository.SettingsRepository
import com.cherryzp.animalshelter.di.AppConfig
import com.cherryzp.animalshelter.model.ListLayout
import com.cherryzp.animalshelter.model.SearchFilter
import com.cherryzp.animalshelter.model.SortOrder
import com.cherryzp.animalshelter.model.todayInSeoul
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.bookmark_added
import com.cherryzp.animalshelter.resources.bookmark_removed
import com.cherryzp.animalshelter.resources.bookmark_save_failed
import com.cherryzp.animalshelter.resources.list_noticing_count
import com.cherryzp.animalshelter.resources.list_total_count
import com.cherryzp.animalshelter.ui.bookmarks.RecoveryNotice
import com.cherryzp.animalshelter.ui.common.MessageAction
import com.cherryzp.animalshelter.ui.common.MessageIds
import com.cherryzp.animalshelter.ui.common.UiMessage
import com.cherryzp.animalshelter.ui.common.canRetry
import com.cherryzp.animalshelter.ui.common.clearIfShown
import com.cherryzp.animalshelter.ui.common.formatCount
import com.cherryzp.animalshelter.ui.common.toCardUi
import com.cherryzp.animalshelter.ui.common.toUiText
import com.cherryzp.animalshelter.ui.common.uiText
import kotlin.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 목록 화면(03 2.1, 브리핑 3a A6~A16). 결과는 [AnimalRepository]의 앱 범위 스트림이고(D8), 이 ViewModel은
 * 그것을 카드·칩·헤더·끝 행·빈/오류 상태로 바꾼다. 탐색은 모른다(화면 콜백). 일회성 안내도 상태(`message`).
 *
 * - 첫 검색: 결과를 한 번도 받지 않았으면 마지막 조건(없으면 기본)으로(A16). 코드표 Repository는 받지 않는다.
 * - 마지막 조건 저장: 결과를 **받는 데 성공한** 조건이 저장값과 다르면 저장(필터·칩 어디서 바꿔도 한곳에서).
 * - 스낵바: 목록이 있는데 새 오류(`errorId`)면 한 번. 다음 페이지 실패는 끝 행이 알린다. 5,000건 초과는 늘 안내.
 */
class AnimalListViewModel(
    private val animals: AnimalRepository,
    private val bookmarks: BookmarkRepository,
    private val settings: SettingsRepository,
    private val endpoints: EndpointSelector,
    private val clock: Clock,
    private val config: AppConfig,
) : ViewModel() {
    private enum class LastAction { SEARCH, REFRESH, SORT, LOAD_MORE }

    private data class Local(
        val pullRefreshing: Boolean = false,
        val message: UiMessage? = null,
        val lastAction: LastAction = LastAction.SEARCH,
    )

    private val local = MutableStateFlow(Local())
    private val messages = MessageIds()
    private val results: StateFlow<SearchResults> = animals.getSearchResultsStream()

    /** 이미 안내한(또는 안내하지 않기로 한) 오류. 만들 때 남아 있던 오류는 다시 알리지 않는다. */
    private var handledErrorId: Long? = results.value.takeIf { it.error != null }?.errorId

    private val bookmarkedIds = bookmarks.getBookmarksStream()
        .map { list -> list.mapTo(HashSet()) { it.animal.desertionNo } }
    private val layout = settings.getSettingsStream().map { it.listLayout }.distinctUntilChanged()

    /** 디버그 엔드포인트를 바꾸면 사진 URL도 다시 만든다(P6). */
    private val endpoint = settings.getSettingsStream().map {
        endpoints.current()
    }.distinctUntilChanged()

    val uiState: StateFlow<AnimalListUiState> =
        combine(results, bookmarkedIds, layout, endpoint, local) {
                r,
                ids,
                layout,
                endpoint,
                local,
            ->
            buildState(r, ids, layout, endpoint, local)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AnimalListUiState(isInitialLoading = true)
        )

    init {
        viewModelScope.launch { startFirstSearch() }
        viewModelScope.launch { saveSuccessfulFilters() }
        viewModelScope.launch { watchErrors() }
        viewModelScope.launch { watchRecovery() }
    }

    fun onRefresh(byPull: Boolean) {
        local.update { it.copy(pullRefreshing = byPull, lastAction = LastAction.REFRESH) }
        viewModelScope.launch {
            try {
                animals.refresh()
            } finally {
                local.update { it.copy(pullRefreshing = false) }
            }
        }
    }

    /** 다시 시도: 마지막 동작이 다음 페이지면 그것, 아니면 새로고침(A11). */
    fun onRetry() {
        if (local.value.lastAction ==
            LastAction.LOAD_MORE
        ) {
            loadMore()
        } else {
            onRefresh(byPull = false)
        }
    }

    /**
     * 끝에 가까워졌다(마지막에서 6번째가 보인다). 최신순·다음 있음·오류 없음·로딩 없음일 때만 다음 페이지 —
     * 실패 뒤에는 자동으로 다시 부르지 않는다(R6, 끝 행의 다시 시도로만). 5,000건 초과 안내는 오류로 보지 않는다.
     */
    fun onNearEnd() {
        val r = results.value
        val blockingError = r.error != null && r.error !is ApiError.TooManyResults
        if (r.sort != SortOrder.LATEST || !r.hasMore || blockingError) return
        if (r.loading != LoadState.IDLE || r.pendingSort != null) return
        loadMore()
    }

    fun onSortSelected(order: SortOrder) {
        local.update { it.copy(lastAction = LastAction.SORT) }
        viewModelScope.launch { animals.setSort(order) }
    }

    /** 정렬 칩(마감 임박순)을 누르면 최신순으로. */
    fun onSortChipClicked() = onSortSelected(SortOrder.LATEST)

    fun onToggleLayout() {
        viewModelScope.launch {
            val current = settings.getSettingsStream().first().listLayout
            settings.setListLayout(
                if (current ==
                    ListLayout.GRID
                ) {
                    ListLayout.LARGE
                } else {
                    ListLayout.GRID
                }
            )
        }
    }

    /** 카드의 북마크(A15): 켜져 있으면 제거, 아니면 그 카드의 스냅샷으로 추가. 끝나면 안내. */
    fun onToggleBookmark(desertionNo: String) {
        viewModelScope.launch {
            val bookmarked = bookmarks.getBookmarksStream().first().any {
                it.animal.desertionNo == desertionNo
            }
            val animal = results.value.items.firstOrNull { it.desertionNo == desertionNo }
            try {
                if (bookmarked) {
                    bookmarks.remove(desertionNo)
                    showMessage(messages.create(uiText(Res.string.bookmark_removed)))
                } else if (animal != null) {
                    bookmarks.add(animal)
                    showMessage(messages.create(uiText(Res.string.bookmark_added)))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                showMessage(messages.create(uiText(Res.string.bookmark_save_failed)))
            }
        }
    }

    /** 보여 준 안내([id])가 지금 것일 때만 지운다(F3). */
    fun onMessageShown(id: Long) {
        val current = local.value.message
        if (current?.id != id) return
        if (current == RecoveryNotice) bookmarks.dismissRecoveryNotice()
        local.update { it.copy(message = it.message.clearIfShown(id)) }
    }

    /** 스낵바 버튼. 다시 시도는 여기서, 북마크 보기는 화면이 탐색한다. */
    fun onMessageAction(action: MessageAction, id: Long) {
        onMessageShown(id)
        if (action == MessageAction.RETRY) onRetry()
    }

    private fun loadMore() {
        local.update { it.copy(lastAction = LastAction.LOAD_MORE) }
        viewModelScope.launch { animals.loadMore() }
    }

    private fun showMessage(message: UiMessage) {
        local.update { it.copy(message = message) }
    }

    private suspend fun startFirstSearch() {
        val r = results.value
        val neverFetched = r.fetchedAt == null && r.loading == LoadState.IDLE && r.error == null
        if (!neverFetched || r.items.isNotEmpty()) return
        val filter = settings.getSettingsStream().first().lastFilter ?: SearchFilter()
        local.update { it.copy(lastAction = LastAction.SEARCH) }
        animals.search(filter)
    }

    private suspend fun saveSuccessfulFilters() {
        results
            .filter { it.loading == LoadState.IDLE && it.fetchedAt != null }
            .map { it.filter }
            .distinctUntilChanged()
            .collect { filter ->
                try {
                    if (settings.getSettingsStream().first().lastFilter != filter) {
                        settings.saveLastFilter(filter)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // 저장 실패는 다음 성공 때 다시 시도된다(조건이 같으면 건너뛴다)
                }
            }
    }

    /** 북마크 파일이 깨져 비웠으면 시작 화면에서 한 번 알린다(B11). 보인 뒤 [onMessageShown]이 끈다. */
    private suspend fun watchRecovery() {
        bookmarks.getRecoveredFromCorruptionStream().collect { recovered ->
            if (recovered && local.value.message == null) showMessage(RecoveryNotice)
        }
    }

    private suspend fun watchErrors() {
        results.collect { r ->
            val error = r.error ?: return@collect
            if (r.errorId == handledErrorId) return@collect
            handledErrorId = r.errorId
            val message = when {
                error is ApiError.TooManyResults -> messages.create(error.toUiText(config.isDebug))

                // 결과가 비었으면 한 화면 오류가, 다음 페이지 실패는 끝 행이 알린다
                r.items.isEmpty() || local.value.lastAction == LastAction.LOAD_MORE -> null

                else -> messages.create(
                    text = error.toUiText(config.isDebug),
                    action = MessageAction.RETRY.takeIf { error.canRetry() }
                )
            }
            message?.let(::showMessage)
        }
    }

    private fun buildState(
        r: SearchResults,
        bookmarked: Set<String>,
        layout: ListLayout,
        endpoint: ApiEndpoint,
        local: Local,
    ): AnimalListUiState {
        val today = clock.todayInSeoul()
        val error = r.error?.takeUnless { it is ApiError.TooManyResults }
        val hasItems = r.items.isNotEmpty()
        val waitingFirst = !hasItems &&
            (r.loading != LoadState.IDLE || (r.fetchedAt == null && r.error == null))
        return AnimalListUiState(
            chips = summaryChips(r.filter, r.sort, today),
            sort = r.sort,
            pendingSort = r.pendingSort,
            layout = layout,
            header = r.totalCount?.let {
                val id = if (r.sort == SortOrder.CLOSING_SOON) {
                    Res.string.list_noticing_count
                } else {
                    Res.string.list_total_count
                }
                uiText(id, formatCount(it))
            },
            items = r.items.map { it.toCardUi(today, endpoint, it.desertionNo in bookmarked) },
            isInitialLoading = waitingFirst,
            refreshIndicator = when {
                !hasItems -> null
                local.pullRefreshing -> RefreshIndicator.PULL
                r.loading == LoadState.REFRESH -> RefreshIndicator.BAR
                else -> null
            },
            footer = when {
                !hasItems -> ListFooter.NONE
                r.loading == LoadState.MORE -> ListFooter.LOADING
                error != null && local.lastAction == LastAction.LOAD_MORE -> ListFooter.RETRY
                !r.hasMore && r.loading == LoadState.IDLE && r.fetchedAt != null -> ListFooter.END
                else -> ListFooter.NONE
            },
            fullScreen = when {
                hasItems || waitingFirst -> null

                error != null -> FullScreenState.Error(
                    error.toUiText(config.isDebug),
                    error.canRetry()
                )

                else -> FullScreenState.Empty
            },
            message = local.message,
            queryKey = "${r.sort.name}|${r.filter}"
        )
    }
}
