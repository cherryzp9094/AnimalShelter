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

        state == ENDED -> AnimalStatus.Ended(reason = null)

        state.startsWith("$ENDED(") ->
            AnimalStatus.Ended(
                reason = state.removePrefix("$ENDED(").removeSuffix(")").trim().ifEmpty {
                    null
                }
            )

        else -> AnimalStatus.Unknown(raw = processState?.ifBlank { null })
    }
}

/** [Animal]의 상태. */
fun Animal.statusOn(today: LocalDate): AnimalStatus = animalStatusOf(processState, noticeEnd, today)
