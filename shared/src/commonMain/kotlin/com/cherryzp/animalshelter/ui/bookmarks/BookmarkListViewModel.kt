package com.cherryzp.animalshelter.ui.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cherryzp.animalshelter.data.api.EndpointSelector
import com.cherryzp.animalshelter.data.repository.BookmarkRepository
import com.cherryzp.animalshelter.data.repository.SettingsRepository
import com.cherryzp.animalshelter.model.Animal
import com.cherryzp.animalshelter.model.ListLayout
import com.cherryzp.animalshelter.model.todayInSeoul
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.bookmark_removed
import com.cherryzp.animalshelter.resources.bookmark_save_failed
import com.cherryzp.animalshelter.resources.bookmarks_recovered
import com.cherryzp.animalshelter.ui.common.MessageAction
import com.cherryzp.animalshelter.ui.common.UiMessage
import com.cherryzp.animalshelter.ui.common.toCardUi
import com.cherryzp.animalshelter.ui.common.uiText
import kotlin.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 북마크 복구 안내(B11). 목록·북마크 화면이 같은 문구로 한 번 낸다. */
val RecoveryNotice: UiMessage = UiMessage(uiText(Res.string.bookmarks_recovered))

private val SaveFailed = UiMessage(uiText(Res.string.bookmark_save_failed))

/**
 * 북마크 목록(03 2.5, 브리핑 B10·B11). 카드는 목록과 같은 [toCardUi](오늘 기준 배지, 지금 엔드포인트의 사진).
 * 카드의 북마크를 누르면 바로 지우고 "되돌리기"를 단다 — 되돌리기는 지운 스냅샷으로 다시 `add`.
 * 북마크 파일이 깨져 비웠으면 아직 알리지 않았을 때 한 번 알리고, 보인 뒤 `dismissRecoveryNotice()`.
 */
class BookmarkListViewModel(
    private val bookmarks: BookmarkRepository,
    private val settings: SettingsRepository,
    private val endpoints: EndpointSelector,
    private val clock: Clock,
) : ViewModel() {
    private data class Local(val message: UiMessage? = null, val lastRemoved: Animal? = null)

    private val local = MutableStateFlow(Local())

    private val layout = settings.getSettingsStream().map { it.listLayout }.distinctUntilChanged()
    private val endpoint = settings.getSettingsStream().map { endpoints.current() }
        .distinctUntilChanged()

    val uiState: StateFlow<BookmarkListUiState> = combine(
        bookmarks.getBookmarksStream(),
        layout,
        endpoint,
        local
    ) { list, layout, endpoint, local ->
        val today = clock.todayInSeoul()
        BookmarkListUiState(
            items = list.map { it.animal.toCardUi(today, endpoint, isBookmarked = true) },
            layout = layout,
            isEmpty = list.isEmpty(),
            message = local.message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BookmarkListUiState()
    )

    init {
        viewModelScope.launch {
            bookmarks.getRecoveredFromCorruptionStream().collect { recovered ->
                if (recovered) local.update { it.copy(message = RecoveryNotice) }
            }
        }
    }

    /** 카드의 북마크(B10): 바로 지우고 되돌리기 안내. */
    fun onToggleBookmark(desertionNo: String) {
        viewModelScope.launch {
            val snapshot = bookmarks.getBookmarksStream().first()
                .firstOrNull { it.animal.desertionNo == desertionNo }?.animal ?: return@launch
            try {
                bookmarks.remove(desertionNo)
                local.update {
                    it.copy(
                        message = UiMessage(
                            uiText(Res.string.bookmark_removed),
                            MessageAction.UNDO
                        ),
                        lastRemoved = snapshot
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                local.update { it.copy(message = SaveFailed) }
            }
        }
    }

    fun onToggleLayout() {
        viewModelScope.launch {
            val current = settings.getSettingsStream().first().listLayout
            val next = if (current == ListLayout.GRID) ListLayout.LARGE else ListLayout.GRID
            settings.setListLayout(next)
        }
    }

    fun onMessageShown() {
        if (local.value.message == RecoveryNotice) bookmarks.dismissRecoveryNotice()
        local.update { it.copy(message = null) }
    }

    /** 되돌리기 = 지운 스냅샷으로 다시 추가(맨 앞에 온다). */
    fun onMessageAction(action: MessageAction) {
        val restore = local.value.lastRemoved
        onMessageShown()
        if (action != MessageAction.UNDO || restore == null) return
        local.update { it.copy(lastRemoved = null) }
        viewModelScope.launch {
            try {
                bookmarks.add(restore)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                local.update { it.copy(message = SaveFailed) }
            }
        }
    }
}
