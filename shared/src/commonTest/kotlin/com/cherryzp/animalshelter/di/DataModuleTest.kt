package com.cherryzp.animalshelter.di

import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.data.repository.AnimalRepository
import com.cherryzp.animalshelter.data.repository.BookmarkRepository
import com.cherryzp.animalshelter.data.repository.CodeTableRepository
import com.cherryzp.animalshelter.data.repository.SettingsRepository
import com.cherryzp.animalshelter.data.repository.ShelterRepository
import com.cherryzp.animalshelter.model.SearchFilter
import com.cherryzp.animalshelter.platform.storageTestModule
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

/** Koin 그래프: Fixture 설정으로 시작해 Repository 5개를 꺼내고, 네트워크 없이 목록을 받는다. */
class DataModuleTest {
    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `Fixture 설정으로 Repository 5개를 꺼내고 목록을 받는다`() = runTest {
        startKoin {
            modules(
                appModule(AppConfig(ApiEndpoint.Fixture, isDebug = false, versionName = "test")),
                dataModule,
                storageTestModule(),
                // 플랫폼 엔진 자리: 쓰이면 403으로 실패한다(Fixture는 이 엔진을 쓰지 않는다)
                module {
                    single<HttpClientEngine> {
                        MockEngine { respondError(HttpStatusCode.Forbidden) }
                    }
                }
            )
        }
        val koin = KoinPlatform.getKoin()

        val animals = koin.get<AnimalRepository>()
        koin.get<CodeTableRepository>()
        koin.get<ShelterRepository>()
        koin.get<BookmarkRepository>()
        koin.get<SettingsRepository>()

        animals.search(SearchFilter())
        assertEquals(5, animals.getSearchResultsStream().value.items.size)
    }
}
