package com.cherryzp.animalshelter.platform

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.BufferedSink
import okio.BufferedSource
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

/**
 * 플랫폼 저장소 팩토리가 두 타깃(Android 호스트·wasmJs 브라우저)에서 동작하는지 확인한다.
 * 첫 DataStore로 쓰고 닫은 뒤 **새 DataStore**로 읽어, 메모리 캐시가 아니라 저장소에서 읽히는지 본다.
 */
class StorageTest {
    // 실행마다 다른 이름(브라우저 localStorage·OPFS가 테스트 사이에 남을 수 있다)
    private val suffix = Random.nextLong().toULong().toString(16)

    @BeforeTest
    fun setUp() {
        startKoin { modules(storageTestModule()) }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `Preferences 저장소에 쓴 값을 새 DataStore로 다시 읽는다`() = runTest {
        val name = "test-$suffix.preferences_pb"
        val key = stringPreferencesKey("list_layout")

        withDataStore(::preferencesDataStore, name) { dataStore ->
            dataStore.edit { it[key] = "grid" }
        }
        val read = withDataStore(::preferencesDataStore, name) { it.data.first()[key] }

        assertEquals("grid", read)
    }

    @Test
    fun `직렬화 모델 저장소에 쓴 값을 새 DataStore로 다시 읽는다`() = runTest {
        val name = "test-$suffix.json"
        val json = "{\"entries\":{}}"

        withDataStore(::jsonDataStore, name) { dataStore -> dataStore.updateData { json } }
        val read = withDataStore(::jsonDataStore, name) { it.data.first() }

        assertEquals(json, read)
    }

    private fun preferencesDataStore(name: String, scope: CoroutineScope): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(storage = createPreferencesStorage(name), scope = scope)

    private fun jsonDataStore(name: String, scope: CoroutineScope): DataStore<String> =
        DataStoreFactory.create(storage = createJsonStorage(name, StringSerializer), scope = scope)

    /** DataStore 하나를 만들어 쓰고, 끝나면 범위를 닫는다(같은 파일에 동시에 두 개를 두지 않는다). */
    private suspend fun <T, R> withDataStore(
        create: (String, CoroutineScope) -> DataStore<T>,
        name: String,
        block: suspend (DataStore<T>) -> R,
    ): R {
        val job = Job()
        try {
            return block(create(name, CoroutineScope(job)))
        } finally {
            job.cancelAndJoin()
        }
    }

    private object StringSerializer : OkioSerializer<String> {
        override val defaultValue: String = ""

        override suspend fun readFrom(source: BufferedSource): String = source.readUtf8()

        override suspend fun writeTo(t: String, sink: BufferedSink) {
            sink.writeUtf8(t)
        }
    }
}
