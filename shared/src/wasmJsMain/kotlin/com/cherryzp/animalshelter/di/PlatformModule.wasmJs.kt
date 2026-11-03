package com.cherryzp.animalshelter.di

import com.cherryzp.animalshelter.ui.common.ImageDiskCacheProvider
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js
import org.koin.core.module.Module
import org.koin.dsl.module

// 웹 저장소는 이름만으로 만든다. HTTP는 브라우저 Fetch(Js 엔진) — 키 없이 중계만 부른다.
actual fun platformModule(): Module = module {
    single<HttpClientEngine> { Js.create() }
    // 사진은 메모리 캐시만(브라우저 HTTP 캐시가 뒤에 있다).
    single { ImageDiskCacheProvider { null } }
}
