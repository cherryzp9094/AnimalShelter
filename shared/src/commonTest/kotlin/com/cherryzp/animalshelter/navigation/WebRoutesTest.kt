package com.cherryzp.animalshelter.navigation

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/** 웹 주소·기록 state·동기화 규칙(브리핑 W2·W3). */
class WebRoutesTest {
    private val no = "450650202602738"
    private val list: NavKey = AnimalListKey
    private val detail = AnimalDetailKey(no, happenDate = "20261103")
    private val photo =
        PhotoViewerKey(no, index = 1, photos = listOf("https://x/a.jpg"), title = "고양이")

    @Test
    fun `화면마다 해시`() {
        assertEquals("#/", WebRoutes.hashOf(AnimalListKey))
        assertEquals("#/filter", WebRoutes.hashOf(FilterKey))
        assertEquals("#/animal/$no", WebRoutes.hashOf(detail))
        assertEquals("#/animal/$no/photo/1", WebRoutes.hashOf(photo))
        assertEquals("#/bookmarks", WebRoutes.hashOf(BookmarkListKey))
        assertEquals(
            "#/shelter/311322201900001",
            WebRoutes.hashOf(ShelterInfoKey("311322201900001", "이름"))
        )
        assertEquals("#/settings", WebRoutes.hashOf(SettingsKey))
        assertEquals("#/animal/$no", WebRoutes.hashOf(listOf(list, detail)))
        assertEquals("#/", WebRoutes.hashOf(emptyList()))
    }

    @Test
    fun `해시는 계층형 스택으로 연다`() {
        assertEquals(listOf(list), WebRoutes.stackFromHash("#/"))
        assertEquals(listOf(list, FilterKey), WebRoutes.stackFromHash("#/filter"))
        assertEquals(listOf(list, AnimalDetailKey(no)), WebRoutes.stackFromHash("#/animal/$no"))
        assertEquals(
            listOf(list, AnimalDetailKey(no), PhotoViewerKey(no, index = 3)),
            WebRoutes.stackFromHash("#/animal/$no/photo/3")
        )
        assertEquals(listOf(list, BookmarkListKey), WebRoutes.stackFromHash("#/bookmarks"))
        assertEquals(
            listOf(list, ShelterInfoKey("3113")),
            WebRoutes.stackFromHash("#/shelter/3113")
        )
        assertEquals(listOf(list, SettingsKey), WebRoutes.stackFromHash("#/settings"))
    }

    @Test
    fun `오픈소스 라이선스는 설정 아래 주소, 해시는 목록·설정·라이선스 스택`() {
        assertEquals("#/settings/licenses", WebRoutes.hashOf(LicensesKey))
        assertEquals(
            "#/settings/licenses",
            WebRoutes.hashOf(listOf(list, SettingsKey, LicensesKey))
        )
        assertEquals(
            listOf(list, SettingsKey, LicensesKey),
            WebRoutes.stackFromHash("#/settings/licenses")
        )
        listOf("#/settings/licenses/", "#/settings/licenses/x", "#/settings/x", "#/licenses")
            .forEach { assertEquals(listOf(list), WebRoutes.stackFromHash(it), it) }
    }

    @Test
    fun `라이선스 화면이 든 기록 state 왕복과 주소 진입 뒤 앱 안 뒤로`() {
        val stack = listOf(list, SettingsKey, LicensesKey)
        assertEquals(
            HistoryEntry(2, stack),
            decodeHistoryState(encodeHistoryState(HistoryEntry(2, stack)))
        )

        // 주소로 들어오면 깊이 0 — 앱 안 뒤로(설정)는 기록을 쌓지 않고 바꾼다(브라우저 뒤로는 온 곳으로)
        val (sync, start) = WebHistorySync.start(null, "#/settings/licenses")
        assertEquals(HistoryEntry(0, stack), start.entry())
        val back = sync.onAppChanged(listOf(list, SettingsKey))
        assertIs<HistoryCommand.Replace>(back)
        assertEquals("#/settings", back.url)
    }

    @Test
    fun `형식이 맞지 않는 해시는 목록`() {
        listOf(
            "",
            "#",
            "#/unknown",
            "#animal/$no",
            "#/animal/12345",
            "#/animal/${no}1",
            "#/animal/45065020260273a",
            "#/animal/$no/",
            "#/animal/$no/photo/-1",
            "#/animal/$no/photo/x",
            "#/animal/$no/photo/",
            "#/shelter/",
            "#/shelter/abc",
            "#/filter/x",
            "#/animal/$no?x=1"
        ).forEach { assertEquals(listOf(list), WebRoutes.stackFromHash(it), it) }
    }

    @Test
    fun `기록 state는 스택 전체와 깊이를 그대로 되살린다`() {
        val stack = listOf(list, detail.copy(fromBookmarks = true), photo)
        val raw = encodeHistoryState(HistoryEntry(2, stack))

        assertEquals(HistoryEntry(2, stack), decodeHistoryState(raw))
        val shelter = listOf(list, ShelterInfoKey("3113", "이름", "02-000", "주소"))
        assertEquals(
            HistoryEntry(0, shelter),
            decodeHistoryState(encodeHistoryState(HistoryEntry(0, shelter)))
        )
    }

    @Test
    fun `읽을 수 없는 state는 null`() {
        val valid = encodeHistoryState(HistoryEntry(1, listOf(list, FilterKey)))
        listOf(
            null,
            "",
            "x",
            "null",
            "[]",
            valid.replace("\"v\":1", "\"v\":2"),
            valid.replace("\"d\":1", "\"d\":-1"),
            encodeHistoryState(HistoryEntry(0, listOf(FilterKey))),
            encodeHistoryState(HistoryEntry(0, emptyList())),
            """{"v":1,"d":0,"s":[{"type":"Nope"}]}""",
            """{"key":"value"}"""
        ).forEach { assertNull(decodeHistoryState(it), "$it") }
    }

    private fun HistoryCommand?.entry(): HistoryEntry = when (this) {
        is HistoryCommand.Push -> decodeHistoryState(state)!!
        is HistoryCommand.Replace -> decodeHistoryState(state)!!
        else -> error("state 없는 명령: $this")
    }

    @Test
    fun `시작 — state가 있으면 그것, 없으면 해시를 깊이 0으로 정규화`() {
        val saved = encodeHistoryState(HistoryEntry(2, listOf(list, detail, photo)))
        val (restored, none) = WebHistorySync.start(saved, "#/whatever")
        assertEquals(listOf(list, detail, photo), restored.lastSynced)
        assertEquals(2, restored.depth)
        assertNull(none)

        val (fresh, replace) = WebHistorySync.start(null, "#/animal/$no")
        assertEquals(listOf(list, AnimalDetailKey(no)), fresh.lastSynced)
        assertEquals(0, fresh.depth)
        assertEquals("#/animal/$no", replace?.url)
        assertEquals(HistoryEntry(0, listOf(list, AnimalDetailKey(no))), replace.entry())
    }

    @Test
    fun `앱이 화면 하나를 더하면 push, 위에서 빼면 go(-n)`() {
        val (sync, _) = WebHistorySync.start(null, "#/")

        val push = sync.onAppChanged(listOf(list, detail))
        assertIs<HistoryCommand.Push>(push)
        assertEquals("#/animal/$no", push.url)
        assertEquals(HistoryEntry(1, listOf(list, detail)), push.entry())
        assertEquals(
            HistoryEntry(2, listOf(list, detail, photo)),
            sync.onAppChanged(listOf(list, detail, photo)).entry()
        )
        assertNull(sync.onAppChanged(listOf(list, detail, photo)), "같으면 없음")

        assertEquals(HistoryCommand.Go(-2), sync.onAppChanged(listOf(list)))
        // popstate가 오기 전까지 지금 기록·깊이를 바꾸지 않는다
        assertEquals(2, sync.depth)
        val arrived = encodeHistoryState(HistoryEntry(0, listOf(list)))
        assertEquals(PopOutcome(apply = null, deferred = null), sync.onPopState(arrived, "#/"))
        assertEquals(0, sync.depth)
        assertEquals(listOf(list), sync.lastSynced)
    }

    @Test
    fun `링크로 들어온 상세에서 앱 안 뒤로는 기록을 쌓지 않고 바꾼다`() {
        val (sync, _) = WebHistorySync.start(null, "#/animal/$no")

        val replace = sync.onAppChanged(listOf(list))

        assertIs<HistoryCommand.Replace>(replace)
        assertEquals("#/", replace.url)
        assertEquals(HistoryEntry(0, listOf(list)), replace.entry())
    }

    @Test
    fun `두 칸에서 상세를 바꾸면 replace(깊이 그대로)`() {
        val (sync, _) = WebHistorySync.start(null, "#/")
        sync.onAppChanged(listOf(list, AnimalDetailKey("450650202600001")))

        val replace = sync.onAppChanged(listOf(list, AnimalDetailKey("450650202600002")))

        assertIs<HistoryCommand.Replace>(replace)
        assertEquals(
            HistoryEntry(1, listOf(list, AnimalDetailKey("450650202600002"))),
            replace.entry()
        )
    }

    @Test
    fun `브라우저 뒤로·앞으로는 기록의 스택으로 맞춘다`() {
        val (sync, _) = WebHistorySync.start(null, "#/")
        sync.onAppChanged(listOf(list, detail))

        val back = sync.onPopState(encodeHistoryState(HistoryEntry(0, listOf(list))), "#/")
        assertEquals(PopOutcome(apply = listOf(list), deferred = null), back)
        assertNull(sync.onAppChanged(listOf(list)), "맞춘 스택은 다시 기록하지 않는다")

        val forward = sync.onPopState(
            encodeHistoryState(HistoryEntry(1, listOf(list, detail))),
            "#/animal/$no"
        )
        assertEquals(PopOutcome(apply = listOf(list, detail), deferred = null), forward)
        assertEquals(1, sync.depth)
        assertNull(sync.onAppChanged(listOf(list, detail)))
    }

    @Test
    fun `state 없는 popstate(주소창·해시 링크)는 해시를 계층형으로 열고 깊이 + 1로 replace(미뤄서)`() {
        val (sync, _) = WebHistorySync.start(null, "#/")

        val outcome = sync.onPopState(null, "#/settings")

        assertEquals(listOf(list, SettingsKey), outcome.apply)
        assertIs<HistoryCommand.Replace>(outcome.deferred)
        assertEquals(HistoryEntry(1, listOf(list, SettingsKey)), outcome.deferred.entry())
        assertNull(sync.onAppChanged(listOf(list, SettingsKey)))
        // 이어서 앱 안 뒤로 → 깊이 1이니 go(-1)
        assertEquals(HistoryCommand.Go(-1), sync.onAppChanged(listOf(list)))
    }

    @Test
    fun `go(-n)이 주소창으로 만든 기록에 닿으면 그 기록을 앱 스택으로 고친다`() {
        val (sync, _) = WebHistorySync.start(null, "#/")
        sync.onAppChanged(listOf(list, detail))
        assertEquals(HistoryCommand.Go(-1), sync.onAppChanged(listOf(list)))

        val outcome = sync.onPopState(null, "#/bookmarks")

        assertNull(outcome.apply, "앱은 기대 스택 그대로")
        assertIs<HistoryCommand.Replace>(outcome.deferred)
        assertEquals(HistoryEntry(0, listOf(list)), outcome.deferred.entry())
        assertEquals("#/", outcome.deferred.url)
    }

    @Test
    fun `go를 기다리는 사이 앱이 또 바꾸면 도착한 뒤 이어서 맞춘다`() {
        val (sync, _) = WebHistorySync.start(null, "#/")
        sync.onAppChanged(listOf(list, detail))
        sync.onAppChanged(listOf(list, detail, photo))
        assertEquals(HistoryCommand.Go(-1), sync.onAppChanged(listOf(list, detail)))
        assertNull(sync.onAppChanged(listOf(list)), "기다리는 동안은 명령 없음")

        val outcome = sync.onPopState(
            encodeHistoryState(HistoryEntry(1, listOf(list, detail))),
            "#/animal/$no"
        )

        assertEquals(PopOutcome(apply = null, deferred = HistoryCommand.Go(-1)), outcome)
    }
}
