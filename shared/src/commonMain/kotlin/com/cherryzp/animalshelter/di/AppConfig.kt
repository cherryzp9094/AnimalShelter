package com.cherryzp.animalshelter.di

import com.cherryzp.animalshelter.data.api.ApiEndpoint

/**
 * 앱 모듈(:androidApp, :webApp)이 :shared에 넘기는 시작 설정.
 * `versionName`은 플랫폼마다 앱 모듈이 알고 있어 여기로 받는다([PlatformInfo]).
 */
data class AppConfig(val endpoint: ApiEndpoint, val isDebug: Boolean, val versionName: String)
