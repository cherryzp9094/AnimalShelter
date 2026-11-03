package com.cherryzp.animalshelter.di

import com.cherryzp.animalshelter.data.api.AnimalShelterApi
import com.cherryzp.animalshelter.data.api.EndpointOverrideSource
import com.cherryzp.animalshelter.data.api.EndpointSelector
import com.cherryzp.animalshelter.data.api.createApiHttpClient
import com.cherryzp.animalshelter.data.api.fixture.createFixtureEngine
import com.cherryzp.animalshelter.data.repository.AnimalRepository
import com.cherryzp.animalshelter.data.repository.BookmarkRepository
import com.cherryzp.animalshelter.data.repository.CodeTableRepository
import com.cherryzp.animalshelter.data.repository.DefaultAnimalRepository
import com.cherryzp.animalshelter.data.repository.DefaultBookmarkRepository
import com.cherryzp.animalshelter.data.repository.DefaultCodeTableRepository
import com.cherryzp.animalshelter.data.repository.DefaultSettingsRepository
import com.cherryzp.animalshelter.data.repository.DefaultShelterRepository
import com.cherryzp.animalshelter.data.repository.SettingsRepository
import com.cherryzp.animalshelter.data.repository.ShelterRepository
import com.cherryzp.animalshelter.data.store.BookmarkStore
import com.cherryzp.animalshelter.data.store.CodeTableStore
import com.cherryzp.animalshelter.data.store.SettingsStore
import com.cherryzp.animalshelter.data.store.ShelterStore
import com.cherryzp.animalshelter.platform.createJsonStorage
import com.cherryzp.animalshelter.platform.createPreferencesStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

/** Repository의 앱 범위(`SupervisorJob() + Dispatchers.Default`, D8). */
const val APP_SCOPE = "appScope"
private const val PLATFORM_HTTP_CLIENT = "platformHttpClient"
private const val FIXTURE_HTTP_CLIENT = "fixtureHttpClient"

/**
 * 데이터 레이어(02-data, 브리핑 4.8). 플랫폼 HTTP 엔진(`HttpClientEngine`)은 [platformModule]이 준다.
 * 저장소마다 DataStore 하나(single). 화면은 Repository 인터페이스만 꺼낸다.
 */
val dataModule: Module = module {
    single<Clock> { Clock.System }
    single(named(APP_SCOPE)) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    single(named(PLATFORM_HTTP_CLIENT)) { createApiHttpClient(get<HttpClientEngine>()) }
    // Fixture 엔드포인트용. AnimalShelterApi가 처음 Fixture로 부를 때 만든다.
    single(named(FIXTURE_HTTP_CLIENT)) { createApiHttpClient(createFixtureEngine()) }

    single { SettingsStore(createPreferencesStorage(SettingsStore.FILE_NAME)) } bind
        EndpointOverrideSource::class
    single {
        CodeTableStore(createJsonStorage(CodeTableStore.FILE_NAME, CodeTableStore.serializer))
    }
    single { BookmarkStore(createJsonStorage(BookmarkStore.FILE_NAME, BookmarkStore.serializer)) }
    single { ShelterStore(createJsonStorage(ShelterStore.FILE_NAME, ShelterStore.serializer)) }

    single { EndpointSelector(get(), get()) }
    single {
        AnimalShelterApi(
            client = get(named(PLATFORM_HTTP_CLIENT)),
            fixtureClient = lazy { get<HttpClient>(named(FIXTURE_HTTP_CLIENT)) },
            endpoints = get(),
            clock = get()
        )
    }

    single<AnimalRepository> { DefaultAnimalRepository(get(), get(), get(named(APP_SCOPE))) }
    single<CodeTableRepository> {
        DefaultCodeTableRepository(get(), get(), get(), get(named(APP_SCOPE)))
    }
    single<ShelterRepository> {
        DefaultShelterRepository(get(), get(), get(), get(named(APP_SCOPE)))
    }
    single<BookmarkRepository> { DefaultBookmarkRepository(get(), get()) }
    single<SettingsRepository> { DefaultSettingsRepository(get()) }
}
