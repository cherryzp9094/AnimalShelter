package com.cherryzp.animalshelter.data.repository

import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.model.ListLayout
import com.cherryzp.animalshelter.model.SearchFilter
import com.cherryzp.animalshelter.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/** 사용자 설정(02-data 8·9절). */
interface SettingsRepository {
    fun getSettingsStream(): Flow<Settings>

    suspend fun saveLastFilter(filter: SearchFilter)

    suspend fun setTheme(theme: ThemeMode)

    suspend fun setListLayout(layout: ListLayout)

    /** 디버그 엔드포인트 덮어쓰기. Relay·Fixture만 저장하고 Direct·null은 지운다(D3). */
    suspend fun setEndpointOverride(endpoint: ApiEndpoint?)
}
