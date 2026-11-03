package com.cherryzp.animalshelter.di

import com.cherryzp.animalshelter.platform.DATASTORE_DIR
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import java.util.concurrent.TimeUnit
import okio.Path.Companion.toOkioPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    // DataStore 파일 위치. createPreferencesStorage·createJsonStorage가 읽는다.
    single(named(DATASTORE_DIR)) { androidContext().filesDir.resolve("datastore").toOkioPath() }
    single<HttpClientEngine> { createOkHttpEngine() }
}

/**
 * 공공데이터·중계 호출 엔진. 연결 10초. 읽기·쓰기는 요청 전체 제한(HttpTimeout 20초)과 맞춘다 —
 * OkHttp 기본 읽기 제한 10초로는 `desertion_no` 조회(실측 10초 넘게 걸릴 때가 있다)가 끊긴다(2c 실호출).
 */
internal fun createOkHttpEngine(): HttpClientEngine = OkHttp.create {
    config {
        connectTimeout(10, TimeUnit.SECONDS)
        readTimeout(20, TimeUnit.SECONDS)
        writeTimeout(20, TimeUnit.SECONDS)
    }
}
