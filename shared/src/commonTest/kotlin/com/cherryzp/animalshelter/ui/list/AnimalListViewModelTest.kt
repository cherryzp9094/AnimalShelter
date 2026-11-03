package com.cherryzp.animalshelter.ui.list

import androidx.lifecycle.ViewModel
import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.data.api.ApiError
import com.cherryzp.animalshelter.data.api.EndpointSelector
import com.cherryzp.animalshelter.data.repository.BookmarkRepository
import com.cherryzp.animalshelter.data.repository.LoadState
import com.cherryzp.animalshelter.data.repository.SearchResults
import com.cherryzp.animalshelter.data.repository.Settings
import com.cherryzp.animalshelter.di.AppConfig
import com.cherryzp.animalshelter.fake.FakeAnimalRepository
import com.cherryzp.animalshelter.fake.FakeBookmarkRepository
import com.cherryzp.animalshelter.fake.FakeSettingsRepository
import com.cherryzp.animalshelter.fake.testAnimal
import com.cherryzp.animalshelter.fake.testAnimals
import com.cherryzp.animalshelter.model.Animal
import com.cherryzp.animalshelter.model.AnimalKind
import com.cherryzp.animalshelter.model.ListLayout
import com.cherryzp.animalshelter.model.SearchFilter
import com.cherryzp.animalshelter.model.SortOrder
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.bookmark_added
import com.cherryzp.animalshelter.resources.bookmark_removed
import com.cherryzp.animalshelter.resources.bookmark_save_failed
import com.cherryzp.animalshelter.resources.date_default
import com.cherryzp.animalshelter.resources.error_network
import com.cherryzp.animalshelter.resources.filter_none
import com.cherryzp.animalshelter.resources.list_noticing_count
import com.cherryzp.animalshelter.resources.list_total_count
import com.cherryzp.animalshelter.resources.sort_too_many
import com.cherryzp.animalshelter.testing.FakeClock
import com.cherryzp.animalshelter.testing.runMainTest
import com.cherryzp.animalshelter.ui.common.MessageAction
import com.cherryzp.animalshelter.ui.common.UiMessage
import com.cherryzp.animalshelter.ui.common.uiText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle

/** 목록 ViewModel(브리핑 3a A7~A16, 8절 완료 기준). Fake Repository만 쓴다. */
class AnimalListViewModelTest {
    private val clock = FakeClock()
    private val fetched = Instant.parse("2026-11-04T00:50:00Z")
    private val created = mutableListOf<ViewModel>()

    private fun runVmTest(block: suspend TestScope.() -> Unit) = runMainTest({ created }, block)

    private inner class Env(
        initial: SearchResults = SearchResults(),
        settings: Settings = Settings(),
        val bookmarks: BookmarkRepository = FakeBookmarkRepository(),
        isDebug: Boolean = false,
    ) {
        val animals = FakeAnimalRepository(initial)
        val settings = FakeSettingsRepository(settings)
        val vm = AnimalListViewModel(
            animals = animals,
            bookmarks = bookmarks,
            settings = this.settings,
            endpoints = EndpointSelector(AppConfig(ApiEndpoint.Fixture, isDebug, "test")) {
                this.settings.settings.value.endpointOverride
            },
            clock = clock,
            config = AppConfig(ApiEndpoint.Fixture, isDebug, "test")
        )
        val state get() = vm.uiState.value

        init {
            created += vm
        }
    }

    /** 받은 결과(최신순, 다음 페이지 있음). */
    private fun loaded(
        items: List<Animal> = testAnimals(3),
        filter: SearchFilter = SearchFilter(),
        sort: SortOrder = SortOrder.LATEST,
        hasMore: Boolean = true,
        totalCount: Int? = 90,
    ) = SearchResults(
        filter = filter,
        sort = sort,
        items = items,
        totalCount = totalCount,
        pageNo = 1,
        hasMore = hasMore,
        fetchedAt = fetched
    )

    private fun TestScope.subscribe(env: Env) {
        backgroundScope.launch { env.vm.uiState.collect {} }
        advanceUntilIdle()
    }

    @Test
    fun `첫 검색은 마지막 조건을 쓰고 없으면 기본 조건`() = runVmTest {
        val last = SearchFilter(kind = AnimalKind.CAT, sidoCode = "6110000", sidoName = "서울특별시")
        val withLast = Env(settings = Settings(lastFilter = last))
        val withoutLast = Env()
        advanceUntilIdle()

        assertEquals(listOf(last), withLast.animals.searchCalls)
        assertEquals(listOf(SearchFilter()), withoutLast.animals.searchCalls)
    }

    @Test
    fun `결과가 이미 있거나 받는 중이면 다시 검색하지 않는다`() = runVmTest {
        val done = Env(initial = loaded())
        val loading = Env(initial = SearchResults(loading = LoadState.INITIAL))
        advanceUntilIdle()

        assertTrue(done.animals.searchCalls.isEmpty())
        assertTrue(loading.animals.searchCalls.isEmpty())
    }

    @Test
    fun `받는 데 성공한 조건만 한 번 저장한다`() = runVmTest {
        val f1 = SearchFilter(sidoCode = "6110000", sidoName = "서울특별시")
        val env = Env(initial = SearchResults(filter = f1, loading = LoadState.INITIAL))
        advanceUntilIdle()
        env.animals.results.value =
            SearchResults(filter = f1, error = ApiError.Network, errorId = 1)
        advanceUntilIdle()
        assertTrue(env.settings.savedFilters.isEmpty())

        env.animals.results.value = loaded(filter = f1)
        advanceUntilIdle()
        env.animals.results.value = loaded(filter = f1, items = testAnimals(4))
        advanceUntilIdle()

        assertEquals(listOf(f1), env.settings.savedFilters)
    }

    @Test
    fun `저장값과 같은 조건은 다시 저장하지 않는다`() = runVmTest {
        val f1 = SearchFilter(sidoCode = "6110000")
        val env = Env(initial = loaded(filter = f1), settings = Settings(lastFilter = f1))
        advanceUntilIdle()

        assertTrue(env.settings.savedFilters.isEmpty())
    }

    @Test
    fun `정렬 선택은 Repository에 넘기고 전환 중에는 목표를 체크하고 상단 선`() = runVmTest {
        val env = Env(initial = loaded())
        subscribe(env)

        env.vm.onSortSelected(SortOrder.CLOSING_SOON)
        advanceUntilIdle()
        env.animals.results.value =
            loaded().copy(loading = LoadState.REFRESH, pendingSort = SortOrder.CLOSING_SOON)
        advanceUntilIdle()

        assertEquals(listOf(SortOrder.CLOSING_SOON), env.animals.sortCalls)
        assertEquals(SortOrder.LATEST, env.state.sort)
        assertEquals(SortOrder.CLOSING_SOON, env.state.pendingSort)
        assertEquals(RefreshIndicator.BAR, env.state.refreshIndicator)

        // 전환 중에 보이는 정렬을 고르면 그대로 넘긴다(취소는 Repository가 한다)
        env.vm.onSortSelected(SortOrder.LATEST)
        advanceUntilIdle()
        assertEquals(listOf(SortOrder.CLOSING_SOON, SortOrder.LATEST), env.animals.sortCalls)
    }

    @Test
    fun `정렬 칩을 누르면 최신순으로`() = runVmTest {
        val env = Env(initial = loaded(sort = SortOrder.CLOSING_SOON, hasMore = false))
        advanceUntilIdle()

        env.vm.onSortChipClicked()
        advanceUntilIdle()

        assertEquals(listOf(SortOrder.LATEST), env.animals.sortCalls)
    }

    @Test
    fun `자동 다음 페이지는 최신순·다음 있음·오류 없음·로딩 없음일 때만`() = runVmTest {
        val cases = listOf(
            loaded() to 1,
            loaded(sort = SortOrder.CLOSING_SOON) to 0,
            loaded(hasMore = false) to 0,
            loaded().copy(error = ApiError.Network, errorId = 3) to 0,
            loaded().copy(loading = LoadState.MORE) to 0,
            loaded().copy(loading = LoadState.REFRESH) to 0,
            // 5,000건 초과 안내 뒤 최신순 목록은 이어 받는다
            loaded().copy(error = ApiError.TooManyResults(7544), errorId = 4) to 1
        )
        for ((results, expected) in cases) {
            val env = Env(initial = results)
            advanceUntilIdle()
            env.vm.onNearEnd()
            advanceUntilIdle()
            assertEquals(expected, env.animals.loadMoreCalls, results.toString())
        }
    }

    @Test
    fun `새 errorId마다 스낵바 한 번, 보여 주면 지운다`() = runVmTest {
        val env = Env(initial = loaded())
        subscribe(env)
        env.vm.onRefresh(byPull = false)
        advanceUntilIdle()

        env.animals.results.value = loaded().copy(error = ApiError.Network, errorId = 1)
        advanceUntilIdle()
        assertEquals(
            UiMessage(uiText(Res.string.error_network), MessageAction.RETRY),
            env.state.message
        )

        env.vm.onMessageShown()
        env.animals.results.value =
            loaded(items = testAnimals(4)).copy(error = ApiError.Network, errorId = 1)
        advanceUntilIdle()
        assertNull(env.state.message) // 같은 오류는 다시 알리지 않는다

        env.animals.results.value = loaded().copy(error = ApiError.Network, errorId = 2)
        advanceUntilIdle()
        assertEquals(uiText(Res.string.error_network), env.state.message?.text)
    }

    @Test
    fun `만들 때 남아 있던 오류는 알리지 않는다`() = runVmTest {
        val env = Env(initial = loaded().copy(error = ApiError.Network, errorId = 9))
        subscribe(env)

        assertNull(env.state.message)
    }

    @Test
    fun `다시 시도는 마지막 동작이 다음 페이지면 다음 페이지, 아니면 새로고침`() = runVmTest {
        val env = Env(initial = loaded())
        subscribe(env)

        env.vm.onNearEnd()
        advanceUntilIdle()
        env.animals.results.value = loaded().copy(error = ApiError.Network, errorId = 1)
        advanceUntilIdle()
        assertEquals(ListFooter.RETRY, env.state.footer)
        assertNull(env.state.message) // 다음 페이지 실패는 끝 행이 알린다
        env.vm.onRetry()
        advanceUntilIdle()
        assertEquals(2, env.animals.loadMoreCalls)

        env.vm.onRefresh(byPull = true)
        advanceUntilIdle()
        env.vm.onMessageAction(MessageAction.RETRY)
        advanceUntilIdle()
        assertEquals(2, env.animals.refreshCalls)
    }

    @Test
    fun `5000건 초과는 sort_too_many 안내만(동작 없음)`() = runVmTest {
        val env = Env(initial = loaded())
        subscribe(env)

        env.vm.onSortSelected(SortOrder.CLOSING_SOON)
        advanceUntilIdle()
        env.animals.results.value =
            loaded().copy(error = ApiError.TooManyResults(7544), errorId = 1)
        advanceUntilIdle()

        assertEquals(UiMessage(uiText(Res.string.sort_too_many), action = null), env.state.message)
        assertEquals(SortOrder.LATEST, env.state.sort)
        assertNull(env.state.fullScreen)
    }

    @Test
    fun `북마크 토글은 추가·제거 안내`() = runVmTest {
        val bookmarks = FakeBookmarkRepository()
        val animal = testAnimal(desertionNo = "450650202600001")
        val env = Env(initial = loaded(items = listOf(animal)), bookmarks = bookmarks)
        subscribe(env)

        env.vm.onToggleBookmark(animal.desertionNo)
        advanceUntilIdle()
        assertEquals(listOf(animal), bookmarks.addCalls)
        assertEquals(uiText(Res.string.bookmark_added), env.state.message?.text)
        assertTrue(env.state.items.single().isBookmarked)

        env.vm.onToggleBookmark(animal.desertionNo)
        advanceUntilIdle()
        assertEquals(listOf(animal.desertionNo), bookmarks.removeCalls)
        assertEquals(uiText(Res.string.bookmark_removed), env.state.message?.text)
    }

    @Test
    fun `북마크 쓰기 실패는 안내`() = runVmTest {
        val failing = object : BookmarkRepository by FakeBookmarkRepository() {
            override suspend fun add(animal: Animal) = error("쓰기 실패")
        }
        val env = Env(initial = loaded(), bookmarks = failing)
        subscribe(env)

        env.vm.onToggleBookmark(env.animals.results.value.items.first().desertionNo)
        advanceUntilIdle()

        assertEquals(uiText(Res.string.bookmark_save_failed), env.state.message?.text)
    }

    @Test
    fun `보기 전환은 설정에 저장하고 상태에 보인다`() = runVmTest {
        val env = Env(initial = loaded())
        subscribe(env)

        env.vm.onToggleLayout()
        advanceUntilIdle()

        assertEquals(ListLayout.LARGE, env.settings.settings.value.listLayout)
        assertEquals(ListLayout.LARGE, env.state.layout)
    }

    @Test
    fun `기본 칩은 전체와 최근 1개월`() = runVmTest {
        val env = Env(initial = loaded())
        subscribe(env)

        assertEquals(
            listOf(
                SummaryChip(uiText(Res.string.filter_none), ChipTarget.FILTER),
                SummaryChip(uiText(Res.string.date_default), ChipTarget.FILTER)
            ),
            env.state.chips
        )
    }

    @Test
    fun `헤더는 쉼표 숫자, 마감 임박순이면 공고중 n마리, 모르면 없음`() = runVmTest {
        val latest = Env(initial = loaded(totalCount = 7544)).also { subscribe(it) }
        val closing = Env(
            initial = loaded(sort = SortOrder.CLOSING_SOON, totalCount = 1234, hasMore = false)
        )
            .also { subscribe(it) }
        val unknown = Env(initial = loaded(totalCount = null)).also { subscribe(it) }

        assertEquals(uiText(Res.string.list_total_count, "7,544"), latest.state.header)
        assertEquals(uiText(Res.string.list_noticing_count, "1,234"), closing.state.header)
        assertNull(unknown.state.header)
    }

    @Test
    fun `디버그 Relay 덮어쓰기면 사진이 중계 img로, 지우면 원본`() = runVmTest {
        val env = Env(
            initial = loaded(items = listOf(testAnimal())),
            settings = Settings(endpointOverride = ApiEndpoint.Relay("http://10.0.2.2:8788")),
            isDebug = true
        )
        subscribe(env)
        assertEquals(
            "http://10.0.2.2:8788/img/shelter/2026/10/a.jpg",
            env.state.items.single().photoUrl
        )

        env.settings.setEndpointOverride(null)
        advanceUntilIdle()
        assertEquals(testAnimal().photos.single(), env.state.items.single().photoUrl)
    }

    @Test
    fun `빈 결과·오류·첫 로딩·끝 행`() = runVmTest {
        val empty = Env(initial = loaded(items = emptyList(), totalCount = 0, hasMore = false))
            .also { subscribe(it) }
        val failed = Env(initial = SearchResults(error = ApiError.Gateway("22", null), errorId = 1))
            .also { subscribe(it) }
        val first = Env(initial = SearchResults(loading = LoadState.INITIAL)).also { subscribe(it) }
        val end = Env(initial = loaded(hasMore = false)).also { subscribe(it) }

        assertEquals(FullScreenState.Empty, empty.state.fullScreen)
        val error = failed.state.fullScreen as FullScreenState.Error
        assertEquals(false, error.canRetry)
        assertTrue(first.state.isInitialLoading)
        assertNull(first.state.fullScreen)
        assertEquals(ListFooter.END, end.state.footer)
    }
}
