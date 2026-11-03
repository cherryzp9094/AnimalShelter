package com.cherryzp.animalshelter.data.store

import com.cherryzp.animalshelter.model.Animal
import com.cherryzp.animalshelter.model.AnimalKind
import com.cherryzp.animalshelter.model.Kind
import com.cherryzp.animalshelter.model.ShelterCode
import com.cherryzp.animalshelter.model.ShelterInfo
import com.cherryzp.animalshelter.model.Sido
import com.cherryzp.animalshelter.model.Sigungu
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// 저장 모델(02-data 8절). data/ 밖으로 내보내지 않는다. 시각은 epoch 밀리초(D11).

/** 저장용 Json: 필드가 늘어도 옛 저장값을 읽는다(D12). */
internal val StoreJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}

/** 받아 둔 표 하나와 받은 시각. */
@Serializable
internal data class CachedTable<T>(val items: List<T>, val fetchedAtMillis: Long)

/** `code_tables.json`. 시군구 키는 시도 코드, 보호소 키는 `"시도/시군구"`. */
@Serializable
internal data class CodeTableCache(
    val sido: CachedTable<Sido>? = null,
    val sigungu: Map<String, CachedTable<Sigungu>> = emptyMap(),
    val shelters: Map<String, CachedTable<ShelterCode>> = emptyMap(),
    val kinds: Map<AnimalKind, CachedTable<Kind>> = emptyMap(),
)

/** 북마크 한 건: 저장 시점의 스냅샷과 처음 저장한 시각. */
@Serializable
internal data class StoredBookmark(val animal: Animal, val savedAtMillis: Long)

/** `bookmarks.json`. `order`는 표시 순서(추가 역순, 맨 앞이 최근). */
@Serializable
internal data class BookmarkData(
    val entries: Map<String, StoredBookmark> = emptyMap(),
    val order: List<String> = emptyList(),
)

/** `shelters.json`: `shelterInfo_v2` 전체. */
@Serializable
internal data class ShelterCache(val table: CachedTable<ShelterInfo>? = null)
