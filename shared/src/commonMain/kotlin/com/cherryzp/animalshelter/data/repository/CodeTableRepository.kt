package com.cherryzp.animalshelter.data.repository

import com.cherryzp.animalshelter.data.api.ApiError
import com.cherryzp.animalshelter.model.AnimalKind
import com.cherryzp.animalshelter.model.Kind
import com.cherryzp.animalshelter.model.ShelterCode
import com.cherryzp.animalshelter.model.Sido
import com.cherryzp.animalshelter.model.Sigungu
import kotlinx.coroutines.flow.Flow

/**
 * 캐시된 값의 상태(stale-while-revalidate).
 * - `data == null && error == null && !fresh` → 첫 로딩 중
 * - `fresh = false`이고 `data`가 있으면 만료된 캐시(뒤에서 갱신 중이거나 갱신 실패)
 * - `error`는 마지막 갱신 실패(캐시는 그대로 둔다)
 */
data class CacheState<T>(val data: T?, val fresh: Boolean, val error: ApiError?)

/** 코드표 종류. [CodeTableRepository.refresh]의 대상. */
sealed interface CodeTable {
    data object Sido : CodeTable

    data class Sigungu(val sidoCode: String) : CodeTable

    data class Shelters(val sidoCode: String, val sigunguCode: String) : CodeTable

    data class Kinds(val kind: AnimalKind) : CodeTable
}

/**
 * 코드표(시도·시군구·보호소·품종). TTL 7일, 만료돼도 먼저 내보내고 뒤에서 갱신한다(같은 표는 동시에 하나만).
 * 오류·0건 응답은 저장하지 않는다(02-data 8절). 0건이면 `CacheState(emptyList(), fresh = true, null)`.
 */
interface CodeTableRepository {
    fun getSidoStream(): Flow<CacheState<List<Sido>>>

    fun getSigunguStream(sidoCode: String): Flow<CacheState<List<Sigungu>>>

    fun getSheltersStream(
        sidoCode: String,
        sigunguCode: String,
    ): Flow<CacheState<List<ShelterCode>>>

    fun getKindsStream(kind: AnimalKind): Flow<CacheState<List<Kind>>>

    /** 만료와 무관하게 다시 받는다. 결과는 스트림으로 나간다. */
    suspend fun refresh(table: CodeTable)
}
