package com.cherryzp.animalshelter.model

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * 구조동물 한 마리(02-data 4절). 북마크 스냅샷으로 그대로 저장하므로 `@Serializable`이다.
 * 저장값이 옛 형식이어도 읽히도록 `desertionNo` 말고는 전부 기본값이 있다.
 */
@Serializable
data class Animal(
    /** 구조번호(15자리). 북마크 키. */
    val desertionNo: String,
    val noticeNo: String? = null,
    val noticeStart: LocalDate? = null,
    val noticeEnd: LocalDate? = null,
    /** 접수일. 재조회 창(02-data 6절)에도 쓴다. */
    val happenDate: LocalDate? = null,
    val happenPlace: String? = null,
    val kind: AnimalKind? = null,
    val kindCode: String? = null,
    val kindName: String? = null,
    val kindFullName: String? = null,
    val color: String? = null,
    /** 나이 원문(예 `2024(년생)`). */
    val ageText: String? = null,
    val birthYear: Int? = null,
    /** `2026(60일미만)(년생)`처럼 60일 미만이면 true. */
    val isUnderTwoMonths: Boolean = false,
    /** 체중 원문(예 `5(Kg)`). */
    val weightText: String? = null,
    /** 체중(kg). 읽을 수 없거나 0·100kg 초과면 null. */
    val weightKg: Double? = null,
    val sex: Sex? = null,
    val neutered: Neutered? = null,
    val specialMark: String? = null,
    /** 특징(사회성) `sfeSoci`. */
    val sociability: String? = null,
    /** 특징(건강) `sfeHealth`. */
    val health: String? = null,
    val vaccinationChecks: List<String> = emptyList(),
    val healthChecks: List<String> = emptyList(),
    /** 상태 원문(`보호중`, `종료(반환)` …). 판정은 [animalStatusOf]. */
    val processState: String? = null,
    val endReason: String? = null,
    /** 수정 시각 원문(`2026-11-03 14:09:49.0`). */
    val updatedAt: String? = null,
    /** 사진 원본 URL(https로 정규화, 중복 제거). 화면에 낼 때는 `ImageUrlPolicy.displayUrl`. */
    val photos: List<String> = emptyList(),
    val shelter: ShelterSummary = ShelterSummary(),
    val promotions: List<Promotion> = emptyList(),
    val rfid: String? = null,
)

/** 목록이 주는 보호소 요약. 대표자 이름(`careOwnerNm`)은 두지 않는다(05-service 2.3). */
@Serializable
data class ShelterSummary(
    val regNo: String? = null,
    val name: String? = null,
    val tel: String? = null,
    val address: String? = null,
    val orgName: String? = null,
)

/** 홍보 묶음 하나. 제목이 없으면 만들지 않는다. */
@Serializable
data class Promotion(
    val type: PromotionType,
    val title: String? = null,
    val start: String? = null,
    val end: String? = null,
    val condition: String? = null,
    val text: String? = null,
    val imageUrl: String? = null,
)

/**
 * 목록 한 페이지. `hasMore`는 받은 페이지가 비지 않았고 `pageNo * numOfRows < totalCount`일 때만 참(D9).
 */
data class AnimalPage(
    val items: List<Animal>,
    val pageNo: Int,
    val numOfRows: Int,
    val totalCount: Int,
) {
    val hasMore: Boolean get() = items.isNotEmpty() && pageNo * numOfRows < totalCount
}

/** 북마크 한 건. `savedAt`은 처음 저장한 시각(스냅샷을 갱신해도 그대로). */
data class Bookmark(val animal: Animal, val savedAt: Instant)
