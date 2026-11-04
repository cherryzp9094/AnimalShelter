package com.cherryzp.animalshelter.data.repository

import com.cherryzp.animalshelter.data.api.ApiTestHarness
import com.cherryzp.animalshelter.data.api.fixture.ApiFixtures
import com.cherryzp.animalshelter.data.store.ShelterStore
import com.cherryzp.animalshelter.platform.createJsonStorage
import com.cherryzp.animalshelter.platform.storageTestModule
import com.cherryzp.animalshelter.testing.FakeClock
import com.cherryzp.animalshelter.testing.FakeFailingStorage
import com.cherryzp.animalshelter.testing.withTestStores
import io.ktor.client.engine.mock.respond
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

/** 보호소 전체 캐시와 등록번호 조회(02-data 9절). */
class ShelterRepositoryTest {
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

    @Test
    fun `전체를 한 번 받아 캐시하고 등록번호로 찾는다`() = runTest {
        withTestStores { stores ->
            val harness = ApiTestHarness(clock = clock) { respond(ApiFixtures.shelterInfoV2) }
            val repo = DefaultShelterRepository(harness.api, stores.shelters(), clock, appScope)

            val found = repo.getShelterInfoStream("348535202400001").first { it.fresh }
            val other = repo.getShelterInfoStream("326331201100001").first()
            val missing = repo.getShelterInfoStream("999999999999999").first()

            assertEquals("(사)동물보호관리협회", found.data?.name)
            assertEquals("(사)동부동물보호협회", other.data?.name)
            assertNull(missing.data)
            assertEquals(true, missing.fresh) // 받은 뒤라 "없음"
            assertEquals(1, harness.urls.size)
            assertEquals("1000", harness.urls.single().parameters["numOfRows"])
        }
    }

    @Test
    fun `만료되면 캐시를 먼저 내보내고 다시 받는다`() = runTest {
        withTestStores { stores ->
            val harness = ApiTestHarness(clock = clock) { respond(ApiFixtures.shelterInfoV2) }
            val repo = DefaultShelterRepository(harness.api, stores.shelters(), clock, appScope)
            repo.getShelterInfoStream("348535202400001").first { it.fresh }

            clock.advance(8.days)
            val stale = repo.getShelterInfoStream("348535202400001").first()
            val fresh = repo.getShelterInfoStream("348535202400001").first { it.fresh }

            assertEquals(false, stale.fresh)
            assertEquals("(사)동물보호관리협회", stale.data?.name)
            assertEquals("(사)동물보호관리협회", fresh.data?.name)
            assertEquals(2, harness.urls.size)
        }
    }

    @Test
    fun `스트림 경로의 갱신은 사본이 신선하면 받지 않는다`() = runTest {
        withTestStores { stores ->
            val harness = ApiTestHarness(clock = clock) { respond(ApiFixtures.shelterInfoV2) }
            val repo = DefaultShelterRepository(harness.api, stores.shelters(), clock, appScope)
            repo.refresh() // 끝까지 기다린다(진행 중인 갱신에 합쳐지지 않게)

            repo.refreshIfStale()

            assertEquals(1, harness.urls.size)
        }
    }

    @Test
    fun `스트림 경로의 갱신은 사본이 만료면 받는다`() = runTest {
        withTestStores { stores ->
            val harness = ApiTestHarness(clock = clock) { respond(ApiFixtures.shelterInfoV2) }
            val repo = DefaultShelterRepository(harness.api, stores.shelters(), clock, appScope)
            repo.refresh() // 끝까지 기다린다(진행 중인 갱신에 합쳐지지 않게)
            clock.advance(8.days)

            repo.refreshIfStale()

            assertEquals(2, harness.urls.size)
            assertEquals(true, repo.getShelterInfoStream("348535202400001").first().fresh)
        }
    }

    @Test
    fun `refresh는 사본이 신선해도 받는다`() = runTest {
        withTestStores { stores ->
            val harness = ApiTestHarness(clock = clock) { respond(ApiFixtures.shelterInfoV2) }
            val repo = DefaultShelterRepository(harness.api, stores.shelters(), clock, appScope)
            repo.refresh() // 끝까지 기다린다(진행 중인 갱신에 합쳐지지 않게)

            repo.refresh()

            assertEquals(2, harness.urls.size)
        }
    }

    @Test
    fun `캐시 쓰기가 실패해도 받은 보호소 정보를 내보낸다`() = runTest {
        withTestStores { stores ->
            val harness = ApiTestHarness(clock = clock) { respond(ApiFixtures.shelterInfoV2) }
            val store = ShelterStore(
                FakeFailingStorage(
                    createJsonStorage(stores.name("shelters.json"), ShelterStore.serializer)
                ),
                stores.scope
            )
            val repo = DefaultShelterRepository(harness.api, store, clock, appScope)

            // 실제 시간으로 기다린다 — 고치기 전에는 진행 표시에서 멈춘다
            val found = withContext(Dispatchers.Default) {
                withTimeout(10.seconds) {
                    repo.getShelterInfoStream("348535202400001").first {
                        it.data != null || it.fresh
                    }
                }
            }
            val other = repo.getShelterInfoStream("326331201100001").first()

            assertEquals("(사)동물보호관리협회", found.data?.name)
            assertEquals(true, found.fresh)
            assertNull(found.error)
            assertEquals("(사)동부동물보호협회", other.data?.name)
            assertEquals(1, harness.urls.size) // 같은 실행 안에서는 메모리 사본이 신선하다
        }
    }
}
