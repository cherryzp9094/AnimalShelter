package com.cherryzp.animalshelter.platform

import com.cherryzp.animalshelter.data.api.ApiTestHarness
import com.cherryzp.animalshelter.data.api.fixture.ApiFixtures
import com.cherryzp.animalshelter.data.repository.DefaultBookmarkRepository
import com.cherryzp.animalshelter.data.repository.DefaultCodeTableRepository
import com.cherryzp.animalshelter.model.Animal
import com.cherryzp.animalshelter.testing.FakeClock
import com.cherryzp.animalshelter.testing.withTestStores
import io.ktor.client.engine.mock.respond
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okio.IOException

/**
 * `createWritable()`이 없는 옛 Safari(18.x) 흉내(5a-fixes2 H1). 읽기는 되고 쓰기만 JS에서 동기로
 * `TypeError`를 던진다 — Kotlin/Wasm에서는 `Exception`이 아닌 `Throwable`이라, 저장소 actual이
 * `IOException`으로 바꾸지 않으면 공통 코드의 `catch (e: Exception)`을 모두 지나친다.
 */
class OpfsJsExceptionTest {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val clock = FakeClock()

    @BeforeTest
    fun setUp() {
        removeCreateWritable()
    }

    @AfterTest
    fun tearDown() {
        restoreCreateWritable()
        appScope.cancel()
    }

    @Test
    fun `createWritable이 없어도 받은 시도 목록을 낸다`() = runTest {
        withTestStores { stores ->
            val harness = ApiTestHarness(clock = clock) { respond(ApiFixtures.sidoV2) }
            val repo = DefaultCodeTableRepository(harness.api, stores.codeTables(), clock, appScope)

            val state = withContext(Dispatchers.Default) {
                withTimeout(10.seconds) {
                    repo.getSidoStream().first { it.data != null || it.error != null }
                }
            }

            assertEquals(16, state.data?.size)
            assertTrue(state.fresh)
            assertNull(state.error)
        }
    }

    @Test
    fun `createWritable이 없으면 북마크 추가는 IOException이고 목록은 그대로`() = runTest {
        withTestStores { stores ->
            val repo = DefaultBookmarkRepository(stores.bookmarks(), clock)
            assertEquals(emptyList(), repo.getBookmarksStream().first())

            assertFailsWith<IOException> { repo.add(Animal("A")) }

            assertEquals(emptyList(), repo.getBookmarksStream().first())
            assertFalse(repo.isBookmarkedStream("A").first())
        }
    }
}

/** 원래 메서드를 전역에 맡겨 두고 `undefined`로 바꾼다. Karma는 한 페이지에서 차례로 돌아 꼭 되돌린다. */
private fun removeCreateWritable(): Unit = js(
    """{
        globalThis.__asSavedCreateWritable = FileSystemFileHandle.prototype.createWritable;
        FileSystemFileHandle.prototype.createWritable = undefined;
    }"""
)

private fun restoreCreateWritable(): Unit = js(
    """{
        if (globalThis.__asSavedCreateWritable !== undefined) {
            FileSystemFileHandle.prototype.createWritable = globalThis.__asSavedCreateWritable;
            globalThis.__asSavedCreateWritable = undefined;
        }
    }"""
)
