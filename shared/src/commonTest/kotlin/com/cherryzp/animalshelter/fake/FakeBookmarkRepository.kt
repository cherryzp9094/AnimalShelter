package com.cherryzp.animalshelter.fake

import com.cherryzp.animalshelter.data.repository.BookmarkRepository
import com.cherryzp.animalshelter.model.Animal
import com.cherryzp.animalshelter.model.Bookmark
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** 가짜 북마크 Repository. 메모리 목록으로 실제와 같은 규칙(유일·역순·스냅샷 갱신)을 따른다. */
class FakeBookmarkRepository(
    private val now: () -> Instant = { Instant.parse("2026-11-04T01:00:00Z") },
) : BookmarkRepository {
    val bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val recovered = MutableStateFlow(false)
    val addCalls = mutableListOf<Animal>()
    val removeCalls = mutableListOf<String>()
    val restoreCalls = mutableListOf<Pair<Bookmark, Int>>()
    val updateCalls = mutableListOf<Animal>()

    override fun getBookmarksStream(): Flow<List<Bookmark>> = bookmarks

    override fun isBookmarkedStream(desertionNo: String): Flow<Boolean> = bookmarks.map { list ->
        list.any { it.animal.desertionNo == desertionNo }
    }.distinctUntilChanged()

    override suspend fun add(animal: Animal) {
        addCalls += animal
        val list = bookmarks.value
        val index = list.indexOfFirst { it.animal.desertionNo == animal.desertionNo }
        bookmarks.value = if (index >= 0) {
            // 실제 구현처럼 순서·savedAt을 두고, 새 스냅샷에 happenDate가 없으면 옛 값을 지킨다(R8)
            list.toMutableList().also { it[index] = it[index].withSnapshot(animal) }
        } else {
            listOf(Bookmark(animal, now())) + list
        }
    }

    override suspend fun remove(desertionNo: String) {
        removeCalls += desertionNo
        bookmarks.value = bookmarks.value.filterNot { it.animal.desertionNo == desertionNo }
    }

    /** 실제 구현처럼 원래 `savedAt`으로 [index](0..size로 자른다)에, 이미 있는 번호면 그대로(F4). */
    override suspend fun restore(bookmark: Bookmark, index: Int) {
        restoreCalls += bookmark to index
        val list = bookmarks.value
        if (list.any { it.animal.desertionNo == bookmark.animal.desertionNo }) return
        bookmarks.value = list.toMutableList().also { it.add(index.coerceIn(0, it.size), bookmark) }
    }

    override suspend fun updateSnapshot(animal: Animal) {
        updateCalls += animal
        bookmarks.value = bookmarks.value.map { bookmark ->
            if (bookmark.animal.desertionNo == animal.desertionNo) {
                bookmark.withSnapshot(animal)
            } else {
                bookmark
            }
        }
    }

    /** 스냅샷만 바꾼다(`savedAt` 유지, 새 스냅샷에 `happenDate`가 없으면 옛 값) — `DefaultBookmarkRepository`와 같다. */
    private fun Bookmark.withSnapshot(animal: Animal): Bookmark = copy(
        animal = if (animal.happenDate == null) {
            animal.copy(happenDate = this.animal.happenDate)
        } else {
            animal
        }
    )

    override fun getRecoveredFromCorruptionStream(): StateFlow<Boolean> = recovered

    override fun dismissRecoveryNotice() {
        recovered.value = false
    }
}
