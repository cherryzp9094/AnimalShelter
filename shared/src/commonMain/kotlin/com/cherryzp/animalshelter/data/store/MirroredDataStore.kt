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
 *
 * 쓰기 실패 규칙 두 가지(5a-fixes F1):
 * - [update](북마크): 파일 쓰기가 성공해야 사본을 바꾼다. 실패하면 사본은 그대로, 예외를 던진다 —
 *   저장되지 않은 값을 화면에만 보여 주지 않는다(새로고침하면 사라진다).
 * - [updateKeepingMemoryOnFailure](코드표·보호소 캐시): 파일 쓰기가 실패해도 사본은 바꾸고 예외를 던진다.
 *   웹 OPFS를 못 쓰는 브라우저(Safari 개인 정보 보호 창 — `getDirectory()` 거절, `createWritable()`이 없는
 *   옛 Safari)나 Android 저장 공간 부족에서도 받은 표가 화면에 나오고, 같은 실행 안에서는 사본이 신선해
 *   다시 받지 않는다. 파일은 비어 있으니 다음 실행에서 다시 받는다. 전에는 받은 표를 버려 스트림이
 *   진행 표시에 묶였다(`reports/phase5a.md` 5절 N1).
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

    /**
     * 캐시용 쓰기: 사본을 기준으로 바꾸고(사본이 비어 있으면 파일 값 — 쓰기 실패 땐 기본값), 파일 쓰기가
     * 실패해도 사본은 `transform(지금 사본 ?: 기본값)`으로 바꾼 뒤 예외를 다시 던진다(부르는 `saveQuietly`가
     * 삼킨다). 사본을 기준으로 하는 이유: 앞 쓰기가 실패해 사본에만 있는 표가 다음 쓰기가 성공할 때
     * 사라지지 않게. [CancellationException]은 사본을 바꾸지 않고 그대로 던진다.
     * `Mutex`는 재진입이 안 되므로 잠금 안에서 [load]를 부르지 않는다.
     */
    suspend fun updateKeepingMemoryOnFailure(transform: (T) -> T): T = mutex.withLock {
        val base = state.value
        val updated = try {
            dataStore.updateData { current -> transform(base ?: current) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            state.value = transform(base ?: defaultValue)
            throw e
        }
        state.value = updated
        updated
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
