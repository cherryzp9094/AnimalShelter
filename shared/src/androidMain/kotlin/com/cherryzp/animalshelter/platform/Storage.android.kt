package com.cherryzp.animalshelter.platform

import androidx.datastore.core.Storage
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import okio.FileSystem
import okio.Path
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatform

/** Koin에서 DataStore 디렉터리(okio [Path])를 찾는 이름. 앱은 `filesDir/datastore`. */
internal const val DATASTORE_DIR = "datastoreDir"

private fun dataStoreDir(): Path = KoinPlatform.getKoin().get(named(DATASTORE_DIR))

actual fun createPreferencesStorage(name: String): Storage<Preferences> =
    OkioStorage(FileSystem.SYSTEM, PreferencesSerializer) { dataStoreDir().resolve(name) }

actual fun <T> createJsonStorage(name: String, serializer: OkioSerializer<T>): Storage<T> =
    OkioStorage(FileSystem.SYSTEM, serializer) { dataStoreDir().resolve(name) }
