package com.cherryzp.animalshelter.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cherryzp.animalshelter.model.ListLayout
import com.cherryzp.animalshelter.model.ThemeMode
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.about_version
import com.cherryzp.animalshelter.resources.action_back
import com.cherryzp.animalshelter.resources.app_name
import com.cherryzp.animalshelter.resources.endpoint_direct
import com.cherryzp.animalshelter.resources.endpoint_fixture
import com.cherryzp.animalshelter.resources.endpoint_relay
import com.cherryzp.animalshelter.resources.layout_grid
import com.cherryzp.animalshelter.resources.layout_large
import com.cherryzp.animalshelter.resources.layout_large_text_hint
import com.cherryzp.animalshelter.resources.setting_about
import com.cherryzp.animalshelter.resources.setting_endpoint
import com.cherryzp.animalshelter.resources.setting_endpoint_hint
import com.cherryzp.animalshelter.resources.setting_list_layout
import com.cherryzp.animalshelter.resources.setting_theme
import com.cherryzp.animalshelter.resources.theme_dark
import com.cherryzp.animalshelter.resources.theme_light
import com.cherryzp.animalshelter.resources.theme_system
import com.cherryzp.animalshelter.resources.title_settings
import com.cherryzp.animalshelter.ui.detail.SourceText
import com.cherryzp.animalshelter.ui.theme.AnimalShelterTheme
import org.jetbrains.compose.resources.stringResource

/** 본문 최대 폭. */
private val ContentMaxWidth = 720.dp

/** 설정(ViewModel을 받아 상태를 모은다, P2). */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onBack = onBack,
        onLayoutSelected = viewModel::onLayoutSelected,
        onThemeSelected = viewModel::onThemeSelected,
        onEndpointSelected = viewModel::onEndpointSelected,
        modifier = modifier
    )
}

/**
 * 설정(상태만, 03 2.7·브리핑 B13): 목록 보기 · 테마 · 정보 · (디버그만) API 연결.
 * 라디오 묶음은 `selectableGroup`, 줄 전체가 누르는 곳(48dp 이상).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onLayoutSelected: (ListLayout) -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onEndpointSelected: (EndpointChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.title_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize().padding(
                innerPadding
            ).verticalScroll(rememberScrollState())
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.widthIn(
                    max = ContentMaxWidth
                ).fillMaxWidth().padding(vertical = 8.dp)
            ) {
                SettingGroup(stringResource(Res.string.setting_list_layout)) {
                    RadioRow(
                        stringResource(Res.string.layout_grid),
                        state.layout == ListLayout.GRID
                    ) {
                        onLayoutSelected(ListLayout.GRID)
                    }
                    RadioRow(
                        stringResource(Res.string.layout_large),
                        state.layout == ListLayout.LARGE
                    ) {
                        onLayoutSelected(ListLayout.LARGE)
                    }
                    Hint(stringResource(Res.string.layout_large_text_hint))
                }
                HorizontalDivider()
                SettingGroup(stringResource(Res.string.setting_theme)) {
                    listOf(
                        ThemeMode.SYSTEM to Res.string.theme_system,
                        ThemeMode.LIGHT to Res.string.theme_light,
                        ThemeMode.DARK to Res.string.theme_dark
                    ).forEach { (mode, label) ->
                        RadioRow(stringResource(label), state.theme == mode) {
                            onThemeSelected(mode)
                        }
                    }
                }
                state.endpoint?.let { choice ->
                    HorizontalDivider()
                    SettingGroup(stringResource(Res.string.setting_endpoint)) {
                        val defaultLabel = if (state.defaultIsFixture) {
                            Res.string.endpoint_fixture
                        } else {
                            Res.string.endpoint_direct
                        }
                        RadioRow(stringResource(defaultLabel), choice == EndpointChoice.DEFAULT) {
                            onEndpointSelected(EndpointChoice.DEFAULT)
                        }
                        state.relayBaseUrl?.let { url ->
                            RadioRow(
                                stringResource(Res.string.endpoint_relay, url),
                                choice == EndpointChoice.RELAY
                            ) { onEndpointSelected(EndpointChoice.RELAY) }
                        }
                        if (!state.defaultIsFixture) {
                            RadioRow(
                                stringResource(Res.string.endpoint_fixture),
                                choice == EndpointChoice.FIXTURE
                            ) { onEndpointSelected(EndpointChoice.FIXTURE) }
                        }
                        Hint(stringResource(Res.string.setting_endpoint_hint))
                    }
                }
                HorizontalDivider()
                SettingGroup(stringResource(Res.string.setting_about)) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            stringResource(Res.string.app_name),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            stringResource(Res.string.about_version, state.versionName),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SourceText(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingGroup(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).semantics { heading() }
        )
        Column(Modifier.selectableGroup()) { content() }
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
        )
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Preview
@Composable
private fun SettingsDebugPreview() {
    AnimalShelterTheme {
        SettingsScreen(
            state = SettingsUiState(
                versionName = "2.0.0",
                endpoint = EndpointChoice.RELAY,
                relayBaseUrl = "http://10.0.2.2:8788"
            ),
            onBack = {},
            onLayoutSelected = {},
            onThemeSelected = {},
            onEndpointSelected = {}
        )
    }
}

@Preview
@Composable
private fun SettingsReleasePreview() {
    AnimalShelterTheme {
        SettingsScreen(
            state = SettingsUiState(versionName = "2.0.0"),
            onBack = {},
            onLayoutSelected = {},
            onThemeSelected = {},
            onEndpointSelected = {}
        )
    }
}
