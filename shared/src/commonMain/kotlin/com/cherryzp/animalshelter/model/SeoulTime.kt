package com.cherryzp.animalshelter.model

import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlinx.datetime.todayIn

/**
 * 서울 시간(UTC+9 고정, D2). Wasm/JS에는 시간대 DB가 없어 `TimeZone.of("Asia/Seoul")`을 쓰지 않는다.
 * 한국은 1988년 뒤 일광절약시간이 없으므로 고정 오프셋이 정확하다.
 */
val SeoulTimeZone: TimeZone = UtcOffset(hours = 9).asTimeZone()

/** 서울 기준 오늘. 상태 판정의 `today`로 넘긴다. */
fun Clock.todayInSeoul(): LocalDate = todayIn(SeoulTimeZone)
