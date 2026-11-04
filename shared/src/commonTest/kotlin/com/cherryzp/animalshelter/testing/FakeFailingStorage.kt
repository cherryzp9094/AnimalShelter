package com.cherryzp.animalshelter.testing

import androidx.datastore.core.InterProcessCoordinator
import androidx.datastore.core.ReadScope
import androidx.datastore.core.Storage
import androidx.datastore.core.StorageConnection
import androidx.datastore.core.WriteScope
import kotlin.concurrent.Volatile
import okio.IOException

/**
 * 쓰기가 늘 실패하는 저장소(5a-fixes F1). 실제 저장소(`createJsonStorage(…)`)를 감싸 읽기와 조정자
 * (`coordinator`)는 그 연결에 맡기고, `writeScope`에서 [IOException]을 던진다 — 웹 OPFS 쓰기를 못 하는 브라우저
 * (`createWritable()`이 없는 옛 Safari)와 Android 저장 공간 부족을 흉내 낸다.
 * [failReads]이면 `readScope`에서도 던진다 — OPFS 자체가 없는 곳(Safari 개인 정보 보호 창, `getDirectory()` 거절).
 * 던지는 모양은 [readError]로 고른다 — 기본 [IOException], 웹 OPFS 읽기 실패처럼 우리 직렬화기가 아닌 원인의
 * `CorruptionException`도(5a-fixes2 H2). [failWrites]가 false면 쓰기는 실제 연결로 간다(손상 처리기가 파일을
 * 덮어쓰는지 보려고).
 *
 * `androidDeviceTest`에도 컴파일되는 폴더라 테스트 전용 도우미(`TestStores`)를 쓰지 않는다.
 */
internal class FakeFailingStorage<T>(
    private val delegate: Storage<T>,
    private val failReads: Boolean = false,
    failWrites: Boolean = true,
    private val readError: () -> Exception = { IOException("가짜 저장소: 읽기 실패") },
) : Storage<T> {
    /** 쓰기를 실패시킬지. 테스트가 도중에 끌 수 있다 — 실패 뒤 성공하는 쓰기(5b-prep P3). */
    @Volatile
    var failWrites: Boolean = failWrites

    /** 실패한 쓰기 시도 수(쓰기를 시도했는지 테스트가 본다). */
    @Volatile
    var failedWrites: Int = 0
        private set

    override fun createConnection(): StorageConnection<T> =
        FailingConnection(delegate.createConnection())

    private inner class FailingConnection(private val real: StorageConnection<T>) :
        StorageConnection<T> {
        override val coordinator: InterProcessCoordinator get() = real.coordinator

        override suspend fun <R> readScope(block: suspend ReadScope<T>.(locked: Boolean) -> R): R {
            if (failReads) throw readError()
            return real.readScope(block)
        }

        override suspend fun writeScope(block: suspend WriteScope<T>.() -> Unit) {
            if (!failWrites) return real.writeScope(block)
            failedWrites++
            throw IOException("가짜 저장소: 쓰기 실패")
        }

        override fun close() {
            real.close()
        }
    }
}
