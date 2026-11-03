package com.cherryzp.animalshelter.data.api.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTransformingSerializer

// 공공데이터 응답 봉투. 근거: docs/design/02-data.md 1.2·3.2.

@Serializable
internal data class ApiEnvelope<T>(val response: ApiResponse<T>)

@Serializable
internal data class ApiResponse<T>(val header: ApiHeader, val body: ApiBody<T>? = null)

@Serializable
internal data class ApiHeader(
    // 실제 응답에서는 숫자로 온다. lenient 설정으로 문자열로 받는다.
    val reqNo: String? = null,
    val resultCode: String,
    val resultMsg: String? = null,
    val errorMsg: String? = null,
)

@Serializable
internal data class ApiBody<T>(
    @Serializable(with = ItemsSerializer::class)
    val items: List<T> = emptyList(),
    val numOfRows: Int = 0,
    val pageNo: Int = 1,
    val totalCount: Int = 0,
)

/**
 * `items` 모양 세 가지를 목록 하나로 맞춘다.
 * - 0건: `{}` → `[]`
 * - 여러 건: `{"item": [...]}` → 그 배열
 * - 한 건(표본 없음, 방어): `{"item": {...}}` → `[{...}]`
 */
internal class ItemsSerializer<T>(itemSerializer: KSerializer<T>) :
    JsonTransformingSerializer<List<T>>(ListSerializer(itemSerializer)) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element is JsonArray) return element
        val item = (element as? JsonObject)?.get("item") ?: return JsonArray(emptyList())
        return when (item) {
            is JsonArray -> item
            is JsonObject -> JsonArray(listOf(item))
            else -> JsonArray(emptyList())
        }
    }

    override fun transformSerialize(element: JsonElement): JsonElement =
        JsonObject(mapOf("item" to element))
}
