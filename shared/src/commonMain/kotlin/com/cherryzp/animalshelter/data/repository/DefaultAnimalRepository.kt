package com.cherryzp.animalshelter.data.repository

import com.cherryzp.animalshelter.data.api.AnimalShelterApi
import com.cherryzp.animalshelter.data.api.ApiError
import com.cherryzp.animalshelter.data.api.ApiResult
import com.cherryzp.animalshelter.data.api.PAGE_SIZE
import com.cherryzp.animalshelter.model.Animal
import com.cherryzp.animalshelter.model.NoticeState
import com.cherryzp.animalshelter.model.SearchFilter
import com.cherryzp.animalshelter.model.SortOrder
import com.cherryzp.animalshelter.model.hidesEndedByDeath
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

/** 지금 결과로 상세를 대신하는 기간, 마감 임박순 메모리 캐시 기간. */
internal val RESULTS_REUSE_WINDOW: Duration = 10.minutes

/**
 * 최신순 한 동작(검색·새로고침·최신순 전환·5,000건 초과 되돌림·다음 쪽)이 받는 쪽 수의 상한(처음 쪽 포함, 5d).
 * 보이는 것이 하나도 늘지 않은 쪽만 이어 받는다 — 실측 연속 사망 최대 8건(survey 8절)이라 3쪽이면 넉넉하다.
 */
internal const val MAX_PAGES_PER_LOAD = 3

/** 마감 임박순: 공고 종료일 → 접수일 오름차순, 없는 값은 뒤. */
internal val ClosingSoonOrder: Comparator<Animal> =
    compareBy<Animal, LocalDate?>(nullsLast()) { it.noticeEnd }
        .thenBy(nullsLast()) { it.happenDate }

/**
 * 목록 Repository(02-data 9절, D8·D9·D13). 로딩 작업은 한 번에 하나이고 [appScope]에서 돈다.
 * 작업이 직렬로 돌므로 [closingSoonCache]·[errorCounter]는 작업 안에서만 읽고 쓴다.
 *
 * 작업 교체(잠금 → 이전 작업 취소·대기 → 새 작업 시작)는 `NonCancellable`로 감싼다(R1): 부른 쪽이
 * 이전 작업을 기다리는 동안 취소돼도 새 작업은 시작된다. 끝까지 기다리는 `join()`만 취소될 수 있다.
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
    private var errorCounter = 0L

    /** 마감 임박순 10분 캐시 — 나눈 채로(보이는 것은 정렬, 사망 종료는 [hidden]). */
    private class ClosingSoonCache(
        val filter: SearchFilter,
        val items: List<Animal>,
        val hidden: List<Animal>,
        val fetchedAt: Instant,
    )

    override fun getSearchResultsStream(): StateFlow<SearchResults> = results.asStateFlow()

    override suspend fun search(filter: SearchFilter) = restart { runSearch(filter) }

    override suspend fun setSort(order: SortOrder) {
        val job = withContext(NonCancellable) {
            jobLock.withLock {
                val current = results.value
                val pending = current.pendingSort
                when {
                    order == (pending ?: current.sort) -> null

                    // 전환 중에 보이는 정렬을 골랐다: 전환만 취소하고 지금 목록으로 돌아간다(호출 0)
                    pending != null && order == current.sort -> {
                        loadJob?.cancelAndJoin()
                        results.value =
                            results.value.copy(loading = LoadState.IDLE, pendingSort = null)
                        null
                    }

                    else -> {
                        loadJob?.cancelAndJoin()
                        results.value = results.value.copy(
                            loading = LoadState.REFRESH,
                            error = null,
                            pendingSort = order
                        )
                        appScope.launch { runSwitchSort(order, useCache = true) }.also {
                            loadJob =
                                it
                        }
                    }
                }
            }
        }
        job?.join()
    }

    override suspend fun refresh() = restart { runRefresh() }

    override suspend fun loadMore() {
        val job = withContext(NonCancellable) {
            jobLock.withLock {
                if (loadJob?.isActive == true) return@withLock null
                val current = results.value
                if (current.sort != SortOrder.LATEST || !current.hasMore) return@withLock null
                appScope.launch { runLoadMore() }.also { loadJob = it }
            }
        }
        job?.join()
    }

    override suspend fun getAnimal(
        desertionNo: String,
        happenDate: LocalDate?,
    ): ApiResult<Animal?> {
        val current = results.value
        val fetchedAt = current.fetchedAt
        if (fetchedAt != null && clock.now() - fetchedAt < RESULTS_REUSE_WINDOW) {
            // 목록에서 숨긴 사망 종료도 그대로 돌려준다 — 보일지는 부르는 쪽이 정한다(5d)
            (
                current.items.firstOrNull { it.desertionNo == desertionNo }
                    ?: current.hidden.firstOrNull { it.desertionNo == desertionNo }
                )
                ?.let { return ApiResult.Success(it) }
        }
        return api.getAnimal(desertionNo, happenDate)
    }

    /**
     * 진행 중인 작업을 취소하고(끝날 때까지 기다린 뒤) 새 작업을 앱 범위에서 시작해 끝까지 기다린다.
     * 교체 구간은 취소되지 않는다(R1).
     */
    private suspend fun restart(block: suspend () -> Unit) {
        val job = withContext(NonCancellable) {
            jobLock.withLock {
                loadJob?.cancelAndJoin()
                appScope.launch { block() }.also { loadJob = it }
            }
        }
        job.join()
    }

    private suspend fun runSearch(filter: SearchFilter) {
        // 정렬 전환 중이면(취소된 전환이 남긴 목표) 그 정렬로 검색한다(R2)
        val sort = results.value.let { it.pendingSort ?: it.sort }
        results.value = SearchResults(filter = filter, sort = sort, loading = LoadState.INITIAL)
        if (sort == SortOrder.LATEST) {
            publish(
                when (val latest = fetchLatest(filter, base = null)) {
                    is ApiResult.Success -> latest.value
                    is ApiResult.Failure -> SearchResults(filter = filter, error = latest.error)
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

    private suspend fun runSwitchSort(order: SortOrder, useCache: Boolean) {
        val current = results.value.copy(loading = LoadState.IDLE, error = null, pendingSort = null)
        results.value = current.copy(loading = LoadState.REFRESH, pendingSort = order)
        val next = when (order) {
            SortOrder.CLOSING_SOON -> collectClosingSoon(current.filter, useCache = useCache)
            SortOrder.LATEST -> fetchLatest(current.filter, base = null)
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
        // 정렬 전환 중의 새로고침은 그 전환을 캐시 없이 다시 한다
        results.value.pendingSort?.let { return runSwitchSort(it, useCache = false) }
        val current = results.value.copy(loading = LoadState.IDLE, error = null)
        val loading = if (current.items.isEmpty()) LoadState.INITIAL else LoadState.REFRESH
        results.value = current.copy(loading = loading)
        if (current.sort == SortOrder.LATEST) {
            publish(
                when (val latest = fetchLatest(current.filter, base = null)) {
                    is ApiResult.Success -> latest.value
                    is ApiResult.Failure -> current.copy(error = latest.error)
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
        val next = when (val latest = fetchLatest(current.filter, base = current)) {
            is ApiResult.Success -> latest.value
            is ApiResult.Failure -> current.copy(loading = LoadState.IDLE, error = latest.error)
        }
        publish(next)
    }

    /**
     * 최신순 쪽을 [base] 뒤에 받는다(base가 null이면 1쪽부터 — `fetchedAt`은 지금). 받은 쪽마다
     * `filter.hidesEndedByDeath`는 `hidden`으로(필터 켬이면 모두 `items`), 번호는 `items`·`hidden`을 통틀어 한 번.
     * 보이는 것이 하나도 늘지 않았고 `hasMore`면 이어 받는다 — 많아야 [MAX_PAGES_PER_LOAD]쪽(5d). 결과는 부르는 쪽이 끝에 한 번 낸다(중간 결과 없음).
     * 첫 쪽이 실패하면 `Failure`, 뒤 쪽이 실패하면 그때까지의 결과 + `error`(`pageNo`는 마지막 성공 쪽).
     * `totalCount`·`pageNo`·`hasMore`는 원본 기준이다 — 다 숨겨진 쪽은 빈 쪽이 아니다.
     */
    private suspend fun fetchLatest(
        filter: SearchFilter,
        base: SearchResults?,
    ): ApiResult<SearchResults> {
        var acc = base ?: SearchResults(filter = filter, sort = SortOrder.LATEST)
        val known = (acc.items + acc.hidden).mapTo(HashSet()) { it.desertionNo }
        var pages = 0
        while (true) {
            val page = when (
                val result = api.searchAnimals(filter, acc.pageNo + 1, PAGE_SIZE)
            ) {
                is ApiResult.Success -> result.value

                is ApiResult.Failure ->
                    return if (pages ==
                        0
                    ) {
                        result
                    } else {
                        ApiResult.Success(acc.copy(error = result.error))
                    }
            }
            pages++
            val (dead, alive) = page.items.filter { known.add(it.desertionNo) }
                .partition { filter.hidesEndedByDeath(it) }
            acc = acc.copy(
                items = acc.items + alive,
                hidden = acc.hidden + dead,
                // 원본이 빈 쪽(끝)은 totalCount를 0으로 주기도 한다(Fixture 2페이지) — 그때는 앞의 값을 둔다
                totalCount = if (page.items.isEmpty() && acc.totalCount != null) {
                    acc.totalCount
                } else {
                    page.totalCount
                },
                pageNo = page.pageNo,
                hasMore = page.hasMore
            )
            if (alive.isNotEmpty() || !page.hasMore || pages >= MAX_PAGES_PER_LOAD) break
        }
        return ApiResult.Success(
            if (base == null) {
                acc.copy(loading = LoadState.IDLE, error = null, fetchedAt = clock.now())
            } else {
                acc
            }
        )
    }

    /** 마감 임박순이 5,000건을 넘으면 LATEST 1페이지로 바꾸되 안내는 남긴다(D13). */
    private suspend fun latestFallback(
        filter: SearchFilter,
        tooMany: ApiError.TooManyResults,
        onFailure: SearchResults,
    ): SearchResults = when (val latest = fetchLatest(filter, base = null)) {
        is ApiResult.Success -> latest.value.let { it.copy(error = it.error ?: tooMany) }
        is ApiResult.Failure -> onFailure.copy(loading = LoadState.IDLE, error = latest.error)
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
            return ApiResult.Success(closingSoonResults(cached))
        }
        return when (val all = api.searchAll(filter.copy(state = NoticeState.NOTICE))) {
            is ApiResult.Failure -> all

            is ApiResult.Success -> {
                // 공고중만이라 보통 0건이지만 규칙은 한곳에 둔다(5d S2)
                val (dead, alive) = all.value.partition { filter.hidesEndedByDeath(it) }
                val cache = ClosingSoonCache(filter, alive.sortedWith(ClosingSoonOrder), dead, now)
                closingSoonCache = cache
                ApiResult.Success(closingSoonResults(cache))
            }
        }
    }

    private fun closingSoonResults(cache: ClosingSoonCache) = SearchResults(
        filter = cache.filter,
        sort = SortOrder.CLOSING_SOON,
        items = cache.items,
        totalCount = cache.items.size,
        pageNo = 0,
        hasMore = false,
        loading = LoadState.IDLE,
        error = null,
        fetchedAt = cache.fetchedAt,
        hidden = cache.hidden
    )

    /** 취소된 작업은 결과를 쓰지 않는다. 오류를 실은 결과에는 새 [SearchResults.errorId]를 붙인다(R3). */
    private suspend fun publish(next: SearchResults) {
        currentCoroutineContext().ensureActive()
        val errorId = if (next.error != null) ++errorCounter else next.errorId
        results.value = next.copy(loading = LoadState.IDLE, pendingSort = null, errorId = errorId)
    }
}
