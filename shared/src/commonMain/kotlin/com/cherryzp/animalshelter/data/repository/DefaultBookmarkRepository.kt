package com.cherryzp.animalshelter.data.repository

import com.cherryzp.animalshelter.data.store.BookmarkData
import com.cherryzp.animalshelter.data.store.BookmarkStore
import com.cherryzp.animalshelter.data.store.StoredBookmark
import com.cherryzp.animalshelter.model.Animal
import com.cherryzp.animalshelter.model.Bookmark
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** 북마크 Repository(02-data 8절). */
internal class DefaultBookmarkRepository(
    private val store: BookmarkStore,
    private val clock: Clock,
) : BookmarkRepository {
    override fun getBookmarksStream(): Flow<List<Bookmark>> = store.data.map { data ->
        data.order.mapNotNull { no ->
            data.entries[no]?.let {
                Bookmark(it.animal, Instant.fromEpochMilliseconds(it.savedAtMillis))
            }
        }
    }.distinctUntilChanged()

    override fun isBookmarkedStream(desertionNo: String): Flow<Boolean> =
        store.data.map { desertionNo in it.entries }.distinctUntilChanged()

    override suspend fun add(animal: Animal) {
        val now = clock.now().toEpochMilliseconds()
        store.update { data ->
            val no = animal.desertionNo
            val existing = data.entries[no]
            if (existing != null) {
                data.withSnapshot(existing, animal)
            } else {
                BookmarkData(
                    entries = data.entries + (no to StoredBookmark(animal, now)),
                    order = listOf(no) + data.order.filter { it != no }
                )
            }
        }
    }

    override suspend fun remove(desertionNo: String) {
        store.update { data ->
            BookmarkData(entries = data.entries - desertionNo, order = data.order - desertionNo)
        }
    }

    override suspend fun updateSnapshot(animal: Animal) {
        store.update { data ->
            val existing = data.entries[animal.desertionNo] ?: return@update data
            data.withSnapshot(existing, animal)
        }
    }

    override fun getRecoveredFromCorruptionStream(): StateFlow<Boolean> =
        store.recoveredFromCorruption

    override fun dismissRecoveryNotice() {
        store.clearRecovered()
    }

    /** 스냅샷만 바꾼다(`savedAt`·순서 유지, 새 스냅샷에 `happenDate`가 없으면 옛 값). */
    private fun BookmarkData.withSnapshot(existing: StoredBookmark, animal: Animal): BookmarkData {
        val merged = if (animal.happenDate == null) {
            animal.copy(happenDate = existing.animal.happenDate)
        } else {
            animal
        }
        return copy(entries = entries + (animal.desertionNo to existing.copy(animal = merged)))
    }
}
