package com.cherryzp.animalshelter.platform

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.InterProcessCoordinator
import androidx.datastore.core.ReadScope
import androidx.datastore.core.Storage
import androidx.datastore.core.StorageConnection
import androidx.datastore.core.WriteScope
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.WebLocalStorage
import androidx.datastore.core.okio.WebOpfsStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import okio.BufferedSink
import okio.BufferedSource
import okio.IOException

// 설정은 localStorage(탭 사이 동기화), 북마크·캐시는 OPFS(용량·비동기). 02-data 8절.
actual fun createPreferencesStorage(name: String): Storage<Preferences> =
    WebLocalStorage(PreferencesSerializer, name)

actual fun <T> createJsonStorage(name: String, serializer: OkioSerializer<T>): Storage<T> {
    val capturing = CapturingSerializer(serializer)
    return JsSafeStorage(WebOpfsStorage(capturing, name), capturing, name)
}

/**
 * OPFS 저장소를 공통 코드의 예외 규칙에 맞춘다. 두 가지:
 *
 * 1. JS 예외를 [IOException]으로(5a-fixes2 H1, `reports/phase5a.md` 13.5 N10). Kotlin/Wasm에서 JS가 **동기로**
 *    던진 예외(없는 메서드 → `TypeError`)는 `kotlin.Exception`이 아닌 `Throwable`(`JsException`)로 올라온다.
 *    datastore-core-okio 1.3.0의 OPFS 연결은 `createWritable()`을 catch 없이 부르므로, 그 메서드가 없는 옛
 *    Safari(18.x)에서는 공통 코드의 `catch (e: Exception)`(`MirroredDataStore`·`saveQuietly`·ViewModel)을 모두
 *    지나 코루틴 밖으로 나갔다 — 캐시가 진행 표시에 묶이고 북마크 실패 안내도 없었다. 여기서 바꾸면 Safari 개인
 *    정보 보호 창(Promise 거절 — 이미 `Exception`)과 같은 길로 간다. 공통 코드의 catch를 `Throwable`로 넓히지 않는
 *    이유는 Android에서 `Error`까지 삼키기 때문이다. `Exception`(취소 포함)은 그대로 지나간다. 메시지에는 OPFS
 *    파일 이름만 넣고(키·URL과 무관) 원인은 보존한다.
 * 2. 우리 직렬화기의 손상 예외를 되살린다(5a-fixes2 H2, N11). OPFS `readData`는 직렬화기 예외를 포함한 읽기 실패를
 *    모두 새 `CorruptionException("Unable to read from OPFS", e)`로 감싸는데, 웹 `IOException`이 cause를 버려
 *    손상 처리기(`createJsonDataStore`)가 진짜 손상을 알아볼 수 없다. 그 읽기에서 직렬화기가 `CorruptionException`을
 *    던졌으면 그 원래 예외를, 아니면 감싼 것을 그대로 던진다.
 *
 * 설정 저장소(localStorage)는 감싸지 않는다 — 동기 예외는 용량 초과뿐이고 값이 작다.
 */
private class JsSafeStorage<T>(
    private val delegate: Storage<T>,
    private val serializer: CapturingSerializer<T>,
    private val name: String,
) : Storage<T> {
    override fun createConnection(): StorageConnection<T> =
        JsSafeConnection(guard("열기") { delegate.createConnection() })

    private inner class JsSafeConnection(private val real: StorageConnection<T>) :
        StorageConnection<T> {
        override val coordinator: InterProcessCoordinator get() = real.coordinator

        override suspend fun <R> readScope(block: suspend ReadScope<T>.(locked: Boolean) -> R): R =
            guard("읽기") {
                real.readScope { locked -> RestoringReadScope(this).block(locked) }
            }

        override suspend fun writeScope(block: suspend WriteScope<T>.() -> Unit) {
            guard("쓰기") { real.writeScope(block) }
        }

        override fun close() {
            real.close()
        }
    }

    private inner class RestoringReadScope(private val real: ReadScope<T>) : ReadScope<T> {
        override suspend fun readData(): T {
            serializer.lastReadFailure = null
            try {
                return real.readData()
            } catch (e: CorruptionException) {
                throw serializer.lastReadFailure ?: e
            } finally {
                serializer.lastReadFailure = null
            }
        }

        override fun close() {
            real.close()
        }
    }

    private inline fun <R> guard(action: String, block: () -> R): R = try {
        block()
    } catch (e: Exception) {
        throw e
    } catch (e: Throwable) {
        throw IOException("OPFS $action 실패($name)", e)
    }
}

/**
 * 직렬화기가 읽기에서 던진 `CorruptionException`을 기억한다(저장소마다 하나). 읽기·쓰기의 차례는 `MirroredDataStore`의
 * 잠금이 만든다. 읽기가 겹쳐도 섞이지 않는다 — wasm은 한 스레드이고, 직렬화기가 던진 뒤 `RestoringReadScope`의
 * catch에 닿기까지 멈춤(`await`)이 없는 동기 구간이라 그 사이에 다른 읽기가 끼지 못한다(`reports/phase5a.md` 15.2 ①,
 * `02-data.md` 8절).
 */
private class CapturingSerializer<T>(private val real: OkioSerializer<T>) : OkioSerializer<T> {
    var lastReadFailure: CorruptionException? = null

    override val defaultValue: T get() = real.defaultValue

    override suspend fun readFrom(source: BufferedSource): T = try {
        real.readFrom(source)
    } catch (e: CorruptionException) {
        lastReadFailure = e
        throw e
    }

    override suspend fun writeTo(t: T, sink: BufferedSink) {
        real.writeTo(t, sink)
    }
}
