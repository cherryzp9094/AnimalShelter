package com.cherryzp.animalshelter.ui.settings

import androidx.compose.runtime.Immutable
import com.cherryzp.animalshelter.model.ListLayout
import com.cherryzp.animalshelter.model.ThemeMode

/**
 * 설정 상태(브리핑 3b 3절·B13).
 * @property endpoint 디버그 빌드가 아니면 null(항목을 숨긴다)
 * @property relayBaseUrl 중계 선택지의 주소. 없으면 그 선택지를 숨긴다
 * @property defaultIsFixture 기본 엔드포인트가 Fixture(키 없는 빌드)면 기본 문구를 픽스처로, 따로 고를 픽스처는 숨긴다
 */
@Immutable
data class SettingsUiState(
    val layout: ListLayout = ListLayout.GRID,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val versionName: String = "",
    val endpoint: EndpointChoice? = null,
    val relayBaseUrl: String? = null,
    val defaultIsFixture: Boolean = false,
)

/** 디버그 엔드포인트 선택: 기본(덮어쓰기 없음) / 중계 / 픽스처. */
enum class EndpointChoice { DEFAULT, RELAY, FIXTURE }
