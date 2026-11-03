package com.cherryzp.animalshelter.data.store

import androidx.datastore.core.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch

// JSON 파일 저장소 3개(02-data 8절). 저장소마다 DataStore 하나(Koin single). 같은 파일에 두 개를 만들지 않는다.

/** 코드표 캐시(`code_tables.json`). 깨지면 빈 캐시로 복구한다. */
internal class CodeTableStore(storage: Storage<CodeTableCache>, scope: CoroutineScope? = null) {
    private val dataStore = createJsonDataStore(storage, CodeTableCache(), scope)

    val data: Flow<CodeTableCache> = dataStore.data.catch { emit(CodeTableCache()) }

    suspend fun update(transform: (CodeTableCache) -> CodeTableCache) {
        dataStore.updateData { transform(it) }
    }

    companion object {
        const val FILE_NAME = "code_tables.json"
        val serializer = JsonOkioSerializer(CodeTableCache.serializer(), CodeTableCache())
    }
}

/** 보호소 전체 캐시(`shelters.json`). 깨지면 빈 캐시로 복구한다. */
internal class ShelterStore(storage: Storage<ShelterCache>, scope: CoroutineScope? = null) {
    private val dataStore = createJsonDataStore(storage, ShelterCache(), scope)

    val data: Flow<ShelterCache> = dataStore.data.catch { emit(ShelterCache()) }

    suspend fun update(transform: (ShelterCache) -> ShelterCache) {
        dataStore.updateData { transform(it) }
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
    private val dataStore = createJsonDataStore(storage, BookmarkData(), scope) {
        recovered.value =
            true
    }

    // 읽기 실패(손상 말고 I/O 오류)는 빈 목록으로 보여 준다. 쓰기는 그대로 실패한다.
    val data: Flow<BookmarkData> = dataStore.data.catch { emit(BookmarkData()) }

    val recoveredFromCorruption: StateFlow<Boolean> = recovered.asStateFlow()

    fun clearRecovered() {
        recovered.value = false
    }

    suspend fun update(transform: (BookmarkData) -> BookmarkData) {
        dataStore.updateData { transform(it) }
    }

    companion object {
        const val FILE_NAME = "bookmarks.json"
        val serializer = JsonOkioSerializer(BookmarkData.serializer(), BookmarkData())
    }
}
