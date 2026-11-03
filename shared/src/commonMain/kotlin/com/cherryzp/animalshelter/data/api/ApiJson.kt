package com.cherryzp.animalshelter.data.api

import kotlinx.serialization.json.Json

/**
 * 공공데이터 응답용 Json 설정(02-data 3.1).
 * 숫자/문자열 혼용(`totalCount`), 명세에 없는 필드, null을 받아 준다.
 */
internal val ApiJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
}
