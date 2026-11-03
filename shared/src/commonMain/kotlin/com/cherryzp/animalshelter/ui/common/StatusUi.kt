package com.cherryzp.animalshelter.ui.common

import androidx.compose.runtime.Immutable
import com.cherryzp.animalshelter.model.AnimalStatus
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.a11y_days_left
import com.cherryzp.animalshelter.resources.a11y_ends_today
import com.cherryzp.animalshelter.resources.badge_closing_soon
import com.cherryzp.animalshelter.resources.dday
import com.cherryzp.animalshelter.resources.dday_today
import com.cherryzp.animalshelter.resources.status_ended
import com.cherryzp.animalshelter.resources.status_ended_reason
import com.cherryzp.animalshelter.resources.status_protecting
import com.cherryzp.animalshelter.resources.status_unknown

/** 배지 갈래(색). 마감 임박 = 공고중 D-3 이하(오늘 마감 포함). */
enum class StatusKind { NOTICING, CLOSING_SOON, PROTECTING, ENDED, UNKNOWN }

/** 상태 배지 하나. `label`은 배지 글자, `a11y`는 카드 접근성 문장에 들어가는 말. */
@Immutable
data class StatusUi(val kind: StatusKind, val label: UiText, val a11y: UiText)

/** 마감 임박으로 보는 남은 날(D-3까지, Phase 0 결정). */
const val CLOSING_SOON_DAYS = 3

/**
 * 상태 → 배지(브리핑 P9, 05 2.2). 공고중 D-4 이상 "D-n", D-1~D-3 "마감 임박 · D-n", 0일 "오늘 마감",
 * 보호중, 종료(사유 원문 또는 "종료"), 미상(원문 또는 "상태 미상"). 종료의 뜻은 꾸미지 않는다.
 */
fun AnimalStatus.toStatusUi(): StatusUi = when (this) {
    is AnimalStatus.Noticing -> when {
        daysLeft <= 0 -> StatusUi(
            StatusKind.CLOSING_SOON,
            uiText(Res.string.dday_today),
            uiText(Res.string.a11y_ends_today)
        )

        daysLeft <= CLOSING_SOON_DAYS -> StatusUi(
            StatusKind.CLOSING_SOON,
            uiText(Res.string.badge_closing_soon, daysLeft),
            pluralText(Res.plurals.a11y_days_left, daysLeft)
        )

        else -> StatusUi(
            StatusKind.NOTICING,
            uiText(Res.string.dday, daysLeft),
            pluralText(Res.plurals.a11y_days_left, daysLeft)
        )
    }

    AnimalStatus.Protecting -> uiText(Res.string.status_protecting).let {
        StatusUi(StatusKind.PROTECTING, it, it)
    }

    is AnimalStatus.Ended -> (
        reason?.let { uiText(Res.string.status_ended_reason, it) }
            ?: uiText(Res.string.status_ended)
        ).let { StatusUi(StatusKind.ENDED, it, it) }

    is AnimalStatus.Unknown -> (raw?.let { UiText.Raw(it) } ?: uiText(Res.string.status_unknown))
        .let { StatusUi(StatusKind.UNKNOWN, it, it) }
}
