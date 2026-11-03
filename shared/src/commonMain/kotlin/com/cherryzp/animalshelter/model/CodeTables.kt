package com.cherryzp.animalshelter.model

import kotlinx.serialization.Serializable

// 코드표 항목(02-data 4절). 코드표 캐시에 그대로 저장하므로 @Serializable.

/** 시도. */
@Serializable
data class Sido(val code: String, val name: String)

/** 시군구. `uprCode`는 시도 코드. */
@Serializable
data class Sigungu(val uprCode: String, val code: String, val name: String)

/** 보호소 코드(`shelter_v2`). */
@Serializable
data class ShelterCode(val regNo: String, val name: String)

/** 품종. */
@Serializable
data class Kind(val code: String, val name: String)
