package com.cherryzp.animalshelter.data.repository

import com.cherryzp.animalshelter.data.api.ApiError
import com.cherryzp.animalshelter.data.api.ApiTestHarness
import com.cherryzp.animalshelter.data.api.fixture.ApiFixtures
import com.cherryzp.animalshelter.data.store.CodeTableStore
import com.cherryzp.animalshelter.model.AnimalKind
import com.cherryzp.animalshelter.model.Kind
import com.cherryzp.animalshelter.model.Sido
import com.cherryzp.animalshelter.platform.createJsonStorage
import com.cherryzp.animalshelter.platform.storageTestModule
import com.cherryzp.animalshelter.testing.ApiTestResponses
import com.cherryzp.animalshelter.testing.FakeClock
import com.cherryzp.animalshelter.testing.FakeFailingStorage
import com.cherryzp.animalshelter.testing.TestStores
import com.cherryzp.animalshelter.testing.withTestStores
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

/** 코드표 캐시 규칙(02-data 8절): 7일, stale-while-revalidate, 오류·0건 비저장, 동시 갱신 하나. */
class CodeTableRepositoryTest {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val clock = FakeClock()

    @BeforeTest
    fun setUp() {
        startKoin { modules(storageTestModule()) }
    }

    @AfterTest
    fun tearDown() {
        appScope.cancel()
        stopKoin()
    }

    private fun repository(
        stores: TestStores,
        handler: MockRequestHandler,
    ): Pair<DefaultCodeTableRepository, ApiTestHarness> = repository(stores.codeTables(), handler)

    private fun repository(
        store: CodeTableStore,
        handler: MockRequestHandler,
    ): Pair<DefaultCodeTableRepository, ApiTestHarness> {
        val harness = ApiTestHarness(clock = clock, handler = handler)
        return DefaultCodeTableRepository(harness.api, store, clock, appScope) to harness
    }

    /** 쓰기가(그리고 [failReads]면 읽기도) 늘 실패하는 코드표 저장소(5a-fixes F1). */
    private fun failingStore(
        stores: TestStores,
        failReads: Boolean = false,
    ): Pair<CodeTableStore, FakeFailingStorage<*>> {
        val storage = FakeFailingStorage(
            createJsonStorage(stores.name("code_tables.json"), CodeTableStore.serializer),
            failReads
        )
        return CodeTableStore(storage, stores.scope) to storage
    }

    private suspend fun <T> Flow<CacheState<T>>.settled(): CacheState<T> =
        first { it.data != null || it.error != null }

    /** 실제 시간으로 기다린다 — 고치기 전에는 진행 표시에서 멈춰 끝없이 기다리므로 시간 제한을 둔다. */
    private suspend fun <T> Flow<CacheState<T>>.settledWithin(): CacheState<T> =
        withContext(Dispatchers.Default) { withTimeout(10.seconds) { settled() } }

    @Test
    fun `처음에는 로딩 상태 뒤 받은 표를 내보내고 저장한다`() = runTest {
        withTestStores { stores ->
            val gate = CompletableDeferred<Unit>()
            val (repo, harness) = repository(stores) {
                gate.await()
                respond(ApiFixtures.sidoV2)
            }

            val loading = repo.getSidoStream().first() // 응답을 막아 둔 동안
            gate.complete(Unit)
            val loaded = repo.getSidoStream().settled()
            val again = repo.getSidoStream().first()

            assertEquals(CacheState<List<Sido>>(null, fresh = false, error = null), loading)
            assertEquals(16, loaded.data?.size)
            assertEquals(true, loaded.fresh)
            assertEquals(16, again.data?.size) // 신선한 캐시: 다시 부르지 않는다
            assertEquals(1, harness.urls.size)
        }
    }

    @Test
    fun `만료된 캐시는 먼저 내보내고 뒤에서 갱신한다`() = runTest {
        withTestStores { stores ->
            var gate = CompletableDeferred(Unit)
            val (repo, harness) = repository(stores) {
                gate.await()
                respond(ApiFixtures.sidoV2)
            }
            repo.getSidoStream().settled()

            clock.advance(8.days)
            gate = CompletableDeferred()
            val stale = repo.getSidoStream().first()
            gate.complete(Unit)
            val fresh = repo.getSidoStream().first { it.fresh }

            assertEquals(16, stale.data?.size)
            assertEquals(false, stale.fresh)
            assertEquals(16, fresh.data?.size)
            assertEquals(2, harness.urls.size)
        }
    }

    @Test
    fun `오류 응답은 저장하지 않고 상태에 싣는다`() = runTest {
        withTestStores { stores ->
            var fail = true
            val (repo, _) = repository(stores) {
                if (fail) {
                    respond(
                        "x",
                        HttpStatusCode.ServiceUnavailable
                    )
                } else {
                    respond(ApiFixtures.kindV2Dog)
                }
            }

            val failed = repo.getKindsStream(AnimalKind.DOG).settled()
            assertEquals(
                CacheState<List<Kind>>(null, fresh = false, error = ApiError.Http(503)),
                failed
            )
            assertNull(stores.codeTables("unused.json").data.first().kinds[AnimalKind.DOG])

            fail = false
            repo.refresh(CodeTable.Kinds(AnimalKind.DOG))
            val ok = repo.getKindsStream(AnimalKind.DOG).first()
            assertEquals(10, ok.data?.size)
            assertNull(ok.error)
        }
    }

    @Test
    fun `갱신이 실패해도 캐시는 그대로 두고 오류만 싣는다`() = runTest {
        withTestStores { stores ->
            var fail = false
            val (repo, _) = repository(stores) {
                if (fail) {
                    respond(
                        "x",
                        HttpStatusCode.InternalServerError
                    )
                } else {
                    respond(ApiFixtures.sigunguV2Seoul)
                }
            }
            repo.getSigunguStream("6110000").settled()
            fail = true

            repo.refresh(CodeTable.Sigungu("6110000"))
            val state = repo.getSigunguStream("6110000").first { it.error != null }

            assertEquals(10, state.data?.size)
            assertEquals(ApiError.Http(500), state.error)
        }
    }

    @Test
    fun `0건 응답은 저장하지 않고 빈 목록으로 내보낸다`() = runTest {
        withTestStores { stores ->
            val (repo, harness) = repository(stores) { respond(ApiTestResponses.empty) }

            val state = repo.getSheltersStream("6110000", "3220000").settled()
            repo.getSheltersStream("6110000", "3220000").settled()

            assertEquals(CacheState(emptyList(), fresh = true, error = null), state)
            assertEquals(2, harness.urls.size) // 저장하지 않았으니 다시 부른다
            assertEquals("3220000", harness.urls.first().parameters["org_cd"])
        }
    }

    @Test
    fun `같은 표의 갱신은 동시에 하나만`() = runTest {
        withTestStores { stores ->
            val gate = CompletableDeferred<Unit>()
            val (repo, harness) = repository(stores) {
                gate.await()
                respond(ApiFixtures.sidoV2)
            }

            val a = async(Dispatchers.Default) { repo.getSidoStream().settled() }
            val b = async(Dispatchers.Default) { repo.getSidoStream().settled() }
            val c = async(Dispatchers.Default) { repo.refresh(CodeTable.Sido) }
            repo.getSidoStream().first() // 셋 다 시작할 시간
            gate.complete(Unit)

            assertEquals(16, a.await().data?.size)
            assertEquals(16, b.await().data?.size)
            c.await()
            assertEquals(1, harness.urls.size)
        }
    }

    @Test
    fun `스트림 경로의 갱신은 사본이 신선하면 받지 않는다`() = runTest {
        withTestStores { stores ->
            val (repo, harness) = repository(stores) { respond(ApiFixtures.sidoV2) }
            repo.refresh(CodeTable.Sido) // 끝까지 기다린다(진행 중인 갱신에 합쳐지지 않게)

            repo.refreshIfStale(CodeTable.Sido)

            assertEquals(1, harness.urls.size)
            assertEquals(true, repo.getSidoStream().first().fresh)
        }
    }

    @Test
    fun `스트림 경로의 갱신은 사본이 만료면 받는다`() = runTest {
        withTestStores { stores ->
            val (repo, harness) = repository(stores) { respond(ApiFixtures.sidoV2) }
            repo.refresh(CodeTable.Sido) // 끝까지 기다린다(진행 중인 갱신에 합쳐지지 않게)
            clock.advance(8.days)

            repo.refreshIfStale(CodeTable.Sido)

            assertEquals(2, harness.urls.size)
            assertEquals(true, repo.getSidoStream().first().fresh)
        }
    }

    @Test
    fun `refresh는 사본이 신선해도 받는다`() = runTest {
        withTestStores { stores ->
            val (repo, harness) = repository(stores) { respond(ApiFixtures.sidoV2) }
            repo.refresh(CodeTable.Sido) // 끝까지 기다린다(진행 중인 갱신에 합쳐지지 않게)

            repo.refresh(CodeTable.Sido)

            assertEquals(2, harness.urls.size)
        }
    }

    @Test
    fun `캐시 쓰기가 실패해도 받은 시도 목록을 내보낸다`() = runTest {
        withTestStores { stores ->
            val (store, storage) = failingStore(stores)
            val (repo, _) = repository(store) { respond(ApiFixtures.sidoV2) }

            val state = repo.getSidoStream().settledWithin()

            assertEquals(16, state.data?.size)
            assertEquals(true, state.fresh)
            assertNull(state.error)
            assertTrue(storage.failedWrites > 0) // 파일에 쓰려고는 했다
        }
    }

    @Test
    fun `캐시 쓰기가 실패해도 같은 실행 안에서는 다시 받지 않는다`() = runTest {
        withTestStores { stores ->
            val (store, _) = failingStore(stores)
            val (repo, harness) = repository(store) { respond(ApiFixtures.sidoV2) }
            repo.getSidoStream().settledWithin()

            val again = repo.getSidoStream().settledWithin()
            repo.refreshIfStale(CodeTable.Sido)

            assertEquals(16, again.data?.size)
            assertEquals(true, again.fresh)
            assertEquals(1, harness.urls.size)
        }
    }

    @Test
    fun `저장소를 읽지도 쓰지도 못해도 받은 시도 목록을 내보낸다`() = runTest {
        withTestStores { stores ->
            val (store, _) = failingStore(stores, failReads = true)
            val (repo, harness) = repository(store) { respond(ApiFixtures.sidoV2) }

            val state = repo.getSidoStream().settledWithin()
            val again = repo.getSidoStream().settledWithin()

            assertEquals(16, state.data?.size)
            assertEquals(true, state.fresh)
            assertNull(state.error)
            assertEquals(16, again.data?.size)
            assertEquals(1, harness.urls.size)
        }
    }

    @Test
    fun `캐시 쓰기가 실패해도 표를 하나씩 더해 모두 남긴다`() = runTest {
        withTestStores { stores ->
            val (store, _) = failingStore(stores)
            val (repo, _) = repository(store) { request ->
                when {
                    request.url.encodedPath.endsWith("sido_v2") -> respond(ApiFixtures.sidoV2)
                    else -> respond(ApiFixtures.sigunguV2Seoul)
                }
            }

            repo.getSidoStream().settledWithin()
            repo.getSigunguStream("6110000").settledWithin()

            assertEquals(16, repo.getSidoStream().first().data?.size)
            assertEquals(10, repo.getSigunguStream("6110000").first().data?.size)
        }
    }
}
