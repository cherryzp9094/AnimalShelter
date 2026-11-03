package com.cherryzp.animalshelter.data.api.dto

import kotlinx.serialization.Serializable

/**
 * `shelterInfo_v2` item(02-data 1.5, 28개 필드, 이름은 명세 그대로 — `quarabtineCnt` 철자 포함).
 * 숫자 칸(`lat`·`lng`·`*Cnt`)은 숫자·숫자 문자열·빈 문자열·null을 모두 받는다(D7).
 * 값 하나가 이상해도 338건 응답 전체가 깨지지 않게 한다.
 */
@Serializable
internal data class ShelterInfoDto(
    val dataStdDt: String? = null,
    val careNm: String? = null,
    val careRegNo: String? = null,
    val orgNm: String? = null,
    val divisionNm: String? = null,
    val saveTrgtAnimal: String? = null,
    val careAddr: String? = null,
    val jibunAddr: String? = null,
    @Serializable(with = LenientDoubleSerializer::class) val lat: Double? = null,
    @Serializable(with = LenientDoubleSerializer::class) val lng: Double? = null,
    val dsignationDate: String? = null,
    val weekOprStime: String? = null,
    val weekOprEtime: String? = null,
    val weekCellStime: String? = null,
    val weekCellEtime: String? = null,
    val weekendOprStime: String? = null,
    val weekendOprEtime: String? = null,
    val weekendCellStime: String? = null,
    val weekendCellEtime: String? = null,
    val closeDay: String? = null,
    @Serializable(with = LenientIntSerializer::class) val vetPersonCnt: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val specsPersonCnt: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val medicalCnt: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val breedCnt: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val quarabtineCnt: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val feedCnt: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val transCarCnt: Int? = null,
    val careTel: String? = null,
)
