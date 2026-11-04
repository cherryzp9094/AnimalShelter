package com.cherryzp.animalshelter.data.repository

import com.cherryzp.animalshelter.data.api.AnimalShelterApi
import com.cherryzp.animalshelter.data.api.ApiResult
import com.cherryzp.animalshelter.data.store.CachedTable
import com.cherryzp.animalshelter.data.store.CodeTableCache
import com.cherryzp.animalshelter.data.store.CodeTableStore
import com.cherryzp.animalshelter.model.AnimalKind
import com.cherryzp.animalshelter.model.Kind
import com.cherryzp.animalshelter.model.ShelterCode
import com.cherryzp.animalshelter.model.Sido
import com.cherryzp.animalshelter.model.Sigungu
import kotlin.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 코드표 Repository(02-data 8·9절). 스트림을 모으기 시작할 때 캐시가 없거나 만료면 뒤에서 갱신한다. */
internal class DefaultCodeTableRepository(
    private val api: AnimalShelterApi,
    private val store: CodeTableStore,
    private val clock: Clock,
    private val appScope: CoroutineScope,
) : CodeTableRepository {
    private val outcomes = MutableStateFlow<Map<CodeTable, RefreshOutcome>>(emptyMap())
    private val singleFlight = SingleFlight<CodeTable>(appScope)

    override fun getSidoStream(): Flow<CacheState<List<Sido>>> = stream(CodeTable.Sido, sidoSpec())

    override fun getSigunguStream(sidoCode: String): Flow<CacheState<List<Sigungu>>> =
        stream(CodeTable.Sigungu(sidoCode), sigunguSpec(sidoCode))

    override fun getSheltersStream(
        sidoCode: String,
        sigunguCode: String,
    ): Flow<CacheState<List<ShelterCode>>> =
        stream(CodeTable.Shelters(sidoCode, sigunguCode), sheltersSpec(sidoCode, sigunguCode))

    override fun getKindsStream(kind: AnimalKind): Flow<CacheState<List<Kind>>> =
        stream(CodeTable.Kinds(kind), kindsSpec(kind))

    override suspend fun refresh(table: CodeTable) {
        outcomes.update { it - table }
        refreshByTable(table, onlyIfStale = false)
    }

    /** 스트림이 거는 갱신과 같은 길(테스트용): 이미 신선하면 받지 않는다. */
    internal suspend fun refreshIfStale(table: CodeTable) {
        refreshByTable(table, onlyIfStale = true)
    }

    private suspend fun refreshByTable(table: CodeTable, onlyIfStale: Boolean) {
        when (table) {
            CodeTable.Sido -> refreshTable(table, sidoSpec(), onlyIfStale)

            is CodeTable.Sigungu -> refreshTable(table, sigunguSpec(table.sidoCode), onlyIfStale)

            is CodeTable.Shelters -> refreshTable(
                table,
                sheltersSpec(table.sidoCode, table.sigunguCode),
                onlyIfStale
            )

            is CodeTable.Kinds -> refreshTable(table, kindsSpec(table.kind), onlyIfStale)
        }
    }

    /** 표 하나를 어떻게 읽고·받고·쓰는지. */
    private class TableSpec<T>(
        val read: (CodeTableCache) -> CachedTable<T>?,
        val fetch: suspend () -> ApiResult<List<T>>,
        val write: (CodeTableCache, CachedTable<T>) -> CodeTableCache,
    )

    private fun sidoSpec() = TableSpec(
        read = { it.sido },
        fetch = { api.getSido() },
        write = { cache, table -> cache.copy(sido = table) }
    )

    private fun sigunguSpec(sidoCode: String) = TableSpec(
        read = { it.sigungu[sidoCode] },
        fetch = { api.getSigungu(sidoCode) },
        write = { cache, table -> cache.copy(sigungu = cache.sigungu + (sidoCode to table)) }
    )

    private fun sheltersSpec(sidoCode: String, sigunguCode: String): TableSpec<ShelterCode> {
        val key = "$sidoCode/$sigunguCode"
        return TableSpec(
            read = { it.shelters[key] },
            fetch = { api.getShelters(sidoCode, sigunguCode) },
            write = { cache, table -> cache.copy(shelters = cache.shelters + (key to table)) }
        )
    }

    private fun kindsSpec(kind: AnimalKind) = TableSpec(
        read = { it.kinds[kind] },
        fetch = { api.getKinds(kind) },
        write = { cache, table -> cache.copy(kinds = cache.kinds + (kind to table)) }
    )

    private fun <T> stream(table: CodeTable, spec: TableSpec<T>): Flow<CacheState<List<T>>> = flow {
        val cached = spec.read(store.data.first())
        if (cached == null || !cached.isFresh(clock)) {
            outcomes.update { it - table }
            appScope.launch { refreshTable(table, spec, onlyIfStale = true) }
        }
        val states = combine(
            store.data.map(spec.read).distinctUntilChanged(),
            outcomes.map { it[table] }.distinctUntilChanged()
        ) { current, outcome -> toState(current, outcome) }
        emitAll(states)
    }.distinctUntilChanged()

    private fun <T> toState(
        cached: CachedTable<T>?,
        outcome: RefreshOutcome?,
    ): CacheState<List<T>> = when {
        cached != null -> CacheState(
            data = cached.items,
            fresh = cached.isFresh(clock),
            error = (outcome as? RefreshOutcome.Failed)?.error
        )

        outcome is RefreshOutcome.Empty -> CacheState(emptyList(), fresh = true, error = null)

        outcome is RefreshOutcome.Failed -> CacheState(
            null,
            fresh = false,
            error = outcome.error
        )

        else -> CacheState(null, fresh = false, error = null)
    }

    /**
     * 표 하나를 받아 저장한다(같은 표는 동시에 하나만).
     *
     * [onlyIfStale]이면 single-flight 블록 **안에서** 저장소를 다시 읽어 이미 신선하면 받지 않는다(3e 검토 N3).
     * 스트림은 모으기 시작할 때 본 사본으로 갱신을 거는데, 앞 갱신이 저장하기 직전의 사본을 본 스트림이
     * 그 갱신이 끝난 뒤에 블록에 닿으면 같은 표를 한 번 더 받았다. 저장은 앞 갱신이 끝나기 전에 끝나므로
     * 여기서 다시 읽으면 늘 그 결과가 보인다. 사용자 다시 시도([refresh])는 늘 받는다.
     */
    private suspend fun <T> refreshTable(
        table: CodeTable,
        spec: TableSpec<T>,
        onlyIfStale: Boolean,
    ) {
        val outcome = singleFlight.run(table) {
            if (onlyIfStale && spec.read(store.data.first())?.isFresh(clock) == true) {
                return@run RefreshOutcome.Saved
            }
            when (val result = spec.fetch()) {
                is ApiResult.Failure -> RefreshOutcome.Failed(result.error)

                is ApiResult.Success -> if (result.value.isEmpty()) {
                    RefreshOutcome.Empty
                } else {
                    val fresh = CachedTable(result.value, clock.now().toEpochMilliseconds())
                    saveQuietly { store.update { spec.write(it, fresh) } }
                    RefreshOutcome.Saved
                }
            }
        }
        outcomes.update { it + (table to outcome) }
    }
}

/**
 * 캐시 쓰기 실패는 화면을 막지 않는다(다음에 다시 받는다). 코드표·보호소 저장소는 파일 쓰기가 실패해도
 * 메모리 사본을 바꾸므로(`MirroredDataStore.updateKeepingMemoryOnFailure`, 5a-fixes F1) 받은 표는 이 실행
 * 동안 화면에 남고, 파일이 비어 있어 다음 실행에서 다시 받는다. 여기서는 그 예외만 삼킨다.
 */
internal suspend fun saveQuietly(block: suspend () -> Unit) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        // 저장 실패: 캐시 없이 계속 간다
    }
}
