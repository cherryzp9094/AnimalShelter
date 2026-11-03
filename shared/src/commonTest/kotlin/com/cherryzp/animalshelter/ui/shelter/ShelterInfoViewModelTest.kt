package com.cherryzp.animalshelter.ui.shelter

import androidx.lifecycle.ViewModel
import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.data.api.ApiError
import com.cherryzp.animalshelter.data.repository.CacheState
import com.cherryzp.animalshelter.data.repository.SearchResults
import com.cherryzp.animalshelter.di.AppConfig
import com.cherryzp.animalshelter.fake.FakeAnimalRepository
import com.cherryzp.animalshelter.fake.FakeShelterRepository
import com.cherryzp.animalshelter.fake.testShelterInfo
import com.cherryzp.animalshelter.model.AnimalKind
import com.cherryzp.animalshelter.model.NoticeState
import com.cherryzp.animalshelter.model.RoomCounts
import com.cherryzp.animalshelter.model.SearchFilter
import com.cherryzp.animalshelter.navigation.ShelterInfoKey
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.error_network
import com.cherryzp.animalshelter.resources.label_vehicles
import com.cherryzp.animalshelter.resources.room_medical
import com.cherryzp.animalshelter.resources.staff_vets
import com.cherryzp.animalshelter.testing.FakeClock
import com.cherryzp.animalshelter.testing.runMainTest
import com.cherryzp.animalshelter.ui.common.uiText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.datetime.LocalDate

/** 보호소 정보 ViewModel(브리핑 B7·B12). */
class ShelterInfoViewModelTest {
    private val clock = FakeClock()
    private val created = mutableListOf<ViewModel>()
    private val regNo = "350000202000001"
    private val key = ShelterInfoKey(
        regNo = regNo,
        fallbackName = "목록의 이름",
        fallbackTel = "064-000-0000",
        fallbackAddress = "목록의 주소"
    )

    private fun runVmTest(block: suspend TestScope.() -> Unit) = runMainTest({ created }, block)

    private inner class Env(results: SearchResults = SearchResults()) {
        val shelters = FakeShelterRepository()
        val animals = FakeAnimalRepository(results)
        val vm = ShelterInfoViewModel(
            key = key,
            shelters = shelters,
            animals = animals,
            clock = clock,
            config = AppConfig(ApiEndpoint.Fixture, isDebug = false, versionName = "test")
        ).also { created += it }
        val state get() = vm.uiState.value
        val cache get() = shelters.state(regNo)
    }

    private fun TestScope.start(env: Env): Env {
        backgroundScope.launch { env.vm.uiState.collect {} }
        advanceUntilIdle()
        return env
    }

    @Test
    fun `첫 로딩은 가운데 진행, 제목은 목록의 이름`() = runVmTest {
        val env = start(Env())
        assertTrue(env.state.isLoading)
        assertEquals("목록의 이름", env.state.title)
    }

    @Test
    fun `찾으면 섹션 전부, 제목은 받은 이름, 가려진 전화는 목록 값`() = runVmTest {
        val env = start(Env())
        env.cache.value = CacheState(
            testShelterInfo(regNo, name = "받은 이름").copy(
                tel = null,
                jibunAddress = "  ",
                targetAnimals = "개+고양이+기타",
                vetCount = 2,
                roomCounts = RoomCounts(medical = 1),
                vehicleCount = 3,
                dataDate = "2023-01-01"
            ),
            fresh = true,
            error = null
        )
        advanceUntilIdle()

        val info = assertNotNull(env.state.info)
        assertEquals("받은 이름", env.state.title)
        assertFalse(env.state.isLoading)
        assertFalse(env.state.isLimited)
        assertEquals("064-000-0000", info.tel)
        assertNull(info.jibunAddress)
        assertEquals("개, 고양이, 기타", info.targetAnimals)
        assertNotNull(info.map)
        assertEquals(
            listOf(
                listOf(uiText(Res.string.staff_vets, 2)),
                listOf(uiText(Res.string.room_medical, 1)),
                listOf(uiText(Res.string.label_vehicles, 3))
            ),
            info.facilities
        )
        assertTrue(env.state.today.isNotEmpty())
        assertTrue(env.state.isDataOld) // 2023-01-01은 2026-11-04 기준 2년이 넘었다
    }

    @Test
    fun `목록에 없는 보호소는 배너와 줄인 화면`() = runVmTest {
        val env = start(Env())
        env.cache.value = CacheState(null, fresh = true, error = null)
        advanceUntilIdle()

        assertTrue(env.state.isLimited)
        assertNull(env.state.info)
        assertNull(env.state.error)
        assertEquals(ShelterFallbackUi("목록의 이름", "064-000-0000", "목록의 주소"), env.state.fallback)
    }

    @Test
    fun `받지 못하면 오류와 다시 시도, 다시 시도는 refresh`() = runVmTest {
        val env = start(Env())
        env.cache.value = CacheState(null, fresh = false, error = ApiError.Network)
        advanceUntilIdle()

        assertEquals(uiText(Res.string.error_network), env.state.error)
        assertTrue(env.state.canRetry)
        assertFalse(env.state.isLoading)
        assertFalse(env.state.isLimited)

        env.vm.onRetry()
        advanceUntilIdle()
        assertEquals(1, env.shelters.refreshCalls)
    }

    @Test
    fun `이 보호소의 공고 보기는 지금 조건에 보호소를 넣고 지역을 비워 검색`() = runVmTest {
        val current = SearchFilter(
            kind = AnimalKind.CAT,
            state = NoticeState.NOTICE,
            dateFrom = LocalDate(2026, 10, 1),
            sidoCode = "6500000",
            sidoName = "제주특별자치도",
            sigunguCode = "6520000",
            sigunguName = "제주시"
        )
        val env = start(Env(SearchResults(filter = current)))
        env.cache.value = CacheState(testShelterInfo(regNo, name = "받은 이름"), true, null)
        advanceUntilIdle()

        env.vm.onShowNotices()
        advanceUntilIdle()

        assertEquals(
            listOf(
                current.copy(
                    shelterRegNo = regNo,
                    shelterName = "받은 이름",
                    sidoCode = null,
                    sidoName = null,
                    sigunguCode = null,
                    sigunguName = null
                )
            ),
            env.animals.searchCalls
        )
    }

    @Test
    fun `기준일이 2년 안이거나 읽을 수 없으면 오래된 정보 아님`() {
        val today = LocalDate(2026, 11, 4)
        assertFalse(isShelterDataOld("2025-01-01", today))
        assertFalse(isShelterDataOld("모름", today))
        assertFalse(isShelterDataOld(null, today))
        assertTrue(isShelterDataOld("2024-11-03", today))
    }
}
