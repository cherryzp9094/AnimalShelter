package com.cherryzp.animalshelter.di

import coil3.disk.DiskCache
import com.cherryzp.animalshelter.platform.DATASTORE_DIR
import com.cherryzp.animalshelter.ui.common.ImageDiskCacheProvider
import com.cherryzp.animalshelter.ui.common.ImageUrlResolver
import com.cherryzp.animalshelter.ui.common.PhotoVariants
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
    // 사진 디스크 캐시 200MB(브리핑 A5). 북마크 사진을 오프라인에서도 보인다(05 5.2).
    single<ImageDiskCacheProvider> {
        val directory = androidContext().cacheDir.resolve("image_cache").toOkioPath()
        ImageDiskCacheProvider {
            DiskCache.Builder().directory(directory).maxSizeBytes(IMAGE_DISK_CACHE_BYTES).build()
        }
    }
    // Android의 사진 주소는 늘 절대 주소다(Direct·Fixture 원본, Relay는 http://10.0.2.2:8788 같은 base).
    single { ImageUrlResolver { it } }
    // 사진 주소 변형 없음(5c — Android는 원본 그대로, 자리표시 없음)
    single<PhotoVariants> { PhotoVariants.Original }
}

private const val IMAGE_DISK_CACHE_BYTES = 200L * 1024 * 1024

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
