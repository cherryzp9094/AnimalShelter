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
    // 공공데이터·중계 호출 엔진. 요청 전체 제한(20초)은 HttpTimeout, 연결 제한은 여기서.
    single<HttpClientEngine> {
        OkHttp.create {
            config { connectTimeout(10, TimeUnit.SECONDS) }
        }
    }
}
