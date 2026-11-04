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
 * [ReplaceFileCorruptionHandler]가 기본값으로 바꾸게 한다. 그 원인에는 [UnreadableJson]을 둔다 —
 * 손상 처리기([createJsonDataStore])가 이것이 원인 사슬에 있을 때만 진짜 손상으로 본다(5a-fixes2 H2).
 * 웹 OPFS(datastore-core-okio 1.3.0)는 읽기 실패를 모두 `CorruptionException`으로 감싸 올리기 때문이다.
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
            throw unreadableJsonCorruption(e)
        }
    }

    override suspend fun writeTo(t: T, sink: BufferedSink) {
        sink.writeUtf8(json.encodeToString(serializer, t))
    }
}

/** 우리 직렬화기가 JSON을 해석하지 못했다는 표시(진짜 손상의 판정 근거). 메시지는 고정 — 파일 내용을 싣지 않는다. */
internal class UnreadableJson(cause: IllegalArgumentException) :
    Exception("저장 파일 JSON 해석 실패", cause)

/**
 * 우리 직렬화기의 손상 예외. 표시([UnreadableJson])를 원인과 함께 suppressed에도 둔다 — datastore-core 1.3.0의
 * 웹(wasmJs) `IOException`은 생성자의 message·cause를 버려(`CorruptionException("x", e)`의 `cause`가 null,
 * 2026-11-05 브라우저 테스트로 확인) 원인 사슬로는 웹에서 판정할 수 없다. suppressed는 남는다.
 */
internal fun unreadableJsonCorruption(e: IllegalArgumentException): CorruptionException {
    val marker = UnreadableJson(e)
    return CorruptionException("저장 파일을 읽지 못했다", marker).apply { addSuppressed(marker) }
}

/** 원인 사슬(또는 그 suppressed)에 [UnreadableJson]이 있으면 진짜 손상이다. */
internal fun Throwable.isUnreadableJson(): Boolean =
    generateSequence(this) { it.cause }.take(MAX_CAUSE_DEPTH).any { t ->
        t is UnreadableJson || t.suppressedExceptions.any { it is UnreadableJson }
    }

private const val MAX_CAUSE_DEPTH = 16

/**
 * 손상 복구가 붙은 DataStore. `scope`는 테스트만 넘긴다 — 운영에서는 라이브러리 기본값
 * (플랫폼 I/O 디스패처)을 쓰므로 commonMain에서 `Dispatchers.IO`를 쓰지 않아도 된다.
 *
 * 손상 판정(5a-fixes2 H2, `reports/phase5a.md` 13.5 N11): 웹 OPFS(datastore-core-okio 1.3.0)의 읽기는
 * 파일 없음 말고 모든 실패(`getDirectory()` 거절·`getFile` 실패·일시 오류)를
 * `CorruptionException("Unable to read from OPFS", e)`로 올린다. 그것을 모두 손상으로 보면 개인 정보 보호 창에서
 * 거짓 북마크 복구 안내가 뜨고, 읽기가 한 번 일시 실패하면 북마크 파일을 빈 목록으로 덮는다. 그래서 원인 사슬에
 * 우리 직렬화기의 해석 실패([UnreadableJson] — [unreadableJsonCorruption])가 있을 때만 기본값으로 바꾸고
 * [onCorruption]을 부른다. Android는 우리 예외가 한 겹으로 온다. 웹은 OPFS가 우리 예외를 새
 * `CorruptionException`으로 감싸며 원인을 잃으므로(웹 `IOException`이 cause를 버린다) 웹 저장소 actual
 * (`createJsonStorage`)이 그 읽기에서 우리 직렬화기가 던진 원래 예외를 되살려 던진다. 그 밖(OPFS 읽기 실패)은
 * 파일을 건드리지 않고 다시 던진다 — [MirroredDataStore]가 기본값으로 보여 주고, 쓰기는 다시 파일을 읽는다.
 * 라이브러리 메시지 글자는 비교하지 않는다.
 */
internal fun <T> createJsonDataStore(
    storage: Storage<T>,
    defaultValue: T,
    scope: CoroutineScope?,
    onCorruption: () -> Unit = {},
): DataStore<T> {
    val handler = ReplaceFileCorruptionHandler<T> { e ->
        if (!e.isUnreadableJson()) throw e
        onCorruption()
        defaultValue
    }
    return if (scope == null) {
        DataStoreFactory.create(storage = storage, corruptionHandler = handler)
    } else {
        DataStoreFactory.create(storage = storage, corruptionHandler = handler, scope = scope)
    }
}
