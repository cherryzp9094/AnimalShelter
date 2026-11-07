package com.cherryzp.animalshelter.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/** 공고 상태 판정 결과(02-data 5.2, 05-service 2.2). */
sealed interface AnimalStatus {
    /** 보호중이고 공고 종료일이 오늘 이후. `daysLeft` 0 = 오늘 마감. */
    data class Noticing(val daysLeft: Int) : AnimalStatus

    /** 보호중이고 공고 기간이 지났다(= API `state=protect`). */
    data object Protecting : AnimalStatus

    /** `종료(…)`. `reason`은 괄호 안 원문(`종료`만이면 null). */
    data class Ended(val reason: String?) : AnimalStatus

    /** 그 밖의 값·빈 값. */
    data class Unknown(val raw: String?) : AnimalStatus
}

private const val PROTECTING = "보호중"
private const val ENDED = "종료"

/**
 * 상태를 판정한다. `today`는 부르는 쪽이 `clock.todayInSeoul()`로 넘긴다.
 * 종료의 뜻을 추측하는 가공은 하지 않는다(05-service 2.3).
 */
fun animalStatusOf(processState: String?, noticeEnd: LocalDate?, today: LocalDate): AnimalStatus {
    val state = processState?.trim().orEmpty()
    return when {
        state == PROTECTING && noticeEnd != null && noticeEnd >= today ->
            AnimalStatus.Noticing(daysLeft = today.daysUntil(noticeEnd))

        state == PROTECTING -> AnimalStatus.Protecting

        else -> endedOf(state) ?: AnimalStatus.Unknown(raw = processState?.ifBlank { null })
    }
}

/**
 * `종료` 갈래의 해석 — 앞뒤 공백을 뗀 [state]가 `종료`면 사유 없음, `종료(`로 시작하면 괄호 안(끝 `)`는 있으면 떼고
 * 공백도 뗀다, 비면 사유 없음), 그 밖은 null. 배지([animalStatusOf])와 숨김([isEndedByDeath])이 같은 해석을 쓴다(5d).
 */
private fun endedOf(state: String): AnimalStatus.Ended? = when {
    state == ENDED -> AnimalStatus.Ended(reason = null)

    state.startsWith("$ENDED(") ->
        AnimalStatus.Ended(
            reason = state.removePrefix("$ENDED(").removeSuffix(")").trim().ifEmpty { null }
        )

    else -> null
}

/** 사망으로 끝난 종료 사유 — `processState` 괄호 안의 공식 값(실측 7종 중 둘, survey 1·8절). 5d. */
internal val DEATH_END_REASONS: Set<String> = setOf("자연사", "안락사")

/**
 * 사망 종료(`종료(자연사)`·`종료(안락사)`)인지(5d — 기본으로 숨기는 대상). 날짜와 무관하고 `endReason`(처분
 * 사유 문장)은 보지 않는다. 관찰되지 않은 사유(`폐사` 등)는 추측하지 않는다(05 2.3 1번).
 */
fun isEndedByDeath(processState: String?): Boolean =
    endedOf(processState?.trim().orEmpty())?.reason?.let { it in DEATH_END_REASONS } == true

/** [Animal]이 사망 종료인지(5d). */
fun Animal.isEndedByDeath(): Boolean = isEndedByDeath(processState)

/** [Animal]의 상태. */
fun Animal.statusOn(today: LocalDate): AnimalStatus = animalStatusOf(processState, noticeEnd, today)
