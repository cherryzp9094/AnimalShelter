package com.cherryzp.animalshelter.data.store

import androidx.datastore.preferences.core.preferencesOf
import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.data.repository.Settings
import com.cherryzp.animalshelter.model.Animal
import com.cherryzp.animalshelter.model.AnimalKind
import com.cherryzp.animalshelter.model.Kind
import com.cherryzp.animalshelter.model.ListLayout
import com.cherryzp.animalshelter.model.NoticeState
import com.cherryzp.animalshelter.model.SearchFilter
import com.cherryzp.animalshelter.model.Sido
import com.cherryzp.animalshelter.model.ThemeMode
import com.cherryzp.animalshelter.platform.storageTestModule
import com.cherryzp.animalshelter.testing.TestStores
import com.cherryzp.animalshelter.testing.withTestStores
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

/** DataStore 저장소 4개(02-data 8절): 왕복, 모르는 값, 깨진 파일 복구. */
class StoreTest {
    @BeforeTest
    fun setUp() {
        startKoin { modules(storageTestModule()) }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `설정을 쓰고 다시 읽는다`() = runTest {
        withTestStores { stores ->
            val store = stores.settings()
            val filter = SearchFilter(
                kind = AnimalKind.CAT,
                dateFrom = LocalDate(2026, 10, 5),
                sidoCode = "6110000",
                state = NoticeState.NOTICE
            )

            assertEquals(Settings(), store.settings.first())
            store.saveLastFilter(filter)
            store.setTheme(ThemeMode.DARK)
            store.setListLayout(ListLayout.LARGE)
            store.setEndpointOverride(ApiEndpoint.Relay("http://10.0.2.2:8788"))

            assertEquals(
                Settings(
                    filter,
                    ThemeMode.DARK,
                    ListLayout.LARGE,
                    ApiEndpoint.Relay("http://10.0.2.2:8788")
                ),
                store.settings.first()
            )
            assertEquals(ApiEndpoint.Relay("http://10.0.2.2:8788"), store.current())

            store.setEndpointOverride(ApiEndpoint.Fixture)
            assertEquals(ApiEndpoint.Fixture, store.current())
            store.setEndpointOverride(ApiEndpoint.Direct("k"))
            assertNull(store.current())
        }
    }

    @Test
    fun `설정의 모르는 값과 깨진 조건은 기본값`() {
        val settings = preferencesOf(
            SettingsStore.THEME to "purple",
            SettingsStore.LIST_LAYOUT to "tiles",
            SettingsStore.LAST_FILTER to "{not json",
            SettingsStore.ENDPOINT_OVERRIDE to "direct:https://x"
        ).toSettings()

        assertEquals(Settings(), settings)
        assertEquals(
            ThemeMode.LIGHT,
            preferencesOf(SettingsStore.THEME to "light").toSettings().theme
        )
        assertEquals("relay:", encodeEndpointOverride(ApiEndpoint.Relay("")))
        assertEquals(ApiEndpoint.Relay(""), decodeEndpointOverride("relay:"))
    }

    @Test
    fun `표시 이름이 없는 옛 저장 조건도 읽고 이름은 null`() {
        val old = preferencesOf(
            SettingsStore.LAST_FILTER to
                """{"kind":"CAT","sidoCode":"6110000","sigunguCode":"3220000","state":"NOTICE"}"""
        ).toSettings().lastFilter

        assertEquals(
            SearchFilter(
                kind = AnimalKind.CAT,
                sidoCode = "6110000",
                sigunguCode = "3220000",
                state = NoticeState.NOTICE
            ),
            old
        )
        assertNull(old?.sidoName)
        assertNull(old?.kindName)
    }

    @Test
    fun `표시 이름도 저장했다가 그대로 읽는다`() = runTest {
        withTestStores { stores ->
            val store = stores.settings()
            val filter = SearchFilter(sidoCode = "6110000", sidoName = "서울특별시", shelterName = "보호소")
            store.saveLastFilter(filter)

            assertEquals(filter, store.settings.first().lastFilter)
        }
    }

    @Test
    fun `저장된 조건에 모르는 필드가 있어도 읽는다`() {
        val settings = preferencesOf(
            SettingsStore.LAST_FILTER to """{"sidoCode":"6110000","futureField":1}"""
        ).toSettings()

        assertEquals(SearchFilter(sidoCode = "6110000"), settings.lastFilter)
    }

    @Test
    fun `코드표 캐시를 쓰고 다시 읽는다`() = runTest {
        val cache = CodeTableCache(
            sido = CachedTable(listOf(Sido("6110000", "서울특별시")), 1_000L),
            kinds = mapOf(
                AnimalKind.DOG to CachedTable(listOf(Kind("000054", "골든 리트리버")), 2_000L)
            )
        )
        val writer = TestStores()
        withTestStores(writer) { it.codeTables("ct.json").update { cache } }

        // 쓴 DataStore를 닫은 뒤 새 DataStore로 읽어 파일에서 왔는지 본다
        val read =
            withTestStores(TestStores(writer.suffix)) { it.codeTables("ct.json").data.first() }
        assertEquals(cache, read)
    }

    @Test
    fun `깨진 코드표·보호소 파일은 빈 캐시로 복구한다`() = runTest {
        withTestStores { stores ->
            stores.writeRaw("broken-ct.json", "{this is not json")
            stores.writeRaw("broken-sh.json", "[1,2,")

            assertEquals(CodeTableCache(), stores.codeTables("broken-ct.json").data.first())
            assertEquals(ShelterCache(), stores.shelters("broken-sh.json").data.first())
        }
    }

    @Test
    fun `깨진 북마크 파일은 빈 목록으로 복구하고 알림 상태를 켠다`() = runTest {
        withTestStores { stores ->
            stores.writeRaw("broken-bm.json", "{\"entries\":")
            val store = stores.bookmarks("broken-bm.json")

            assertFalse(store.recoveredFromCorruption.value)
            assertEquals(BookmarkData(), store.data.first())
            assertTrue(store.recoveredFromCorruption.value)

            store.clearRecovered()
            assertFalse(store.recoveredFromCorruption.value)
            // 복구한 뒤에는 정상으로 쓰고 읽힌다
            store.update {
                BookmarkData(mapOf("1" to StoredBookmark(Animal("1"), 5L)), listOf("1"))
            }
            assertEquals(listOf("1"), store.data.first().order)
        }
    }

    @Test
    fun `모으는 중인 구독자에게도 쓴 값이 보이고 다시 열어도 남는다`() = runTest {
        val writer = TestStores()
        withTestStores(writer) { stores ->
            val store = stores.bookmarks("live.json")
            store.data.first()
            val seen = async(Dispatchers.Default) { store.data.first { it.order.isNotEmpty() } }
            store.update {
                BookmarkData(mapOf("1" to StoredBookmark(Animal("1"), 5L)), listOf("1"))
            }
            assertEquals(listOf("1"), seen.await().order)
            assertEquals(listOf("1"), store.data.first().order)
        }

        val reopened =
            withTestStores(TestStores(writer.suffix)) { it.bookmarks("live.json").data.first() }
        assertEquals(listOf("1"), reopened.order)
    }

    @Test
    fun `설정을 모으는 중에 보기·테마를 바꾸면 그 구독자에게 보인다(R4)`() = runTest {
        withTestStores { stores ->
            val store = stores.settings()
            store.settings.first()
            val layoutSeen = async(Dispatchers.Default) {
                store.settings.first { it.listLayout == ListLayout.LARGE }
            }
            val themeSeen = async(Dispatchers.Default) {
                store.settings.first { it.theme == ThemeMode.DARK }
            }
            store.setListLayout(ListLayout.LARGE)
            store.setTheme(ThemeMode.DARK)

            assertEquals(ListLayout.LARGE, layoutSeen.await().listLayout)
            assertEquals(ThemeMode.DARK, themeSeen.await().theme)
            assertEquals(ListLayout.LARGE, store.settings.first().listLayout)
            assertEquals(ThemeMode.DARK, store.settings.first().theme)
        }
    }
}
