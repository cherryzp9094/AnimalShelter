package com.cherryzp.animalshelter.ui.photo

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cherryzp.animalshelter.data.api.ApiResult
import com.cherryzp.animalshelter.data.api.EndpointSelector
import com.cherryzp.animalshelter.data.repository.AnimalRepository
import com.cherryzp.animalshelter.data.repository.ImageUrlPolicy
import com.cherryzp.animalshelter.data.repository.SettingsRepository
import com.cherryzp.animalshelter.di.AppConfig
import com.cherryzp.animalshelter.navigation.PhotoViewerKey
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.detail_not_found
import com.cherryzp.animalshelter.ui.common.UiText
import com.cherryzp.animalshelter.ui.common.canRetry
import com.cherryzp.animalshelter.ui.common.toUiText
import com.cherryzp.animalshelter.ui.common.uiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 사진 뷰어 상태(브리핑 3b 3절).
 * @property photos 화면용 URL(Relay면 중계 `/img`)
 * @property title 사진 설명(`photo_a11y`)의 제목 — 카드 제목
 */
@Immutable
data class PhotoViewerUiState(
    val photos: List<String> = emptyList(),
    val title: String? = null,
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val canRetry: Boolean = false,
)

/**
 * 사진 뷰어(03 2.4, 브리핑 B1·B9). 키의 원본 사진을 지금 엔드포인트의 화면용 URL로 바꾼다(디버그 엔드포인트를 바꾸면
 * 다시 계산). 키에 사진이 없으면(Phase 4 주소 진입) `getAnimal(desertionNo, null)`로 찾는다.
 */
class PhotoViewerViewModel(
    private val key: PhotoViewerKey,
    private val animals: AnimalRepository,
    settings: SettingsRepository,
    private val endpoints: EndpointSelector,
    private val config: AppConfig,
) : ViewModel() {
    private data class Local(
        val originals: List<String>,
        val title: String?,
        val isLoading: Boolean = false,
        val error: UiText? = null,
        val canRetry: Boolean = false,
    )

    private val local = MutableStateFlow(Local(originals = key.photos, title = key.title))

    private val endpoint = settings.getSettingsStream().map { endpoints.current() }
        .distinctUntilChanged()

    val uiState: StateFlow<PhotoViewerUiState> = combine(local, endpoint) { local, endpoint ->
        PhotoViewerUiState(
            photos = local.originals.map { ImageUrlPolicy.displayUrl(it, endpoint) },
            title = local.title,
            isLoading = local.isLoading,
            error = local.error,
            canRetry = local.canRetry
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PhotoViewerUiState(title = key.title, isLoading = key.photos.isEmpty())
    )

    init {
        if (key.photos.isEmpty()) load()
    }

    fun onRetry() = load()

    private fun load() {
        local.update { it.copy(isLoading = true, error = null, canRetry = false) }
        viewModelScope.launch {
            when (val result = animals.getAnimal(key.desertionNo, null)) {
                is ApiResult.Success -> {
                    val animal = result.value
                    local.update {
                        if (animal == null) {
                            it.copy(isLoading = false, error = uiText(Res.string.detail_not_found))
                        } else {
                            it.copy(
                                originals = animal.photos,
                                title = it.title ?: animal.kindName,
                                isLoading = false
                            )
                        }
                    }
                }

                is ApiResult.Failure -> local.update {
                    it.copy(
                        isLoading = false,
                        error = result.error.toUiText(config.isDebug),
                        canRetry = result.error.canRetry()
                    )
                }
            }
        }
    }
}
