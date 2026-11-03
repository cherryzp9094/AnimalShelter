package com.cherryzp.animalshelter.data.api.fixture

import com.cherryzp.animalshelter.data.api.AnimalShelterApi
import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.data.api.EndpointSelector
import com.cherryzp.animalshelter.data.api.createApiHttpClient
import com.cherryzp.animalshelter.data.api.getOrNull
import com.cherryzp.animalshelter.di.AppConfig
import com.cherryzp.animalshelter.model.AnimalKind
import com.cherryzp.animalshelter.model.SearchFilter
import com.cherryzp.animalshelter.testing.FakeClock
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/** Fixture 엔드포인트의 오퍼레이션별 응답(D4, 브리핑 4.4). 네트워크를 쓰지 않는다. */
class FixtureEngineTest {
    // 플랫폼 클라이언트로 나가면 실패하도록 막아 둔다.
    private val blocked = createApiHttpClient(MockEngine { respondError(HttpStatusCode.Forbidden) })
    private val fixtureClient = createApiHttpClient(createFixtureEngine())
    private val api = AnimalShelterApi(
        client = blocked,
        fixtureClient = lazy { fixtureClient },
        endpoints = EndpointSelector(
            AppConfig(ApiEndpoint.Fixture, isDebug = false, versionName = "t")
        ) {
            null
        },
        clock = FakeClock()
    )

    @Test
    fun `코드표 오퍼레이션별 건수`() = runTest {
        assertEquals(16, api.getSido().getOrNull()?.size)
        assertEquals(10, api.getSigungu("6110000").getOrNull()?.size)
        assertEquals(0, api.getSigungu("6260000").getOrNull()?.size)
        assertEquals(0, api.getShelters("6110000", "3220000").getOrNull()?.size)
        assertEquals(10, api.getKinds(AnimalKind.DOG).getOrNull()?.size)
        assertEquals(0, api.getKinds(AnimalKind.CAT).getOrNull()?.size)
        assertEquals(3, api.getAllShelterInfo().getOrNull()?.size)
    }

    @Test
    fun `목록은 1페이지 5건, 2페이지부터 0건`() = runTest {
        val first = api.searchAnimals(SearchFilter(), pageNo = 1).getOrNull()!!
        val second = api.searchAnimals(SearchFilter(), pageNo = 2).getOrNull()!!

        assertEquals(5, first.items.size)
        assertTrue(first.hasMore)
        assertEquals(0, second.items.size)
        assertFalse(second.hasMore)
    }

    @Test
    fun `desertion_no 조회는 표본의 그 1건, 없으면 null`() = runTest {
        val found = api.getAnimal("448567202601800").getOrNull()
        val missing = api.getAnimal("450650202699999").getOrNull()

        assertEquals("448567202601800", found?.desertionNo)
        assertEquals("말티즈", found?.kindName)
        assertNull(missing)
    }

    @Test
    fun `모르는 오퍼레이션은 404`() = runTest {
        val response = fixtureClient.get("https://fixture.invalid/1543061/x/nope_v2")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
