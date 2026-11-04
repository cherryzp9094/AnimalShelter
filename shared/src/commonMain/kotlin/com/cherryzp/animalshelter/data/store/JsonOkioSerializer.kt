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
 *
 * 빈 파일은 손상이 아니라 "아직 쓰지 않음"으로 보고 기본값을 돌려준다(4a 확인): 웹 `WebOpfsStorage`
 * (datastore-core-okio 1.3.0)는 처음 열 때 빈 파일을 만들고 그것을 읽게 해, 첫 방문마다 북마크 복구 안내
 * (`bookmarks_recovered`)가 떴다. DataStore는 임시 파일 + 이름 바꾸기로 쓰므로 쓰다 만 빈 파일은 생기지 않는다.
 */
internal class JsonOkioSerializer<T>(
    private val serializer: KSerializer<T>,
    override val defaultValue: T,
    private val json: Json = StoreJson,
) : OkioSerializer<T> {
    override suspend fun readFrom(source: BufferedSource): T {
        val text = source.readUtf8()
        if (text.isBlank()) return defaultValue
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
