package com.cherryzp.animalshelter.data.repository

import com.cherryzp.animalshelter.data.api.ApiError
import com.cherryzp.animalshelter.data.store.CachedTable
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 코드표·보호소 전체 캐시의 유효 기간(02-data 8절). */
internal val CACHE_TTL: Duration = 7.days

/** 갱신 한 번의 결과. 오류·0건은 저장하지 않는다. */
internal sealed interface RefreshOutcome {
    data object Saved : RefreshOutcome

    data object Empty : RefreshOutcome

    data class Failed(val error: ApiError) : RefreshOutcome
}

internal fun CachedTable<*>.isFresh(clock: Clock): Boolean =
    clock.now() - Instant.fromEpochMilliseconds(fetchedAtMillis) < CACHE_TTL

/**
 * 같은 키의 갱신은 동시에 하나만 돈다. 뒤에 온 호출은 진행 중인 것의 결과를 기다린다.
 * 갱신은 앱 범위에서 돌아 부른 쪽이 취소돼도 끝까지 가서 캐시를 채운다.
 */
internal class SingleFlight<K>(private val scope: CoroutineScope) {
    private val mutex = Mutex()
    private val running = mutableMapOf<K, Deferred<RefreshOutcome>>()

    suspend fun run(key: K, block: suspend () -> RefreshOutcome): RefreshOutcome {
        val deferred = mutex.withLock {
            running[key]?.takeIf { it.isActive }
                ?: scope.async { block() }.also { running[key] = it }
        }
        return deferred.await()
    }
}
