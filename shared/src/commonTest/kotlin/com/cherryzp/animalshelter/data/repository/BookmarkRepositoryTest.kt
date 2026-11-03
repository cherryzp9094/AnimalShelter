package com.cherryzp.animalshelter.data.repository

import com.cherryzp.animalshelter.model.Animal
import com.cherryzp.animalshelter.platform.storageTestModule
import com.cherryzp.animalshelter.testing.FakeClock
import com.cherryzp.animalshelter.testing.withTestStores
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

/** 북마크 유일성·순서·스냅샷 갱신(02-data 8절). */
class BookmarkRepositoryTest {
    private val clock = FakeClock()

    @BeforeTest
    fun setUp() {
        startKoin { modules(storageTestModule()) }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `추가 역순이고 같은 번호는 하나만`() = runTest {
        withTestStores { stores ->
            val repo = DefaultBookmarkRepository(stores.bookmarks(), clock)

            repo.add(Animal("A"))
            clock.advance(1.hours)
            repo.add(Animal("B"))
            repo.add(Animal("A", kindName = "갱신"))

            val bookmarks = repo.getBookmarksStream().first()
            assertEquals(listOf("B", "A"), bookmarks.map { it.animal.desertionNo })
            assertEquals("갱신", bookmarks[1].animal.kindName)
            assertEquals(clock.now() - 1.hours, bookmarks[1].savedAt)
            assertTrue(repo.isBookmarkedStream("A").first())

            repo.remove("A")
            assertFalse(repo.isBookmarkedStream("A").first())
            assertEquals(
                listOf("B"),
                repo.getBookmarksStream().first().map {
                    it.animal.desertionNo
                }
            )
        }
    }

    @Test
    fun `updateSnapshot은 있는 것만 바꾸고 savedAt과 happenDate를 지킨다`() = runTest {
        withTestStores { stores ->
            val repo = DefaultBookmarkRepository(stores.bookmarks(), clock)
            val saved = Animal("A", happenDate = LocalDate(2025, 1, 3), processState = "보호중")
            repo.add(saved)
            val savedAt = clock.now()
            clock.advance(5.hours)

            repo.updateSnapshot(Animal("A", happenDate = null, processState = "종료(입양)"))
            repo.updateSnapshot(Animal("Z"))

            val bookmark = repo.getBookmarksStream().first().single()
            assertEquals("종료(입양)", bookmark.animal.processState)
            assertEquals(LocalDate(2025, 1, 3), bookmark.animal.happenDate)
            assertEquals(savedAt, bookmark.savedAt)
        }
    }

    @Test
    fun `깨진 파일은 빈 목록과 복구 알림`() = runTest {
        withTestStores { stores ->
            stores.writeRaw("bookmarks.json", "garbage")
            val repo = DefaultBookmarkRepository(stores.bookmarks(), clock)

            assertEquals(emptyList(), repo.getBookmarksStream().first())
            assertTrue(repo.getRecoveredFromCorruptionStream().value)
            repo.dismissRecoveryNotice()
            assertFalse(repo.getRecoveredFromCorruptionStream().value)
        }
    }
}
