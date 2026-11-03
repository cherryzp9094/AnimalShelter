package com.cherryzp.animalshelter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.map.Mapper
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.cherryzp.animalshelter.data.repository.SettingsRepository
import com.cherryzp.animalshelter.model.ThemeMode
import com.cherryzp.animalshelter.navigation.AppNavDisplay
import com.cherryzp.animalshelter.ui.common.ImageDiskCacheProvider
import com.cherryzp.animalshelter.ui.common.ImageUrlResolver
import com.cherryzp.animalshelter.ui.theme.AnimalShelterTheme
import com.cherryzp.animalshelter.ui.theme.isDarkTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

/** 사진 crossfade(06 5절). */
private const val PHOTO_CROSSFADE_MILLIS = 150

/**
 * 앱 진입 Composable. Android `MainActivity`와 웹 `main()`이 같은 것을 띄운다.
 * 설정의 테마를 읽기 전에는 시스템 테마의 배경만 그린다(잘못된 테마가 번쩍이지 않게, 브리핑 A1).
 * 사진은 Coil 싱글턴 로더(브리핑 P6·A5): 기본 Ktor 클라이언트(API용 `HttpClient`는 `Accept: application/json`
 * 기본 헤더가 있어 쓰지 않는다), crossfade 150ms, 디스크 캐시는 플랫폼 공급자(Android 200MB, 웹 없음).
 */
@Composable
fun App() {
    val diskCacheProvider = koinInject<ImageDiskCacheProvider>()
    val urlResolver = koinInject<ImageUrlResolver>()
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                // 상대 주소(웹 같은 origin 중계 `/img/…`)를 절대 주소로 — 기본 String 매퍼보다 먼저 돈다
                add(Mapper<String, String> { data, _ -> urlResolver.resolve(data) })
                add(KtorNetworkFetcherFactory())
            }
            .crossfade(PHOTO_CROSSFADE_MILLIS)
            .diskCache { diskCacheProvider.diskCache() }
            .build()
    }
    val settingsRepository = koinInject<SettingsRepository>()
    val themeMode by remember(settingsRepository) {
        settingsRepository.getSettingsStream().map { it.theme }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)

    val mode: ThemeMode? = themeMode
    if (mode == null) {
        AnimalShelterTheme {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }
        return
    }
    AnimalShelterTheme(darkTheme = mode.isDarkTheme()) {
        AppNavDisplay()
    }
}
