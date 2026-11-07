package com.cherryzp.animalshelter.ui.detail

import androidx.lifecycle.ViewModel
import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.data.api.ApiError
import com.cherryzp.animalshelter.data.api.ApiResult
import com.cherryzp.animalshelter.data.api.EndpointSelector
import com.cherryzp.animalshelter.data.repository.BookmarkRepository
import com.cherryzp.animalshelter.data.repository.CacheState
import com.cherryzp.animalshelter.data.repository.SearchResults
import com.cherryzp.animalshelter.data.repository.Settings
import com.cherryzp.animalshelter.di.AppConfig
import com.cherryzp.animalshelter.fake.FakeAnimalRepository
import com.cherryzp.animalshelter.fake.FakeBookmarkRepository
import com.cherryzp.animalshelter.fake.FakeSettingsRepository
import com.cherryzp.animalshelter.fake.FakeShelterRepository
import com.cherryzp.animalshelter.fake.testAnimal
import com.cherryzp.animalshelter.fake.testShelterInfo
import com.cherryzp.animalshelter.model.Animal
import com.cherryzp.animalshelter.model.SearchFilter
import com.cherryzp.animalshelter.navigation.AnimalDetailKey
import com.cherryzp.animalshelter.platform.CopyFeedback
import com.cherryzp.animalshelter.platform.ShareResult
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.address_copied
import com.cherryzp.animalshelter.resources.bookmark_added
import com.cherryzp.animalshelter.resources.bookmark_removed
import com.cherryzp.animalshelter.resources.bookmark_save_failed
import com.cherryzp.animalshelter.resources.date_range
import com.cherryzp.animalshelter.resources.error_network
import com.cherryzp.animalshelter.resources.external_open_failed
import com.cherryzp.animalshelter.resources.hours_range
import com.cherryzp.animalshelter.resources.share_copied
import com.cherryzp.animalshelter.resources.today_hours
import com.cherryzp.animalshelter.testing.FakeClock
import com.cherryzp.animalshelter.testing.content
import com.cherryzp.animalshelter.testing.runMainTest
import com.cherryzp.animalshelter.ui.common.MessageAction
import com.cherryzp.animalshelter.ui.common.StatusKind
import com.cherryzp.animalshelter.ui.common.UiMessage
import com.cherryzp.animalshelter.ui.common.uiText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.datetime.LocalDate

/** 상세 ViewModel(브리핑 B6, 6절 완료 기준). Fake Repository만 쓴다. */
class AnimalDetailViewModelTest {
    private val clock = FakeClock()
    private val created = mutableListOf<ViewModel>()
    private val animal = testAnimal()
    private val no = animal.desertionNo
    private val regNo = animal.shelter.regNo!!

    private fun runVmTest(block: suspend TestScope.() -> Unit) = runMainTest({ created }, block)

    private inner class Env(
        val key: AnimalDetailKey = AnimalDetailKey(no, "20261103"),
        results: SearchResults = SearchResults(),
        val bookmarkFake: FakeBookmarkRepository = FakeBookmarkRepository(),
        bookmarks: BookmarkRepository = bookmarkFake,
        shareBaseUrl: String? = null,
        savedSettings: Settings = Settings(),
        configure: FakeAnimalRepository.() -> Unit = {},
    ) {
        val animals = FakeAnimalRepository(results).apply(configure)
        val shelters = FakeShelterRepository()
        val settings = FakeSettingsRepository(savedSettings)
        val config = AppConfig(
            ApiEndpoint.Fixture,
            isDebug = false,
            versionName = "test",
            shareBaseUrl = shareBaseUrl
        )
        val vm = AnimalDetailViewModel(
            key = key,
            animals = animals,
            bookmarks = bookmarks,
            shelters = shelters,
            settings = settings,
            endpoints = EndpointSelector(config) { null },
            clock = clock,
            config = config
        )
        val state get() = vm.uiState.value

        init {
            created += vm
        }
    }

    private fun TestScope.start(env: Env): Env {
        backgroundScope.launch { env.vm.uiState.collect {} }
        advanceUntilIdle()
        return env
    }

    private suspend fun FakeBookmarkRepository.seed(snapshot: Animal) {
        add(snapshot)
        addCalls.clear()
    }

    @Test
    fun `목록에서 연 것은 결과에서 바로 보이고 한 번만 묻는다`() = runVmTest {
        val env = start(Env(results = SearchResults(items = listOf(animal))))

        assertEquals(
            listOf<Pair<String, LocalDate?>>(no to LocalDate(2026, 11, 3)),
            env.animals.getAnimalCalls
        )
        assertEquals<String?>(no, env.state.detail?.desertionNo)
        assertFalse(env.state.isLoading)
        assertFalse(env.state.isStale)
        assertTrue(env.bookmarkFake.updateCalls.isEmpty()) // 북마크가 아니면 스냅샷을 쓰지 않는다
    }

    @Test
    fun `북마크에서 열면 스냅샷을 먼저 보이고 조회 결과로 스냅샷을 갱신한다`() = runVmTest {
        val bookmarks = FakeBookmarkRepository().apply { seed(animal.copy(kindName = "옛 이름")) }
        val fresh = animal.copy(kindName = "새 이름", processState = "종료(반환)")
        val gate = CompletableDeferred<Unit>()
        val env = start(
            Env(
                key = AnimalDetailKey(no, "20261103", fromBookmarks = true),
                bookmarkFake = bookmarks
            ) {
                getAnimalGate = gate
                animalResult = { ApiResult.Success(fresh) }
            }
        )

        assertEquals<String?>("옛 이름", env.state.detail?.name)
        assertTrue(env.state.isLoading)
        assertTrue(env.state.isBookmarked)

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals<String?>("새 이름", env.state.detail?.name)
        assertFalse(env.state.isLoading)
        assertEquals(listOf(fresh), bookmarks.updateCalls)
    }

    @Test
    fun `0건이고 스냅샷이 있으면 저장된 정보와 배너`() = runVmTest {
        val bookmarks = FakeBookmarkRepository().apply { seed(animal) }
        val env = start(
            Env(key = AnimalDetailKey(no, null, fromBookmarks = true), bookmarkFake = bookmarks) {
                animalResult = { ApiResult.Success(null) }
            }
        )

        assertTrue(env.state.isStale)
        assertFalse(env.state.notFound)
        assertEquals<String?>(no, env.state.detail?.desertionNo)
    }

    @Test
    fun `0건이고 스냅샷도 없으면 찾을 수 없음`() = runVmTest {
        val env = start(
            Env(key = AnimalDetailKey(no)) { animalResult = { ApiResult.Success(null) } }
        )

        assertTrue(env.state.notFound)
        assertNull(env.state.detail)
        assertNull(env.state.error)
    }

    @Test
    fun `실패해도 보이는 것이 있으면 두고 스낵바로 다시 시도`() = runVmTest {
        val bookmarks = FakeBookmarkRepository().apply { seed(animal) }
        val env = start(
            Env(key = AnimalDetailKey(no, fromBookmarks = true), bookmarkFake = bookmarks) {
                animalResult = { ApiResult.Failure(ApiError.Network) }
            }
        )

        assertNotNull(env.state.detail)
        assertNull(env.state.error)
        assertEquals(
            UiMessage(uiText(Res.string.error_network), MessageAction.RETRY),
            env.state.message.content()
        )
    }

    @Test
    fun `실패하고 보일 것이 없으면 가운데 오류, 다시 시도로 다시 묻는다`() = runVmTest {
        var result: ApiResult<Animal?> = ApiResult.Failure(ApiError.Network)
        val env = start(Env(key = AnimalDetailKey(no)) { animalResult = { result } })

        assertEquals(uiText(Res.string.error_network), env.state.error)
        assertTrue(env.state.canRetry)
        assertNull(env.state.detail)

        result = ApiResult.Success(animal)
        env.vm.onRetry()
        advanceUntilIdle()

        assertEquals(2, env.animals.getAnimalCalls.size)
        assertNull(env.state.error)
        assertEquals<String?>(no, env.state.detail?.desertionNo)
    }

    @Test
    fun `북마크 토글은 추가·제거하고 안내한다`() = runVmTest {
        val env = start(Env(results = SearchResults(items = listOf(animal))))

        env.vm.onToggleBookmark()
        advanceUntilIdle()
        assertTrue(env.state.isBookmarked)
        assertEquals(UiMessage(uiText(Res.string.bookmark_added)), env.state.message.content())

        env.vm.onMessageShown(env.state.message!!.id)
        env.vm.onToggleBookmark()
        advanceUntilIdle()
        assertFalse(env.state.isBookmarked)
        assertEquals(UiMessage(uiText(Res.string.bookmark_removed)), env.state.message.content())
        assertEquals(listOf(no), env.bookmarkFake.removeCalls)
    }

    @Test
    fun `북마크 쓰기가 실패하면 저장 실패 안내`() = runVmTest {
        val fake = FakeBookmarkRepository()
        val failing = object : BookmarkRepository by fake {
            override suspend fun add(animal: Animal) = throw IllegalStateException("disk")
        }
        val env = start(Env(results = SearchResults(items = listOf(animal)), bookmarks = failing))

        env.vm.onToggleBookmark()
        advanceUntilIdle()

        assertFalse(env.state.isBookmarked)
        assertEquals(
            UiMessage(uiText(Res.string.bookmark_save_failed)),
            env.state.message.content()
        )
    }

    @Test
    fun `보호소 정보가 있으면 오늘 운영시간과 지도, 좌표가 없으면 지도 없음`() = runVmTest {
        val env = start(Env(results = SearchResults(items = listOf(animal))))
        assertNull(env.state.map)
        assertTrue(env.state.shelterToday.isEmpty())

        env.shelters.state(regNo).value =
            CacheState(testShelterInfo(regNo), fresh = true, error = null)
        advanceUntilIdle()
        assertEquals(MapTarget(33.45, 126.4, "제2동물보호센터"), env.state.map)
        assertEquals(
            listOf(
                uiText(Res.string.today_hours, uiText(Res.string.hours_range, "09:00", "18:00"))
            ),
            env.state.shelterToday
        )

        env.shelters.state(regNo).value = CacheState(
            testShelterInfo(regNo).copy(latitude = null, longitude = null),
            fresh = true,
            error = null
        )
        advanceUntilIdle()
        assertNull(env.state.map)
    }

    @Test
    fun `전화·지도 실패와 복사 결과 안내`() = runVmTest {
        val env = start(Env(results = SearchResults(items = listOf(animal))))

        env.vm.onExternalActionResult(opened = true)
        assertNull(env.state.message)
        env.vm.onExternalActionResult(opened = false)
        advanceUntilIdle()
        assertEquals(
            UiMessage(uiText(Res.string.external_open_failed)),
            env.state.message.content()
        )

        env.vm.onMessageShown(env.state.message!!.id)
        env.vm.onCopyResult(CopyFeedback.SHOWN_BY_SYSTEM)
        advanceUntilIdle()
        assertNull(env.state.message) // Android 13 이상은 시스템이 알린다
        env.vm.onCopyResult(CopyFeedback.NEEDS_APP_MESSAGE)
        advanceUntilIdle()
        assertEquals(UiMessage(uiText(Res.string.address_copied)), env.state.message.content())
    }

    @Test
    fun `같은 안내가 연달아 와도 id가 달라 다시 보이고, 지난 안내를 보였다고 해도 새 것은 남는다(F3)`() = runVmTest {
        val env = start(Env())
        env.vm.onExternalActionResult(opened = false)
        advanceUntilIdle()
        val first = env.state.message!!
        env.vm.onExternalActionResult(opened = false)
        advanceUntilIdle()
        val second = env.state.message!!

        assertEquals(first.content(), second.content())
        assertNotEquals(first.id, second.id)
        assertNotEquals(first, second)

        env.vm.onMessageShown(first.id)
        advanceUntilIdle()
        assertEquals(second, env.state.message)
        env.vm.onMessageShown(second.id)
        advanceUntilIdle()
        assertNull(env.state.message)
    }

    @Test
    fun `값이 없는 행은 빼고 비는 섹션은 빈 목록`() {
        val sparse = Animal(desertionNo = "1", processState = "보호중", kindName = "믹스견")
        val ui = sparse.toDetailUi(LocalDate(2026, 11, 4), ApiEndpoint.Fixture)

        assertTrue(ui.summary.isEmpty())
        assertTrue(ui.notice.isEmpty())
        assertTrue(ui.found.isEmpty())
        assertTrue(ui.health.isEmpty())
        assertNull(ui.traits)
        assertNull(ui.shelter)
        assertTrue(ui.photoUrls.isEmpty())
    }

    @Test
    fun `공고 기간은 같은 해면 끝을 월-일로`() {
        val ui = animal.copy(noticeStart = LocalDate(2026, 11, 3))
            .toDetailUi(LocalDate(2026, 11, 4), ApiEndpoint.Fixture)
        val period = ui.notice.first()
        assertEquals(
            listOf(uiText(Res.string.date_range, "2026-11-03", "11-13")),
            period.values
        )
    }

    @Test
    fun `공유 주소가 있으면 동물이 보일 때 공유 링크, 없으면 null`() = runVmTest {
        val shared = start(
            Env(
                results = SearchResults(items = listOf(animal)),
                shareBaseUrl = "https://a.example/"
            )
        )
        assertEquals("https://a.example/a/$no", shared.state.shareUrl)

        val none = start(Env(results = SearchResults(items = listOf(animal)), shareBaseUrl = " "))
        assertNull(none.state.shareUrl)
        val noConfig = start(Env(results = SearchResults(items = listOf(animal))))
        assertNull(noConfig.state.shareUrl)
    }

    @Test
    fun `공유 결과 — 복사면 안내, 실패면 열 앱 없음, 시트는 조용히`() = runVmTest {
        val env = start(Env(results = SearchResults(items = listOf(animal))))

        env.vm.onShareResult(ShareResult.SHEET_SHOWN)
        advanceUntilIdle()
        assertNull(env.state.message)
        env.vm.onShareResult(ShareResult.COPIED)
        advanceUntilIdle()
        assertEquals(UiMessage(uiText(Res.string.share_copied)), env.state.message.content())
        env.vm.onShareResult(ShareResult.FAILED)
        advanceUntilIdle()
        assertEquals(
            UiMessage(uiText(Res.string.external_open_failed)),
            env.state.message.content()
        )
    }

    @Test
    fun `공유 본문은 제목 다음 줄에 보호소와 공고 종료일`() {
        val ui = animal.copy(noticeStart = LocalDate(2026, 11, 3))
            .toDetailUi(LocalDate(2026, 11, 4), ApiEndpoint.Fixture)
        assertEquals("제주특별자치도 제2동물보호센터", ui.shelterLine?.replace(" · ", " "))
        assertEquals("11-13", ui.noticeEnd)
        assertEquals(
            "2026-11-13",
            animal.copy(
                noticeStart = null
            ).toDetailUi(LocalDate(2026, 11, 4), ApiEndpoint.Fixture).noticeEnd
        )

        assertEquals("제목\n보호소 · 공고 ~11-13", shareText("제목", "보호소", "공고 ~11-13"))
        assertEquals("제목\n보호소", shareText("제목", "보호소", null))
        assertEquals("제목\n공고 ~11-13", shareText("제목", null, "공고 ~11-13"))
        assertEquals("제목", shareText("제목", " ", null))
    }

    // ── 5d: 사망 종료 한 마리 — 북마크거나 필터 켬이면 보이고, 아니면 숨김. 사망 종료는 공유 없음(U7~U12) ──

    private val dead = animal.copy(processState = "종료(자연사)")
    private val share = "https://a.example"
    private val includeDeaths = SearchFilter(includeEndedByDeath = true)

    @Test
    fun `링크로 연 사망 종료는 북마크도 필터도 아니면 숨김(5d U7)`() = runVmTest {
        val env = start(
            Env(key = AnimalDetailKey(no), shareBaseUrl = share) {
                animalResult = { ApiResult.Success(dead) }
            }
        )

        assertTrue(env.state.isHidden)
        assertNull(env.state.detail)
        assertFalse(env.state.notFound)
        assertFalse(env.state.isLoading)
        assertFalse(env.state.isStale)
        assertNull(env.state.error)
        assertNull(env.state.shareUrl)
        assertTrue(env.bookmarkFake.updateCalls.isEmpty())
    }

    @Test
    fun `필터가 켜져 있으면 링크로 연 사망 종료도 보이고 공유는 없다(5d U8)`() = runVmTest {
        val saved = start(
            Env(
                key = AnimalDetailKey(no),
                shareBaseUrl = share,
                savedSettings = Settings(lastFilter = includeDeaths)
            ) { animalResult = { ApiResult.Success(dead) } }
        )
        assertFalse(saved.state.isHidden)
        assertEquals(StatusKind.ENDED, saved.state.detail?.status?.kind)
        assertNull(saved.state.shareUrl)

        // 지금 목록 결과(받은 적 있음)의 조건이 저장된 조건보다 앞선다
        val current = start(
            Env(
                key = AnimalDetailKey(no),
                results = SearchResults(
                    filter = includeDeaths,
                    fetchedAt = Instant.parse("2026-11-04T00:50:00Z")
                ),
                shareBaseUrl = share
            ) { animalResult = { ApiResult.Success(dead) } }
        )
        assertFalse(current.state.isHidden)
        assertEquals(StatusKind.ENDED, current.state.detail?.status?.kind)

        val currentOff = start(
            Env(
                key = AnimalDetailKey(no),
                results = SearchResults(fetchedAt = Instant.parse("2026-11-04T00:50:00Z")),
                savedSettings = Settings(lastFilter = includeDeaths)
            ) { animalResult = { ApiResult.Success(dead) } }
        )
        assertTrue(currentOff.state.isHidden)
    }

    @Test
    fun `북마크에서 연 동물이 사망 종료로 바뀌면 보이고 스냅샷을 갱신, 공유는 없다(5d U9)`() = runVmTest {
        val bookmarks = FakeBookmarkRepository().apply { seed(animal) }
        val gate = CompletableDeferred<Unit>()
        val env = start(
            Env(
                key = AnimalDetailKey(no, "20261103", fromBookmarks = true),
                bookmarkFake = bookmarks,
                shareBaseUrl = share
            ) {
                getAnimalGate = gate
                animalResult = { ApiResult.Success(dead) }
            }
        )
        assertNotEquals(StatusKind.ENDED, env.state.detail?.status?.kind)
        assertEquals("$share/a/$no", env.state.shareUrl)

        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse(env.state.isHidden)
        assertEquals(StatusKind.ENDED, env.state.detail?.status?.kind)
        assertEquals(listOf(dead), bookmarks.updateCalls)
        assertNull(env.state.shareUrl)
    }

    @Test
    fun `북마크된 사망 스냅샷은 북마크에서도 링크로도 보인다(5d U10)`() = runVmTest {
        val fromBookmarks = FakeBookmarkRepository().apply { seed(dead) }
        val gate = CompletableDeferred<Unit>()
        val opened = start(
            Env(
                key = AnimalDetailKey(no, "20261103", fromBookmarks = true),
                bookmarkFake = fromBookmarks
            ) {
                getAnimalGate = gate
                animalResult = { ApiResult.Success(dead) }
            }
        )
        assertEquals(StatusKind.ENDED, opened.state.detail?.status?.kind) // 스냅샷이 먼저
        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(opened.state.isHidden)
        assertEquals(1, opened.animals.getAnimalCalls.size)

        val linked = start(
            Env(
                key = AnimalDetailKey(no),
                bookmarkFake = FakeBookmarkRepository().apply { seed(dead) }
            ) { animalResult = { ApiResult.Success(dead) } }
        )
        assertFalse(linked.state.isHidden)
        assertEquals(StatusKind.ENDED, linked.state.detail?.status?.kind)
        assertEquals(1, linked.animals.getAnimalCalls.size)
    }

    @Test
    fun `두 칸 — 목록 결과의 hidden에 들어오면 북마크가 아닐 때만 숨기고 되돌리지 않는다(5d U11)`() = runVmTest {
        val listed = SearchResults(
            items = listOf(animal),
            fetchedAt = Instant.parse("2026-11-04T00:50:00Z")
        )
        val plain = start(Env(results = listed, shareBaseUrl = share))
        val marked = start(
            Env(results = listed, bookmarkFake = FakeBookmarkRepository().apply { seed(animal) })
        )
        assertEquals<String?>(no, plain.state.detail?.desertionNo)

        val refreshed = listed.copy(items = emptyList(), hidden = listOf(dead))
        plain.animals.results.value = refreshed
        marked.animals.results.value = refreshed
        advanceUntilIdle()

        assertTrue(plain.state.isHidden)
        assertNull(plain.state.detail)
        assertNull(plain.state.shareUrl)
        val calls = plain.animals.getAnimalCalls.size
        plain.animals.animalResult = { ApiResult.Success(animal) }
        plain.vm.onRetry()
        advanceUntilIdle()
        assertTrue(plain.state.isHidden) // 다시 시도가 덮지 않는다
        assertEquals(calls, plain.animals.getAnimalCalls.size)

        assertFalse(marked.state.isHidden)
        assertEquals<String?>(no, marked.state.detail?.desertionNo)
    }

    @Test
    fun `살아 있는 동물은 공유 그대로, 북마크 + 조회 실패는 스냅샷과 스낵바(5d U12)`() = runVmTest {
        val alive =
            start(Env(results = SearchResults(items = listOf(animal)), shareBaseUrl = share))
        assertEquals("$share/a/$no", alive.state.shareUrl)
        assertFalse(alive.state.isHidden)

        val bookmarks = FakeBookmarkRepository().apply { seed(animal) }
        val failed = start(
            Env(key = AnimalDetailKey(no, fromBookmarks = true), bookmarkFake = bookmarks) {
                animalResult = { ApiResult.Failure(ApiError.Network) }
            }
        )
        assertEquals<String?>(no, failed.state.detail?.desertionNo)
        assertFalse(failed.state.isHidden)
        assertEquals(
            UiMessage(uiText(Res.string.error_network), MessageAction.RETRY),
            failed.state.message.content()
        )
    }
}
