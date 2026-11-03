package com.cherryzp.animalshelter.data.repository

import com.cherryzp.animalshelter.data.api.AnimalShelterApi
import com.cherryzp.animalshelter.data.api.ApiError
import com.cherryzp.animalshelter.data.api.ApiResult
import com.cherryzp.animalshelter.data.api.PAGE_SIZE
import com.cherryzp.animalshelter.model.Animal
import com.cherryzp.animalshelter.model.AnimalPage
import com.cherryzp.animalshelter.model.NoticeState
import com.cherryzp.animalshelter.model.SearchFilter
import com.cherryzp.animalshelter.model.SortOrder
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate

/** 지금 결과로 상세를 대신하는 기간, 마감 임박순 메모리 캐시 기간. */
internal val RESULTS_REUSE_WINDOW: Duration = 10.minutes

/** 마감 임박순: 공고 종료일 → 접수일 오름차순, 없는 값은 뒤. */
internal val ClosingSoonOrder: Comparator<Animal> =
    compareBy<Animal, LocalDate?>(nullsLast()) { it.noticeEnd }
        .thenBy(nullsLast()) { it.happenDate }

/**
 * 목록 Repository(02-data 9절, D8·D9·D13). 로딩 작업은 한 번에 하나이고 [appScope]에서 돈다.
 * 작업이 직렬로 돌므로 [closingSoonCache]는 작업 안에서만 읽고 쓴다.
 */
internal class DefaultAnimalRepository(
    private val api: AnimalShelterApi,
    private val clock: Clock,
    private val appScope: CoroutineScope,
) : AnimalRepository {
    private val results = MutableStateFlow(SearchResults())
    private val jobLock = Mutex()
    private var loadJob: Job? = null
    private var closingSoonCache: ClosingSoonCache? = null

    private class ClosingSoonCache(
        val filter: SearchFilter,
        val items: List<Animal>,
        val fetchedAt: Instant,
    )

    override fun getSearchResultsStream(): StateFlow<SearchResults> = results.asStateFlow()

    override suspend fun search(filter: SearchFilter) = restart { runSearch(filter) }

    override suspend fun setSort(order: SortOrder) {
        if (order == results.value.sort) return
        restart { runSwitchSort(order) }
    }

    override suspend fun refresh() = restart { runRefresh() }

    override suspend fun loadMore() {
        val job = jobLock.withLock {
            if (loadJob?.isActive == true) return
            val current = results.value
            if (current.sort != SortOrder.LATEST || !current.hasMore) return
            appScope.launch { runLoadMore() }.also { loadJob = it }
        }
        job.join()
    }

    override suspend fun getAnimal(
        desertionNo: String,
        happenDate: LocalDate?,
    ): ApiResult<Animal?> {
        val current = results.value
        val fetchedAt = current.fetchedAt
        if (fetchedAt != null && clock.now() - fetchedAt < RESULTS_REUSE_WINDOW) {
            current.items.firstOrNull {
                it.desertionNo == desertionNo
            }?.let { return ApiResult.Success(it) }
        }
        return api.getAnimal(desertionNo, happenDate)
    }

    /** 진행 중인 작업을 취소하고(끝날 때까지 기다린 뒤) 새 작업을 앱 범위에서 시작해 끝까지 기다린다. */
    private suspend fun restart(block: suspend () -> Unit) {
        val job = jobLock.withLock {
            loadJob?.cancelAndJoin()
            appScope.launch { block() }.also { loadJob = it }
        }
        job.join()
    }

    private suspend fun runSearch(filter: SearchFilter) {
        val sort = results.value.sort
        results.value = SearchResults(filter = filter, sort = sort, loading = LoadState.INITIAL)
        if (sort == SortOrder.LATEST) {
            val page = api.searchAnimals(filter, pageNo = 1, numOfRows = PAGE_SIZE)
            publish(
                when (page) {
                    is ApiResult.Success -> latestResults(filter, page.value)
                    is ApiResult.Failure -> SearchResults(filter = filter, error = page.error)
                }
            )
            return
        }
        when (val all = collectClosingSoon(filter, useCache = true)) {
            is ApiResult.Success -> publish(all.value)

            is ApiResult.Failure -> {
                val error = all.error
                if (error is ApiError.TooManyResults) {
                    publish(
                        latestFallback(filter, error, onFailure = SearchResults(filter = filter))
                    )
                } else {
                    publish(
                        SearchResults(filter = filter, sort = SortOrder.CLOSING_SOON, error = error)
                    )
                }
            }
        }
    }

    private suspend fun runSwitchSort(order: SortOrder) {
        val current = results.value.copy(loading = LoadState.IDLE, error = null)
        results.value = current.copy(loading = LoadState.REFRESH)
        val next = when (order) {
            SortOrder.CLOSING_SOON -> collectClosingSoon(current.filter, useCache = true)

            SortOrder.LATEST -> when (val page = api.searchAnimals(current.filter, 1, PAGE_SIZE)) {
                is ApiResult.Success -> ApiResult.Success(latestResults(current.filter, page.value))
                is ApiResult.Failure -> page
            }
        }
        publish(
            when (next) {
                is ApiResult.Success -> next.value

                // 실패(5,000건 초과 포함): 기존 목록·정렬을 그대로 두고 안내만
                is ApiResult.Failure -> current.copy(error = next.error)
            }
        )
    }

    private suspend fun runRefresh() {
        val current = results.value.copy(loading = LoadState.IDLE, error = null)
        val loading = if (current.items.isEmpty()) LoadState.INITIAL else LoadState.REFRESH
        results.value = current.copy(loading = loading)
        if (current.sort == SortOrder.LATEST) {
            val page = api.searchAnimals(current.filter, pageNo = 1, numOfRows = PAGE_SIZE)
            publish(
                when (page) {
                    is ApiResult.Success -> latestResults(current.filter, page.value)
                    is ApiResult.Failure -> current.copy(error = page.error)
                }
            )
            return
        }
        when (val all = collectClosingSoon(current.filter, useCache = false)) {
            is ApiResult.Success -> publish(all.value)

            is ApiResult.Failure -> {
                val error = all.error
                if (error is ApiError.TooManyResults) {
                    publish(latestFallback(current.filter, error, onFailure = current))
                } else {
                    publish(current.copy(error = error))
                }
            }
        }
    }

    private suspend fun runLoadMore() {
        val current = results.value.copy(error = null)
        results.value = current.copy(loading = LoadState.MORE)
        val next = when (
            val page = api.searchAnimals(
                current.filter,
                current.pageNo + 1,
                PAGE_SIZE
            )
        ) {
            is ApiResult.Failure -> current.copy(loading = LoadState.IDLE, error = page.error)

            is ApiResult.Success -> {
                val known = current.items.mapTo(HashSet()) { it.desertionNo }
                current.copy(
                    items = current.items + page.value.items.filter { it.desertionNo !in known },
                    totalCount = page.value.totalCount,
                    pageNo = page.value.pageNo,
                    hasMore = page.value.hasMore,
                    loading = LoadState.IDLE
                )
            }
        }
        publish(next)
    }

    /** 마감 임박순이 5,000건을 넘으면 LATEST 1페이지로 바꾸되 안내는 남긴다(D13). */
    private suspend fun latestFallback(
        filter: SearchFilter,
        tooMany: ApiError.TooManyResults,
        onFailure: SearchResults,
    ): SearchResults =
        when (val page = api.searchAnimals(filter, pageNo = 1, numOfRows = PAGE_SIZE)) {
            is ApiResult.Success -> latestResults(filter, page.value).copy(error = tooMany)
            is ApiResult.Failure -> onFailure.copy(loading = LoadState.IDLE, error = page.error)
        }

    /** 공고중만 전부 받아 정렬한다. 같은 조건이면 10분 메모리 캐시(`useCache`일 때). */
    private suspend fun collectClosingSoon(
        filter: SearchFilter,
        useCache: Boolean,
    ): ApiResult<SearchResults> {
        val now = clock.now()
        val cached = closingSoonCache
        if (useCache && cached != null && cached.filter == filter &&
            now - cached.fetchedAt < RESULTS_REUSE_WINDOW
        ) {
            return ApiResult.Success(closingSoonResults(filter, cached.items, cached.fetchedAt))
        }
        return when (val all = api.searchAll(filter.copy(state = NoticeState.NOTICE))) {
            is ApiResult.Failure -> all

            is ApiResult.Success -> {
                val sorted = all.value.sortedWith(ClosingSoonOrder)
                closingSoonCache = ClosingSoonCache(filter, sorted, now)
                ApiResult.Success(closingSoonResults(filter, sorted, now))
            }
        }
    }

    private fun latestResults(filter: SearchFilter, page: AnimalPage) = SearchResults(
        filter = filter,
        sort = SortOrder.LATEST,
        items = page.items,
        totalCount = page.totalCount,
        pageNo = page.pageNo,
        hasMore = page.hasMore,
        loading = LoadState.IDLE,
        error = null,
        fetchedAt = clock.now()
    )

    private fun closingSoonResults(filter: SearchFilter, items: List<Animal>, fetchedAt: Instant) =
        SearchResults(
            filter = filter,
            sort = SortOrder.CLOSING_SOON,
            items = items,
            totalCount = items.size,
            pageNo = 0,
            hasMore = false,
            loading = LoadState.IDLE,
            error = null,
            fetchedAt = fetchedAt
        )

    /** 취소된 작업은 결과를 쓰지 않는다. */
    private suspend fun publish(next: SearchResults) {
        currentCoroutineContext().ensureActive()
        results.value = next.copy(loading = LoadState.IDLE)
    }
}
