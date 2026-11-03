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

    /**
     * 지운 북마크를 되살린다(되돌리기, 중간 검토 F4): 원래 `savedAt`으로 순서 위치 [index]에 넣는다(`0..size`로
     * 자른다). 이미 있는 번호면(그새 다시 추가했으면) 아무것도 하지 않는다.
     */
    suspend fun restore(bookmark: Bookmark, index: Int)

    /** 있는 것만 바꾼다. `savedAt`은 그대로, 새 스냅샷에 `happenDate`가 없으면 옛 값을 둔다. */
    suspend fun updateSnapshot(animal: Animal)

    /** 북마크 파일이 깨져 빈 목록으로 복구했으면 true(화면이 한 번 알린다). */
    fun getRecoveredFromCorruptionStream(): StateFlow<Boolean>

    /** 복구 안내를 보여 줬다. */
    fun dismissRecoveryNotice()
}
