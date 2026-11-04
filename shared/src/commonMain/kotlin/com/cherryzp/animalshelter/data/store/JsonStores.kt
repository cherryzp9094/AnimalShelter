package com.cherryzp.animalshelter.data.store

import androidx.datastore.core.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// JSON 파일 저장소 3개(02-data 8절). 저장소마다 DataStore 하나(Koin single). 같은 파일에 두 개를 만들지 않는다.
// 구독은 메모리 사본([MirroredDataStore])으로 한다 — 웹 OPFS 저장소의 제약 때문.
// 캐시 둘(코드표·보호소)은 파일 쓰기가 실패해도 사본을 바꾸고, 북마크는 바꾸지 않는다(5a-fixes F1).

/** 코드표 캐시(`code_tables.json`). 깨지면 빈 캐시로 복구한다. */
internal class CodeTableStore(storage: Storage<CodeTableCache>, scope: CoroutineScope? = null) {
    private val store =
        MirroredDataStore(createJsonDataStore(storage, CodeTableCache(), scope), CodeTableCache())

    val data: Flow<CodeTableCache> = store.data

    /** 파일 쓰기가 실패해도 메모리 사본은 바꾸고 예외를 던진다([MirroredDataStore.updateKeepingMemoryOnFailure]). */
    suspend fun update(transform: (CodeTableCache) -> CodeTableCache) {
        store.updateKeepingMemoryOnFailure(transform)
    }

    companion object {
        const val FILE_NAME = "code_tables.json"
        val serializer = JsonOkioSerializer(CodeTableCache.serializer(), CodeTableCache())
    }
}

/** 보호소 전체 캐시(`shelters.json`). 깨지면 빈 캐시로 복구한다. */
internal class ShelterStore(storage: Storage<ShelterCache>, scope: CoroutineScope? = null) {
    private val store =
        MirroredDataStore(createJsonDataStore(storage, ShelterCache(), scope), ShelterCache())

    val data: Flow<ShelterCache> = store.data

    /** 파일 쓰기가 실패해도 메모리 사본은 바꾸고 예외를 던진다([MirroredDataStore.updateKeepingMemoryOnFailure]). */
    suspend fun update(transform: (ShelterCache) -> ShelterCache) {
        store.updateKeepingMemoryOnFailure(transform)
    }

    companion object {
        const val FILE_NAME = "shelters.json"
        val serializer = JsonOkioSerializer(ShelterCache.serializer(), ShelterCache())
    }
}

/**
 * 북마크(`bookmarks.json`). 깨지면 빈 목록으로 복구하고 [recoveredFromCorruption]을 켠다
 * (화면이 사용자에게 한 번 알린다).
 */
internal class BookmarkStore(storage: Storage<BookmarkData>, scope: CoroutineScope? = null) {
    private val recovered = MutableStateFlow(false)
    private val store = MirroredDataStore(
        createJsonDataStore(storage, BookmarkData(), scope) { recovered.value = true },
        BookmarkData()
    )

    // 읽기 실패(손상 말고 I/O 오류)는 빈 목록으로 보여 준다. 쓰기는 그대로 실패한다.
    val data: Flow<BookmarkData> = store.data

    val recoveredFromCorruption: StateFlow<Boolean> = recovered.asStateFlow()

    fun clearRecovered() {
        recovered.value = false
    }

    /** 파일 쓰기가 실패하면 사본은 그대로 두고 예외를 던진다 — 화면이 `bookmark_save_failed`로 알린다. */
    suspend fun update(transform: (BookmarkData) -> BookmarkData) {
        store.update(transform)
    }

    companion object {
        const val FILE_NAME = "bookmarks.json"
        val serializer = JsonOkioSerializer(BookmarkData.serializer(), BookmarkData())
    }
}
