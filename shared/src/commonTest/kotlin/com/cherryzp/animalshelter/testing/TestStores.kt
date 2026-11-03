package com.cherryzp.animalshelter.testing

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioSerializer
import com.cherryzp.animalshelter.data.store.BookmarkStore
import com.cherryzp.animalshelter.data.store.CodeTableStore
import com.cherryzp.animalshelter.data.store.SettingsStore
import com.cherryzp.animalshelter.data.store.ShelterStore
import com.cherryzp.animalshelter.platform.createJsonStorage
import com.cherryzp.animalshelter.platform.createPreferencesStorage
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import okio.BufferedSink
import okio.BufferedSource

/**
 * 테스트용 저장소 묶음. 파일 이름은 실행마다 무작위(브라우저 localStorage·OPFS가 남을 수 있다),
 * DataStore 범위는 [close]로 닫는다. Android 호스트는 Koin에 `storageTestModule()`이 있어야 한다.
 */
internal class TestStores(val suffix: String = Random.nextLong().toULong().toString(16)) {
    private val job = Job()
    val scope = CoroutineScope(job + Dispatchers.Default)

    fun name(base: String): String = "test-$suffix-$base"

    fun settings() = SettingsStore(createPreferencesStorage(name("settings.preferences_pb")), scope)

    fun codeTables(file: String = "code_tables.json") =
        CodeTableStore(createJsonStorage(name(file), CodeTableStore.serializer), scope)

    fun bookmarks(file: String = "bookmarks.json") =
        BookmarkStore(createJsonStorage(name(file), BookmarkStore.serializer), scope)

    fun shelters(file: String = "shelters.json") =
        ShelterStore(createJsonStorage(name(file), ShelterStore.serializer), scope)

    /** 파일에 원문을 그대로 쓴다(손상 시험용). 쓰고 나면 그 DataStore는 닫는다. */
    suspend fun writeRaw(file: String, text: String) {
        val rawJob = Job()
        try {
            DataStoreFactory.create(
                storage = createJsonStorage(name(file), RawText),
                scope = CoroutineScope(rawJob + Dispatchers.Default)
            ).updateData { text }
        } finally {
            rawJob.cancelAndJoin()
        }
    }

    suspend fun close() {
        job.cancelAndJoin()
    }

    private object RawText : OkioSerializer<String> {
        override val defaultValue: String = ""

        override suspend fun readFrom(source: BufferedSource): String = source.readUtf8()

        override suspend fun writeTo(t: String, sink: BufferedSink) {
            sink.writeUtf8(t)
        }
    }
}

/** [TestStores]를 만들어 쓰고 닫는다. */
internal suspend fun <R> withTestStores(
    stores: TestStores = TestStores(),
    block: suspend (TestStores) -> R,
): R {
    try {
        return block(stores)
    } finally {
        stores.close()
    }
}
