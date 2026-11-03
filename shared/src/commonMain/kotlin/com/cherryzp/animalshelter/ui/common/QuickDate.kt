package com.cherryzp.animalshelter.ui.common

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/** 날짜 빠른 칩(03 2.2, 브리핑 A18): 최근 7일 = 오늘 − 6일 ~ 오늘, 최근 30일 = 오늘 − 29일 ~ 오늘. */
enum class QuickDate(val days: Int) {
    LAST_7(7),
    LAST_30(30),
}

/** 이 빠른 칩의 시작·종료(서울 기준 오늘). */
fun QuickDate.rangeOn(today: LocalDate): Pair<LocalDate, LocalDate> =
    today.minus(DatePeriod(days = days - 1)) to today

/** 시작·종료가 빠른 칩 하나와 같으면 그것, 아니면 null. */
fun quickDateOf(from: LocalDate?, to: LocalDate?, today: LocalDate): QuickDate? {
    if (from == null || to == null) return null
    return QuickDate.entries.firstOrNull { it.rangeOn(today) == (from to to) }
}
