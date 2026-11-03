package com.cherryzp.animalshelter.data.api.dto

import kotlinx.serialization.Serializable

// 코드표 item(02-data 1.4). 전부 nullable — 빈 항목은 매퍼가 버린다.

/** `sigungu_v2` item. 시도 코드·시군구 코드·이름. */
@Serializable
internal data class SigunguDto(
    val uprCd: String? = null,
    val orgCd: String? = null,
    val orgdownNm: String? = null,
)

/** `shelter_v2` item. 보호소 번호·이름. */
@Serializable
internal data class ShelterDto(val careRegNo: String? = null, val careNm: String? = null)

/** `kind_v2` item. 품종 코드·이름. */
@Serializable
internal data class KindDto(val kindCd: String? = null, val kindNm: String? = null)
