package com.cherryzp.animalshelter.data.api.dto

import kotlinx.serialization.Serializable

/** `sido_v2` item(02-data 1.4). 시도 코드·이름. */
@Serializable
internal data class SidoDto(val orgCd: String? = null, val orgdownNm: String? = null)
