package com.cherryzp.animalshelter.data.repository

import com.cherryzp.animalshelter.data.api.AnimalShelterApi
import com.cherryzp.animalshelter.data.api.ApiResult
import com.cherryzp.animalshelter.data.store.CachedTable
import com.cherryzp.animalshelter.data.store.ShelterCache
import com.cherryzp.animalshelter.data.store.ShelterStore
import com.cherryzp.animalshelter.model.ShelterInfo
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** 보호소 정보 Repository. 전체 캐시(`shelters.json`)에서 등록번호로 찾는다. */
internal class DefaultShelterRepository(
    private val api: AnimalShelterApi,
    private val store: ShelterStore,
    private val clock: Clock,
    private val appScope: CoroutineScope,
) : ShelterRepository {
    private val outcome = MutableStateFlow<RefreshOutcome?>(null)
    private val singleFlight = SingleFlight<Unit>(appScope)

    override fun getShelterInfoStream(regNo: String): Flow<CacheState<ShelterInfo?>> = flow {
        val cached = store.data.first().table
        if (cached == null || !cached.isFresh(clock)) {
            outcome.value = null
            appScope.launch { refreshAll() }
        }
        val states =
            combine(store.data.map { it.table }.distinctUntilChanged(), outcome) { table, last ->
                toState(table, last, regNo)
            }
        emitAll(states)
    }.distinctUntilChanged()

    override suspend fun refresh() {
        outcome.value = null
        refreshAll()
    }

    private fun toState(
        table: CachedTable<ShelterInfo>?,
        last: RefreshOutcome?,
        regNo: String,
    ): CacheState<ShelterInfo?> = when {
        table != null -> CacheState(
            data = table.items.firstOrNull { it.regNo == regNo },
            fresh = table.isFresh(clock),
            error = (last as? RefreshOutcome.Failed)?.error
        )

        last is RefreshOutcome.Empty -> CacheState(null, fresh = true, error = null)

        last is RefreshOutcome.Failed -> CacheState(null, fresh = false, error = last.error)

        else -> CacheState(null, fresh = false, error = null)
    }

    private suspend fun refreshAll() {
        outcome.value = singleFlight.run(Unit) {
            when (val result = api.getAllShelterInfo()) {
                is ApiResult.Failure -> RefreshOutcome.Failed(result.error)

                is ApiResult.Success -> if (result.value.isEmpty()) {
                    RefreshOutcome.Empty
                } else {
                    val table = CachedTable(result.value, clock.now().toEpochMilliseconds())
                    saveQuietly { store.update { ShelterCache(table) } }
                    RefreshOutcome.Saved
                }
            }
        }
    }
}
