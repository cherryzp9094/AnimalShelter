package com.cherryzp.animalshelter.ui.common

import androidx.compose.runtime.Immutable
import com.cherryzp.animalshelter.model.AnimalKind
import kotlinx.datetime.LocalDate

/**
 * 카드 하나(목록·북마크 공용, 03 2.1·브리핑 A10). 문구는 [UiText]로 — 화면이 푼다.
 * @property photoUrl 화면용 URL(`ImageUrlPolicy.displayUrl`, 엔드포인트 반영). 사진이 없으면 null
 * @property title 제목 부분(P10) — 있는 것만, 화면이 " · "로 잇는다
 * @property traits 특징 부분(색상 · 체중 · 특징)
 */
@Immutable
data class AnimalCardUi(
    val desertionNo: String,
    val happenDate: LocalDate?,
    val photoUrl: String?,
    val kind: AnimalKind?,
    val status: StatusUi,
    val title: List<UiText>,
    val shelterLine: String?,
    val traits: List<UiText>,
    val happenPlace: String?,
    val isBookmarked: Boolean,
)
