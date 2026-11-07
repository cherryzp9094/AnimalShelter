package com.cherryzp.animalshelter.ui.bookmarks

import androidx.lifecycle.ViewModel
import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.data.api.EndpointSelector
import com.cherryzp.animalshelter.data.repository.SearchResults
import com.cherryzp.animalshelter.di.AppConfig
import com.cherryzp.animalshelter.fake.FakeAnimalRepository
import com.cherryzp.animalshelter.fake.FakeBookmarkRepository
import com.cherryzp.animalshelter.fake.FakeSettingsRepository
import com.cherryzp.animalshelter.fake.testAnimal
import com.cherryzp.animalshelter.model.ListLayout
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.bookmark_removed
import com.cherryzp.animalshelter.testing.FakeClock
import com.cherryzp.animalshelter.testing.content
import com.cherryzp.animalshelter.testing.runMainTest
import com.cherryzp.animalshelter.ui.common.MessageAction
import com.cherryzp.animalshelter.ui.common.StatusKind
import com.cherryzp.animalshelter.ui.common.UiMessage
import com.cherryzp.animalshelter.ui.common.uiText
import com.cherryzp.animalshelter.ui.list.AnimalListViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.datetime.LocalDate

/** 북마크 목록 ViewModel(브리핑 B8·B10·B11). */
class BookmarkListViewModelTest {
    private val clock = FakeClock()
    private val created = mutableListOf<ViewModel>()
    private val config = AppConfig(ApiEndpoint.Fixture, isDebug = false, versionName = "test")

    private fun runVmTest(block: suspend TestScope.() -> Unit) = runMainTest({ created }, block)

    private inner class Env(val bookmarks: FakeBookmarkRepository = FakeBookmarkRepository()) {
        val settings = FakeSettingsRepository()
        val vm = BookmarkListViewModel(
            bookmarks = bookmarks,
            settings = settings,
            endpoints = EndpointSelector(config) { null },
            clock = clock
        ).also { created += it }
        val state get() = vm.uiState.value
    }

    private fun TestScope.start(env: Env): Env {
        backgroundScope.launch { env.vm.uiState.collect {} }
        advanceUntilIdle()
        return env
    }

    @Test
    fun `추가 역순, 배지는 오늘 기준으로 다시 판정`() = runVmTest {
        var now = clock.now()
        val bookmarks = FakeBookmarkRepository { now.also { now += 1.minutes } }
        // 저장할 때는 공고중이었지만 오늘(11-04) 기준으로는 공고 기간이 지났다 → 보호중
        bookmarks.add(testAnimal(desertionNo = "A", noticeEnd = LocalDate(2026, 11, 1)))
        bookmarks.add(testAnimal(desertionNo = "B"))
        val env = start(Env(bookmarks))

        assertEquals(listOf("B", "A"), env.state.items.map { it.desertionNo })
        assertEquals(StatusKind.PROTECTING, env.state.items.last().status.kind)
        assertTrue(env.state.items.all { it.isBookmarked })
        assertFalse(env.state.isEmpty)
    }

    @Test
    fun `비면 빈 상태`() = runVmTest {
        val env = start(Env())
        assertTrue(env.state.isEmpty)
        assertTrue(env.state.items.isEmpty())
    }

    @Test
    fun `카드의 북마크는 바로 지우고 되돌리기로 다시 추가`() = runVmTest {
        val bookmarks = FakeBookmarkRepository()
        val animal = testAnimal(desertionNo = "A")
        bookmarks.add(animal)
        val env = start(Env(bookmarks))

        env.vm.onToggleBookmark("A")
        advanceUntilIdle()
        assertTrue(env.state.isEmpty)
        assertEquals(
            UiMessage(uiText(Res.string.bookmark_removed), MessageAction.UNDO),
            env.state.message.content()
        )

        env.vm.onMessageAction(MessageAction.UNDO, env.state.message!!.id)
        advanceUntilIdle()
        assertNull(env.state.message)
        assertEquals(listOf("A"), env.state.items.map { it.desertionNo })
        assertEquals(animal, bookmarks.restoreCalls.last().first.animal)
        assertEquals("A", env.state.restoredKey)
        env.vm.onRestoreShown()
        advanceUntilIdle()
        assertNull(env.state.restoredKey)
    }

    @Test
    fun `가운데 것을 지우고 되돌리면 같은 자리·같은 저장 시각(F4)`() = runVmTest {
        var now = clock.now()
        val bookmarks = FakeBookmarkRepository { now.also { now += 1.minutes } }
        listOf("C", "B", "A").forEach { bookmarks.add(testAnimal(desertionNo = it)) }
        val savedAt = bookmarks.bookmarks.value.associate { it.animal.desertionNo to it.savedAt }
        val env = start(Env(bookmarks))
        assertEquals(listOf("A", "B", "C"), env.state.items.map { it.desertionNo })

        env.vm.onToggleBookmark("B")
        advanceUntilIdle()
        assertEquals(listOf("A", "C"), env.state.items.map { it.desertionNo })
        env.vm.onMessageAction(MessageAction.UNDO, env.state.message!!.id)
        advanceUntilIdle()

        assertEquals(listOf("A", "B", "C"), env.state.items.map { it.desertionNo })
        assertEquals(
            savedAt,
            bookmarks.bookmarks.value.associate {
                it.animal.desertionNo to
                    it.savedAt
            }
        )
        assertEquals(3, bookmarks.addCalls.size) // 되돌리기는 add가 아니다
    }

    @Test
    fun `둘을 연달아 지우면 두 번째 것만 돌아온다(F4)`() = runVmTest {
        val bookmarks = FakeBookmarkRepository()
        listOf("C", "B", "A").forEach { bookmarks.add(testAnimal(desertionNo = it)) }
        val env = start(Env(bookmarks))

        env.vm.onToggleBookmark("A")
        advanceUntilIdle()
        val first = env.state.message!!
        env.vm.onToggleBookmark("C")
        advanceUntilIdle()
        // 사라진 첫 안내의 버튼은 받지 않는다
        env.vm.onMessageAction(MessageAction.UNDO, first.id)
        advanceUntilIdle()
        assertEquals(listOf("B"), env.state.items.map { it.desertionNo })

        env.vm.onMessageAction(MessageAction.UNDO, env.state.message!!.id)
        advanceUntilIdle()
        assertEquals(listOf("B", "C"), env.state.items.map { it.desertionNo })
    }

    @Test
    fun `되돌리기 전에 다시 추가된 번호면 그대로(F4)`() = runVmTest {
        val bookmarks = FakeBookmarkRepository()
        listOf("B", "A").forEach { bookmarks.add(testAnimal(desertionNo = it)) }
        val env = start(Env(bookmarks))

        env.vm.onToggleBookmark("B")
        advanceUntilIdle()
        bookmarks.add(testAnimal(desertionNo = "B", kindName = "다시 추가"))
        env.vm.onMessageAction(MessageAction.UNDO, env.state.message!!.id)
        advanceUntilIdle()

        assertEquals(listOf("B", "A"), env.state.items.map { it.desertionNo })
        assertEquals("다시 추가", bookmarks.bookmarks.value.first().animal.kindName)
    }

    @Test
    fun `되살린 카드가 숨었을 때만 스크롤 위치(F4)`() {
        val keys = listOf("A", "B", "C", "D", "E")
        // 1열: 첫 카드를 되살렸는데 위 여백에 걸쳐 위가 잘렸다(실측 -515px) → 0으로
        assertEquals(0, restoreScrollTarget(keys, "A", mapOf("A" to -515, "B" to 8, "C" to 531)))
        // 위가 보이면 그대로
        assertNull(restoreScrollTarget(keys, "C", mapOf("B" to 0, "C" to 0, "D" to 300)))
        // 보이는 목록에 없으면(위·아래로 벗어남, 배치 전) 그 자리로
        assertEquals(4, restoreScrollTarget(keys, "E", mapOf("A" to 0, "B" to 0)))
        assertEquals(1, restoreScrollTarget(keys, "B", emptyMap()))
        // 목록에 아직 없으면 null
        assertNull(restoreScrollTarget(keys, "Z", mapOf("A" to 0)))
    }

    @Test
    fun `둘을 연달아 지우면 두 번째 안내는 새 id(F3)`() = runVmTest {
        val bookmarks = FakeBookmarkRepository()
        bookmarks.add(testAnimal(desertionNo = "A"))
        bookmarks.add(testAnimal(desertionNo = "B"))
        val env = start(Env(bookmarks))

        env.vm.onToggleBookmark("A")
        advanceUntilIdle()
        val first = env.state.message!!
        env.vm.onToggleBookmark("B")
        advanceUntilIdle()
        val second = env.state.message!!
        assertEquals(first.content(), second.content())
        assertNotEquals(first.id, second.id)

        // 첫 스낵바가 늦게 닫혀도 두 번째 안내는 남는다
        env.vm.onMessageShown(first.id)
        advanceUntilIdle()
        assertEquals(second, env.state.message)
    }

    @Test
    fun `보기 전환은 목록과 같은 설정값`() = runVmTest {
        val env = start(Env())
        env.vm.onToggleLayout()
        advanceUntilIdle()
        assertEquals(ListLayout.LARGE, env.settings.settings.value.listLayout)
        assertEquals(ListLayout.LARGE, env.state.layout)
    }

    @Test
    fun `복구 안내는 한 번, 보인 뒤 끈다`() = runVmTest {
        val bookmarks = FakeBookmarkRepository().apply { recovered.value = true }
        val env = start(Env(bookmarks))
        assertEquals(RecoveryNotice, env.state.message)

        env.vm.onMessageShown(RecoveryNotice.id)
        advanceUntilIdle()
        assertFalse(bookmarks.recovered.value)
        assertNull(env.state.message)
    }

    @Test
    fun `목록 화면도 복구 안내를 한 번 낸다`() = runVmTest {
        val bookmarks = FakeBookmarkRepository().apply { recovered.value = true }
        val settings = FakeSettingsRepository()
        val vm = AnimalListViewModel(
            animals = FakeAnimalRepository(SearchResults(items = listOf(testAnimal()))),
            bookmarks = bookmarks,
            settings = settings,
            endpoints = EndpointSelector(config) { null },
            clock = clock,
            config = config
        ).also { created += it }
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(RecoveryNotice, vm.uiState.value.message)

        vm.onMessageShown(RecoveryNotice.id)
        advanceUntilIdle()
        assertFalse(bookmarks.recovered.value)
        assertNull(vm.uiState.value.message)
    }

    @Test
    fun `가짜 북마크도 이미 있는 번호의 접수일을 지킨다(R8)`() = runVmTest {
        val bookmarks = FakeBookmarkRepository()
        bookmarks.add(testAnimal(desertionNo = "A", happenDate = LocalDate(2026, 11, 3)))
        bookmarks.add(testAnimal(desertionNo = "A", happenDate = null, kindName = "새 이름"))

        val saved = bookmarks.bookmarks.value.single().animal
        assertEquals(LocalDate(2026, 11, 3), saved.happenDate)
        assertEquals("새 이름", saved.kindName)
    }

    @Test
    fun `북마크는 사망 종료도 숨기지 않고 배지 그대로(5d U4)`() = runVmTest {
        var now = clock.now()
        val bookmarks = FakeBookmarkRepository { now.also { now += 1.minutes } }
        bookmarks.add(testAnimal(desertionNo = "B", processState = "종료(자연사)"))
        bookmarks.add(testAnimal(desertionNo = "A"))
        val env = start(Env(bookmarks))

        assertEquals(listOf("A", "B"), env.state.items.map { it.desertionNo })
        assertEquals(StatusKind.ENDED, env.state.items.last().status.kind)
        assertFalse(env.state.isEmpty)
    }
}
