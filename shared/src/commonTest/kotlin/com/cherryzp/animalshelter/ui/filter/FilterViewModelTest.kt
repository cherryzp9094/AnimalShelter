package com.cherryzp.animalshelter.ui.filter

import androidx.lifecycle.ViewModel
import com.cherryzp.animalshelter.data.api.ApiError
import com.cherryzp.animalshelter.data.repository.CacheState
import com.cherryzp.animalshelter.data.repository.CodeTable
import com.cherryzp.animalshelter.data.repository.LoadState
import com.cherryzp.animalshelter.data.repository.SearchResults
import com.cherryzp.animalshelter.data.repository.Settings
import com.cherryzp.animalshelter.fake.FakeAnimalRepository
import com.cherryzp.animalshelter.fake.FakeCodeTableRepository
import com.cherryzp.animalshelter.fake.FakeSettingsRepository
import com.cherryzp.animalshelter.fake.loaded
import com.cherryzp.animalshelter.fake.loading
import com.cherryzp.animalshelter.model.AnimalKind
import com.cherryzp.animalshelter.model.Kind
import com.cherryzp.animalshelter.model.Neutered
import com.cherryzp.animalshelter.model.NoticeState
import com.cherryzp.animalshelter.model.SearchFilter
import com.cherryzp.animalshelter.model.Sex
import com.cherryzp.animalshelter.model.ShelterCode
import com.cherryzp.animalshelter.model.Sido
import com.cherryzp.animalshelter.model.Sigungu
import com.cherryzp.animalshelter.model.SortOrder
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.filter_date_invalid
import com.cherryzp.animalshelter.resources.filter_date_range_too_long
import com.cherryzp.animalshelter.testing.FakeClock
import com.cherryzp.animalshelter.testing.runMainTest
import com.cherryzp.animalshelter.ui.common.QuickDate
import com.cherryzp.animalshelter.ui.common.uiText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.datetime.LocalDate

/** 필터 ViewModel(브리핑 3a A17·A18, 8절 완료 기준). */
class FilterViewModelTest {
    private val clock = FakeClock() // 2026-11-04 KST
    private val today = LocalDate(2026, 11, 4)
    private val created = mutableListOf<ViewModel>()

    private fun runVmTest(block: suspend TestScope.() -> Unit) = runMainTest({ created }, block)

    private inner class Env(
        results: SearchResults = SearchResults(),
        settings: Settings = Settings(),
    ) {
        val animals = FakeAnimalRepository(results)
        val codes = FakeCodeTableRepository()
        val settings = FakeSettingsRepository(settings)
        val vm = FilterViewModel(animals, codes, this.settings, clock).also { created += it }
        val state get() = vm.uiState.value
    }

    private fun TestScope.env(
        results: SearchResults = SearchResults(),
        settings: Settings = Settings(),
    ) = Env(results, settings).also {
        backgroundScope.launch { it.vm.uiState.collect {} }
        advanceUntilIdle()
    }

    private val seoul = Sido("6110000", "서울특별시")
    private val gangnam = Sigungu("6110000", "3220000", "강남구")
    private val shelter = ShelterCode("311322200900001", "서울동물복지지원센터")

    @Test
    fun `시작 조건은 결과가 있으면 그 조건, 없으면 저장된 마지막 조건`() = runVmTest {
        val current = SearchFilter(kind = AnimalKind.DOG, kindCode = "000054", kindName = "골든 리트리버")
        val saved = SearchFilter(sidoCode = "6110000", sidoName = "서울특별시")
        val fromResults = env(
            results = SearchResults(
                filter = current,
                fetchedAt = Instant.parse("2026-11-04T00:00:00Z")
            ),
            settings = Settings(lastFilter = saved)
        )
        val loadingNow = env(
            results = SearchResults(filter = current, loading = LoadState.INITIAL),
            settings = Settings(lastFilter = saved)
        )
        val afterRestart = env(settings = Settings(lastFilter = saved))

        assertEquals(current, fromResults.state.filter)
        assertEquals("골든 리트리버", fromResults.state.breedQuery)
        assertEquals(current, loadingNow.state.filter)
        assertEquals(saved, afterRestart.state.filter)
    }

    @Test
    fun `축종을 바꾸면 품종을 비우고 전체면 품종 표가 없다`() = runVmTest {
        val e = env()
        assertNull(e.state.kinds)

        e.vm.onKindSelected(AnimalKind.DOG)
        e.codes.kindsState(AnimalKind.DOG).value = loaded(listOf(Kind("000054", "골든 리트리버")))
        advanceUntilIdle()
        e.vm.onBreedSelected(Kind("000054", "골든 리트리버"))
        advanceUntilIdle()
        assertEquals("000054", e.state.filter.kindCode)
        assertEquals(1, e.state.kinds?.data?.size)

        e.vm.onKindSelected(AnimalKind.CAT)
        advanceUntilIdle()
        assertNull(e.state.filter.kindCode)
        assertNull(e.state.filter.kindName)
        assertEquals("", e.state.breedQuery)
        assertEquals(loading(), e.state.kinds)
    }

    @Test
    fun `시도를 바꾸면 시군구·보호소 코드와 이름을 비운다`() = runVmTest {
        val e = env()
        e.vm.onSidoSelected(seoul)
        e.vm.onSigunguSelected(gangnam)
        e.vm.onShelterSelected(shelter)
        advanceUntilIdle()
        assertEquals("서울동물복지지원센터", e.state.filter.shelterName)

        e.vm.onSidoSelected(Sido("6260000", "부산광역시"))
        advanceUntilIdle()

        val filter = e.state.filter
        assertEquals("부산광역시", filter.sidoName)
        assertNull(filter.sigunguCode)
        assertNull(filter.sigunguName)
        assertNull(filter.shelterRegNo)
        assertNull(filter.shelterName)
        assertNull(e.state.shelters)
    }

    @Test
    fun `시군구를 바꾸면 보호소를 비운다`() = runVmTest {
        val e = env()
        e.vm.onSidoSelected(seoul)
        e.vm.onSigunguSelected(gangnam)
        e.vm.onShelterSelected(shelter)
        e.vm.onSigunguSelected(null)
        advanceUntilIdle()

        assertNull(e.state.filter.shelterRegNo)
        assertNull(e.state.filter.shelterName)
        assertEquals("서울특별시", e.state.filter.sidoName)
    }

    @Test
    fun `빠른 날짜는 오늘 기준 7일·30일, 지우기`() = runVmTest {
        val e = env()

        e.vm.onQuickDate(QuickDate.LAST_7)
        advanceUntilIdle()
        assertEquals(LocalDate(2026, 10, 29), e.state.filter.dateFrom)
        assertEquals(today, e.state.filter.dateTo)
        assertEquals(QuickDate.LAST_7, e.state.quickDate)

        e.vm.onQuickDate(QuickDate.LAST_30)
        advanceUntilIdle()
        assertEquals(LocalDate(2026, 10, 6), e.state.filter.dateFrom)
        assertEquals(QuickDate.LAST_30, e.state.quickDate)

        e.vm.onQuickDate(null)
        advanceUntilIdle()
        assertNull(e.state.filter.dateFrom)
        assertNull(e.state.filter.dateTo)
        assertNull(e.state.quickDate)
    }

    @Test
    fun `한쪽 날짜만 고르면 다른 쪽을 채운다`() = runVmTest {
        val fromOnly = env()
        fromOnly.vm.onDateFromPicked(LocalDate(2026, 9, 1))
        val toOnly = env()
        toOnly.vm.onDateToPicked(LocalDate(2026, 10, 30))
        advanceUntilIdle()

        assertEquals(today, fromOnly.state.filter.dateTo)
        assertEquals(LocalDate(2026, 10, 1), toOnly.state.filter.dateFrom)
    }

    @Test
    fun `종료일이 빠르거나 범위가 1년을 넘으면 오류이고 적용할 수 없다`() = runVmTest {
        val e = env()
        e.vm.onDateFromPicked(LocalDate(2026, 10, 10))
        e.vm.onDateToPicked(LocalDate(2026, 10, 1))
        advanceUntilIdle()
        assertEquals(uiText(Res.string.filter_date_invalid), e.state.dateError)
        assertFalse(e.state.canApply)

        e.vm.onDateFromPicked(LocalDate(2025, 9, 30))
        e.vm.onDateToPicked(LocalDate(2026, 10, 1))
        advanceUntilIdle()
        assertEquals(uiText(Res.string.filter_date_range_too_long), e.state.dateError)
        e.vm.onApply()
        advanceUntilIdle()
        assertTrue(e.animals.searchCalls.isEmpty())

        e.vm.onDateFromPicked(LocalDate(2025, 10, 1)) // 정확히 365일
        advanceUntilIdle()
        assertNull(e.state.dateError)
        assertTrue(e.state.canApply)
    }

    @Test
    fun `코드표 상태 셋을 그대로 보인다`() = runVmTest {
        val e = env()
        assertEquals(loading(), e.state.sido)

        e.codes.sido.value = CacheState(null, fresh = false, error = ApiError.Network)
        advanceUntilIdle()
        assertEquals(ApiError.Network, e.state.sido.error)
        assertNull(e.state.sido.data)

        e.codes.sido.value = CacheState(listOf(seoul), fresh = false, error = ApiError.Network)
        advanceUntilIdle()
        assertEquals(listOf(seoul), e.state.sido.data)
        assertEquals(ApiError.Network, e.state.sido.error)

        assertNull(e.state.sigungu)
        e.vm.onSidoSelected(seoul)
        e.codes.sigunguState(seoul.code).value = loaded(listOf(gangnam))
        advanceUntilIdle()
        assertEquals(listOf(gangnam), e.state.sigungu?.data)
    }

    @Test
    fun `다시 시도는 그 표만 다시 받는다`() = runVmTest {
        val e = env()
        e.vm.onRetryCodes(CodeTable.Sigungu(seoul.code))
        e.vm.onRetryCodes(CodeTable.Kinds(AnimalKind.DOG))
        advanceUntilIdle()

        assertEquals(
            listOf(CodeTable.Sigungu(seoul.code), CodeTable.Kinds(AnimalKind.DOG)),
            e.codes.refreshCalls
        )
    }

    @Test
    fun `적용은 이름까지 담은 조건으로 검색을 시작한다`() = runVmTest {
        val e = env()
        e.vm.onKindSelected(AnimalKind.CAT)
        e.vm.onBreedSelected(Kind("000116", "한국 고양이"))
        e.vm.onSidoSelected(seoul)
        e.vm.onSigunguSelected(gangnam)
        e.vm.onShelterSelected(shelter)
        e.vm.onStateSelected(NoticeState.NOTICE)
        e.vm.onSexSelected(Sex.FEMALE)
        e.vm.onNeuteredSelected(Neutered.NO)
        e.vm.onQuickDate(QuickDate.LAST_7)

        e.vm.onApply()

        assertEquals(
            listOf(
                SearchFilter(
                    kind = AnimalKind.CAT,
                    kindCode = "000116",
                    kindName = "한국 고양이",
                    dateFrom = LocalDate(2026, 10, 29),
                    dateTo = today,
                    sidoCode = "6110000",
                    sidoName = "서울특별시",
                    sigunguCode = "3220000",
                    sigunguName = "강남구",
                    shelterRegNo = "311322200900001",
                    shelterName = "서울동물복지지원센터",
                    state = NoticeState.NOTICE,
                    neutered = Neutered.NO,
                    sex = Sex.FEMALE
                )
            ),
            e.animals.searchCalls
        )
        assertTrue(e.settings.savedFilters.isEmpty()) // 저장은 목록 ViewModel이 한다
    }

    @Test
    fun `초기화는 전부 기본값`() = runVmTest {
        val e =
            env(
                results = SearchResults(
                    filter = SearchFilter(kind = AnimalKind.DOG, kindName = "x"),
                    loading = LoadState.INITIAL
                )
            )
        e.vm.onReset()
        advanceUntilIdle()

        assertEquals(SearchFilter(), e.state.filter)
        assertEquals("", e.state.breedQuery)
    }

    @Test
    fun `마감 임박순(또는 전환 중)이면 상태 안내를 더한다`() = runVmTest {
        val latest =
            env(results = SearchResults(sort = SortOrder.LATEST, loading = LoadState.INITIAL))
        val closing =
            env(results = SearchResults(sort = SortOrder.CLOSING_SOON, loading = LoadState.INITIAL))
        val switching = env(
            results = SearchResults(
                loading = LoadState.REFRESH,
                pendingSort = SortOrder.CLOSING_SOON
            )
        )

        assertFalse(latest.state.showClosingSoonHint)
        assertTrue(closing.state.showClosingSoonHint)
        assertTrue(switching.state.showClosingSoonHint)
    }

    @Test
    fun `날짜 선택 millis는 UTC 자정으로 왕복한다`() {
        val date = LocalDate(2026, 11, 4)
        assertEquals(1_793_750_400_000L, date.toPickerMillis())
        assertEquals(date, pickerMillisToDate(date.toPickerMillis()))
        assertEquals(date, pickerMillisToDate(date.toPickerMillis() + 23 * 3_600_000L))
    }

    @Test
    fun `사망으로 끝난 공고도 보기 — 켜면 검색 조건에, 상태 전체에서만 고를 수 있고 초기화는 끔(5d U6)`() = runVmTest {
        val e = env()
        assertFalse(e.state.filter.includeEndedByDeath)
        assertTrue(e.state.canIncludeEndedByDeath)

        e.vm.onIncludeEndedByDeathChanged(true)
        advanceUntilIdle()
        assertTrue(e.state.filter.includeEndedByDeath)

        e.vm.onStateSelected(NoticeState.NOTICE)
        advanceUntilIdle()
        assertFalse(e.state.canIncludeEndedByDeath)
        assertTrue(e.state.filter.includeEndedByDeath) // 값은 남는다

        e.vm.onStateSelected(NoticeState.ALL)
        advanceUntilIdle()
        assertTrue(e.state.canIncludeEndedByDeath)

        e.vm.onApply()
        assertEquals(listOf(SearchFilter(includeEndedByDeath = true)), e.animals.searchCalls)

        e.vm.onReset()
        advanceUntilIdle()
        assertFalse(e.state.filter.includeEndedByDeath)
    }
}
