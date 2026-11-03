package com.cherryzp.animalshelter.ui.shelter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cherryzp.animalshelter.data.repository.AnimalRepository
import com.cherryzp.animalshelter.data.repository.ShelterRepository
import com.cherryzp.animalshelter.di.AppConfig
import com.cherryzp.animalshelter.model.todayInSeoul
import com.cherryzp.animalshelter.navigation.ShelterInfoKey
import com.cherryzp.animalshelter.platform.CopyFeedback
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.address_copied
import com.cherryzp.animalshelter.resources.external_open_failed
import com.cherryzp.animalshelter.ui.common.MessageIds
import com.cherryzp.animalshelter.ui.common.UiMessage
import com.cherryzp.animalshelter.ui.common.canRetry
import com.cherryzp.animalshelter.ui.common.clearIfShown
import com.cherryzp.animalshelter.ui.common.collapseSpaces
import com.cherryzp.animalshelter.ui.common.toUiText
import com.cherryzp.animalshelter.ui.common.uiText
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 보호소 정보(03 2.6, 브리핑 B12). `getShelterInfoStream(regNo)`(전체 7일 캐시)의 세 모양을 화면 상태로:
 * 첫 로딩 `(null, false, null)` → 가운데 진행 / 찾음 → 섹션 전부 / `(null, true, null)` → 배너 + 줄인 화면 /
 * `(null, false, error)` → 오류 + 다시 시도(`refresh()`) + 줄인 화면.
 * "이 보호소의 공고 보기"는 지금 결과의 조건에서 보호소만 바꿔(지역은 비운다) 검색을 시작한다 — 탐색은 화면이.
 */
class ShelterInfoViewModel(
    private val key: ShelterInfoKey,
    private val shelters: ShelterRepository,
    private val animals: AnimalRepository,
    private val clock: Clock,
    private val config: AppConfig,
) : ViewModel() {
    private val message = MutableStateFlow<UiMessage?>(null)
    private val messages = MessageIds()

    private val fallback = ShelterFallbackUi(
        name = key.fallbackName?.trim()?.ifEmpty { null },
        tel = key.fallbackTel?.trim()?.ifEmpty { null },
        address = key.fallbackAddress.collapseSpaces()
    )

    val uiState: StateFlow<ShelterInfoUiState> = combine(
        shelters.getShelterInfoStream(key.regNo),
        message
    ) { cache, message ->
        val today = clock.todayInSeoul()
        val info = cache.data
        val error = cache.error.takeIf { info == null }
        ShelterInfoUiState(
            title = info?.name ?: fallback.name,
            info = info?.toUi(fallbackTel = fallback.tel),
            fallback = fallback,
            isLoading = info == null && !cache.fresh && cache.error == null,
            isLimited = info == null && cache.fresh && cache.error == null,
            error = error?.toUiText(config.isDebug),
            canRetry = error?.canRetry() == true,
            today = info?.todayParts(today).orEmpty(),
            isDataOld = isShelterDataOld(info?.dataDate, today),
            message = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ShelterInfoUiState(
            title = fallback.name,
            fallback = fallback,
            isLoading = true
        )
    )

    /** 보호소 전체를 다시 받는다(1회, 동시에 하나만). */
    fun onRetry() {
        viewModelScope.launch { shelters.refresh() }
    }

    /**
     * 이 보호소의 공고(B12): 지금 결과의 조건 + 이 보호소(시도·시군구는 비운다). 검색은 Repository 앱 범위에서 끝까지
     * 가므로(R1) 화면이 곧바로 목록으로 돌아가도 된다. 마지막 조건 저장·맨 위로 올리기는 목록이 한다.
     */
    fun onShowNotices() {
        // UNDISPATCHED: 화면이 곧바로 목록으로 돌아가며 이 ViewModel 범위가 취소되기 전에 검색이 시작되게
        // (필터의 적용과 같다, F9 — Main.immediate에 기대지 않는다)
        val current = animals.getSearchResultsStream().value.filter
        val filter = current.copy(
            shelterRegNo = key.regNo,
            shelterName = uiState.value.title ?: fallback.name,
            sidoCode = null,
            sidoName = null,
            sigunguCode = null,
            sigunguName = null
        )
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) { animals.search(filter) }
    }

    fun onExternalActionResult(opened: Boolean) {
        if (!opened) message.value = messages.create(uiText(Res.string.external_open_failed))
    }

    fun onCopyResult(feedback: CopyFeedback) {
        message.value = when (feedback) {
            CopyFeedback.SHOWN_BY_SYSTEM -> return
            CopyFeedback.NEEDS_APP_MESSAGE -> messages.create(uiText(Res.string.address_copied))
            CopyFeedback.FAILED -> messages.create(uiText(Res.string.external_open_failed))
        }
    }

    /** 보여 준 안내([id])가 지금 것일 때만 지운다(F3). */
    fun onMessageShown(id: Long) {
        message.update { it.clearIfShown(id) }
    }
}
