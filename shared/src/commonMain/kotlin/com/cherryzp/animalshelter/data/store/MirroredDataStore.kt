package com.cherryzp.animalshelter.data.store

import androidx.datastore.core.DataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * DataStore 앞에 둔 메모리 사본. 처음 한 번만 파일에서 읽고, 쓰기는 `updateData`의 결과로 사본을 바꾼다.
 * 구독자는 사본만 모은다 — `dataStore.data`를 오래 모으지 않는다.
 *
 * 이유(2026-11-04 확인): 웹 `WebOpfsStorage`(datastore-core-okio 1.3.0)는 `data`를 모으는 중에
 * `updateData`를 하면 그 구독자에게도, 뒤의 새 읽기에도 바뀐 값이 보이지 않았다(Chrome headless).
 * 읽기 → 쓰기 → 읽기는 정상이다. 저장소마다 DataStore가 하나(Koin single)라 이 사본은 정확하다.
 * 웹에서 다른 탭의 변경은 다시 열 때 반영된다.
 */
internal class MirroredDataStore<T : Any>(
    private val dataStore: DataStore<T>,
    private val defaultValue: T,
) {
    private val mutex = Mutex()
    private val state = MutableStateFlow<T?>(null)

    val data: Flow<T> = flow {
        load()
        emitAll(state.filterNotNull())
    }

    suspend fun update(transform: (T) -> T): T = mutex.withLock {
        dataStore.updateData { transform(it) }.also { state.value = it }
    }

    private suspend fun load() {
        if (state.value != null) return
        mutex.withLock {
            if (state.value != null) return
            state.value = try {
                dataStore.data.first()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // 읽기 실패(손상 말고 I/O 오류)는 기본값으로 보여 준다. 쓰기는 다시 파일을 읽는다.
                defaultValue
            }
        }
    }
}
