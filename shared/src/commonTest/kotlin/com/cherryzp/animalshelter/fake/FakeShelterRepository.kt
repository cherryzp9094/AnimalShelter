package com.cherryzp.animalshelter.fake

import com.cherryzp.animalshelter.data.repository.CacheState
import com.cherryzp.animalshelter.data.repository.ShelterRepository
import com.cherryzp.animalshelter.model.ShelterInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** 가짜 보호소 Repository. 등록번호마다 상태를 직접 넣는다(기본: 첫 로딩 중). */
class FakeShelterRepository : ShelterRepository {
    private val states = mutableMapOf<String, MutableStateFlow<CacheState<ShelterInfo?>>>()
    var refreshCalls = 0
        private set

    fun state(regNo: String) = states.getOrPut(regNo) { MutableStateFlow(loading()) }

    override fun getShelterInfoStream(regNo: String): Flow<CacheState<ShelterInfo?>> = state(regNo)

    override suspend fun refresh() {
        refreshCalls++
    }
}
