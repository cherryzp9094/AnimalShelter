package com.cherryzp.animalshelter.ui.shelter

import com.cherryzp.animalshelter.model.ShelterInfo
import com.cherryzp.animalshelter.model.TimeRange
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.hours_all_day
import com.cherryzp.animalshelter.resources.hours_range
import com.cherryzp.animalshelter.resources.today_adoption
import com.cherryzp.animalshelter.resources.today_closed
import com.cherryzp.animalshelter.resources.today_hours
import com.cherryzp.animalshelter.ui.common.UiText
import com.cherryzp.animalshelter.ui.common.uiText
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

// 보호소 운영시간 문구(브리핑 B7, 순수 함수). 상세의 보호소 카드와 보호소 정보 화면이 같이 쓴다.

/** 운영·분양 시간 한 칸: `00:00~24:00`이면 "24시간", 아니면 "09:00~18:00". */
fun TimeRange.toUiText(): UiText =
    if (isAllDay) uiText(Res.string.hours_all_day) else uiText(Res.string.hours_range, start, end)

/** 토·일이면 주말 값을 쓴다. 공휴일은 데이터가 없어 다루지 않는다. */
fun LocalDate.isWeekend(): Boolean =
    dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY

/**
 * 오늘(서울) 요약의 부분: "오늘 09:00~18:00" · "분양 13:00~15:00" · "휴무 일요일" — 있는 것만.
 * 화면이 " · "로 잇는다(빈 칸이 생기지 않게 인자 셋짜리 리소스를 쓰지 않는다).
 */
fun ShelterInfo.todayParts(today: LocalDate): List<UiText> = buildList {
    val weekend = today.isWeekend()
    (if (weekend) weekendHours else weekdayHours)?.let {
        add(uiText(Res.string.today_hours, it.toUiText()))
    }
    (if (weekend) weekendAdoptionHours else weekdayAdoptionHours)?.let {
        add(uiText(Res.string.today_adoption, it.toUiText()))
    }
    closedDay?.trim()?.takeIf { it.isNotEmpty() }?.let { add(uiText(Res.string.today_closed, it)) }
}
