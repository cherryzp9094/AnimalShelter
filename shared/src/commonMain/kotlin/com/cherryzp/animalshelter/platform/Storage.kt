package com.cherryzp.animalshelter.platform

import androidx.datastore.core.Storage
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.preferences.core.Preferences

/**
 * 설정(Preferences) 저장소. Android는 `filesDir/datastore/<name>` 파일, 웹은 `localStorage`.
 * `name` 예: `settings.preferences_pb`. DataStore는 Phase 2의 저장소 클래스가 만든다.
 */
expect fun createPreferencesStorage(name: String): Storage<Preferences>

/**
 * 직렬화 모델(북마크·코드표 캐시) 저장소. Android는 `filesDir/datastore/<name>` 파일, 웹은 OPFS.
 * 웹 OPFS는 secure context(https 또는 localhost)에서만 동작한다.
 */
expect fun <T> createJsonStorage(name: String, serializer: OkioSerializer<T>): Storage<T>
