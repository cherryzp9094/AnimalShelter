package com.cherryzp.animalshelter.data.api

import com.cherryzp.animalshelter.data.repository.clean
import com.cherryzp.animalshelter.model.SearchFilter
import kotlinx.datetime.LocalDate

/** 날짜 파라미터 `YYYYMMDD`. */
internal fun LocalDate.toBasicString(): String = LocalDate.Formats.ISO_BASIC.format(this)

/**
 * 목록 조건 → 쿼리(02-data 1.1). 빈 값은 붙이지 않는다(명세: 전체 = 빈 값).
 * 축종은 `upkind`(품종 조회의 `up_kind_cd`와 다르다).
 */
internal fun SearchFilter.toQuery(): List<Pair<String, String>> = buildList {
    dateFrom?.let { add("bgnde" to it.toBasicString()) }
    dateTo?.let { add("endde" to it.toBasicString()) }
    kind?.let { add("upkind" to it.code) }
    kindCode.clean()?.let { add("kind" to it) }
    sidoCode.clean()?.let { add("upr_cd" to it) }
    sigunguCode.clean()?.let { add("org_cd" to it) }
    shelterRegNo.clean()?.let { add("care_reg_no" to it) }
    state.param.ifEmpty { null }?.let { add("state" to it) }
    neutered?.let { add("neuter_yn" to it.code) }
    sex?.let { add("sex_cd" to it.code) }
}

internal fun paging(pageNo: Int, numOfRows: Int): List<Pair<String, String>> =
    listOf("pageNo" to pageNo.toString(), "numOfRows" to numOfRows.toString())
