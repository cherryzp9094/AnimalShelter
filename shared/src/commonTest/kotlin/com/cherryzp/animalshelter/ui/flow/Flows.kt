package com.cherryzp.animalshelter.ui.flow

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.cherryzp.animalshelter.data.repository.SearchResults
import com.cherryzp.animalshelter.fake.testAnimal
import com.cherryzp.animalshelter.model.Animal
import com.cherryzp.animalshelter.model.AnimalKind
import com.cherryzp.animalshelter.model.ListLayout
import com.cherryzp.animalshelter.model.NoticeState
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.action_back
import com.cherryzp.animalshelter.resources.action_sort
import com.cherryzp.animalshelter.resources.action_undo
import com.cherryzp.animalshelter.resources.disclaimer
import com.cherryzp.animalshelter.resources.kind_cat
import com.cherryzp.animalshelter.resources.sort_closing_soon
import com.cherryzp.animalshelter.resources.sort_latest
import com.cherryzp.animalshelter.resources.state_notice
import com.cherryzp.animalshelter.ui.common.TestTags
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import org.jetbrains.compose.resources.getString

// UI 흐름 본문(브리핑 C6 T1~T3, 3d D6·D7 T4·T5, 3d 점검 A1 T6, 3e E1·E3·E8 T7~T9·E2 T4 두 단). `@Test` 래퍼는 wasmJsTest·androidDeviceTest의 `UiFlowTest`.

/** 사진 없는 테스트 동물(네트워크 금지). */
private fun flowAnimal(no: String): Animal = testAnimal(desertionNo = no, photos = emptyList())

private fun listOf3(): SearchResults = SearchResults(
    items = listOf(flowAnimal("A"), flowAnimal("B"), flowAnimal("C")),
    totalCount = 3,
    fetchedAt = Instant.parse("2026-11-04T01:00:00Z")
)

/** T1: 목록 → 상세 → 북마크 추가 → 뒤로 → 북마크 목록에 그 카드. */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.listToBookmarksFlow(fakes: FlowFakes) {
    fakes.animals.results.value = listOf3()

    awaitTag(TestTags.card("B")).performClick()
    awaitTag(TestTags.DETAIL_BOOKMARK).performClick()
    awaitCondition("북마크에 B가 저장됨") {
        fakes.bookmarks.bookmarks.value.any { it.animal.desertionNo == "B" }
    }

    onNode(hasContentDescription(getString(Res.string.action_back))).performClick()
    awaitTag(TestTags.OPEN_BOOKMARKS).performClick()
    awaitTag(TestTags.card("B")).assertIsDisplayed()
    assertEquals(listOf("B"), fakes.bookmarks.bookmarks.value.map { it.animal.desertionNo })
}

/** T2: 필터 — 고양이 + 공고중 → 검색하기 → 목록 칩에 "고양이"·"공고중", Fake의 마지막 검색 조건이 그 값. */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.filterFlow(fakes: FlowFakes) {
    fakes.animals.results.value = listOf3()

    awaitTag(TestTags.OPEN_FILTER).performClick()
    awaitTag(TestTags.choice("kind", AnimalKind.CAT)).performScrollTo().performClick()
    awaitTag(TestTags.choice("state", NoticeState.NOTICE)).performScrollTo().performClick()
    awaitTag(TestTags.FILTER_APPLY).performClick()

    val cat = getString(Res.string.kind_cat)
    val notice = getString(Res.string.state_notice)
    awaitCondition("칩에 고양이") {
        onAllNodes(hasText(cat) and hasAnyAncestor(hasTestTag(TestTags.SUMMARY_CHIPS)))
            .fetchSemanticsNodes().isNotEmpty()
    }
    onNode(
        hasText(notice) and hasAnyAncestor(hasTestTag(TestTags.SUMMARY_CHIPS))
    ).assertIsDisplayed()
    val last = fakes.animals.searchCalls.last()
    assertEquals(AnimalKind.CAT, last.kind)
    assertEquals(NoticeState.NOTICE, last.state)
}

/**
 * T3: 되돌리기(F4) — 북마크 셋 → 북마크 목록에서 카드의 북마크로 제거 → 스낵바 "되돌리기" → 그 카드가 같은 자리에
 * 보인다. [layout] 2열은 가운데 것을, 1열은 첫 카드를 지운다(화면 밖 복원 회귀 — 1열 첫 카드는 맨 위로 숨었다).
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.bookmarkUndoFlow(fakes: FlowFakes, layout: ListLayout) {
    fakes.animals.results.value = listOf3()
    fakes.settings.setListLayout(layout)
    listOf("C", "B", "A").forEach { fakes.bookmarks.add(flowAnimal(it)) }
    val target = if (layout == ListLayout.GRID) "B" else "A"

    awaitTag(TestTags.OPEN_BOOKMARKS).performClick()
    val before = awaitTag(TestTags.card(target)).getUnclippedBoundsInRoot()

    awaitTag(TestTags.cardBookmark(target)).performClick()
    awaitCondition("북마크에서 대상이 빠짐") {
        fakes.bookmarks.bookmarks.value.none { it.animal.desertionNo == target }
    }
    val undo = getString(Res.string.action_undo)
    awaitCondition("스낵바 되돌리기 버튼") {
        onAllNodes(hasText(undo)).fetchSemanticsNodes().isNotEmpty()
    }
    onNode(hasText(undo)).performClick()

    awaitCondition("대상이 되살아남") {
        fakes.bookmarks.bookmarks.value.any { it.animal.desertionNo == target }
    }
    assertEquals(
        listOf("A", "B", "C"),
        fakes.bookmarks.bookmarks.value.map {
            it.animal.desertionNo
        }
    )
    // 되살린 카드가 화면에 보일 때까지(1열 첫 카드는 restoredKey → 스크롤이 한 박자 뒤에 온다)
    val card = awaitTag(TestTags.card(target))
    awaitCondition("되살린 카드가 보임") { runCatching { card.assertIsDisplayed() }.isSuccess }
    awaitIdle()
    val after = card.getUnclippedBoundsInRoot()
    assertTrue(
        after.top == before.top && after.left == before.left,
        "되살린 카드가 원래 자리에 없다: before=$before after=$after"
    )
}

/** T4(D6): 상세 바 제목은 처음엔 없고, 본문을 끝까지 내려 품종명 줄이 바 아래로 지나가면 생긴다. */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.detailTitleFlow(fakes: FlowFakes) {
    fakes.animals.results.value = listOf3()

    awaitTag(TestTags.card("B")).performClick()
    awaitTag(TestTags.DETAIL_BOOKMARK)
    awaitIdle()
    assertTrue(
        onAllNodes(hasTestTag(TestTags.DETAIL_TITLE)).fetchSemanticsNodes().isEmpty(),
        "스크롤 전에 바 제목이 보인다"
    )

    val disclaimer = getString(Res.string.disclaimer)
    awaitCondition("면책 문구") { onAllNodes(hasText(disclaimer)).fetchSemanticsNodes().isNotEmpty() }
    onNode(hasText(disclaimer)).performScrollTo()
    awaitTag(TestTags.DETAIL_TITLE).assertIsDisplayed()
}

/** T5(D7): 두 칸(1280)에서 연 카드만 `selected`, 다른 카드로 바꾸면 표시도 옮겨 간다. */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.openedCardMarkFlow(fakes: FlowFakes) {
    fakes.animals.results.value = listOf3()
    val selected = SemanticsMatcher.expectValue(SemanticsProperties.Selected, true)

    awaitTag(TestTags.card("B")).performClick()
    awaitCondition("B가 열린 카드로 표시됨") {
        onAllNodes(hasTestTag(TestTags.card("B")) and selected).fetchSemanticsNodes().isNotEmpty()
    }
    onNodeWithTag(TestTags.card("B")).assertIsSelected()
    assertTrue(onAllNodes(selected).fetchSemanticsNodes().size == 1, "열린 카드 표시가 하나가 아니다")

    onNodeWithTag(TestTags.card("C")).performClick()
    awaitCondition("표시가 C로 옮겨 감") {
        onAllNodes(hasTestTag(TestTags.card("C")) and selected).fetchSemanticsNodes().isNotEmpty()
    }
    assertTrue(
        onAllNodes(hasTestTag(TestTags.card("B")) and selected).fetchSemanticsNodes().isEmpty(),
        "B에 표시가 남았다"
    )
}

/** T5 반대(D7): 한 칸(폰)에서는 상세를 열었다 돌아와도 카드에 표시가 없다. */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.noMarkOnPhoneFlow(fakes: FlowFakes) {
    fakes.animals.results.value = listOf3()
    val selected = SemanticsMatcher.expectValue(SemanticsProperties.Selected, true)

    awaitTag(TestTags.card("B")).performClick()
    awaitTag(TestTags.DETAIL_BOOKMARK)
    onNode(hasContentDescription(getString(Res.string.action_back))).performClick()
    awaitTag(TestTags.card("B"))
    awaitIdle()
    assertTrue(onAllNodes(selected).fetchSemanticsNodes().isEmpty(), "한 칸에서 열린 카드 표시")
}

/** T6(3d 점검 A1): 정렬 메뉴는 지금 정렬을 `selected`로 알린다(체크 아이콘은 장식). */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.sortMenuSelectedFlow(fakes: FlowFakes) {
    fakes.animals.results.value = listOf3()
    awaitTag(TestTags.card("A"))

    onNode(hasContentDescription(getString(Res.string.action_sort))).performClick()
    val latest = getString(Res.string.sort_latest)
    awaitCondition("정렬 메뉴") { onAllNodes(hasText(latest)).fetchSemanticsNodes().isNotEmpty() }
    onNode(hasText(latest)).assertIsSelected()
    onNode(hasText(getString(Res.string.sort_closing_soon))).assertIsNotSelected()
}

/** T8(3e E3): 창 높이가 compact(폰 가로 900×400)면 폭이 expanded여도 한 칸 — 상세만 보이고 뒤로 화살표가 있다. */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.landscapeSinglePaneFlow(fakes: FlowFakes) {
    fakes.animals.results.value = listOf3()

    awaitTag(TestTags.card("B")).performClick()
    awaitTag(TestTags.DETAIL_BOOKMARK)
    awaitCondition("목록 카드가 사라짐(상세만 보임)") {
        onAllNodes(hasTestTag(TestTags.card("A"))).fetchSemanticsNodes().isEmpty()
    }
    onNode(hasContentDescription(getString(Res.string.action_back))).assertIsDisplayed()
}
