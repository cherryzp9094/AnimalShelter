package com.cherryzp.animalshelter.ui.settings

import androidx.lifecycle.ViewModel
import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.di.AppConfig
import com.cherryzp.animalshelter.fake.FakeSettingsRepository
import com.cherryzp.animalshelter.model.ListLayout
import com.cherryzp.animalshelter.model.ThemeMode
import com.cherryzp.animalshelter.platform.PlatformInfo
import com.cherryzp.animalshelter.testing.runMainTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle

/** 설정 ViewModel(브리핑 B13). */
class SettingsViewModelTest {
    private val created = mutableListOf<ViewModel>()
    private val relay = "http://10.0.2.2:8788"

    private fun runVmTest(block: suspend TestScope.() -> Unit) = runMainTest({ created }, block)

    private fun TestScope.settingsVm(
        isDebug: Boolean = true,
        relayBaseUrl: String? = relay,
        endpoint: ApiEndpoint = ApiEndpoint.Direct("TEST"),
    ): Pair<SettingsViewModel, FakeSettingsRepository> {
        val repo = FakeSettingsRepository()
        val vm = SettingsViewModel(
            settings = repo,
            config = AppConfig(endpoint, isDebug, "2.0.0", relayBaseUrl),
            platformInfo = PlatformInfo("2.0.0")
        ).also { created += it }
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        return vm to repo
    }

    @Test
    fun `보기와 테마를 저장하고 상태에 보인다`() = runVmTest {
        val (vm, repo) = settingsVm()
        vm.onLayoutSelected(ListLayout.LARGE)
        vm.onThemeSelected(ThemeMode.DARK)
        advanceUntilIdle()

        assertEquals(ListLayout.LARGE, repo.settings.value.listLayout)
        assertEquals(ThemeMode.DARK, repo.settings.value.theme)
        assertEquals(ListLayout.LARGE, vm.uiState.value.layout)
        assertEquals(ThemeMode.DARK, vm.uiState.value.theme)
        assertEquals("2.0.0", vm.uiState.value.versionName)
    }

    @Test
    fun `디버그가 아니면 엔드포인트 항목이 없고 골라도 저장하지 않는다`() = runVmTest {
        val (vm, repo) = settingsVm(isDebug = false)
        assertNull(vm.uiState.value.endpoint)
        assertNull(vm.uiState.value.relayBaseUrl)

        vm.onEndpointSelected(EndpointChoice.FIXTURE)
        advanceUntilIdle()
        assertNull(repo.settings.value.endpointOverride)
    }

    @Test
    fun `중계 주소가 없으면 중계 선택지가 없다`() = runVmTest {
        val (vm, repo) = settingsVm(relayBaseUrl = null)
        assertEquals(EndpointChoice.DEFAULT, vm.uiState.value.endpoint)
        assertNull(vm.uiState.value.relayBaseUrl)

        vm.onEndpointSelected(EndpointChoice.RELAY)
        advanceUntilIdle()
        assertNull(repo.settings.value.endpointOverride)
    }

    @Test
    fun `선택은 setEndpointOverride로 — 중계·픽스처·기본`() = runVmTest {
        val (vm, repo) = settingsVm()

        vm.onEndpointSelected(EndpointChoice.RELAY)
        advanceUntilIdle()
        assertEquals(ApiEndpoint.Relay(relay), repo.settings.value.endpointOverride)
        assertEquals(EndpointChoice.RELAY, vm.uiState.value.endpoint)

        vm.onEndpointSelected(EndpointChoice.FIXTURE)
        advanceUntilIdle()
        assertEquals(ApiEndpoint.Fixture, repo.settings.value.endpointOverride)
        assertEquals(EndpointChoice.FIXTURE, vm.uiState.value.endpoint)

        vm.onEndpointSelected(EndpointChoice.DEFAULT)
        advanceUntilIdle()
        assertNull(repo.settings.value.endpointOverride)
        assertEquals(EndpointChoice.DEFAULT, vm.uiState.value.endpoint)
    }

    @Test
    fun `키 없는 빌드(기본 Fixture)는 기본이 픽스처`() = runVmTest {
        val (vm, _) = settingsVm(endpoint = ApiEndpoint.Fixture)
        assertTrue(vm.uiState.value.defaultIsFixture)
        assertFalse(settingsVm().first.uiState.value.defaultIsFixture)
    }
}
