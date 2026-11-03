package com.cherryzp.animalshelter.data.api.dto

import kotlinx.serialization.Serializable

/**
 * `abandonmentPublic_v2` item. 명세(02-data 1.3)의 필드 64개 전부, 이름은 명세 그대로.
 * 실제 표본에 있던 필드도 데이터 누락을 막으려고 모두 nullable이다.
 */
@Serializable
internal data class AbandonmentItemDto(
    // 식별
    val desertionNo: String? = null,
    val noticeNo: String? = null,
    val noticeSdt: String? = null,
    val noticeEdt: String? = null,
    val rfidCd: String? = null,
    // 접수
    val happenDt: String? = null,
    val happenPlace: String? = null,
    // 동물
    val upKindCd: String? = null,
    val upKindNm: String? = null,
    val kindCd: String? = null,
    val kindNm: String? = null,
    val kindFullNm: String? = null,
    val colorCd: String? = null,
    val age: String? = null,
    val weight: String? = null,
    val sexCd: String? = null,
    val neuterYn: String? = null,
    val specialMark: String? = null,
    val sfeSoci: String? = null,
    val sfeHealth: String? = null,
    val etcBigo: String? = null,
    val vaccinationChk: String? = null,
    val healthChk: String? = null,
    // 상태
    val processState: String? = null,
    val endReason: String? = null,
    val updTm: String? = null,
    // 사진
    val popfile1: String? = null,
    val popfile2: String? = null,
    val popfile3: String? = null,
    val popfile4: String? = null,
    val popfile5: String? = null,
    val popfile6: String? = null,
    val popfile7: String? = null,
    val popfile8: String? = null,
    // 보호소
    val careRegNo: String? = null,
    val careNm: String? = null,
    val careTel: String? = null,
    val careAddr: String? = null,
    val careOwnerNm: String? = null,
    val orgNm: String? = null,
    // 홍보 — 입양절차
    val adptnTitle: String? = null,
    val adptnSDate: String? = null,
    val adptnEDate: String? = null,
    val adptnConditionLimitTxt: String? = null,
    val adptnTxt: String? = null,
    val adptnImg: String? = null,
    // 홍보 — 입양지원
    val sprtTitle: String? = null,
    val sprtSDate: String? = null,
    val sprtEDate: String? = null,
    val sprtConditionLimitTxt: String? = null,
    val sprtTxt: String? = null,
    val sprtImg: String? = null,
    // 홍보 — 봉사안내
    val srvcTitle: String? = null,
    val srvcSDate: String? = null,
    val srvcEDate: String? = null,
    val srvcConditionLimitTxt: String? = null,
    val srvcTxt: String? = null,
    val srvcImg: String? = null,
    // 홍보 — 행사안내
    val evntTitle: String? = null,
    val evntSDate: String? = null,
    val evntEDate: String? = null,
    val evntConditionLimitTxt: String? = null,
    val evntTxt: String? = null,
    val evntImg: String? = null,
)
