package com.cherryzp.animalshelter.di

import com.cherryzp.animalshelter.platform.PlatformInfo
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/** 공통 모듈(시작 설정). 데이터는 [dataModule], ViewModel 모듈은 Phase 3에서 더한다. */
fun appModule(config: AppConfig): Module = module {
    single { config }
    single { config.endpoint }
    single { PlatformInfo(versionName = config.versionName) }
}

/** 플랫폼 모듈(Android: 저장소 경로·OkHttp 엔진, 웹: Js 엔진). */
expect fun platformModule(): Module

/**
 * Koin을 시작한다. Android는 `extra`에서 `androidContext(...)`를 넘긴다.
 * 앱 프로세스에서 한 번만 부른다.
 */
fun initKoin(config: AppConfig, extra: KoinAppDeclaration = {}): KoinApplication = startKoin {
    extra()
    modules(appModule(config), dataModule, platformModule())
}
