package com.cherryzp.animalshelter.ui.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cherryzp.animalshelter.data.api.EndpointSelector
import com.cherryzp.animalshelter.data.repository.BookmarkRepository
import com.cherryzp.animalshelter.data.repository.SettingsRepository
import com.cherryzp.animalshelter.model.Bookmark
import com.cherryzp.animalshelter.model.ListLayout
import com.cherryzp.animalshelter.model.todayInSeoul
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.bookmark_removed
import com.cherryzp.animalshelter.resources.bookmark_save_failed
import com.cherryzp.animalshelter.resources.bookmarks_recovered
import com.cherryzp.animalshelter.ui.common.MessageAction
import com.cherryzp.animalshelter.ui.common.MessageIds
import com.cherryzp.animalshelter.ui.common.UiMessage
import com.cherryzp.animalshelter.ui.common.clearIfShown
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

/**
 * 북마크 목록(03 2.5, 브리핑 B10·B11). 카드는 목록과 같은 [toCardUi](오늘 기준 배지, 지금 엔드포인트의 사진).
 * 카드의 북마크를 누르면 바로 지우고 "되돌리기"를 단다 — 되돌리기는 지운 북마크를 원래 자리·원래 `savedAt`으로
 * `restore`(F4, 마지막에 지운 하나만). 되살린 카드는 `restoredKey`로 알려 화면이 보이게 스크롤한다.
 * 북마크 파일이 깨져 비웠으면 아직 알리지 않았을 때 한 번 알리고, 보인 뒤 `dismissRecoveryNotice()`.
 */
class BookmarkListViewModel(
    private val bookmarks: BookmarkRepository,
    private val settings: SettingsRepository,
    private val endpoints: EndpointSelector,
    private val clock: Clock,
) : ViewModel() {
    /** 마지막에 지운 북마크와 그 순서 위치, 그때 낸 안내의 id(그 안내의 되돌리기만 받는다). */
    private data class Removed(val bookmark: Bookmark, val index: Int, val messageId: Long)

    private data class Local(
        val message: UiMessage? = null,
        val lastRemoved: Removed? = null,
        val restoredKey: String? = null,
    )

    private val local = MutableStateFlow(Local())
    private val messages = MessageIds()

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
            message = local.message,
            restoredKey = local.restoredKey
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
            val list = bookmarks.getBookmarksStream().first()
            val index = list.indexOfFirst { it.animal.desertionNo == desertionNo }
            if (index < 0) return@launch
            try {
                bookmarks.remove(desertionNo)
                val message = messages.create(
                    uiText(Res.string.bookmark_removed),
                    MessageAction.UNDO
                )
                local.update {
                    it.copy(
                        message = message,
                        lastRemoved = Removed(list[index], index, message.id)
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                local.update { it.copy(message = saveFailed()) }
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

    /** 보여 준 안내([id])가 지금 것일 때만 지운다(F3). */
    fun onMessageShown(id: Long) {
        val current = local.value.message
        if (current?.id != id) return
        if (current == RecoveryNotice) bookmarks.dismissRecoveryNotice()
        local.update { it.copy(message = it.message.clearIfShown(id)) }
    }

    /** 되돌리기 = 지운 북마크를 원래 자리·원래 저장 시각으로(F4). 그 안내([id])의 버튼일 때만. */
    fun onMessageAction(action: MessageAction, id: Long) {
        val removed = local.value.lastRemoved?.takeIf { it.messageId == id }
        onMessageShown(id)
        if (action != MessageAction.UNDO || removed == null) return
        local.update { it.copy(lastRemoved = null) }
        viewModelScope.launch {
            try {
                bookmarks.restore(removed.bookmark, removed.index)
                local.update { it.copy(restoredKey = removed.bookmark.animal.desertionNo) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                local.update { it.copy(message = saveFailed()) }
            }
        }
    }

    /** 화면이 되살린 카드를 보이게 했다. */
    fun onRestoreShown() {
        local.update { it.copy(restoredKey = null) }
    }

    private fun saveFailed(): UiMessage = messages.create(uiText(Res.string.bookmark_save_failed))
}
