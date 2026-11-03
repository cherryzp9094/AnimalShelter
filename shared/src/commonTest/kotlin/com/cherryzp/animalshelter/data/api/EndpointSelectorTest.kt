package com.cherryzp.animalshelter.data.api

import com.cherryzp.animalshelter.di.AppConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

/** 엔드포인트 덮어쓰기는 디버그 빌드에서만(D3). */
class EndpointSelectorTest {
    private val direct = ApiEndpoint.Direct(FAKE_KEY)
    private val relay = ApiEndpoint.Relay("http://10.0.2.2:8788")

    @Test
    fun `디버그이고 덮어쓰기가 있으면 그것`() = runTest {
        val selector =
            EndpointSelector(AppConfig(direct, isDebug = true, versionName = "t")) { relay }
        assertEquals(relay, selector.current())
        val fixture =
            EndpointSelector(AppConfig(direct, isDebug = true, versionName = "t")) {
                ApiEndpoint.Fixture
            }
        assertEquals(ApiEndpoint.Fixture, fixture.current())
    }

    @Test
    fun `디버그라도 덮어쓰기가 없으면 시작 설정`() = runTest {
        val selector =
            EndpointSelector(AppConfig(direct, isDebug = true, versionName = "t")) { null }
        assertEquals(direct, selector.current())
    }

    @Test
    fun `릴리즈·웹은 저장값을 무시한다`() = runTest {
        val selector =
            EndpointSelector(AppConfig(direct, isDebug = false, versionName = "t")) { relay }
        assertEquals(direct, selector.current())
    }

    @Test
    fun `덮어쓰기를 못 읽으면 시작 설정`() = runTest {
        val selector = EndpointSelector(AppConfig(direct, isDebug = true, versionName = "t")) {
            error("broken")
        }
        assertEquals(direct, selector.current())
    }

    @Test
    fun `Direct의 toString은 키를 가린다`() {
        assertEquals(false, FAKE_KEY in direct.toString())
    }
}
