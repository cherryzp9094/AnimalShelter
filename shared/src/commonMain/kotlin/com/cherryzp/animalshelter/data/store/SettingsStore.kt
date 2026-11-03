package com.cherryzp.animalshelter.data.store

import androidx.datastore.core.Storage
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.data.api.EndpointOverrideSource
import com.cherryzp.animalshelter.data.repository.Settings
import com.cherryzp.animalshelter.model.ListLayout
import com.cherryzp.animalshelter.model.SearchFilter
import com.cherryzp.animalshelter.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 설정(`settings.preferences_pb`, 02-data 8절). 키: `last_filter_json`, `endpoint_override`
 * (`relay:<url>`·`fixture`), `theme`(`system`/`light`/`dark`), `list_layout`(`grid`/`large`).
 * 모르는 값은 기본값. 디버그 엔드포인트 덮어쓰기의 출처이기도 하다([EndpointOverrideSource]).
 */
internal class SettingsStore(storage: Storage<Preferences>, scope: CoroutineScope? = null) :
    EndpointOverrideSource {
    private val dataStore = run {
        val handler = ReplaceFileCorruptionHandler { emptyPreferences() }
        if (scope == null) {
            PreferenceDataStoreFactory.create(storage = storage, corruptionHandler = handler)
        } else {
            PreferenceDataStoreFactory.create(
                storage = storage,
                corruptionHandler = handler,
                scope = scope
            )
        }
    }

    private val preferences: Flow<Preferences> = dataStore.data.catch { emit(emptyPreferences()) }

    val settings: Flow<Settings> = preferences.map { it.toSettings() }

    override suspend fun current(): ApiEndpoint? = preferences.first().toSettings().endpointOverride

    suspend fun saveLastFilter(filter: SearchFilter) = edit {
        it[LAST_FILTER] = StoreJson.encodeToString(SearchFilter.serializer(), filter)
    }

    suspend fun setTheme(theme: ThemeMode) = edit { it[THEME] = theme.name.lowercase() }

    suspend fun setListLayout(layout: ListLayout) = edit { it[LIST_LAYOUT] = layout.storedValue() }

    /** Relay·Fixture만 저장하고 Direct·null은 지운다(D3). */
    suspend fun setEndpointOverride(endpoint: ApiEndpoint?) = edit { prefs ->
        val value = encodeEndpointOverride(endpoint)
        if (value == null) prefs.remove(ENDPOINT_OVERRIDE) else prefs[ENDPOINT_OVERRIDE] = value
    }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        dataStore.edit { block(it) }
    }

    companion object {
        const val FILE_NAME = "settings.preferences_pb"
        val LAST_FILTER = stringPreferencesKey("last_filter_json")
        val ENDPOINT_OVERRIDE = stringPreferencesKey("endpoint_override")
        val THEME = stringPreferencesKey("theme")
        val LIST_LAYOUT = stringPreferencesKey("list_layout")
    }
}

private const val RELAY_PREFIX = "relay:"
private const val FIXTURE_VALUE = "fixture"

/** 저장된 설정 → [Settings]. 모르는 값·깨진 JSON은 기본값. */
internal fun Preferences.toSettings(): Settings = Settings(
    lastFilter = this[SettingsStore.LAST_FILTER]?.let {
        runCatching { StoreJson.decodeFromString(SearchFilter.serializer(), it) }.getOrNull()
    },
    theme = when (this[SettingsStore.THEME]) {
        "light" -> ThemeMode.LIGHT
        "dark" -> ThemeMode.DARK
        else -> ThemeMode.SYSTEM
    },
    listLayout = if (this[SettingsStore.LIST_LAYOUT] ==
        "large"
    ) {
        ListLayout.LARGE
    } else {
        ListLayout.GRID
    },
    endpointOverride = this[SettingsStore.ENDPOINT_OVERRIDE]?.let(::decodeEndpointOverride)
)

internal fun encodeEndpointOverride(endpoint: ApiEndpoint?): String? = when (endpoint) {
    is ApiEndpoint.Relay -> RELAY_PREFIX + endpoint.baseUrl
    ApiEndpoint.Fixture -> FIXTURE_VALUE
    is ApiEndpoint.Direct, null -> null
}

internal fun decodeEndpointOverride(value: String): ApiEndpoint? = when {
    value == FIXTURE_VALUE -> ApiEndpoint.Fixture
    value.startsWith(RELAY_PREFIX) -> ApiEndpoint.Relay(value.removePrefix(RELAY_PREFIX))
    else -> null
}

private fun ListLayout.storedValue(): String = when (this) {
    ListLayout.GRID -> "grid"
    ListLayout.LARGE -> "large"
}
