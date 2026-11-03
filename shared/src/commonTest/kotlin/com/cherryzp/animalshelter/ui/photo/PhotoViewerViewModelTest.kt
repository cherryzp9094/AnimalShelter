package com.cherryzp.animalshelter.ui.photo

import androidx.lifecycle.ViewModel
import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.data.api.ApiError
import com.cherryzp.animalshelter.data.api.ApiResult
import com.cherryzp.animalshelter.data.api.EndpointSelector
import com.cherryzp.animalshelter.data.repository.Settings
import com.cherryzp.animalshelter.di.AppConfig
import com.cherryzp.animalshelter.fake.FakeAnimalRepository
import com.cherryzp.animalshelter.fake.FakeSettingsRepository
import com.cherryzp.animalshelter.fake.testAnimal
import com.cherryzp.animalshelter.model.Animal
import com.cherryzp.animalshelter.navigation.PhotoViewerKey
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.detail_not_found
import com.cherryzp.animalshelter.resources.error_network
import com.cherryzp.animalshelter.testing.runMainTest
import com.cherryzp.animalshelter.ui.common.uiText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.datetime.LocalDate

/** 사진 뷰어 ViewModel(브리핑 B1·B9). */
class PhotoViewerViewModelTest {
    private val created = mutableListOf<ViewModel>()
    private val original =
        "https://openapi.animal.go.kr/openapi/service/rest/fileDownloadSrvc/files/" +
            "shelter/2026/10/a[1].jpg"

    private fun runVmTest(block: suspend TestScope.() -> Unit) = runMainTest({ created }, block)

    private fun TestScope.viewer(
        key: PhotoViewerKey,
        override: ApiEndpoint? = null,
        result: (String) -> ApiResult<Animal?> = { ApiResult.Success(null) },
    ): Pair<PhotoViewerViewModel, FakeAnimalRepository> {
        val animals = FakeAnimalRepository().apply { animalResult = result }
        val settings = FakeSettingsRepository(Settings(endpointOverride = override))
        val config = AppConfig(ApiEndpoint.Fixture, isDebug = true, versionName = "test")
        val vm = PhotoViewerViewModel(
            key = key,
            animals = animals,
            settings = settings,
            endpoints = EndpointSelector(config) { settings.settings.value.endpointOverride },
            config = config
        )
        created += vm
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        return vm to animals
    }

    @Test
    fun `키의 원본 사진을 지금 엔드포인트의 화면용 URL로, 조회는 하지 않는다`() = runVmTest {
        val (direct, directAnimals) = viewer(PhotoViewerKey("1", 0, listOf(original), "제목"))
        assertEquals(listOf(original), direct.uiState.value.photos)
        assertEquals("제목", direct.uiState.value.title)
        assertTrue(directAnimals.getAnimalCalls.isEmpty())

        val (relay, _) = viewer(
            PhotoViewerKey("1", 0, listOf(original)),
            override = ApiEndpoint.Relay("http://10.0.2.2:8788")
        )
        assertEquals(
            listOf("http://10.0.2.2:8788/img/shelter/2026/10/a%5B1%5D.jpg"),
            relay.uiState.value.photos
        )
    }

    @Test
    fun `사진이 없으면 getAnimal로 찾는다`() = runVmTest {
        val animal = testAnimal(photos = listOf(original))
        val (vm, animals) = viewer(PhotoViewerKey(animal.desertionNo)) { ApiResult.Success(animal) }

        assertEquals(
            listOf<Pair<String, LocalDate?>>(animal.desertionNo to null),
            animals.getAnimalCalls
        )
        assertEquals(listOf(original), vm.uiState.value.photos)
        assertEquals("한국 고양이", vm.uiState.value.title)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `찾지 못하거나 실패하면 오류, 실패는 다시 시도`() = runVmTest {
        val (missing, _) = viewer(PhotoViewerKey("1"))
        assertEquals(uiText(Res.string.detail_not_found), missing.uiState.value.error)
        assertFalse(missing.uiState.value.canRetry)

        val (failed, animals) = viewer(PhotoViewerKey("2")) { ApiResult.Failure(ApiError.Network) }
        assertEquals(uiText(Res.string.error_network), failed.uiState.value.error)
        assertTrue(failed.uiState.value.canRetry)

        failed.onRetry()
        advanceUntilIdle()
        assertEquals(2, animals.getAnimalCalls.size)
    }
}
