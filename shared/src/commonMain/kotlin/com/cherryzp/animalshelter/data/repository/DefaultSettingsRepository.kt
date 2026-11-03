package com.cherryzp.animalshelter.data.repository

import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.data.store.SettingsStore
import com.cherryzp.animalshelter.model.ListLayout
import com.cherryzp.animalshelter.model.SearchFilter
import com.cherryzp.animalshelter.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/** 설정 Repository. [SettingsStore]를 그대로 감싼다. */
internal class DefaultSettingsRepository(private val store: SettingsStore) : SettingsRepository {
    override fun getSettingsStream(): Flow<Settings> = store.settings.distinctUntilChanged()

    override suspend fun saveLastFilter(filter: SearchFilter) = store.saveLastFilter(filter)

    override suspend fun setTheme(theme: ThemeMode) = store.setTheme(theme)

    override suspend fun setListLayout(layout: ListLayout) = store.setListLayout(layout)

    override suspend fun setEndpointOverride(endpoint: ApiEndpoint?) =
        store.setEndpointOverride(endpoint)
}
