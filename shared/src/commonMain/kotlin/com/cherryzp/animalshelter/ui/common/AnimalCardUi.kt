package com.cherryzp.animalshelter.ui.common

import androidx.compose.runtime.Immutable
import com.cherryzp.animalshelter.model.AnimalKind
import com.cherryzp.animalshelter.model.ListLayout
import kotlinx.datetime.LocalDate

/**
 * 카드 하나(목록·북마크 공용, 03 2.1·브리핑 A10, 중간 검토 F1). 문구는 [UiText]로 — 화면이 푼다.
 * 글자 행: 1행 [breed] / 2행 [facts] / 3행 [shelterLine] / (1열만) 4행 [appearance] · 5행 [happenPlace].
 * @property photoUrl 화면용 URL(`ImageUrlPolicy.displayUrl`, 엔드포인트 반영). 사진이 없으면 null
 * @property breed 1행 품종명. 없으면 행을 뺀다
 * @property facts 2행 부분(성별 · 나이 · 체중) — 있는 것만, 화면이 " · "로 잇는다
 * @property appearance 4행 부분(색상 · 특징) — 1열 카드만 보인다
 * @property titleParts 접근성 문장의 제목 부분(P10 — 품종 · 성별 · 나이)
 */
@Immutable
data class AnimalCardUi(
    val desertionNo: String,
    val happenDate: LocalDate?,
    val photoUrl: String?,
    val kind: AnimalKind?,
    val status: StatusUi,
    val breed: String?,
    val facts: List<UiText>,
    val shelterLine: String?,
    val appearance: List<UiText>,
    val happenPlace: String?,
    val titleParts: List<UiText>,
    val isBookmarked: Boolean,
)

/** 카드 글자 행(중간 검토 F1). */
enum class CardRow { BREED, FACTS, SHELTER, APPEARANCE, HAPPEN_PLACE }

/**
 * 이 형태에서 보일 글자 행(값이 있는 것만, 위에서 아래 순서). 2열([ListLayout.GRID])은 품종 / 성별 · 나이 · 체중 /
 * 보호소 줄까지, 1열([ListLayout.LARGE])은 색상 · 특징과 발견 장소를 더한다.
 */
fun AnimalCardUi.visibleRows(layout: ListLayout): List<CardRow> = buildList {
    if (breed != null) add(CardRow.BREED)
    if (facts.isNotEmpty()) add(CardRow.FACTS)
    if (shelterLine != null) add(CardRow.SHELTER)
    if (layout == ListLayout.LARGE) {
        if (appearance.isNotEmpty()) add(CardRow.APPEARANCE)
        if (happenPlace != null) add(CardRow.HAPPEN_PLACE)
    }
}
