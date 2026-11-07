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
    /**
     * 사망 종료(`종료(자연사)`·`종료(안락사)`)도 결과에 넣는다 — 필터의 "사망으로 끝난 공고도 보기"(5d, 기본 끔).
     * API 파라미터가 아니다. `toQuery`가 무시하고 Repository가 거른다. 옛 저장 JSON(칸 없음)은 false로 읽힌다.
     */
    val includeEndedByDeath: Boolean = false,
)

/**
 * 사망 종료를 실제로 보이는지(5d) — 켰고 상태가 전체일 때만. 상태가 공고중·보호중이면 API가 보호중만 주므로 칸 값은
 * 남기되 뜻이 없다.
 */
val SearchFilter.showsEndedByDeath: Boolean get() = includeEndedByDeath && state == NoticeState.ALL

/** 이 조건의 검색 결과에서 [animal]을 뺄지(5d — 사망 종료이고 [showsEndedByDeath]가 아니면). 목록 Repository가 쓴다. */
fun SearchFilter.hidesEndedByDeath(animal: Animal): Boolean =
    animal.isEndedByDeath() && !showsEndedByDeath
