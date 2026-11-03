package com.cherryzp.animalshelter.fake

import com.cherryzp.animalshelter.data.repository.CacheState
import com.cherryzp.animalshelter.data.repository.CodeTable
import com.cherryzp.animalshelter.data.repository.CodeTableRepository
import com.cherryzp.animalshelter.model.AnimalKind
import com.cherryzp.animalshelter.model.Kind
import com.cherryzp.animalshelter.model.ShelterCode
import com.cherryzp.animalshelter.model.Sido
import com.cherryzp.animalshelter.model.Sigungu
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** 가짜 코드표 Repository. 표마다 상태를 직접 넣는다(기본: 첫 로딩 중). */
class FakeCodeTableRepository : CodeTableRepository {
    val sido = MutableStateFlow(loading<List<Sido>>())
    private val sigungu = mutableMapOf<String, MutableStateFlow<CacheState<List<Sigungu>>>>()
    private val shelters = mutableMapOf<String, MutableStateFlow<CacheState<List<ShelterCode>>>>()
    private val kinds = mutableMapOf<AnimalKind, MutableStateFlow<CacheState<List<Kind>>>>()
    val refreshCalls = mutableListOf<CodeTable>()

    fun sigunguState(sidoCode: String) = sigungu.getOrPut(sidoCode) { MutableStateFlow(loading()) }

    fun sheltersState(sidoCode: String, sigunguCode: String) =
        shelters.getOrPut("$sidoCode/$sigunguCode") { MutableStateFlow(loading()) }

    fun kindsState(kind: AnimalKind) = kinds.getOrPut(kind) { MutableStateFlow(loading()) }

    override fun getSidoStream(): Flow<CacheState<List<Sido>>> = sido

    override fun getSigunguStream(sidoCode: String): Flow<CacheState<List<Sigungu>>> =
        sigunguState(sidoCode)

    override fun getSheltersStream(
        sidoCode: String,
        sigunguCode: String,
    ): Flow<CacheState<List<ShelterCode>>> = sheltersState(sidoCode, sigunguCode)

    override fun getKindsStream(kind: AnimalKind): Flow<CacheState<List<Kind>>> = kindsState(kind)

    override suspend fun refresh(table: CodeTable) {
        refreshCalls += table
    }
}

/** 첫 로딩 중 상태. */
fun <T> loading(): CacheState<T> = CacheState(data = null, fresh = false, error = null)

/** 신선한 캐시 상태. */
fun <T> loaded(data: T): CacheState<T> = CacheState(data = data, fresh = true, error = null)
