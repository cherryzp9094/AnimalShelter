package com.cherryzp.animalshelter.di

import com.cherryzp.animalshelter.data.api.ApiEndpoint

/**
 * 앱 모듈(:androidApp, :webApp)이 :shared에 넘기는 시작 설정.
 * `versionName`은 플랫폼마다 앱 모듈이 알고 있어 여기로 받는다([PlatformInfo]).
 * `relayBaseUrl`은 디버그 설정의 "중계 서버" 선택지(브리핑 B2). 없으면 그 선택지를 숨긴다.
 * `shareBaseUrl`은 공유 링크의 앞부분(`<shareBaseUrl>/a/<구조번호>`, 4a S4). 비거나 없으면 공유 버튼이 없다 —
 * Android `BuildConfig.SHARE_BASE_URL`(Gradle `shareHost`), 웹은 같은 origin이면 `location.origin`.
 */
data class AppConfig(
    val endpoint: ApiEndpoint,
    val isDebug: Boolean,
    val versionName: String,
    val relayBaseUrl: String? = null,
    val shareBaseUrl: String? = null,
)
