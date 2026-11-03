package com.cherryzp.animalshelter.data.api

import com.cherryzp.animalshelter.data.api.fixture.createFixtureEngine
import com.cherryzp.animalshelter.di.AppConfig
import com.cherryzp.animalshelter.testing.FakeClock
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf

/** 테스트용 가짜 키(40자 미만 — .gitleaks.toml 규칙). 이미 퍼센트 인코딩된 모양이다. */
const val FAKE_KEY = "TEST%2BKEY%2FVAL%3D%3D"

/** MockEngine에 붙인 [AnimalShelterApi]. 받은 요청 주소는 [urls]. */
internal class ApiTestHarness(
    endpoint: ApiEndpoint = ApiEndpoint.Direct(FAKE_KEY),
    clock: FakeClock = FakeClock(),
    handler: MockRequestHandler,
) {
    val engine = MockEngine(handler)
    val api = AnimalShelterApi(
        client = createApiHttpClient(engine),
        fixtureClient = lazy { createApiHttpClient(createFixtureEngine()) },
        endpoints = EndpointSelector(AppConfig(endpoint, isDebug = false, versionName = "test")) {
            null
        },
        clock = clock
    )
    val urls: List<Url> get() = engine.requestHistory.map { it.url }
}

internal fun jsonOk(body: String): MockRequestHandler = {
    respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
}
