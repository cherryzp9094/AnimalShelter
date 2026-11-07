package com.cherryzp.animalshelter.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cherryzp.animalshelter.data.api.ApiError
import com.cherryzp.animalshelter.data.api.ApiResult
import com.cherryzp.animalshelter.data.api.EndpointSelector
import com.cherryzp.animalshelter.data.repository.AnimalRepository
import com.cherryzp.animalshelter.data.repository.BookmarkRepository
import com.cherryzp.animalshelter.data.repository.SettingsRepository
import com.cherryzp.animalshelter.data.repository.ShelterRepository
import com.cherryzp.animalshelter.di.AppConfig
import com.cherryzp.animalshelter.model.Animal
import com.cherryzp.animalshelter.model.ShelterInfo
import com.cherryzp.animalshelter.model.isEndedByDeath
import com.cherryzp.animalshelter.model.todayInSeoul
import com.cherryzp.animalshelter.navigation.AnimalDetailKey
import com.cherryzp.animalshelter.platform.CopyFeedback
import com.cherryzp.animalshelter.platform.ShareResult
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.address_copied
import com.cherryzp.animalshelter.resources.bookmark_added
import com.cherryzp.animalshelter.resources.bookmark_removed
import com.cherryzp.animalshelter.resources.bookmark_save_failed
import com.cherryzp.animalshelter.resources.external_open_failed
import com.cherryzp.animalshelter.resources.share_copied
import com.cherryzp.animalshelter.ui.common.MessageAction
import com.cherryzp.animalshelter.ui.common.MessageIds
import com.cherryzp.animalshelter.ui.common.UiMessage
import com.cherryzp.animalshelter.ui.common.canRetry
import com.cherryzp.animalshelter.ui.common.clearIfShown
import com.cherryzp.animalshelter.ui.common.showsEndedByDeath
import com.cherryzp.animalshelter.ui.common.toUiText
import com.cherryzp.animalshelter.ui.common.uiText
import com.cherryzp.animalshelter.ui.shelter.todayParts
import kotlin.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * 상세 화면(03 2.3, 브리핑 B6). 키 하나에 ViewModel 하나(`parametersOf(key)`).
 *
 * - 시작: 북마크에서 열었으면 스냅샷을 먼저 보이고, 그다음 `getAnimal(no, happenDate)` — 목록에서 10분 안에 연 것은
 *   Repository가 호출 없이 돌려준다. 찾으면 보이고 북마크돼 있으면 스냅샷을 갱신한다.
 * - 0건: 스냅샷이 있으면 그것 + `isStale`, 없으면 `notFound`. 실패: 보이는 것이 있으면 두고 스낵바, 없으면 가운데 오류.
 * - 보호소 정보(오늘 운영시간·좌표)는 보이는 동물의 보호소 번호로 스트림을 모은다(전체 7일 캐시).
 * - 전화·지도·복사는 화면이 부르고 결과만 알려 준다([onExternalActionResult]·[onCopyResult]).
 * - 사망 종료(5d 1.7): 북마크거나 필터가 켜졌으면 보이고(공유 없음), 아니면 숨김(`isHidden`). 처음 사망 종료 값을 얻을 때
 *   (조회 결과·목록 결과의 `hidden`) 한 번 정하고 이 화면에서는 바꾸지 않는다. 숨긴 뒤에는 늦은 결과·다시 시도가 덮지 않는다.
 *   스냅샷 쓰임(먼저 보이기·대신 보이기)은 그대로다 — 북마크는 늘 보인다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AnimalDetailViewModel(
    private val key: AnimalDetailKey,
    private val animals: AnimalRepository,
    private val bookmarks: BookmarkRepository,
    private val shelters: ShelterRepository,
    private val settings: SettingsRepository,
    private val endpoints: EndpointSelector,
    private val clock: Clock,
    private val config: AppConfig,
) : ViewModel() {
    private data class Local(
        val animal: Animal? = null,
        val isLoading: Boolean = true,
        val isStale: Boolean = false,
        val notFound: Boolean = false,
        val error: ApiError? = null,
        val message: UiMessage? = null,
        /** 사망 종료라 숨겼다(5d). 되돌리지 않는다. */
        val hidden: Boolean = false,
    )

    private val local = MutableStateFlow(Local())

    /** 공유 링크(S4) — 공유 주소가 없으면 null(공유 버튼 없음). */
    private val shareUrl: String? = config.shareBaseUrl?.trim()?.trimEnd('/')
        ?.takeIf(String::isNotEmpty)?.let { "$it/a/${key.desertionNo}" }
    private val messages = MessageIds()
    private var loadJob: Job? = null

    /** 사망 종료를 보일지 — 처음 사망 종료 값을 얻을 때 한 번 정한다(5d). */
    private var showsDeath: Boolean? = null

    private val endpoint = settings.getSettingsStream().map { endpoints.current() }
        .distinctUntilChanged()

    private val shelterInfo: Flow<ShelterInfo?> = local
        .map { it.animal?.shelter?.regNo?.trim()?.takeIf(String::isNotEmpty) }
        .distinctUntilChanged()
        .flatMapLatest { regNo ->
            if (regNo ==
                null
            ) {
                flowOf(null)
            } else {
                shelters.getShelterInfoStream(regNo).map { it.data }
            }
        }

    val uiState: StateFlow<AnimalDetailUiState> = combine(
        local,
        bookmarks.isBookmarkedStream(key.desertionNo),
        shelterInfo,
        endpoint
    ) { local, bookmarked, info, endpoint ->
        val today = clock.todayInSeoul()
        val error = local.error?.takeIf { local.animal == null }
        if (local.hidden) {
            return@combine AnimalDetailUiState(
                isHidden = true,
                isBookmarked = bookmarked,
                message = local.message
            )
        }
        AnimalDetailUiState(
            detail = local.animal?.toDetailUi(today, endpoint),
            isLoading = local.isLoading,
            isBookmarked = bookmarked,
            isStale = local.isStale,
            notFound = local.notFound,
            error = error?.toUiText(config.isDebug),
            canRetry = error?.canRetry() == true,
            shelterToday = info?.todayParts(today).orEmpty(),
            map = info?.mapTarget(),
            message = local.message,
            // 사망 종료에는 공유가 없다(5d) — 받는 쪽(미리보기·웹 기본값)에서는 볼 수 없다
            shareUrl = shareUrl.takeIf { local.animal?.isEndedByDeath() == false }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnimalDetailUiState(isLoading = true)
    )

    init {
        load(showSnapshotFirst = key.fromBookmarks)
        watchHiddenInResults()
    }

    /** 가운데 오류·스낵바의 다시 시도. */
    fun onRetry() = load(showSnapshotFirst = false)

    /** 상단 바의 북마크: 켜져 있으면 제거, 아니면 보이는 것으로 추가. */
    fun onToggleBookmark() {
        val animal = local.value.animal ?: return
        viewModelScope.launch {
            try {
                if (bookmarks.isBookmarkedStream(animal.desertionNo).first()) {
                    bookmarks.remove(animal.desertionNo)
                    showMessage(messages.create(uiText(Res.string.bookmark_removed)))
                } else {
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

    /** 전화·지도를 열지 못했으면 안내(R7). */
    fun onExternalActionResult(opened: Boolean) {
        if (!opened) showMessage(messages.create(uiText(Res.string.external_open_failed)))
    }

    /** 주소 복사 결과. Android 13 이상은 시스템이 알리므로 앱은 조용히 있는다(B3). */
    fun onCopyResult(feedback: CopyFeedback) {
        when (feedback) {
            CopyFeedback.SHOWN_BY_SYSTEM -> Unit

            CopyFeedback.NEEDS_APP_MESSAGE -> showMessage(
                messages.create(uiText(Res.string.address_copied))
            )

            CopyFeedback.FAILED -> showMessage(
                messages.create(uiText(Res.string.external_open_failed))
            )
        }
    }

    /** 공유 결과(S3): 클립보드에 복사했으면 `share_copied`, 못 했으면 `external_open_failed`. 시트는 조용히. */
    fun onShareResult(result: ShareResult) {
        when (result) {
            ShareResult.SHEET_SHOWN -> Unit

            ShareResult.COPIED -> showMessage(messages.create(uiText(Res.string.share_copied)))

            ShareResult.FAILED -> showMessage(
                messages.create(uiText(Res.string.external_open_failed))
            )
        }
    }

    /** 보여 준 안내([id])가 지금 것일 때만 지운다(F3). */
    fun onMessageShown(id: Long) {
        local.update { it.copy(message = it.message.clearIfShown(id)) }
    }

    fun onMessageAction(action: MessageAction, id: Long) {
        onMessageShown(id)
        if (action == MessageAction.RETRY) onRetry()
    }

    private fun showMessage(message: UiMessage) {
        local.update { it.copy(message = message) }
    }

    private suspend fun snapshot(): Animal? = try {
        bookmarks.getBookmarksStream().first()
            .firstOrNull { it.animal.desertionNo == key.desertionNo }?.animal
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private fun load(showSnapshotFirst: Boolean) {
        if (local.value.hidden) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (showSnapshotFirst && local.value.animal == null) {
                snapshot()?.let { snap -> local.update { it.copy(animal = snap) } }
            }
            local.update { it.copy(isLoading = true, error = null, notFound = false) }
            val happenDate = key.happenDate?.let { LocalDate.Formats.ISO_BASIC.parseOrNull(it) }
            val result = animals.getAnimal(key.desertionNo, happenDate)
            if (local.value.hidden) return@launch
            when (result) {
                is ApiResult.Success -> {
                    val found = result.value
                    if (found != null && found.isEndedByDeath() && !showsDeath()) {
                        hide()
                    } else if (found != null) {
                        local.update { it.copy(animal = found, isLoading = false, isStale = false) }
                        refreshSnapshot(found)
                    } else {
                        val shown = local.value.animal ?: snapshot()
                        local.update {
                            it.copy(
                                animal = shown,
                                isLoading = false,
                                isStale = shown != null,
                                notFound = shown == null
                            )
                        }
                    }
                }

                is ApiResult.Failure -> {
                    val shown = local.value.animal ?: snapshot()
                    val error = result.error
                    local.update {
                        if (shown != null) {
                            it.copy(
                                animal = shown,
                                isLoading = false,
                                message = messages.create(
                                    text = error.toUiText(config.isDebug),
                                    action = MessageAction.RETRY.takeIf { error.canRetry() }
                                )
                            )
                        } else {
                            it.copy(isLoading = false, error = error)
                        }
                    }
                }
            }
        }
    }

    /** 사망 종료를 보일지(북마크거나 필터 켬) — 처음 한 번 정하고 이 화면에서는 그대로(5d). */
    private suspend fun showsDeath(): Boolean = showsDeath
        ?: showsEndedByDeath(key.desertionNo, animals, bookmarks, settings).also { showsDeath = it }

    /** 숨김(5d): 보이던 것·로딩·오류를 지우고 되돌리지 않는다. */
    private fun hide() {
        local.update { Local(isLoading = false, message = it.message, hidden = true) }
    }

    /**
     * 두 칸(S7): 목록 결과의 `hidden`(필터 끔일 때 뺀 사망 종료)에 이 번호가 들어오면, 보이지 않을 동물이면 진행 중인
     * 조회를 취소하고 숨긴다(호출 0). 한 번 보면 더 보지 않는다.
     */
    private fun watchHiddenInResults() {
        viewModelScope.launch {
            animals.getSearchResultsStream()
                .first { results -> results.hidden.any { it.desertionNo == key.desertionNo } }
            if (local.value.hidden || showsDeath()) return@launch
            loadJob?.cancel()
            hide()
        }
    }

    /** 북마크돼 있으면 새 스냅샷으로(상태·사진이 바뀌면 북마크 카드도 바뀐다). 실패는 조용히 둔다. */
    private suspend fun refreshSnapshot(animal: Animal) {
        try {
            if (bookmarks.isBookmarkedStream(animal.desertionNo).first()) {
                bookmarks.updateSnapshot(animal)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // 다음에 열 때 다시 갱신한다
        }
    }
}
