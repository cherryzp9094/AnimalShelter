package com.cherryzp.animalshelter

import android.app.Application
import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.di.AppConfig
import com.cherryzp.animalshelter.di.initKoin
import org.koin.android.ext.koin.androidContext

class AnimalShelterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 키가 없으면(local.properties 비어 있음) 픽스처로 뜬다. 키 값은 로그에 쓰지 않는다.
        val endpoint = if (BuildConfig.SERVICE_KEY.isNotBlank()) {
            ApiEndpoint.Direct(BuildConfig.SERVICE_KEY)
        } else {
            ApiEndpoint.Fixture
        }
        initKoin(
            AppConfig(
                endpoint = endpoint,
                isDebug = BuildConfig.DEBUG,
                versionName = BuildConfig.VERSION_NAME,
                // 디버그 설정의 "중계 서버" 선택지(에뮬레이터 기본 http://10.0.2.2:8788)
                relayBaseUrl = BuildConfig.RELAY_BASE_URL,
                // 공유 링크 앞부분(Gradle shareHost, 4a L1·S4). 비면 공유 버튼이 없다
                shareBaseUrl = BuildConfig.SHARE_BASE_URL
            )
        ) {
            androidContext(this@AnimalShelterApplication)
        }
    }
}
