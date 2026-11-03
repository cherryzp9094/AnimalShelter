package com.cherryzp.animalshelter.model

import kotlinx.serialization.Serializable

// 코드 값 enum(02-data 4절). `code`·`param`은 공공데이터 API에 그대로 보내는 값이다.

/** 축종. 목록 API의 `upkind`, 품종 API의 `up_kind_cd`. */
@Serializable
enum class AnimalKind(val code: String) {
    DOG("417000"),
    CAT("422400"),
    OTHER("429900"),
    ;

    companion object {
        /** 모르는 코드·빈 값은 null. */
        fun fromCode(code: String?): AnimalKind? = entries.firstOrNull { it.code == code?.trim() }
    }
}

/** 성별. `sex_cd`(M 수컷 / F 암컷 / Q 미상). */
@Serializable
enum class Sex(val code: String) {
    MALE("M"),
    FEMALE("F"),
    UNKNOWN("Q"),
    ;

    companion object {
        fun fromCode(code: String?): Sex? = entries.firstOrNull { it.code == code?.trim() }
    }
}

/** 중성화 여부. `neuter_yn`(Y / N / U 미상). */
@Serializable
enum class Neutered(val code: String) {
    YES("Y"),
    NO("N"),
    UNKNOWN("U"),
    ;

    companion object {
        fun fromCode(code: String?): Neutered? = entries.firstOrNull { it.code == code?.trim() }
    }
}

/**
 * 공고 상태 조건. `state` 파라미터(전체는 보내지 않는다).
 * 실측: notice = 보호중 & 공고 기간 안, protect = 보호중 & 공고 종료일 지남.
 */
@Serializable
enum class NoticeState(val param: String) {
    ALL(""),
    NOTICE("notice"),
    PROTECT("protect"),
}

/** 목록 정렬. LATEST = API 순서(접수일 내림차순), CLOSING_SOON = 공고중만 모아 공고 종료일 오름차순. */
enum class SortOrder { LATEST, CLOSING_SOON }

/** 목록 보기: 2열 사진 그리드(기본) / 1열 큰 사진. */
enum class ListLayout { GRID, LARGE }

/** 테마 설정. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** 공고의 홍보 묶음: 입양 절차(adptn) / 입양 지원(sprt) / 봉사 안내(srvc) / 행사 안내(evnt). */
@Serializable
enum class PromotionType { ADOPTION_PROCESS, ADOPTION_SUPPORT, VOLUNTEER, EVENT }
