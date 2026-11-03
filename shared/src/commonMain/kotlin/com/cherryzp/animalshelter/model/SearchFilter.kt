package com.cherryzp.animalshelter.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * 목록 조회 조건(02-data 4절). 전부 비어 있으면 "전체"(API 기본 창 = 접수일 최근 약 1개월).
 * 마지막 조건으로 설정에 JSON으로 저장하므로 `@Serializable`(D12).
 */
@Serializable
data class SearchFilter(
    val kind: AnimalKind? = null,
    /** 품종 코드(`kind`). */
    val kindCode: String? = null,
    val dateFrom: LocalDate? = null,
    val dateTo: LocalDate? = null,
    val sidoCode: String? = null,
    val sigunguCode: String? = null,
    val shelterRegNo: String? = null,
    val state: NoticeState = NoticeState.ALL,
    val neutered: Neutered? = null,
    val sex: Sex? = null,
)
