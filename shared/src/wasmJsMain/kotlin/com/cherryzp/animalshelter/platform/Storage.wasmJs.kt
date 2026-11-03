package com.cherryzp.animalshelter.platform

import androidx.datastore.core.Storage
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.WebLocalStorage
import androidx.datastore.core.okio.WebOpfsStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer

// 설정은 localStorage(탭 사이 동기화), 북마크·캐시는 OPFS(용량·비동기). 02-data 8절.
actual fun createPreferencesStorage(name: String): Storage<Preferences> =
    WebLocalStorage(PreferencesSerializer, name)

actual fun <T> createJsonStorage(name: String, serializer: OkioSerializer<T>): Storage<T> =
    WebOpfsStorage(serializer, name)
