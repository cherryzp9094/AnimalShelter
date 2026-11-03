package com.cherryzp.animalshelter.data.store

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource

/**
 * JSON 파일 직렬화기(02-data 8절). 깨진 JSON은 [CorruptionException]으로 올려
 * [ReplaceFileCorruptionHandler]가 기본값으로 바꾸게 한다.
 */
internal class JsonOkioSerializer<T>(
    private val serializer: KSerializer<T>,
    override val defaultValue: T,
    private val json: Json = StoreJson,
) : OkioSerializer<T> {
    override suspend fun readFrom(source: BufferedSource): T {
        val text = source.readUtf8()
        return try {
            json.decodeFromString(serializer, text)
        } catch (e: IllegalArgumentException) {
            // SerializationException도 IllegalArgumentException이다.
            throw CorruptionException("저장 파일을 읽지 못했다", e)
        }
    }

    override suspend fun writeTo(t: T, sink: BufferedSink) {
        sink.writeUtf8(json.encodeToString(serializer, t))
    }
}

/**
 * 손상 복구가 붙은 DataStore. `scope`는 테스트만 넘긴다 — 운영에서는 라이브러리 기본값
 * (플랫폼 I/O 디스패처)을 쓰므로 commonMain에서 `Dispatchers.IO`를 쓰지 않아도 된다.
 */
internal fun <T> createJsonDataStore(
    storage: Storage<T>,
    defaultValue: T,
    scope: CoroutineScope?,
    onCorruption: () -> Unit = {},
): DataStore<T> {
    val handler = ReplaceFileCorruptionHandler<T> {
        onCorruption()
        defaultValue
    }
    return if (scope == null) {
        DataStoreFactory.create(storage = storage, corruptionHandler = handler)
    } else {
        DataStoreFactory.create(storage = storage, corruptionHandler = handler, scope = scope)
    }
}
