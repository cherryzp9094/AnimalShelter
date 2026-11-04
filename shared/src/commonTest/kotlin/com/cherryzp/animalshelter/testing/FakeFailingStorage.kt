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
 *
 * `androidDeviceTest`에도 컴파일되는 폴더라 테스트 전용 도우미(`TestStores`)를 쓰지 않는다.
 */
internal class FakeFailingStorage<T>(
    private val delegate: Storage<T>,
    private val failReads: Boolean = false,
) : Storage<T> {
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
            if (failReads) throw IOException("가짜 저장소: 읽기 실패")
            return real.readScope(block)
        }

        override suspend fun writeScope(block: suspend WriteScope<T>.() -> Unit) {
            failedWrites++
            throw IOException("가짜 저장소: 쓰기 실패")
        }

        override fun close() {
            real.close()
        }
    }
}
