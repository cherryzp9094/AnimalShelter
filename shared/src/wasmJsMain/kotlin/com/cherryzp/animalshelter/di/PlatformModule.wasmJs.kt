package com.cherryzp.animalshelter.di

import com.cherryzp.animalshelter.ui.common.ImageDiskCacheProvider
import com.cherryzp.animalshelter.ui.common.ImageUrlResolver
import com.cherryzp.animalshelter.ui.common.PhotoVariants
import com.cherryzp.animalshelter.ui.common.RelayPhotoVariants
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js
import org.koin.core.module.Module
import org.koin.dsl.module

// 웹 저장소는 이름만으로 만든다. HTTP는 브라우저 Fetch(Js 엔진) — 키 없이 중계만 부른다.
actual fun platformModule(): Module = module {
    single<HttpClientEngine> { Js.create() }
    // 사진은 메모리 캐시만(브라우저 HTTP 캐시가 뒤에 있다).
    single { ImageDiskCacheProvider { null } }
    // 같은 origin 중계면 사진 주소가 `/img/…` — 지금 페이지의 origin을 붙인다
    single { ImageUrlResolver { url -> if (url.startsWith("/")) pageOrigin() + url else url } }
    // 사진(5c): 중계 `/img?size=` 변환 + 작은 사진 `_s` 자리표시
    single<PhotoVariants> { RelayPhotoVariants }
}

@JsFun("() => window.location.origin")
private external fun pageOrigin(): String
