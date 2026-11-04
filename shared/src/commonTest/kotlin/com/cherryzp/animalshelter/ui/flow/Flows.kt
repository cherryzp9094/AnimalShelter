package com.cherryzp.animalshelter.ui.flow

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cherryzp.animalshelter.data.repository.SearchResults
import com.cherryzp.animalshelter.fake.testAnimal
import com.cherryzp.animalshelter.model.Animal
import com.cherryzp.animalshelter.model.AnimalKind
import com.cherryzp.animalshelter.model.ListLayout
import com.cherryzp.animalshelter.model.Neutered
import com.cherryzp.animalshelter.model.NoticeState
import com.cherryzp.animalshelter.model.SearchFilter
import com.cherryzp.animalshelter.model.SortOrder
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.a11y_sort_to_latest
import com.cherryzp.animalshelter.resources.action_back
import com.cherryzp.animalshelter.resources.action_sort
import com.cherryzp.animalshelter.resources.action_undo
import com.cherryzp.animalshelter.resources.disclaimer
import com.cherryzp.animalshelter.resources.empty_change_filter
import com.cherryzp.animalshelter.resources.kind_cat
import com.cherryzp.animalshelter.resources.sort_closing_soon
import com.cherryzp.animalshelter.resources.sort_latest
import com.cherryzp.animalshelter.resources.state_notice
import com.cherryzp.animalshelter.ui.common.TestTags
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import org.jetbrains.compose.resources.getString

// UI 흐름 본문(브리핑 C6 T1~T3, 3d D6·D7 T4·T5, 3d 점검 A1 T6, 3e E1·E3·E8 T7~T9·E2 T4 두 단, 4a N4 T10). `@Test` 래퍼는 wasmJsTest·androidDeviceTest의 `UiFlowTest`.

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

/**
 * T4 두 단(3e E2, R9): 두 칸 창(1280×600)의 2단 상세는 품종명 줄이 바 아래로 못 지나가기도 한다 → 본문을 조금
 * (약 40dp) 스크롤해 바가 스크롤 색이 되면 제목이 보이고, 맨 위로 돌아오면 사라진다.
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.detailTitleTwoColumnFlow(fakes: FlowFakes) {
    fakes.animals.results.value = listOf3()

    awaitTag(TestTags.card("B")).performClick()
    awaitTag(TestTags.DETAIL_BOOKMARK)
    awaitIdle()
    assertTrue(
        onAllNodes(hasTestTag(TestTags.DETAIL_TITLE)).fetchSemanticsNodes().isEmpty(),
        "스크롤 전에 바 제목이 보인다"
    )

    // 천천히 끌어 fling 없이 조금만(터치 슬롭을 빼면 약 40dp)
    onNodeWithTag(TestTags.DETAIL_SCROLL).performTouchInput {
        swipeUp(startY = centerY, endY = centerY - 48.dp.toPx(), durationMillis = 500)
    }
    // 제목 노드는 보일 때만 있다(AnimatedVisibility). 기기(세로 411dp)에서는 1280 창의 상세 바가 화면 밖이라
    // assertIsDisplayed 대신 노드가 생기는지로 본다
    awaitTag(TestTags.DETAIL_TITLE)

    onNodeWithTag(TestTags.DETAIL_SCROLL).performTouchInput {
        swipeDown(startY = centerY, endY = centerY + 200.dp.toPx(), durationMillis = 500)
    }
    awaitCondition("맨 위에서 바 제목이 사라짐") {
        onAllNodes(hasTestTag(TestTags.DETAIL_TITLE)).fetchSemanticsNodes().isEmpty()
    }
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

/**
 * T7(3e E1, R1): 상세 요약은 한 줄에 둘, 같은 폭 — 성별 | 중성화, 나이 | 체중, 색상(줄 전체). 둘째 열(중성화·체중)의
 * 왼쪽 끝이 1dp 안으로 같다. 폰 창과 두 칸 창(2단 왼쪽 열) 둘 다.
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.summaryGridFlow(fakes: FlowFakes) {
    val neutered = flowAnimal("B").copy(neutered = Neutered.YES)
    fakes.animals.results.value = listOf3().copy(
        items = listOf(flowAnimal("A"), neutered, flowAnimal("C"))
    )

    awaitTag(TestTags.card("B")).performClick()
    val items = (0..4).map { awaitTag(TestTags.summaryItem(it)) }
    awaitIdle()
    // 두 칸 창은 테스트 화면보다 넓고 길어 일부가 잘린다 — 잘리지 않은 위치로 잰다
    val bounds = items.map { it.getUnclippedBoundsInRoot() }
    val (sex, neuter, age, weight, color) = bounds
    assertNear(neuter.left, weight.left, "둘째 열(중성화·체중)")
    assertNear(sex.left, age.left, "첫째 열(성별·나이)")
    assertNear(sex.top, neuter.top, "첫 줄(성별 | 중성화)")
    assertNear(age.top, weight.top, "둘째 줄(나이 | 체중)")
    assertTrue(neuter.left > sex.right, "중성화가 성별 오른쪽에 없다: $sex $neuter")
    assertTrue(color.top > weight.top, "색상이 셋째 줄에 없다: $weight $color")
    assertNear(sex.left, color.left, "색상 왼쪽 끝")
    assertNear(neuter.right, color.right, "색상(줄 전체) 오른쪽 끝")
}

private fun assertNear(a: Dp, b: Dp, what: String) {
    assertTrue(abs((a - b).value) <= 1f, "$what: $a ≠ $b")
}

/**
 * T9(3e E8, R4): 요약 칩의 클릭 동작 이름 — 조건 칩 "조건 바꾸기", 마감 임박순 칩 "최신순으로 바꾸기". 칩마다 클릭 동작이
 * 하나(안쪽 노드에 따로 없다)이고, 이름 있는 그 동작이 실제로 정렬을 바꾼다.
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.summaryChipActionLabelFlow(fakes: FlowFakes) {
    fakes.animals.results.value = listOf3().copy(sort = SortOrder.CLOSING_SOON)
    val changeFilter = getString(Res.string.empty_change_filter)
    val toLatest = getString(Res.string.a11y_sort_to_latest)
    val sortChipText = getString(Res.string.sort_closing_soon)
    val inChips = hasAnyAncestor(hasTestTag(TestTags.SUMMARY_CHIPS))

    awaitCondition("마감 임박순 칩") {
        onAllNodes(hasText(sortChipText) and inChips).fetchSemanticsNodes().isNotEmpty()
    }
    // 칩: 공고중 · 최근 1개월 · 마감 임박순 — 클릭 노드는 칩마다 하나(병합 전 트리에서도)
    val clickNodes = onAllNodes(hasClickAction() and inChips, useUnmergedTree = true)
        .fetchSemanticsNodes()
    val labels = clickNodes.map { it.config.getOrNull(SemanticsActions.OnClick)?.label }
    assertEquals(listOf(changeFilter, changeFilter, toLatest), labels)

    onNode(hasText(sortChipText) and inChips).performSemanticsAction(SemanticsActions.OnClick)
    awaitCondition("최신순으로 바뀜") { fakes.animals.sortCalls.lastOrNull() == SortOrder.LATEST }
}

/** 스크롤할 만큼의 테스트 동물(사진 없음). 번호는 정렬해도 같은 순서가 되게 0으로 채운다. */
private fun manyResults(count: Int = 40): SearchResults = SearchResults(
    items = (1..count).map { flowAnimal("N" + it.toString().padStart(3, '0')) },
    totalCount = count,
    fetchedAt = Instant.parse("2026-11-04T01:00:00Z")
)

private const val FIRST_MANY = "N001"

/** 바가 다 접히거나 펼쳐졌는지 볼 때의 허용 오차와 바 높이(M3 작은 상단 바 64dp). */
private val BarHeight = 64.dp
private const val BAR_TOLERANCE_DP = 1f

/**
 * 그리드를 끈 뒤 멈췄다가 뗀다(+면 아래로 — 위 내용이 내려온다). 끝에서 멈추므로 fling이 없다 — fling이 남으면
 * 다음 단계(조건 바꾸기)와 겹친다. 시작 x는 그리드 왼쪽 가장자리 여백(12dp — 카드 밖): 카드를 누르면 웹에서는 그
 * 카드가 초점을 받고, 바가 펼쳐져 칸이 줄 때 Compose가 초점 카드를 보이게 하려고 스크롤해 그만큼 바를 다시 접는다.
 * 기기(세로 411dp)에서 900dp 창의 가운데는 화면 밖이기도 하다(P14).
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.dragGrid(byDp: Float) {
    onNodeWithTag(TestTags.ANIMAL_GRID).performTouchInput { dragAndHold(byDp) }
}

private fun TouchInjectionScope.dragAndHold(byDp: Float) {
    val steps = 20
    val stepPx = byDp.dp.toPx() / steps
    down(Offset(12.dp.toPx(), centerY))
    repeat(steps) {
        advanceEventTime(16)
        moveBy(Offset(0f, stepPx))
    }
    repeat(10) {
        advanceEventTime(30)
        moveBy(Offset.Zero)
    }
    up()
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.heightOf(tag: String): Dp =
    onNodeWithTag(tag).getUnclippedBoundsInRoot().let { it.bottom - it.top }

@OptIn(ExperimentalTestApi::class)
private suspend fun ComposeUiTest.awaitBarCollapsed(tag: String, expanded: Dp) {
    awaitCondition("상단 바가 접힘($tag)") {
        (expanded - heightOf(tag)).value >= BarHeight.value - BAR_TOLERANCE_DP
    }
}

@OptIn(ExperimentalTestApi::class)
private suspend fun ComposeUiTest.awaitBarExpanded(tag: String, expanded: Dp) {
    awaitCondition("상단 바가 펼쳐짐($tag)") {
        abs((heightOf(tag) - expanded).value) <= BAR_TOLERANCE_DP
    }
}

/**
 * T10(4a N4): 창 높이가 compact(폰 가로 900×400)면 목록 상단 바가 `enterAlways` — 그리드를 위로 약 200dp 끌면
 * 바가 접히고(높이 −64dp — 기기는 상태 바 inset이 남을 수 있어 처음 높이와의 차로 잰다), 조금 내리면 바로 다시
 * 64dp. 접힌 채 조건이 바뀌면(결과의 조건을 바꾼다) 그리드가 맨 위로 가고 바도 펼쳐진다.
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.landscapeTopBarHidesFlow(fakes: FlowFakes) {
    fakes.animals.results.value = manyResults()
    awaitTag(TestTags.card(FIRST_MANY))
    awaitIdle()
    val bar = TestTags.LIST_TOP_BAR
    val expanded = heightOf(bar)

    dragGrid(-200f)
    awaitBarCollapsed(bar, expanded)

    dragGrid(120f)
    awaitBarExpanded(bar, expanded)

    dragGrid(-200f)
    awaitBarCollapsed(bar, expanded)
    awaitCondition("첫 카드가 위로 지나감") {
        onAllNodes(hasTestTag(TestTags.card(FIRST_MANY))).fetchSemanticsNodes().isEmpty() ||
            onNodeWithTag(TestTags.card(FIRST_MANY)).getUnclippedBoundsInRoot().top < 0.dp
    }

    // 접힌 채 조건이 바뀌면(예: 보호소 화면의 "이 보호소의 공고 보기"로 돌아온 목록) 맨 위 + 바 펼침
    fakes.animals.results.value =
        fakes.animals.results.value.copy(filter = SearchFilter(kind = AnimalKind.DOG))
    awaitBarExpanded(bar, expanded)
    awaitCondition("그리드가 맨 위(첫 카드가 바 아래)") {
        onAllNodes(hasTestTag(TestTags.card(FIRST_MANY))).fetchSemanticsNodes().isNotEmpty() &&
            onNodeWithTag(TestTags.card(FIRST_MANY)).getUnclippedBoundsInRoot().top >=
            onNodeWithTag(bar).getUnclippedBoundsInRoot().bottom
    }
}

/** T10 북마크 목록(4a N4): 폰 가로에서 북마크 목록 상단 바도 끌면 접히고 내리면 펼쳐진다. */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.landscapeBookmarksTopBarHidesFlow(fakes: FlowFakes) {
    fakes.animals.results.value = listOf3()
    manyResults().items.reversed().forEach { fakes.bookmarks.add(it) }

    awaitTag(TestTags.OPEN_BOOKMARKS).performClick()
    awaitTag(TestTags.card(FIRST_MANY))
    awaitIdle()
    val bar = TestTags.BOOKMARKS_TOP_BAR
    val expanded = heightOf(bar)

    dragGrid(-200f)
    awaitBarCollapsed(bar, expanded)
    dragGrid(120f)
    awaitBarExpanded(bar, expanded)
}

/** T10 반대(4a N4): 폰 세로(400×760)에서는 같은 끌기에도 바가 pinned 그대로(그리드만 스크롤된다). */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.portraitTopBarStaysFlow(fakes: FlowFakes) {
    fakes.animals.results.value = manyResults()
    awaitTag(TestTags.card(FIRST_MANY))
    awaitIdle()
    val bar = TestTags.LIST_TOP_BAR
    val expanded = heightOf(bar)
    val firstTop = onNodeWithTag(TestTags.card(FIRST_MANY)).getUnclippedBoundsInRoot().top

    dragGrid(-200f)
    awaitCondition("그리드가 스크롤됨") {
        onAllNodes(hasTestTag(TestTags.card(FIRST_MANY))).fetchSemanticsNodes().isEmpty() ||
            onNodeWithTag(TestTags.card(FIRST_MANY)).getUnclippedBoundsInRoot().top < firstTop
    }
    awaitIdle()
    assertNear(heightOf(bar), expanded, "세로 상단 바 높이")
}
