package com.cherryzp.animalshelter.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.data.repository.SettingsRepository
import com.cherryzp.animalshelter.di.AppConfig
import com.cherryzp.animalshelter.model.ListLayout
import com.cherryzp.animalshelter.model.ThemeMode
import com.cherryzp.animalshelter.platform.PlatformInfo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 설정(03 2.7, 브리핑 B13): 목록 보기·테마 저장, 정보(버전), 디버그 빌드만 엔드포인트 덮어쓰기.
 * 덮어쓰기는 다음 요청부터 쓰인다(`EndpointSelector`가 요청마다 읽는다). 릴리즈·웹에서는 항목이 없다.
 */
class SettingsViewModel(
    private val settings: SettingsRepository,
    private val config: AppConfig,
    platformInfo: PlatformInfo,
) : ViewModel() {
    private val relayBaseUrl = config.relayBaseUrl?.trim()
        ?.takeIf { config.isDebug && it.isNotEmpty() }
    private val defaultIsFixture = config.endpoint is ApiEndpoint.Fixture

    val uiState: StateFlow<SettingsUiState> = settings.getSettingsStream().map { s ->
        SettingsUiState(
            layout = s.listLayout,
            theme = s.theme,
            versionName = platformInfo.versionName,
            endpoint = if (config.isDebug) choiceOf(s.endpointOverride) else null,
            relayBaseUrl = relayBaseUrl,
            defaultIsFixture = defaultIsFixture
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(versionName = platformInfo.versionName)
    )

    fun onLayoutSelected(layout: ListLayout) {
        viewModelScope.launch { settings.setListLayout(layout) }
    }

    fun onThemeSelected(theme: ThemeMode) {
        viewModelScope.launch { settings.setTheme(theme) }
    }

    /** 디버그 빌드에서만. 중계 주소가 없으면 중계는 고를 수 없다. */
    fun onEndpointSelected(choice: EndpointChoice) {
        if (!config.isDebug) return
        val endpoint = when (choice) {
            EndpointChoice.DEFAULT -> null
            EndpointChoice.RELAY -> ApiEndpoint.Relay(relayBaseUrl ?: return)
            EndpointChoice.FIXTURE -> if (defaultIsFixture) null else ApiEndpoint.Fixture
        }
        viewModelScope.launch { settings.setEndpointOverride(endpoint) }
    }

    private fun choiceOf(override: ApiEndpoint?): EndpointChoice = when (override) {
        is ApiEndpoint.Relay -> relayBaseUrl?.let { EndpointChoice.RELAY }
        ApiEndpoint.Fixture -> EndpointChoice.FIXTURE.takeUnless { defaultIsFixture }
        else -> null
    } ?: EndpointChoice.DEFAULT
}
