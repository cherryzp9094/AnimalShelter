package com.cherryzp.animalshelter.data.api.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

// 관대한 숫자 직렬화기(D7). 숫자 `3`, 문자열 `"3"`, 빈 문자열 `""`, `null`을 모두 받는다.
// 숫자로 읽을 수 없는 값은 예외 대신 null로 본다. JSON 전용(공공데이터 응답 DTO에만 쓴다).

/** 정수 칸. `"3.0"`처럼 소수점 아래가 0인 값도 받는다. */
internal object LenientIntSerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("animalshelter.LenientInt", PrimitiveKind.INT).nullable

    override fun deserialize(decoder: Decoder): Int? {
        val text = lenientContent(decoder) ?: return null
        text.toIntOrNull()?.let { return it }
        val double = text.toDoubleOrNull() ?: return null
        return if (double % 1.0 == 0.0 &&
            double in Int.MIN_VALUE.toDouble()..Int.MAX_VALUE.toDouble()
        ) {
            double.toInt()
        } else {
            null
        }
    }

    override fun serialize(encoder: Encoder, value: Int?) {
        if (value == null) encoder.encodeNull() else encoder.encodeInt(value)
    }
}

/** 실수 칸(위도·경도). NaN·무한대는 null. */
internal object LenientDoubleSerializer : KSerializer<Double?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("animalshelter.LenientDouble", PrimitiveKind.DOUBLE).nullable

    override fun deserialize(decoder: Decoder): Double? =
        lenientContent(decoder)?.toDoubleOrNull()?.takeIf { it.isFinite() }

    override fun serialize(encoder: Encoder, value: Double?) {
        if (value == null) encoder.encodeNull() else encoder.encodeDouble(value)
    }
}

/** JSON 원시값의 글자(앞뒤 공백 제거). null·빈 문자열·객체·배열이면 null. */
private fun lenientContent(decoder: Decoder): String? {
    val element = (decoder as? JsonDecoder)?.decodeJsonElement()
        ?: return decoder.decodeString().trim().ifEmpty { null }
    val primitive = element as? JsonPrimitive ?: return null
    if (primitive.content == "null" && !primitive.isString) return null
    return primitive.content.trim().ifEmpty { null }
}
