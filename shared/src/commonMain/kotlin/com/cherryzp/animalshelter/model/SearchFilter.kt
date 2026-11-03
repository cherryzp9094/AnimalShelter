package com.cherryzp.animalshelter.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * 목록 조회 조건(02-data 4절). 전부 비어 있으면 "전체"(API 기본 창 = 접수일 최근 약 1개월).
 * 마지막 조건으로 설정에 JSON으로 저장하므로 `@Serializable`(D12).
 *
 * `kindName`·`sidoName`·`sigunguName`·`shelterName`은 요약 칩에 보일 표시 이름이다(조회 파라미터가 아니다 —
 * `toQuery`가 무시한다). 필터 화면이 코드를 고를 때 함께 채워, 목록이 코드표를 부르지 않게 한다.
 * 옛 저장 JSON(이름 없음)은 null로 읽힌다.
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
    /** 품종 표시 이름(`kindCode`의 이름). */
    val kindName: String? = null,
    /** 시도 표시 이름(`sidoCode`의 이름). */
    val sidoName: String? = null,
    /** 시군구 표시 이름(`sigunguCode`의 이름). */
    val sigunguName: String? = null,
    /** 보호소 표시 이름(`shelterRegNo`의 이름). */
    val shelterName: String? = null,
)
