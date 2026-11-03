package com.cherryzp.animalshelter.fake

import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.data.repository.Settings
import com.cherryzp.animalshelter.data.repository.SettingsRepository
import com.cherryzp.animalshelter.model.ListLayout
import com.cherryzp.animalshelter.model.SearchFilter
import com.cherryzp.animalshelter.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** 가짜 설정 Repository. [settings]를 직접 바꿀 수 있고 저장 호출은 그 값에 반영된다. */
class FakeSettingsRepository(initial: Settings = Settings()) : SettingsRepository {
    val settings = MutableStateFlow(initial)
    val savedFilters = mutableListOf<SearchFilter>()

    override fun getSettingsStream(): Flow<Settings> = settings

    override suspend fun saveLastFilter(filter: SearchFilter) {
        savedFilters += filter
        settings.value = settings.value.copy(lastFilter = filter)
    }

    override suspend fun setTheme(theme: ThemeMode) {
        settings.value = settings.value.copy(theme = theme)
    }

    override suspend fun setListLayout(layout: ListLayout) {
        settings.value = settings.value.copy(listLayout = layout)
    }

    override suspend fun setEndpointOverride(endpoint: ApiEndpoint?) {
        val stored = endpoint.takeIf { it is ApiEndpoint.Relay || it is ApiEndpoint.Fixture }
        settings.value = settings.value.copy(endpointOverride = stored)
    }
}
