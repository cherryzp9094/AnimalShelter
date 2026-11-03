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
            list.toMutableList().also { it[index] = it[index].copy(animal = animal) }
        } else {
            listOf(Bookmark(animal, now())) + list
        }
    }

    override suspend fun remove(desertionNo: String) {
        removeCalls += desertionNo
        bookmarks.value = bookmarks.value.filterNot { it.animal.desertionNo == desertionNo }
    }

    override suspend fun updateSnapshot(animal: Animal) {
        updateCalls += animal
        bookmarks.value = bookmarks.value.map { bookmark ->
            if (bookmark.animal.desertionNo != animal.desertionNo) return@map bookmark
            val merged = if (animal.happenDate == null) {
                animal.copy(happenDate = bookmark.animal.happenDate)
            } else {
                animal
            }
            bookmark.copy(animal = merged)
        }
    }

    override fun getRecoveredFromCorruptionStream(): StateFlow<Boolean> = recovered

    override fun dismissRecoveryNotice() {
        recovered.value = false
    }
}
