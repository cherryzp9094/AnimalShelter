package com.cherryzp.animalshelter.data.api

import com.cherryzp.animalshelter.di.AppConfig
import kotlinx.coroutines.CancellationException

/** 디버그 빌드에서 저장해 둔 엔드포인트 덮어쓰기(설정의 `endpoint_override`). 없으면 null. */
fun interface EndpointOverrideSource {
    suspend fun current(): ApiEndpoint?
}

/**
 * 요청마다 어느 엔드포인트를 부를지 정한다(D3).
 * `AppConfig.isDebug`이고 덮어쓰기가 있으면 그것, 아니면 `AppConfig.endpoint`. 릴리즈·웹은 저장값을 무시한다.
 */
class EndpointSelector(
    private val config: AppConfig,
    private val overrides: EndpointOverrideSource,
) {
    suspend fun current(): ApiEndpoint {
        if (!config.isDebug) return config.endpoint
        val override = try {
            overrides.current()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null // 설정을 못 읽으면 기본값으로 간다
        }
        return override ?: config.endpoint
    }
}
