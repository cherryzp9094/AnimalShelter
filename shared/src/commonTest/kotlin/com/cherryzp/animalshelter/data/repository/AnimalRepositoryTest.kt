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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

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
}
