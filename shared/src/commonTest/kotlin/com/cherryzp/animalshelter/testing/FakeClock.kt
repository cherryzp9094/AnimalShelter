package com.cherryzp.animalshelter.testing

import kotlin.concurrent.Volatile
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/** 테스트용 시계. 기본값 2026-11-04 10:00 KST. */
class FakeClock(start: Instant = Instant.parse("2026-11-04T01:00:00Z")) : Clock {
    @Volatile
    var current: Instant = start

    override fun now(): Instant = current

    fun advance(duration: Duration) {
        current += duration
    }
}
