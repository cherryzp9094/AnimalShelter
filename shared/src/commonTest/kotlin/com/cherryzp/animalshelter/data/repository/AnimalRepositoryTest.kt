package com.cherryzp.animalshelter.data.repository

import com.cherryzp.animalshelter.data.api.ApiError
import com.cherryzp.animalshelter.data.api.ApiTestHarness
import com.cherryzp.animalshelter.data.api.getOrNull
import com.cherryzp.animalshelter.model.NoticeState
import com.cherryzp.animalshelter.model.SearchFilter
import com.cherryzp.animalshelter.model.SortOrder
import com.cherryzp.animalshelter.testing.ApiTestResponses
import com.cherryzp.animalshelter.testing.ApiTestResponses.animalItem
import com.cherryzp.animalshelter.testing.FakeClock
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject

/** 목록 Repository(02-data 9절, D8·D9·D13). MockEngine으로 응답을 정한다. */
class AnimalRepositoryTest {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val clock = FakeClock()

    @AfterTest
    fun tearDown() {
        appScope.cancel()
    }

    private fun repository(
        handler: MockRequestHandler,
    ): Pair<DefaultAnimalRepository, ApiTestHarness> {
        val harness = ApiTestHarness(clock = clock, handler = handler)
        return DefaultAnimalRepository(harness.api, clock, appScope) to harness
    }

    private fun ApiTestHarness.pageRequests(pageNo: Int) = urls.count {
        it.parameters["numOfRows"] == "30" && it.parameters["pageNo"] == pageNo.toString()
    }

    private fun ApiTestHarness.closingSoonRequests() = urls.count {
        it.parameters["numOfRows"] ==
            "1000"
    }

    @Test
    fun `search는 1페이지를 받고 loadMore로 이어 붙인다`() = runTest {
        val (repo, harness) = repository { request ->
            val body = when (request.url.parameters["pageNo"]) {
                "1" -> ApiTestResponses.animals("A", "B", totalCount = 45)
                "2" -> ApiTestResponses.animals("B", "C", totalCount = 45)
                else -> ApiTestResponses.empty
            }
            respond(body, HttpStatusCode.OK)
        }

        repo.search(SearchFilter(sidoCode = "6110000"))
        val first = repo.getSearchResultsStream().value
        repo.loadMore()
        val second = repo.getSearchResultsStream().value

        assertEquals(listOf("A", "B"), first.items.map { it.desertionNo })
        assertEquals(45, first.totalCount)
        assertEquals(1, first.pageNo)
        assertTrue(first.hasMore)
        assertEquals(clock.now(), first.fetchedAt)
        assertEquals(listOf("A", "B", "C"), second.items.map { it.desertionNo })
        assertEquals(2, second.pageNo)
        assertFalse(second.hasMore) // 2 * 30 >= 45
        assertEquals(LoadState.IDLE, second.loading)
        assertEquals("6110000", harness.urls.first().parameters["upr_cd"])
    }

    @Test
    fun `빈 페이지를 받으면 totalCount와 무관하게 끝(D9)`() = runTest {
        val (repo, harness) = repository { request ->
            val body = if (request.url.parameters["pageNo"] == "1") {
                ApiTestResponses.animals("A", totalCount = 500)
            } else {
                ApiTestResponses.envelope(emptyList(), totalCount = 500)
            }
            respond(body, HttpStatusCode.OK)
        }

        repo.search(SearchFilter())
        repo.loadMore()
        val ended = repo.getSearchResultsStream().value
        repo.loadMore()

        assertFalse(ended.hasMore)
        assertEquals(listOf("A"), ended.items.map { it.desertionNo })
        assertEquals(1, harness.pageRequests(2))
        assertEquals(0, harness.pageRequests(3))
    }

    @Test
    fun `빈 끝 페이지의 totalCount 0은 앞의 전체 수를 지우지 않는다`() = runTest {
        val (repo, _) = repository { request ->
            val body = if (request.url.parameters["pageNo"] == "1") {
                ApiTestResponses.animals("A", totalCount = 7544)
            } else {
                ApiTestResponses.empty
            }
            respond(body, HttpStatusCode.OK)
        }

        repo.search(SearchFilter())
        repo.loadMore()

        val ended = repo.getSearchResultsStream().value
        assertFalse(ended.hasMore)
        assertEquals(7544, ended.totalCount)
    }

    @Test
    fun `loadMore는 로딩 중이면 무시한다`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val (repo, harness) = repository { request ->
            if (request.url.parameters["pageNo"] == "2") gate.await()
            respond(
                ApiTestResponses.animals("P${request.url.parameters["pageNo"]}", totalCount = 100)
            )
        }
        repo.search(SearchFilter())

        val loading = launch(Dispatchers.Default) { repo.loadMore() }
        repo.getSearchResultsStream().first { it.loading == LoadState.MORE }
        repo.loadMore() // 무시
        gate.complete(Unit)
        loading.join()

        assertEquals(1, harness.pageRequests(2))
        assertEquals(
            listOf("P1", "P2"),
            repo.getSearchResultsStream().value.items.map {
                it.desertionNo
            }
        )
    }

    @Test
    fun `부른 쪽 범위가 취소돼도 search는 끝까지 간다(D8)`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val (repo, _) = repository {
            gate.await()
            respond(ApiTestResponses.animals("A", "B"))
        }

        val caller = launch(Dispatchers.Default) { repo.search(SearchFilter(sidoCode = "6110000")) }
        repo.getSearchResultsStream().first { it.loading == LoadState.INITIAL }
        caller.cancelAndJoin()
        gate.complete(Unit)
        val done = repo.getSearchResultsStream().first { it.items.isNotEmpty() }

        assertEquals(listOf("A", "B"), done.items.map { it.desertionNo })
        assertEquals(LoadState.IDLE, done.loading)
    }

    @Test
    fun `search를 연달아 부르면 마지막 것만 남는다`() = runTest {
        val never = CompletableDeferred<Unit>()
        val (repo, _) = repository { request ->
            if (request.url.parameters["upr_cd"] == "1") never.await()
            respond(ApiTestResponses.animals("X${request.url.parameters["upr_cd"]}"))
        }

        val first = launch(Dispatchers.Default) { repo.search(SearchFilter(sidoCode = "1")) }
        repo.getSearchResultsStream().first { it.loading == LoadState.INITIAL }
        repo.search(SearchFilter(sidoCode = "2"))
        first.join()

        val results = repo.getSearchResultsStream().value
        assertEquals("2", results.filter.sidoCode)
        assertEquals(listOf("X2"), results.items.map { it.desertionNo })
        assertEquals(LoadState.IDLE, results.loading)
    }

    @Test
    fun `마감 임박순은 공고중만 모아 공고 종료일·접수일 순, null은 뒤`() = runTest {
        val (repo, harness) = repository { request ->
            val body = if (request.url.parameters["numOfRows"] == "1000") {
                ApiTestResponses.envelope(
                    listOf(
                        animalItem("late", noticeEdt = "20261120", happenDt = "20261101"),
                        animalItem("none", noticeEdt = null, happenDt = "20261001"),
                        animalItem("soonB", noticeEdt = "20261105", happenDt = "20261103"),
                        animalItem("soonA", noticeEdt = "20261105", happenDt = "20261102"),
                        animalItem("soonNoDate", noticeEdt = "20261105", happenDt = null)
                    )
                )
            } else {
                ApiTestResponses.animals("L1", "L2", totalCount = 2)
            }
            respond(body)
        }
        repo.search(SearchFilter(sidoCode = "6110000"))

        repo.setSort(SortOrder.CLOSING_SOON)

        val results = repo.getSearchResultsStream().value
        assertEquals(SortOrder.CLOSING_SOON, results.sort)
        assertEquals(
            listOf("soonA", "soonB", "soonNoDate", "late", "none"),
            results.items.map { it.desertionNo }
        )
        assertEquals(NoticeState.ALL, results.filter.state) // 사용자가 고른 조건은 그대로
        assertFalse(results.hasMore)
        val request = harness.urls.single { it.parameters["numOfRows"] == "1000" }
        assertEquals("notice", request.parameters["state"])
        assertEquals("6110000", request.parameters["upr_cd"])
    }

    @Test
    fun `마감 임박순 전환 중에는 기존 목록을 두고 REFRESH`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val (repo, _) = repository { request ->
            if (request.url.parameters["numOfRows"] == "1000") {
                gate.await()
                respond(ApiTestResponses.animals("C1"))
            } else {
                respond(ApiTestResponses.animals("L1", "L2"))
            }
        }
        repo.search(SearchFilter())

        val switching = launch(Dispatchers.Default) { repo.setSort(SortOrder.CLOSING_SOON) }
        val during = repo.getSearchResultsStream().first { it.loading == LoadState.REFRESH }
        gate.complete(Unit)
        switching.join()

        assertEquals(SortOrder.LATEST, during.sort)
        assertEquals(listOf("L1", "L2"), during.items.map { it.desertionNo })
        assertEquals(listOf("C1"), repo.getSearchResultsStream().value.items.map { it.desertionNo })
    }

    @Test
    fun `마감 임박순은 같은 조건이면 10분 캐시, refresh는 다시 받는다`() = runTest {
        val (repo, harness) = repository { request ->
            val body = if (request.url.parameters["numOfRows"] == "1000") {
                ApiTestResponses.animals("C1")
            } else {
                ApiTestResponses.animals("L1")
            }
            respond(body)
        }
        repo.search(SearchFilter())

        repo.setSort(SortOrder.CLOSING_SOON)
        repo.setSort(SortOrder.LATEST)
        repo.setSort(SortOrder.CLOSING_SOON)
        assertEquals(1, harness.closingSoonRequests())

        repo.refresh()
        assertEquals(2, harness.closingSoonRequests())

        repo.setSort(SortOrder.LATEST)
        clock.advance(11.minutes)
        repo.setSort(SortOrder.CLOSING_SOON)
        assertEquals(3, harness.closingSoonRequests())
        assertEquals(SortOrder.CLOSING_SOON, repo.getSearchResultsStream().value.sort)
    }

    @Test
    fun `마감 임박순이 5000건을 넘으면 기존 목록과 최신순을 두고 안내(D13)`() = runTest {
        val (repo, harness) = repository { request ->
            val body = if (request.url.parameters["numOfRows"] == "1000") {
                ApiTestResponses.animals("C1", totalCount = 7000)
            } else {
                ApiTestResponses.animals("L1", "L2", totalCount = 7000)
            }
            respond(body)
        }
        repo.search(SearchFilter())

        repo.setSort(SortOrder.CLOSING_SOON)

        val results = repo.getSearchResultsStream().value
        assertEquals(SortOrder.LATEST, results.sort)
        assertEquals(listOf("L1", "L2"), results.items.map { it.desertionNo })
        assertEquals(ApiError.TooManyResults(7000), results.error)
        assertEquals(LoadState.IDLE, results.loading)

        // 다음 사용자 동작이 시작되면 안내를 지운다
        repo.refresh()
        assertNull(repo.getSearchResultsStream().value.error)
        assertEquals(1, harness.closingSoonRequests())
    }

    @Test
    fun `마감 임박순에서 조건을 바꾸다 넘치면 최신순 1페이지를 받고 안내를 남긴다`() = runTest {
        val (repo, _) = repository { request ->
            val tooMany = request.url.parameters["upr_cd"] == "big"
            val body = when {
                request.url.parameters["numOfRows"] == "1000" && tooMany ->
                    ApiTestResponses.animals("C", totalCount = 9000)

                request.url.parameters["numOfRows"] == "1000" -> ApiTestResponses.animals("C1")

                else -> ApiTestResponses.animals("B1", "B2", totalCount = 9000)
            }
            respond(body)
        }
        repo.search(SearchFilter(sidoCode = "small"))
        repo.setSort(SortOrder.CLOSING_SOON)

        repo.search(SearchFilter(sidoCode = "big"))

        val results = repo.getSearchResultsStream().value
        assertEquals(SortOrder.LATEST, results.sort)
        assertEquals("big", results.filter.sidoCode)
        assertEquals(listOf("B1", "B2"), results.items.map { it.desertionNo })
        assertEquals(ApiError.TooManyResults(9000), results.error)
    }

    @Test
    fun `오류는 목록을 두고 싣고 다음 동작에서 지운다`() = runTest {
        var fail = false
        val (repo, _) = repository {
            if (fail) {
                respond(
                    "x",
                    HttpStatusCode.InternalServerError
                )
            } else {
                respond(ApiTestResponses.animals("A", totalCount = 90))
            }
        }
        repo.search(SearchFilter())
        fail = true

        repo.refresh()
        val failed = repo.getSearchResultsStream().value
        fail = false
        repo.loadMore()

        assertEquals(ApiError.Http(500), failed.error)
        assertEquals(listOf("A"), failed.items.map { it.desertionNo })
        assertNull(repo.getSearchResultsStream().value.error)
    }

    @Test
    fun `getAnimal은 10분 안의 결과에 있으면 호출하지 않는다`() = runTest {
        val (repo, harness) = repository { request ->
            val body = if (request.url.parameters["desertion_no"] != null) {
                ApiTestResponses.animals(request.url.parameters["desertion_no"]!!)
            } else {
                ApiTestResponses.animals("450650202602738")
            }
            respond(body)
        }
        repo.search(SearchFilter())

        val cached = repo.getAnimal("450650202602738", null)
        assertEquals(1, harness.urls.size)
        clock.advance(11.minutes)
        val fetched = repo.getAnimal("450650202602738", null)

        assertEquals("450650202602738", cached.getOrNull()?.desertionNo)
        assertEquals("450650202602738", fetched.getOrNull()?.desertionNo)
        assertEquals(2, harness.urls.size)
        assertEquals("450650202602738", harness.urls.last().parameters["desertion_no"])
    }

    /** 실제 시간으로 기다린다(앱 범위가 Dispatchers.Default라 가상 시간이 흐르지 않는다). */
    private suspend fun <T> realTimeout(block: suspend () -> T): T =
        withContext(Dispatchers.Default) { withTimeout(10.seconds) { block() } }

    @Test
    fun `이전 작업 취소를 기다리는 동안 부른 쪽이 취소돼도 새 검색은 시작된다(R1)`() = runTest {
        val cancelling = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        val (repo, _) = repository { request ->
            if (request.url.parameters["upr_cd"] == "1") {
                // 취소돼도 바로 끝나지 않는 첫 검색: cancelAndJoin이 gate까지 기다린다
                try {
                    awaitCancellation()
                } finally {
                    withContext(NonCancellable) {
                        cancelling.complete(Unit)
                        gate.await()
                    }
                }
            }
            respond(ApiTestResponses.animals("X${request.url.parameters["upr_cd"]}"))
        }
        launch(Dispatchers.Default) { repo.search(SearchFilter(sidoCode = "1")) }
        realTimeout { repo.getSearchResultsStream().first { it.loading == LoadState.INITIAL } }

        val caller = launch(Dispatchers.Default) { repo.search(SearchFilter(sidoCode = "2")) }
        realTimeout { cancelling.await() } // caller가 이전 작업의 취소를 기다리는 중
        caller.cancelAndJoin()
        gate.complete(Unit)
        val done = realTimeout {
            repo.getSearchResultsStream().first {
                it.filter.sidoCode == "2" && it.fetchedAt != null
            }
        }

        assertEquals(listOf("X2"), done.items.map { it.desertionNo })
        assertEquals(LoadState.IDLE, done.loading)
    }

    @Test
    fun `마감 임박순을 받는 중에 최신순을 고르면 전환을 취소하고 호출하지 않는다(R2)`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val reached = CompletableDeferred<Unit>()
        var closingStarted = 0 // requestHistory는 끝난 요청만 담는다
        val (repo, harness) = repository { request ->
            if (request.url.parameters["numOfRows"] == "1000") {
                closingStarted++
                reached.complete(Unit)
                gate.await()
                respond(ApiTestResponses.animals("C1"))
            } else {
                respond(ApiTestResponses.animals("L1", "L2", totalCount = 90))
            }
        }
        repo.search(SearchFilter())

        val switching = launch(Dispatchers.Default) { repo.setSort(SortOrder.CLOSING_SOON) }
        val during = realTimeout {
            repo.getSearchResultsStream().first { it.loading == LoadState.REFRESH }
        }
        realTimeout { reached.await() } // 마감 임박순 요청이 나간 뒤
        val cancelling = launch(Dispatchers.Default) { repo.setSort(SortOrder.LATEST) }
        realTimeout {
            repo.getSearchResultsStream().first {
                it.pendingSort == null &&
                    it.loading == LoadState.IDLE
            }
        }
        gate.complete(Unit) // 받던 결과가 나중에 와도 반영되지 않는다
        realTimeout {
            switching.join()
            cancelling.join()
        }

        val results = repo.getSearchResultsStream().value
        assertEquals(SortOrder.CLOSING_SOON, during.pendingSort)
        assertEquals(SortOrder.LATEST, results.sort)
        assertEquals(listOf("L1", "L2"), results.items.map { it.desertionNo })
        assertEquals(LoadState.IDLE, results.loading)
        assertNull(results.pendingSort)
        assertEquals(1, harness.pageRequests(1)) // 최신순을 다시 받지 않았다
        assertEquals(1, closingStarted)
    }

    @Test
    fun `전환 중에 같은 정렬을 다시 고르면 무시한다(R2)`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val (repo, harness) = repository { request ->
            if (request.url.parameters["numOfRows"] == "1000") {
                gate.await()
                respond(ApiTestResponses.animals("C1"))
            } else {
                respond(ApiTestResponses.animals("L1"))
            }
        }
        repo.search(SearchFilter())

        val switching = launch(Dispatchers.Default) { repo.setSort(SortOrder.CLOSING_SOON) }
        realTimeout { repo.getSearchResultsStream().first { it.loading == LoadState.REFRESH } }
        repo.setSort(SortOrder.CLOSING_SOON) // 무시
        gate.complete(Unit)
        realTimeout { switching.join() }

        assertEquals(1, harness.closingSoonRequests())
        assertEquals(SortOrder.CLOSING_SOON, repo.getSearchResultsStream().value.sort)
        assertEquals(listOf("C1"), repo.getSearchResultsStream().value.items.map { it.desertionNo })
    }

    @Test
    fun `전환 중에 search하면 목표 정렬로 검색한다(R2)`() = runTest {
        val never = CompletableDeferred<Unit>()
        val reached = CompletableDeferred<Unit>()
        var closingStarted = 0 // requestHistory는 끝난 요청만 담는다
        val (repo, _) = repository { request ->
            val closing = request.url.parameters["numOfRows"] == "1000"
            if (closing) closingStarted++
            if (closing && request.url.parameters["upr_cd"] == null) {
                reached.complete(Unit)
                never.await()
            }
            respond(ApiTestResponses.animals(if (closing) "C" else "L"))
        }
        repo.search(SearchFilter())
        val switching = launch(Dispatchers.Default) { repo.setSort(SortOrder.CLOSING_SOON) }
        realTimeout { reached.await() } // 전환의 마감 임박순 요청이 나간 뒤

        repo.search(SearchFilter(sidoCode = "6110000"))
        realTimeout { switching.join() }

        val results = repo.getSearchResultsStream().value
        assertEquals(SortOrder.CLOSING_SOON, results.sort)
        assertEquals("6110000", results.filter.sidoCode)
        assertEquals(listOf("C"), results.items.map { it.desertionNo })
        assertNull(results.pendingSort)
        assertEquals(2, closingStarted)
    }

    @Test
    fun `같은 오류가 두 번 나도 errorId가 다르다(R3)`() = runTest {
        val (repo, _) = repository { error("연결 실패") }

        repo.search(SearchFilter())
        val first = repo.getSearchResultsStream().value
        repo.refresh()
        val second = repo.getSearchResultsStream().value

        assertEquals(ApiError.Network, first.error)
        assertEquals(ApiError.Network, second.error)
        assertNotEquals(first.errorId, second.errorId)
    }

    // ── 5d: 사망 종료(자연사·안락사) 숨기기 — H2~H12 ──

    private fun dead(no: String) = animalItem(no, processState = "종료(자연사)")

    private fun euthanized(no: String) = animalItem(no, processState = "종료(안락사)")

    private fun page(vararg items: JsonObject, totalCount: Int) =
        ApiTestResponses.envelope(items.toList(), totalCount = totalCount)

    private fun SearchResults.itemNos() = items.map { it.desertionNo }

    private fun SearchResults.hiddenNos() = hidden.map { it.desertionNo }

    @Test
    fun `사망 종료는 items에서 빼 hidden으로, 원본 수는 그대로(5d H2)`() = runTest {
        val (repo, harness) = repository {
            respond(
                page(
                    animalItem("A"),
                    dead("B"),
                    animalItem("C", processState = "종료(입양)"),
                    euthanized("D"),
                    totalCount = 40
                )
            )
        }

        repo.search(SearchFilter())

        val results = repo.getSearchResultsStream().value
        assertEquals(listOf("A", "C"), results.itemNos())
        assertEquals(listOf("B", "D"), results.hiddenNos())
        assertEquals(40, results.totalCount)
        assertTrue(results.hasMore)
        assertEquals(1, harness.urls.size)
    }

    @Test
    fun `다음 쪽의 같은 번호는 items·hidden을 통틀어 한 번(5d H3)`() = runTest {
        val (repo, _) = repository { request ->
            val body = when (request.url.parameters["pageNo"]) {
                "1" -> page(animalItem("A"), dead("B"), totalCount = 90)
                else -> page(dead("B"), animalItem("C"), euthanized("D"), totalCount = 90)
            }
            respond(body)
        }

        repo.search(SearchFilter())
        repo.loadMore()

        val results = repo.getSearchResultsStream().value
        assertEquals(listOf("A", "C"), results.itemNos())
        assertEquals(listOf("B", "D"), results.hiddenNos())
    }

    @Test
    fun `다 숨겨진 첫 쪽은 같은 작업에서 다음 쪽을 이어 받고 결과는 끝에 한 번(5d H4)`() = runTest {
        var duringPage2: SearchResults? = null
        lateinit var repoRef: DefaultAnimalRepository
        val (repo, harness) = repository { request ->
            val body = when (request.url.parameters["pageNo"]) {
                "1" -> page(dead("X1"), euthanized("X2"), dead("X3"), totalCount = 90)

                else -> {
                    duringPage2 = repoRef.getSearchResultsStream().value
                    page(animalItem("E"), dead("F"), totalCount = 90)
                }
            }
            respond(body)
        }
        repoRef = repo

        repo.search(SearchFilter())

        val results = repo.getSearchResultsStream().value
        assertEquals(1, harness.pageRequests(1))
        assertEquals(1, harness.pageRequests(2))
        assertEquals(listOf("E"), results.itemNos())
        assertEquals(listOf("X1", "X2", "X3", "F"), results.hiddenNos())
        assertEquals(2, results.pageNo)
        assertTrue(results.hasMore)
        assertEquals(LoadState.IDLE, results.loading)
        // 1쪽 결과를 중간에 내지 않았다 — 2쪽을 받는 동안 아직 첫 로딩이고 빈 목록
        val during = duringPage2!!
        assertEquals(LoadState.INITIAL, during.loading)
        assertTrue(during.items.isEmpty() && during.hidden.isEmpty())
        assertNull(during.fetchedAt)
    }

    @Test
    fun `이어 받기는 한 동작에 많아야 3쪽, loadMore도 같은 상한(5d H5)`() = runTest {
        val (repo, harness) = repository { request ->
            val n = request.url.parameters["pageNo"]!!
            respond(page(dead("D${n}a"), euthanized("D${n}b"), totalCount = 300))
        }

        repo.search(SearchFilter())
        val first = repo.getSearchResultsStream().value
        assertEquals(3, harness.urls.size)
        assertTrue(first.items.isEmpty())
        assertEquals(
            listOf("D1a", "D1b", "D2a", "D2b", "D3a", "D3b"),
            first.hiddenNos()
        )
        assertTrue(first.hasMore)
        assertEquals(3, first.pageNo)
        assertEquals(3, MAX_PAGES_PER_LOAD)

        repo.loadMore()
        val second = repo.getSearchResultsStream().value
        assertEquals(6, harness.urls.size)
        assertEquals(
            (4..6).map {
                it.toString()
            },
            harness.urls.drop(3).map { it.parameters["pageNo"] }
        )
        assertEquals(6, second.pageNo)
        assertEquals(12, second.hidden.size)
        assertTrue(second.items.isEmpty())
    }

    @Test
    fun `다 숨겨진 쪽이 끝이면 더 받지 않는다(5d H6)`() = runTest {
        val (repo, harness) = repository {
            respond(page(dead("X1"), euthanized("X2"), totalCount = 30))
        }

        repo.search(SearchFilter())

        val results = repo.getSearchResultsStream().value
        assertEquals(1, harness.urls.size)
        assertFalse(results.hasMore)
        assertTrue(results.items.isEmpty())
        assertEquals(listOf("X1", "X2"), results.hiddenNos())
    }

    @Test
    fun `다음 쪽이 다 숨겨지면 그 작업에서 다음 쪽을 이어 받는다(5d H7)`() = runTest {
        var duringPage3: SearchResults? = null
        lateinit var repoRef: DefaultAnimalRepository
        val (repo, harness) = repository { request ->
            val body = when (request.url.parameters["pageNo"]) {
                "1" -> page(animalItem("A"), totalCount = 90)

                "2" -> page(dead("B1"), euthanized("B2"), totalCount = 90)

                else -> {
                    duringPage3 = repoRef.getSearchResultsStream().value
                    page(animalItem("C"), totalCount = 90)
                }
            }
            respond(body)
        }
        repoRef = repo
        repo.search(SearchFilter())

        repo.loadMore()

        val results = repo.getSearchResultsStream().value
        assertEquals(1, harness.pageRequests(2))
        assertEquals(1, harness.pageRequests(3))
        assertEquals(3, harness.urls.size)
        assertEquals(listOf("A", "C"), results.itemNos())
        assertEquals(listOf("B1", "B2"), results.hiddenNos())
        assertEquals(3, results.pageNo)
        assertEquals(LoadState.IDLE, results.loading)
        assertEquals(LoadState.MORE, duringPage3!!.loading)
        assertEquals(listOf("A"), duringPage3!!.itemNos())
    }

    @Test
    fun `이어 받던 뒤 쪽이 실패하면 그때까지의 결과에 오류를 싣는다(5d H8)`() = runTest {
        val (repo, _) = repository { request ->
            if (request.url.parameters["pageNo"] == "1") {
                respond(page(dead("X1"), euthanized("X2"), totalCount = 90))
            } else {
                respond("x", HttpStatusCode.InternalServerError)
            }
        }
        val before = repo.getSearchResultsStream().value.errorId

        repo.search(SearchFilter())

        val results = repo.getSearchResultsStream().value
        assertTrue(results.items.isEmpty())
        assertEquals(listOf("X1", "X2"), results.hiddenNos())
        assertEquals(1, results.pageNo)
        assertTrue(results.hasMore)
        assertEquals(ApiError.Http(500), results.error)
        assertEquals(before + 1, results.errorId)
        assertEquals(LoadState.IDLE, results.loading)
    }

    @Test
    fun `다 숨겨진 쪽은 빈 쪽이 아니라 그 쪽의 totalCount를 쓴다(5d H9)`() = runTest {
        val (repo, _) = repository { request ->
            val body = when (request.url.parameters["pageNo"]) {
                "1" -> page(animalItem("A"), totalCount = 90)
                "2" -> page(dead("B"), totalCount = 70)
                else -> ApiTestResponses.empty // 원본이 빈 끝 쪽(totalCount 0)은 앞의 수를 둔다
            }
            respond(body)
        }
        repo.search(SearchFilter())

        repo.loadMore()

        val results = repo.getSearchResultsStream().value
        assertEquals(listOf("A"), results.itemNos())
        assertEquals(listOf("B"), results.hiddenNos())
        assertEquals(70, results.totalCount)
        assertFalse(results.hasMore)
        assertEquals(3, results.pageNo)
    }

    @Test
    fun `getAnimal은 10분 안이면 hidden에서도 찾아 그대로 돌려준다(5d H10)`() = runTest {
        val (repo, harness) = repository { request ->
            val no = request.url.parameters["desertion_no"]
            respond(
                if (no != null) {
                    page(dead(no), totalCount = 1)
                } else {
                    page(animalItem("A"), dead("B"), totalCount = 2)
                }
            )
        }
        repo.search(SearchFilter())
        assertEquals(listOf("B"), repo.getSearchResultsStream().value.hiddenNos()) // items에는 없다

        val cached = repo.getAnimal("B", null).getOrNull()
        assertEquals(1, harness.urls.size)
        assertEquals("B", cached?.desertionNo)
        assertEquals("종료(자연사)", cached?.processState)

        clock.advance(11.minutes)
        val fetched = repo.getAnimal("B", null).getOrNull()
        assertEquals(2, harness.urls.size)
        assertEquals("B", fetched?.desertionNo) // 사망 종료도 그대로 — 숨기는 것은 부르는 쪽
    }

    @Test
    fun `새로고침·최신순 전환·5000건 초과 되돌림 결과에도 사망 종료가 없다(5d H11)`() = runTest {
        var tooMany = false
        val (repo, _) = repository { request ->
            val body = if (request.url.parameters["numOfRows"] == "1000") {
                if (tooMany) {
                    page(animalItem("C1"), totalCount = 7000)
                } else {
                    page(animalItem("C1"), totalCount = 1)
                }
            } else {
                page(animalItem("A"), dead("B"), euthanized("C"), totalCount = 90)
            }
            respond(body)
        }
        repo.search(SearchFilter())

        repo.refresh()
        val refreshed = repo.getSearchResultsStream().value
        repo.setSort(SortOrder.CLOSING_SOON)
        repo.setSort(SortOrder.LATEST)
        val switched = repo.getSearchResultsStream().value
        tooMany = true
        repo.setSort(SortOrder.CLOSING_SOON)
        repo.search(SearchFilter(sidoCode = "6110000")) // 마감 임박순 검색이 넘쳐 최신순으로 되돌린다
        val fallback = repo.getSearchResultsStream().value

        for (r in listOf(refreshed, switched, fallback)) {
            assertEquals(SortOrder.LATEST, r.sort)
            assertEquals(listOf("A"), r.itemNos())
            assertEquals(listOf("B", "C"), r.hiddenNos())
        }
        assertEquals(ApiError.TooManyResults(7000), fallback.error)
    }

    @Test
    fun `마감 임박순도 사망 종료를 빼고 totalCount는 보이는 수(5d H12)`() = runTest {
        val (repo, _) = repository { request ->
            val body = if (request.url.parameters["numOfRows"] == "1000") {
                page(
                    animalItem("C1", noticeEdt = "20261106"),
                    dead("C2"),
                    animalItem("C3", noticeEdt = "20261105"),
                    totalCount = 3
                )
            } else {
                page(animalItem("L1"), totalCount = 1)
            }
            respond(body)
        }
        repo.search(SearchFilter())

        repo.setSort(SortOrder.CLOSING_SOON)

        val results = repo.getSearchResultsStream().value
        assertEquals(SortOrder.CLOSING_SOON, results.sort)
        assertEquals(listOf("C3", "C1"), results.itemNos())
        assertEquals(listOf("C2"), results.hiddenNos())
        assertEquals(2, results.totalCount)
        // 10분 캐시에서 다시 꺼내도 나눈 그대로
        repo.setSort(SortOrder.LATEST)
        repo.setSort(SortOrder.CLOSING_SOON)
        assertEquals(listOf("C3", "C1"), repo.getSearchResultsStream().value.itemNos())
        assertEquals(listOf("C2"), repo.getSearchResultsStream().value.hiddenNos())
    }

    @Test
    fun `필터 켬이면 사망 종료도 items, 이어 받기 없음 — 상태 공고중이면 효과 없음(5d H13)`() = runTest {
        val (repo, harness) = repository { request ->
            val body = when {
                request.url.parameters["upr_cd"] == "allDead" ->
                    page(dead("X1"), euthanized("X2"), totalCount = 90)

                else -> page(
                    animalItem("A"),
                    dead("B"),
                    animalItem("C", processState = "종료(입양)"),
                    euthanized("D"),
                    totalCount = 40
                )
            }
            respond(body)
        }
        val on = SearchFilter(includeEndedByDeath = true)

        repo.search(on)
        val shown = repo.getSearchResultsStream().value
        assertEquals(listOf("A", "B", "C", "D"), shown.itemNos())
        assertTrue(shown.hidden.isEmpty())

        repo.search(on.copy(sidoCode = "allDead"))
        val allDead = repo.getSearchResultsStream().value
        assertEquals(listOf("X1", "X2"), allDead.itemNos())
        assertEquals(1, harness.urls.count { it.parameters["upr_cd"] == "allDead" }) // 이어 받기 없음

        // 상태 공고중이면 칸 값은 남지만 뜻이 없다 — 섞여 온 사망 종료는 숨긴다
        repo.search(on.copy(state = NoticeState.NOTICE))
        val notice = repo.getSearchResultsStream().value
        assertTrue(notice.filter.includeEndedByDeath)
        assertEquals(listOf("A", "C"), notice.itemNos())
        assertEquals(listOf("B", "D"), notice.hiddenNos())
    }
}
