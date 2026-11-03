package com.cherryzp.animalshelter.data.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.takeFrom

/** 요청 전체 제한(연결 10초는 Android OkHttp 엔진 설정). */
internal const val REQUEST_TIMEOUT_MILLIS: Long = 20_000

/**
 * 공공데이터·중계 호출용 HttpClient(02-data 6절). 상태 코드는 직접 분류한다(`expectSuccess = false`).
 * 로깅 플러그인·재시도는 넣지 않는다(URL에 키가 있다, 한도를 아낀다).
 */
internal fun createApiHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    expectSuccess = false
    install(HttpTimeout) {
        requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
    }
    defaultRequest {
        header(HttpHeaders.Accept, ContentType.Application.Json.toString())
    }
}

/** 서비스·오퍼레이션(02-data 1.1). */
internal enum class ApiOperation(val service: String, val operation: String) {
    ANIMALS(ABANDONMENT_SERVICE, "abandonmentPublic_v2"),
    SIDO(ABANDONMENT_SERVICE, "sido_v2"),
    SIGUNGU(ABANDONMENT_SERVICE, "sigungu_v2"),
    SHELTERS(ABANDONMENT_SERVICE, "shelter_v2"),
    KINDS(ABANDONMENT_SERVICE, "kind_v2"),
    SHELTER_INFO(SHELTER_SERVICE, "shelterInfo_v2"),
}

internal const val ABANDONMENT_SERVICE = "abandonmentPublicService_v2"
internal const val SHELTER_SERVICE = "animalShelterSrvc_v2"

/** Fixture 엔드포인트의 가짜 주소. Direct 모양에서 키만 뺐다. 네트워크로 나가지 않는다. */
internal const val FIXTURE_BASE_URL = "https://fixture.invalid/1543061"

/** 쿼리를 뺀 주소. Relay의 빈 base는 같은 origin의 `/api/...`. */
internal fun apiUrl(endpoint: ApiEndpoint, operation: ApiOperation): String = when (endpoint) {
    is ApiEndpoint.Direct -> "${endpoint.baseUrl.trimEnd(
        '/'
    )}/${operation.service}/${operation.operation}"

    is ApiEndpoint.Relay -> "${endpoint.baseUrl.trimEnd(
        '/'
    )}/api/${operation.service}/${operation.operation}"

    ApiEndpoint.Fixture -> "$FIXTURE_BASE_URL/${operation.service}/${operation.operation}"
}

/**
 * 요청 주소를 채운다. 파라미터 → `_type=json` → (Direct만) `serviceKey`.
 * 키는 이미 퍼센트 인코딩된 형태라 `encodedParameters`로 붙인다(D5). `parameters`로 넣으면 `%`가 다시 인코딩된다.
 */
internal fun HttpRequestBuilder.apiRequest(
    endpoint: ApiEndpoint,
    operation: ApiOperation,
    params: List<Pair<String, String>>,
) {
    url {
        takeFrom(apiUrl(endpoint, operation))
        params.forEach { (name, value) -> parameters.append(name, value) }
        parameters.append("_type", "json")
        if (endpoint is ApiEndpoint.Direct) {
            encodedParameters.append(
                "serviceKey",
                endpoint.serviceKey
            )
        }
    }
}
