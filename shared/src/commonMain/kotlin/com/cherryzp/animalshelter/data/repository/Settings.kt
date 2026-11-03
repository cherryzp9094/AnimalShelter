package com.cherryzp.animalshelter.data.repository

import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.model.ListLayout
import com.cherryzp.animalshelter.model.SearchFilter
import com.cherryzp.animalshelter.model.ThemeMode

/**
 * 사용자 설정(02-data 8절). 저장값이 없거나 모르는 값이면 기본값.
 * `endpointOverride`는 디버그 빌드에서만 쓰인다(D3, [com.cherryzp.animalshelter.data.api.EndpointSelector]).
 */
data class Settings(
    val lastFilter: SearchFilter? = null,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val listLayout: ListLayout = ListLayout.GRID,
    val endpointOverride: ApiEndpoint? = null,
)
