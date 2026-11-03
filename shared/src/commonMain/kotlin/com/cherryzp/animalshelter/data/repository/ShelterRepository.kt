package com.cherryzp.animalshelter.data.repository

import com.cherryzp.animalshelter.model.ShelterInfo
import kotlinx.coroutines.flow.Flow

/**
 * 보호소 정보(`shelterInfo_v2` 전체 캐시, TTL 7일, stale-while-revalidate).
 * 전체 목록에 없는 보호소는 `data = null`(받은 뒤라 `fresh = true`) — 화면은 목록의 요약으로 줄인다.
 */
interface ShelterRepository {
    fun getShelterInfoStream(regNo: String): Flow<CacheState<ShelterInfo?>>

    /** 전체를 다시 받는다(`getAllShelterInfo` 1회, 동시에 하나만). */
    suspend fun refresh()
}
