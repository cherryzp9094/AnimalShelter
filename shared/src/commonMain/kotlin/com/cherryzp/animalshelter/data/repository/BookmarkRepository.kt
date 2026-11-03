package com.cherryzp.animalshelter.data.repository

import com.cherryzp.animalshelter.model.Animal
import com.cherryzp.animalshelter.model.Bookmark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** 북마크(02-data 8절). `desertionNo` 유일, 추가 역순. */
interface BookmarkRepository {
    fun getBookmarksStream(): Flow<List<Bookmark>>

    fun isBookmarkedStream(desertionNo: String): Flow<Boolean>

    /** 스냅샷을 저장한다. 이미 있으면 스냅샷만 바꾸고 순서·`savedAt`은 둔다. */
    suspend fun add(animal: Animal)

    suspend fun remove(desertionNo: String)

    /** 있는 것만 바꾼다. `savedAt`은 그대로, 새 스냅샷에 `happenDate`가 없으면 옛 값을 둔다. */
    suspend fun updateSnapshot(animal: Animal)

    /** 북마크 파일이 깨져 빈 목록으로 복구했으면 true(화면이 한 번 알린다). */
    fun getRecoveredFromCorruptionStream(): StateFlow<Boolean>

    /** 복구 안내를 보여 줬다. */
    fun dismissRecoveryNotice()
}
