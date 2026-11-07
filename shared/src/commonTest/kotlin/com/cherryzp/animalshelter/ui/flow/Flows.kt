package com.cherryzp.animalshelter.ui.flow

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.cherryzp.animalshelter.data.api.ApiError
import com.cherryzp.animalshelter.data.api.ApiResult
import com.cherryzp.animalshelter.data.repository.SearchResults
import com.cherryzp.animalshelter.fake.loaded
import com.cherryzp.animalshelter.fake.testAnimal
import com.cherryzp.animalshelter.model.Animal
import com.cherryzp.animalshelter.model.AnimalKind
import com.cherryzp.animalshelter.model.Kind
import com.cherryzp.animalshelter.model.ListLayout
import com.cherryzp.animalshelter.model.Neutered
import com.cherryzp.animalshelter.model.NoticeState
import com.cherryzp.animalshelter.model.SearchFilter
import com.cherryzp.animalshelter.model.SortOrder
import com.cherryzp.animalshelter.navigation.AnimalDetailKey
import com.cherryzp.animalshelter.navigation.AnimalListKey
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.a11y_days_left
import com.cherryzp.animalshelter.resources.a11y_loading
import com.cherryzp.animalshelter.resources.a11y_sort_to_latest
import com.cherryzp.animalshelter.resources.about_licenses
import com.cherryzp.animalshelter.resources.action_back
import com.cherryzp.animalshelter.resources.action_more
import com.cherryzp.animalshelter.resources.action_next_photo
import com.cherryzp.animalshelter.resources.action_previous_photo
import com.cherryzp.animalshelter.resources.action_sort
import com.cherryzp.animalshelter.resources.action_undo
import com.cherryzp.animalshelter.resources.chip_include_deaths
import com.cherryzp.animalshelter.resources.detail_hidden
import com.cherryzp.animalshelter.resources.detail_placeholder
import com.cherryzp.animalshelter.resources.disclaimer
import com.cherryzp.animalshelter.resources.empty_bookmarks
import com.cherryzp.animalshelter.resources.empty_change_filter
import com.cherryzp.animalshelter.resources.empty_results
import com.cherryzp.animalshelter.resources.error_network
import com.cherryzp.animalshelter.resources.filter_include_deaths
import com.cherryzp.animalshelter.resources.filter_include_deaths_disabled
import com.cherryzp.animalshelter.resources.kind_cat
import com.cherryzp.animalshelter.resources.list_load_more_failed
import com.cherryzp.animalshelter.resources.photo_a11y
import com.cherryzp.animalshelter.resources.photo_counter
import com.cherryzp.animalshelter.resources.sort_closing_soon
import com.cherryzp.animalshelter.resources.sort_latest
import com.cherryzp.animalshelter.resources.state_notice
import com.cherryzp.animalshelter.resources.status_ended_reason
import com.cherryzp.animalshelter.resources.status_protecting
import com.cherryzp.animalshelter.resources.title_settings
import com.cherryzp.animalshelter.ui.common.TestTags
import kotlin.math.abs
import kotlin.math.min
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

// UI 흐름 본문(브리핑 C6 T1~T3, 3d D6·D7 T4·T5, 3d 점검 A1 T6, 3e E1·E3·E8 T7~T9·E2 T4 두 단, 4a N4·4a 검토 F2 T10, 4a 검토 F4 T11,
// 4a-fixes 검토 G1 T12, 5a K1 T13·U1·U2 T14·Y1·Y2 T15·O1 T16, 5a-live-fixes L5 T17·L6 T18,
// 4b-fixes B1 T19). `@Test` 래퍼는 wasmJsTest·androidDeviceTest의 `UiFlowTest`.

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

/**
 * T10 북마크(4a 검토 F2): 폰 가로에서 바가 접힌 채 북마크가 줄어 그리드가 스크롤할 수 없게 되면 바가 다시 64dp, 다
 * 지워 빈 안내만 남아도 64dp(제목·뒤로가 보인다).
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.landscapeBookmarksBarReturnsFlow(fakes: FlowFakes) {
    fakes.animals.results.value = listOf3()
    manyResults().items.reversed().forEach { fakes.bookmarks.add(it) }

    awaitTag(TestTags.OPEN_BOOKMARKS).performClick()
    awaitTag(TestTags.card(FIRST_MANY))
    awaitIdle()
    val bar = TestTags.BOOKMARKS_TOP_BAR
    val expanded = heightOf(bar)

    dragGrid(-200f)
    awaitBarCollapsed(bar, expanded)

    // 카드의 북마크 버튼으로 지운 것과 같다(지우면 목록에서 바로 빠진다) — 3개면 한 줄이라 스크롤할 수 없다
    fakes.bookmarks.bookmarks.value = fakes.bookmarks.bookmarks.value.take(3)
    awaitBarExpanded(bar, expanded)

    fakes.bookmarks.bookmarks.value = emptyList()
    val emptyText = getString(Res.string.empty_bookmarks)
    awaitCondition("빈 북마크 안내") { onAllNodes(hasText(emptyText)).fetchSemanticsNodes().isNotEmpty() }
    awaitBarExpanded(bar, expanded)
}

/**
 * T10 목록(4a 검토 F2): 폰 가로에서 바가 접힌 채 같은 조건의 결과가 줄어도(`queryKey`가 그대로 — 맨 위로 가는 효과가
 * 돌지 않는다) 바가 숨은 채 갇히지 않는다.
 * - 3개: 목록은 요약 칩·결과 헤더가 있어 900×400에서는 한 줄이어도 그리드가 아직 스크롤된다(웹 22px) — 그때는 끌어서
 *   바를 꺼낼 수 있으므로, "바가 펼쳐졌거나 그리드가 스크롤된다"를 본다(브리핑의 "3개 → 64dp"는 이 창에서 성립하지
 *   않는다)
 * - 0개: 빈 결과 전체 화면(그리드 없음) — 바 64dp
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.landscapeListBarReturnsFlow(fakes: FlowFakes) {
    fakes.animals.results.value = manyResults()
    awaitTag(TestTags.card(FIRST_MANY))
    awaitIdle()
    val bar = TestTags.LIST_TOP_BAR
    val expanded = heightOf(bar)

    dragGrid(-200f)
    awaitBarCollapsed(bar, expanded)

    val shrunk = fakes.animals.results.value
    fakes.animals.results.value = shrunk.copy(items = shrunk.items.take(3), totalCount = 3)
    awaitCondition("카드 3개만") {
        onAllNodes(hasTestTag(TestTags.card("N004"))).fetchSemanticsNodes().isEmpty()
    }
    awaitCondition("바가 펼쳐졌거나 그리드가 아직 스크롤된다") {
        abs((heightOf(bar) - expanded).value) <= BAR_TOLERANCE_DP || gridScrollMax() > 0f
    }

    fakes.animals.results.value = shrunk.copy(items = emptyList(), totalCount = 0)
    val emptyText = getString(Res.string.empty_results)
    awaitCondition("빈 결과 안내") { onAllNodes(hasText(emptyText)).fetchSemanticsNodes().isNotEmpty() }
    awaitBarExpanded(bar, expanded)
}

/** 그리드의 세로 스크롤 범위 최댓값(px, 의미 트리). 0이면 스크롤할 수 없다. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.gridScrollMax(): Float =
    onAllNodes(hasTestTag(TestTags.ANIMAL_GRID)).fetchSemanticsNodes().firstOrNull()
        ?.config?.getOrNull(SemanticsProperties.VerticalScrollAxisRange)?.maxValue?.invoke() ?: 0f

/**
 * 화면 전환을 볼 프레임 수(10프레임 = 160ms). Android Nav3 기본 전환(페이드 700ms)보다 훨씬 짧고, 전환이 없을 때
 * 바깥 화면이 빠지는 데 걸리는 몇 프레임(wasm 3프레임, Android 기기는 3프레임으로 모자랐다)보다 넉넉하다.
 */
private const val TRANSITION_PROBE_FRAMES = 10

/**
 * T11(4a 검토 F4): 폰 세로(한 칸 — 상세가 목록을 덮는다)에서 카드를 누른 뒤 시계를 [TRANSITION_PROBE_FRAMES]만 돌린다.
 * 모션 줄이기면 전환이 없어 목록 카드가 이미 없고 상세(뒤로 버튼)만 있다. 기본이면 전환 중이라 둘 다 있다.
 * 프레임마다 목록 카드가 있었는지를 실패 문구에 싣는다. 시계는 끝에 자동으로 되돌린다.
 *
 * 기본 모션 쪽은 Android에서만 뜻이 있다 — CMP 웹의 Nav3 기본 전환은 `EnterTransition.None`/`ExitTransition.None`
 * (navigation3-ui `NavDisplay.web.kt`)이라 웹은 모션 줄이기와 무관하게 전환이 없다.
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.screenTransitionFlow(fakes: FlowFakes, reducedMotion: Boolean) {
    fakes.animals.results.value = listOf3()
    awaitTag(TestTags.card("B"))
    awaitIdle()
    val back = getString(Res.string.action_back)
    mainClock.autoAdvance = false
    try {
        onNodeWithTag(TestTags.card("B")).performClick()
        val listByFrame = (1..TRANSITION_PROBE_FRAMES).map {
            mainClock.advanceTimeByFrame()
            onAllNodes(hasTestTag(TestTags.card("B"))).fetchSemanticsNodes().isNotEmpty()
        }
        val detailShown = onAllNodes(hasContentDescription(back)).fetchSemanticsNodes().isNotEmpty()
        val trace = "프레임별 목록 카드: $listByFrame"
        assertTrue(detailShown, "$TRANSITION_PROBE_FRAMES 프레임 뒤 상세가 있다")
        if (reducedMotion) {
            assertTrue(!listByFrame.last(), "모션 줄이기: 목록 카드가 없다(전환 없음) — $trace")
        } else {
            assertTrue(listByFrame.last(), "기본 모션: 목록 카드가 아직 있다(전환 중) — $trace")
        }
    } finally {
        mainClock.autoAdvance = true
    }
    awaitCondition("전환이 끝나 목록 카드가 없다") {
        onAllNodes(hasTestTag(TestTags.card("B"))).fetchSemanticsNodes().isEmpty()
    }
}

/** 그리드의 첫 카드([tag])가 위로 지나갔는가 — 노드가 없거나(첫 카드라 위쪽뿐) 위 끝이 그리드 위 끝보다 위. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.isAboveGrid(tag: String): Boolean {
    if (onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isEmpty()) return true
    return onNodeWithTag(tag).getUnclippedBoundsInRoot().top <
        onNodeWithTag(TestTags.ANIMAL_GRID).getUnclippedBoundsInRoot().top
}

/** [condition]이 될 때까지 그리드를 [byDp]씩 끈다(많아야 [maxDrags]번). 그래도 아니면 [awaitCondition]의 실패 문구로. */
@OptIn(ExperimentalTestApi::class)
private suspend fun ComposeUiTest.dragGridUntil(
    what: String,
    byDp: Float,
    maxDrags: Int,
    condition: () -> Boolean,
) {
    repeat(maxDrags) {
        awaitIdle()
        if (condition()) return
        dragGrid(byDp)
    }
    awaitCondition(what, condition)
}

/** T12 목록: 이 번호 이후의 카드가 보이면 뒤쪽까지 스크롤된 것(40개 중 30번째 — 폰 2열이면 15번째 줄). */
private const val FAR_CARD_FROM = 30

/** T12 목록·북마크를 한 칸 장면에서 끄는 거리와 최대 횟수(폰 2열 40개 = 20줄을 끝까지 내릴 만큼). */
private const val SHRINK_DRAG_DP = -500f
private const val SHRINK_MAX_DRAGS = 16

/**
 * T12 목록(4a-fixes 검토 G1): 두 칸([WIDE_WIDTH_DP]×[WIDE_HEIGHT_DP])에서 목록 칸을 끌어 첫 카드가 위로 지나가게 한
 * 뒤 폰 세로(400×760)로 줄인다 — 목록 entry가 두 칸 장면(`LookaheadScope` 안)에서 한 칸 장면으로 옮겨진다. 위치가
 * 유지되고(첫 카드가 여전히 위), 그 뒤로도 끌면 뒤쪽 카드까지 내려가 다음 쪽을 요청한다. 고치기 전에는 그리드 상태가
 * 굳어 첫 줄 안에서만 조금 움직이고 다음 쪽이 없었다.
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.listKeepsScrollingAfterPaneShrinkFlow(fakes: FlowFakes) {
    fakes.animals.results.value = manyResults().copy(totalCount = 200, pageNo = 1, hasMore = true)
    awaitTag(TestTags.card(FIRST_MANY))
    awaitIdle()

    dragGrid(-600f)
    awaitCondition("두 칸에서 첫 카드가 위로 지나감") { isAboveGrid(TestTags.card(FIRST_MANY)) }
    val loadsBefore = fakes.animals.loadMoreCalls

    resizeWindow(fakes, PHONE_WIDTH_DP, PHONE_HEIGHT_DP)
    awaitTag(TestTags.ANIMAL_GRID)
    awaitIdle()
    assertTrue(isAboveGrid(TestTags.card(FIRST_MANY)), "한 칸으로 줄인 뒤 목록이 맨 위로 돌아갔다(위치 유지 안 됨)")

    val farTags = (FAR_CARD_FROM..40).map { TestTags.card("N" + it.toString().padStart(3, '0')) }
    dragGridUntil(
        "한 칸으로 줄인 뒤 끌어서 뒤쪽 카드(N0$FAR_CARD_FROM 이후)가 보임",
        SHRINK_DRAG_DP,
        SHRINK_MAX_DRAGS
    ) {
        farTags.any { onAllNodes(hasTestTag(it)).fetchSemanticsNodes().isNotEmpty() }
    }
    // 끝에서 NEAR_END_OFFSET 안쪽(마지막 카드 근처)까지 더 끌면 다음 쪽
    dragGridUntil(
        "한 칸으로 줄인 뒤 끝까지 끌어 다음 쪽 요청(줄이기 전 ${loadsBefore}회)",
        SHRINK_DRAG_DP,
        SHRINK_MAX_DRAGS
    ) {
        fakes.animals.loadMoreCalls > loadsBefore
    }
}

/**
 * T12 사진 창: 두 칸 900×600(상세 칸이 600dp보다 좁아 1단) → 한 칸 599×600(600 미만이라 1단). 단이 같아야
 * `PhotoPager`가 같은 자리 그대로 옮겨진다 — 1단 → 2단이면 새로 만들어져 굳는 것이 가려진다(`repo-pager.txt` b).
 */
const val PAGER_WIDE_WIDTH_DP = 900
const val PAGER_HEIGHT_DP = 600
private const val PAGER_NARROW_WIDTH_DP = 599

/**
 * T12 사진(4a-fixes 검토 G1): 사진 2장인 동물을 두 칸에서 열어 [목록 | 상세] → 상세가 같은 1단으로 남는 한 칸으로
 * 줄인다 — 상세 entry가 두 칸 장면에서 한 칸 장면으로 옮겨진다. 사진을 왼쪽으로 넘기면 2/2가 1/2 자리에 온다. 고치기
 * 전에는 페이저 상태가 굳어 1/2에서 넘어가지 않았다.
 *
 * 사진 주소는 http·https가 아닌 서로 다른 값 — 네트워크를 타지 않는다(Coil의 네트워크 fetcher는 http·https만 받는다).
 * 빈 주소 둘(`""`·`" "`)은 화면용 주소가 정규화(trim)로 같아져 페이저 `key`가 겹친다.
 * 기기(세로 411dp)에서는 넓은 창의 상세 칸이 화면 밖이라 줄인 뒤에 넘기고, 시작점은 화면 안(사진 왼쪽에서 300dp 이내).
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.photoPagerKeepsPagingAfterPaneShrinkFlow(fakes: FlowFakes) {
    val withPhotos = flowAnimal("P").copy(photos = listOf("photo-1", "photo-2"))
    fakes.animals.results.value =
        listOf3().copy(items = listOf(withPhotos, flowAnimal("B"), flowAnimal("C")))
    val first = getString(Res.string.photo_a11y, 1, 2, "")
    val second = getString(Res.string.photo_a11y, 2, 2, "")

    awaitTag(TestTags.card("P")).performClick()
    awaitCondition("두 칸 상세의 사진 1/2") {
        onAllNodes(
            hasContentDescription(first, substring = true)
        ).fetchSemanticsNodes().isNotEmpty()
    }
    assertTrue(
        onAllNodes(hasTestTag(TestTags.card("B"))).fetchSemanticsNodes().isNotEmpty(),
        "두 칸([목록 | 상세])이 아니다"
    )

    resizeWindow(fakes, PAGER_NARROW_WIDTH_DP, PAGER_HEIGHT_DP)
    awaitCondition("한 칸(상세만)") {
        onAllNodes(hasTestTag(TestTags.card("B"))).fetchSemanticsNodes().isEmpty()
    }
    awaitIdle()
    val photo = onNode(hasContentDescription(first, substring = true))
    val pageLeft = photo.getUnclippedBoundsInRoot().left

    photo.performTouchInput {
        val startX = min(width - 1f, 300.dp.toPx())
        swipe(
            start = Offset(startX, centerY),
            end = Offset(8.dp.toPx(), centerY),
            durationMillis = 200
        )
    }
    awaitCondition("한 칸으로 줄인 뒤 사진이 2/2로 넘어감") {
        val nodes = onAllNodes(hasContentDescription(second, substring = true))
        nodes.fetchSemanticsNodes().isNotEmpty() &&
            abs(
                (
                    onNode(
                        hasContentDescription(second, substring = true)
                    ).getUnclippedBoundsInRoot().left -
                        pageLeft
                    ).value
            ) <=
            1f
    }
}

/** T12 북마크 처음 창(900×900 — 폭 expanded·높이 medium이라 두 칸). */
const val BOOKMARKS_WIDE_DP = 900

/**
 * T12 북마크(4a-fixes 검토 G1, 구현자가 찾은 것): 900×900(두 칸)에서 북마크 40개를 열고 900×400(폰 가로 — 한 칸,
 * `enterAlways`)으로 줄인 뒤 끌어 바를 접는다. 3개만 남겨 그리드가 스크롤할 수 없게 되면 바가 64dp로 돌아온다.
 * 고치기 전에는 굳은 그리드 상태가 "스크롤할 수 있다"를 내놓아 바가 접힌 채 남았다.
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.bookmarksBarReturnsAfterPaneShrinkFlow(fakes: FlowFakes) {
    fakes.animals.results.value = listOf3()
    manyResults().items.reversed().forEach { fakes.bookmarks.add(it) }

    awaitTag(TestTags.OPEN_BOOKMARKS).performClick()
    awaitTag(TestTags.card(FIRST_MANY))
    awaitIdle()

    resizeWindow(fakes, LANDSCAPE_WIDTH_DP, LANDSCAPE_HEIGHT_DP)
    awaitTag(TestTags.card(FIRST_MANY))
    awaitIdle()
    val bar = TestTags.BOOKMARKS_TOP_BAR
    val expanded = heightOf(bar)

    dragGrid(-200f)
    awaitBarCollapsed(bar, expanded)

    fakes.bookmarks.bookmarks.value = fakes.bookmarks.bookmarks.value.take(3)
    awaitBarExpanded(bar, expanded)
}

/** T13 창: 한 칸 700×800(상세 칸이 600dp 이상이라 2단) ↔ 590×800(600 미만이라 1단). */
const val COLUMN_WIDE_WIDTH_DP = 700
const val COLUMN_HEIGHT_DP = 800
private const val COLUMN_NARROW_WIDTH_DP = 590

/** 사진 설명 앞부분("사진 n/m") — 쪽마다(0부터). */
private suspend fun photoTexts(count: Int): List<String> =
    (1..count).map { getString(Res.string.photo_a11y, it, count, "") }

/**
 * [texts]의 [index]번째 쪽이 지금 쪽인가 — 그 설명 노드의 왼쪽 끝이 페이저(가로 스크롤 노드)의 왼쪽 끝과 1dp 안으로
 * 같다. 다른 쪽이 그려져 있어도 된다(웹은 끌기로 초점을 받은 쪽을 남겨 둔다).
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.isCurrentPhoto(texts: List<String>, index: Int): Boolean {
    val page = hasContentDescription(texts[index], substring = true)
    if (onAllNodes(page).fetchSemanticsNodes().isEmpty()) return false
    val anyPhoto = texts.map {
        hasContentDescription(it, substring = true)
    }.reduce { a, b -> a or b }
    val pager = SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange) and
        hasAnyDescendant(anyPhoto)
    if (onAllNodes(pager).fetchSemanticsNodes().isEmpty()) return false
    val pagerLeft = onNode(pager).getUnclippedBoundsInRoot().left
    return abs((onNode(page).getUnclippedBoundsInRoot().left - pagerLeft).value) <= 1f
}

/**
 * T13(5a K1, 4a-fixes2 검토 12.3 ②): 한 칸 700×800에서 사진 2장 동물의 상세(2단)를 열어 2/2로 넘긴 뒤 창을 590×800
 * (1단) → 700×800(2단)으로 바꿔도 2/2가 남는다. 고치기 전에는 `PhotoPager`가 단마다 다른 자리에서 상태를 새로 만들어
 * 첫 사진으로 돌아갔다. 넘기기는 T12처럼 끌기, 기기(세로 411dp)에서는 시작점이 화면 안(사진 왼쪽에서 300dp 이내).
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.photoPageKeptAcrossColumnChangeFlow(fakes: FlowFakes) {
    fakes.animals.results.value = listWithTwoPhotos()
    val texts = photoTexts(2)
    val first = texts[0]

    awaitTag(TestTags.card("P")).performClick()
    awaitCondition("상세 사진 1/2") {
        onAllNodes(
            hasContentDescription(first, substring = true)
        ).fetchSemanticsNodes().isNotEmpty()
    }
    awaitIdle()
    onNode(hasContentDescription(first, substring = true)).performTouchInput {
        val startX = min(width - 1f, 300.dp.toPx())
        swipe(
            start = Offset(startX, centerY),
            end = Offset(8.dp.toPx(), centerY),
            durationMillis = 200
        )
    }
    awaitCondition("2단에서 사진이 2/2로 넘어감") { isCurrentPhoto(texts, 1) }

    resizeWindow(fakes, COLUMN_NARROW_WIDTH_DP, COLUMN_HEIGHT_DP)
    awaitIdle()
    assertTrue(isCurrentPhoto(texts, 1), "2단 → 1단으로 바꾼 뒤 사진이 2/2가 아니다(첫 사진으로 돌아감)")

    resizeWindow(fakes, COLUMN_WIDE_WIDTH_DP, COLUMN_HEIGHT_DP)
    awaitIdle()
    assertTrue(isCurrentPhoto(texts, 1), "1단 → 2단으로 되돌린 뒤 사진이 2/2가 아니다(첫 사진으로 돌아감)")
}

/** [text]가 든 노드가 polite 라이브 영역인가(병합 전 트리 — 문장 `Text` 자신). */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.isPoliteLiveText(text: String): Boolean = onAllNodes(
    hasText(text) and
        SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
    useUnmergedTree = true
).fetchSemanticsNodes().isNotEmpty()

/**
 * T15 첫째(5a Y1): 폰 창 — 목록을 네트워크 오류(결과 없음)로 두면 전체 화면 문장이 polite 라이브 영역. 결과가 있는
 * 목록에서 다음 쪽이 실패하면(끝에 가까워 저절로 부른다) 끝 행 문장도 polite.
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.statusMessagesLiveRegionFlow(fakes: FlowFakes) {
    val network = getString(Res.string.error_network)
    fakes.animals.results.value = SearchResults(error = ApiError.Network, errorId = 1)
    awaitCondition("오류 전체 화면 문장") {
        onAllNodes(hasText(network)).fetchSemanticsNodes().isNotEmpty()
    }
    assertTrue(isPoliteLiveText(network), "전체 화면 오류 문장이 polite 라이브 영역이 아니다")

    fakes.animals.onLoadMore = {
        fakes.animals.results.value =
            fakes.animals.results.value.copy(error = ApiError.Network, errorId = 2)
    }
    fakes.animals.results.value = listOf3().copy(totalCount = 200, pageNo = 1, hasMore = true)
    val failed = getString(Res.string.list_load_more_failed)
    awaitCondition("끝 행 실패 문장") { onAllNodes(hasText(failed)).fetchSemanticsNodes().isNotEmpty() }
    assertTrue(isPoliteLiveText(failed), "끝 행 실패 문장이 polite 라이브 영역이 아니다")
}

/** T15 둘째(5a Y2): 목록 첫 로딩(Fake가 아직 결과를 내지 않았다)의 진행 표시에 이름 `a11y_loading`. */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.loadingIndicatorNameFlow(fakes: FlowFakes) {
    assertTrue(fakes.animals.results.value.fetchedAt == null, "Fake가 첫 로딩 상태가 아니다")
    val loading = getString(Res.string.a11y_loading)
    awaitCondition("진행 표시 이름") {
        onAllNodes(hasContentDescription(loading), useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()
    }
}

/** 사진 2장 동물("P", 네트워크 없는 주소 — T12와 같다)을 첫 카드로 둔 목록. */
private fun listWithTwoPhotos(): SearchResults {
    val withPhotos = flowAnimal("P").copy(photos = listOf("photo-1", "photo-2"))
    return listOf3().copy(items = listOf(withPhotos, flowAnimal("B"), flowAnimal("C")))
}

/**
 * T14 첫째(5a U1): 폰 400×760 상세의 사진 아래 줄 — [다음 사진]을 누르면 2/2이고 다음 사진은 비활성, [이전 사진]을
 * 누르면 1/2이고 이전 사진은 비활성.
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.detailPhotoButtonsFlow(fakes: FlowFakes) {
    fakes.animals.results.value = listWithTwoPhotos()
    val texts = photoTexts(2)
    val previous =
        hasContentDescription(getString(Res.string.action_previous_photo)) and hasClickAction()
    val next = hasContentDescription(getString(Res.string.action_next_photo)) and hasClickAction()

    awaitTag(TestTags.card("P")).performClick()
    awaitCondition("상세 사진 1/2") { isCurrentPhoto(texts, 0) }
    onNode(previous).assertIsNotEnabled()

    onNode(next).performClick()
    awaitCondition("다음 사진으로 2/2") { isCurrentPhoto(texts, 1) }
    onNode(next).assertIsNotEnabled()
    onNode(previous).assertIsEnabled()

    onNode(previous).performClick()
    awaitCondition("이전 사진으로 1/2") { isCurrentPhoto(texts, 0) }
    onNode(previous).assertIsNotEnabled()
    onNode(next).assertIsEnabled()
}

/**
 * T14 둘째(5a U2): 상세 사진을 눌러 연 뷰어 "1 / 2"에는 이전 사진 단추가 없다 → [다음 사진] → "2 / 2"이고 다음 사진
 * 단추가 없다 → [이전 사진] → "1 / 2".
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.viewerPhotoButtonsFlow(fakes: FlowFakes) {
    fakes.animals.results.value = listWithTwoPhotos()
    val texts = photoTexts(2)
    val previous =
        hasContentDescription(getString(Res.string.action_previous_photo)) and hasClickAction()
    val next = hasContentDescription(getString(Res.string.action_next_photo)) and hasClickAction()
    val firstCounter = getString(Res.string.photo_counter, 1, 2)
    val secondCounter = getString(Res.string.photo_counter, 2, 2)

    awaitTag(TestTags.card("P")).performClick()
    awaitCondition("상세 사진 1/2") { isCurrentPhoto(texts, 0) }
    onNode(hasContentDescription(texts[0], substring = true)).performClick()
    awaitCondition("뷰어 1 / 2") {
        onAllNodes(hasText(firstCounter)).fetchSemanticsNodes().isNotEmpty()
    }
    // 전환이 끝나 상세(사진 아래 줄의 단추)가 빠질 때까지
    awaitCondition("상세가 빠짐") {
        onAllNodes(hasTestTag(TestTags.DETAIL_BOOKMARK)).fetchSemanticsNodes().isEmpty()
    }
    assertTrue(onAllNodes(previous).fetchSemanticsNodes().isEmpty(), "첫 쪽에 이전 사진 단추가 있다")

    onNode(next).performClick()
    awaitCondition("뷰어 2 / 2") {
        onAllNodes(hasText(secondCounter)).fetchSemanticsNodes().isNotEmpty()
    }
    awaitIdle()
    assertTrue(onAllNodes(next).fetchSemanticsNodes().isEmpty(), "마지막 쪽에 다음 사진 단추가 있다")

    onNode(previous).performClick()
    awaitCondition("뷰어 1 / 2로 돌아옴") {
        onAllNodes(hasText(firstCounter)).fetchSemanticsNodes().isNotEmpty()
    }
}

/**
 * T16(5a O1): 폰 창 — 목록 더 보기 → 설정 → 오픈소스 라이선스 → "Pretendard"·"Apache License 2.0" → Apache 묶음
 * [전문 보기] → "Version 2.0, January 2004"가 든 전문 → 뒤로 → 설정.
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.licensesFlow(fakes: FlowFakes) {
    fakes.animals.results.value = listOf3()
    val settings = getString(Res.string.title_settings)
    val licenses = getString(Res.string.about_licenses)

    awaitTag(TestTags.card("A"))
    onNode(hasContentDescription(getString(Res.string.action_more))).performClick()
    awaitCondition("더 보기 메뉴의 설정") {
        onAllNodes(hasText(settings)).fetchSemanticsNodes().isNotEmpty()
    }
    onNode(hasText(settings)).performClick()
    awaitCondition("설정의 오픈소스 라이선스 줄") {
        onAllNodes(hasText(licenses) and hasClickAction()).fetchSemanticsNodes().isNotEmpty()
    }
    onNode(hasText(licenses) and hasClickAction()).performScrollTo().performClick()

    awaitCondition("라이선스 화면") {
        onAllNodes(hasText("Pretendard")).fetchSemanticsNodes().isNotEmpty() &&
            onAllNodes(hasText("Apache License 2.0")).fetchSemanticsNodes().isNotEmpty()
    }
    awaitTag(TestTags.licenseToggle("apache")).performScrollTo().performClick()
    awaitCondition("Apache 전문") {
        onAllNodes(hasText("Version 2.0, January 2004", substring = true))
            .fetchSemanticsNodes().isNotEmpty()
    }

    onNode(hasContentDescription(getString(Res.string.action_back))).performClick()
    awaitCondition("설정으로 돌아옴") {
        onAllNodes(hasText(settings)).fetchSemanticsNodes().isNotEmpty() &&
            onAllNodes(hasText("Pretendard")).fetchSemanticsNodes().isEmpty()
    }
}

/**
 * T17(5a-live-fixes L5, M13): 품종 칸에 입력해 거른 뒤 메뉴에서 고르면 키보드를 닫는다(가짜 컨트롤러의 `hide()`).
 * 칸 글은 고른 이름, [검색하기]의 조건은 그 코드다.
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.breedPickHidesKeyboardFlow(fakes: FlowFakes) {
    fakes.animals.results.value = listOf3()
    fakes.codeTables.kindsState(AnimalKind.DOG).value = loaded(
        listOf(
            Kind("000054", "골든 리트리버"),
            Kind("000056", "그레이트 덴"),
            Kind("000071", "래브라도 리트리버")
        )
    )

    awaitTag(TestTags.OPEN_FILTER).performClick()
    awaitTag(TestTags.choice("kind", AnimalKind.DOG)).performScrollTo().performClick()
    awaitCondition("품종 칸이 켜짐") { onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
    val field = onNode(hasSetTextAction())
    field.performScrollTo().performClick()
    field.performTextInput("리트")
    awaitCondition("메뉴에 골든 리트리버") {
        onAllNodes(hasText("골든 리트리버") and hasClickAction()).fetchSemanticsNodes().isNotEmpty()
    }
    val hidesBefore = fakes.keyboard.hideCount
    onNode(hasText("골든 리트리버") and hasClickAction()).performClick()

    awaitCondition("키보드 닫기") { fakes.keyboard.hideCount > hidesBefore }
    awaitCondition("칸 글이 골든 리트리버") {
        onAllNodes(hasSetTextAction() and hasText("골든 리트리버")).fetchSemanticsNodes().isNotEmpty()
    }
    awaitTag(TestTags.FILTER_APPLY).performClick()
    awaitCondition("검색 조건 반영") { fakes.animals.searchCalls.lastOrNull()?.kindCode == "000054" }
    assertEquals(AnimalKind.DOG, fakes.animals.searchCalls.last().kind)
}

/**
 * T18(5a-live-fixes L6, M5): 상세 상태 배지는 문장을 글 의미로 낸다 — 공고중 D-4는 "공고 4일 남음", 보호중은
 * `status_protecting`. 웹(CMP 1.12.2)은 역할 없는 칸의 설명을 접근성 이름으로 쓰지 않아 글로 줘야 읽힌다.
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.detailBadgeTextFlow(fakes: FlowFakes) {
    val noticing = flowAnimal("A").copy(noticeEnd = LocalDate(2026, 11, 8))
    val protecting = flowAnimal("B").copy(noticeEnd = LocalDate(2026, 11, 1))
    fakes.animals.results.value = listOf3().copy(items = listOf(noticing, protecting))

    awaitTag(TestTags.card("A")).performClick()
    val daysLeft = getPluralString(Res.plurals.a11y_days_left, 4, 4)
    awaitCondition("배지 글 $daysLeft") {
        onAllNodes(hasText(daysLeft)).fetchSemanticsNodes().isNotEmpty()
    }

    onNode(hasContentDescription(getString(Res.string.action_back))).performClick()
    awaitTag(TestTags.card("B")).performClick()
    val protectingText = getString(Res.string.status_protecting)
    awaitCondition("배지 글 $protectingText") {
        onAllNodes(hasText(protectingText)).fetchSemanticsNodes().isNotEmpty()
    }
}

/**
 * T19 첫째(4b-fixes B1): 두 칸([WIDE_WIDTH_DP]×[WIDE_HEIGHT_DP])에서 카드 → [목록 | 상세] → **시스템 뒤로** →
 * [목록 | 자리표시]. 두 칸의 [목록]과 [목록, 상세]는 scaffold 값이 같아 기본 `PopUntilScaffoldValueChange`면 뒤로를
 * 받지 않았다(Android는 앱을 떠나고 웹 Esc는 무반응). 상세 표지는 북마크 단추 — 하네스에는 공유 주소가 없어 [공유]가
 * 없다.
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.twoPaneSystemBackClosesDetailFlow(fakes: FlowFakes) {
    fakes.animals.results.value = listOf3()
    val placeholder = getString(Res.string.detail_placeholder)

    awaitTag(TestTags.card("B")).performClick()
    awaitTag(TestTags.DETAIL_BOOKMARK)
    assertTrue(
        onAllNodes(hasText(placeholder)).fetchSemanticsNodes().isEmpty(),
        "상세를 열었는데 자리표시가 있다"
    )

    pressSystemBack(fakes)
    awaitCondition("상세가 닫히고 자리표시") {
        onAllNodes(hasTestTag(TestTags.DETAIL_BOOKMARK)).fetchSemanticsNodes().isEmpty() &&
            onAllNodes(hasText(placeholder)).fetchSemanticsNodes().isNotEmpty()
    }
    awaitTag(TestTags.card("A"))
    awaitTag(TestTags.card("B"))
}

/** T19 둘째(4b-fixes B1): 두 칸에서 목록 → 북마크 목록([북마크 | 자리표시]) → 시스템 뒤로 → 목록. */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.twoPaneSystemBackClosesBookmarksFlow(fakes: FlowFakes) {
    fakes.animals.results.value = listOf3()

    awaitTag(TestTags.card("A"))
    awaitTag(TestTags.OPEN_BOOKMARKS).performClick()
    awaitTag(TestTags.BOOKMARKS_TOP_BAR)

    pressSystemBack(fakes)
    awaitCondition("북마크 목록이 닫히고 목록") {
        onAllNodes(hasTestTag(TestTags.BOOKMARKS_TOP_BAR)).fetchSemanticsNodes().isEmpty() &&
            onAllNodes(hasTestTag(TestTags.LIST_TOP_BAR)).fetchSemanticsNodes().isNotEmpty()
    }
    awaitTag(TestTags.card("A"))
}

// ── 5d: 사망 종료 — 기본 숨김, 북마크는 늘 보임, 필터 "사망으로 끝난 공고도 보기"(T20~T23) ──

/** 사망 종료(자연사)인 사진 없는 테스트 동물. */
private fun deadFlowAnimal(no: String): Animal = flowAnimal(no).copy(processState = "종료(자연사)")

/** 트리(글·설명)에 [text]가 든 노드가 없는가. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.hasNoText(text: String): Boolean =
    onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isEmpty() &&
        onAllNodes(hasContentDescription(text, substring = true)).fetchSemanticsNodes().isEmpty()

/**
 * T20(5d): 링크로 시작 `[목록, 상세(†)]` — 북마크 아님·필터 끔이면 `detail_hidden`, 북마크 단추 없음, 트리에 "자연사"
 * 없음. 하네스에 [hooks]로 시작 스택을 준다(래퍼).
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.deepLinkDeadHiddenFlow(fakes: FlowFakes) {
    val hidden = getString(Res.string.detail_hidden)

    awaitCondition("숨김 문장 $hidden") {
        onAllNodes(hasText(hidden)).fetchSemanticsNodes().isNotEmpty()
    }
    assertTrue(
        onAllNodes(hasTestTag(TestTags.DETAIL_BOOKMARK)).fetchSemanticsNodes().isEmpty(),
        "숨긴 상세에 북마크 단추가 있다"
    )
    assertTrue(hasNoText("자연사"), "숨긴 상세에 사유가 보인다")
    assertTrue(fakes.bookmarks.updateCalls.isEmpty())
}

/** T20 시작 스택: 목록 위에 사망 종료 상세(날짜 없는 링크). */
val deepLinkDeadStack: List<NavKey> = listOf(AnimalListKey, AnimalDetailKey(DEAD_NO))

/**
 * T20 가짜들: `getAnimal(DEAD_NO)`가 사망 종료. 시작 스택의 상세는 첫 구성에서 조회하므로 띄우기 전에 정한다(래퍼가
 * `runFlowTest(fakes = …)`로 넘긴다).
 */
fun deepLinkDeadFakes(): FlowFakes = FlowFakes().apply {
    val dead = deadFlowAnimal(DEAD_NO)
    animals.animalResult = { no -> ApiResult.Success(dead.takeIf { no == DEAD_NO }) }
}

private const val DEAD_NO = "450650202609999"

/**
 * T21(5d): 북마크(스냅샷 보호중 X) → 북마크 목록 → X 카드 → 재조회가 X† → 상세가 보이고 배지 "종료 · 자연사" →
 * 앱 안 ← → 북마크 목록에 X가 남아 있고 그 카드 문장에 "종료 · 자연사"(스냅샷 갱신). 북마크는 숨기지 않는다.
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.bookmarkedDeadStaysVisibleFlow(fakes: FlowFakes) {
    fakes.animals.results.value = listOf3()
    val alive = flowAnimal("X")
    fakes.bookmarks.add(alive)
    fakes.animals.animalResult =
        { no -> ApiResult.Success(deadFlowAnimal("X").takeIf { no == "X" }) }
    val endedText = getString(Res.string.status_ended_reason, "자연사")

    awaitTag(TestTags.OPEN_BOOKMARKS).performClick()
    awaitTag(TestTags.card("X")).performClick()
    awaitTag(TestTags.DETAIL_BOOKMARK)
    awaitCondition("상세 배지 $endedText") {
        onAllNodes(hasText(endedText)).fetchSemanticsNodes().isNotEmpty()
    }
    assertTrue(
        onAllNodes(hasText(getString(Res.string.detail_hidden))).fetchSemanticsNodes().isEmpty(),
        "북마크된 동물을 숨겼다"
    )

    onNode(hasContentDescription(getString(Res.string.action_back))).performClick()
    awaitCondition("북마크 목록의 X 카드 문장에 $endedText") {
        onAllNodes(
            hasTestTag(TestTags.card("X")) and (
                hasContentDescription(endedText, substring = true) or
                    hasAnyDescendant(hasText(endedText))
                )
        ).fetchSemanticsNodes().isNotEmpty()
    }
    assertEquals(listOf("X"), fakes.bookmarks.bookmarks.value.map { it.animal.desertionNo })
    assertEquals("종료(자연사)", fakes.bookmarks.bookmarks.value.single().animal.processState)
}

/**
 * T22(5d): 두 칸 — 목록 [A, B] → B 열기 → 새로고침 흉내로 Fake 결과를 items [A]·hidden [B†]로 → 상세 칸
 * `detail_hidden`, 카드 B 없음, A 있음(호출 0).
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.twoPaneHiddenAfterRefreshFlow(fakes: FlowFakes) {
    val start = listOf3().copy(items = listOf(flowAnimal("A"), flowAnimal("B")))
    fakes.animals.results.value = start
    val hidden = getString(Res.string.detail_hidden)

    awaitTag(TestTags.card("B")).performClick()
    awaitTag(TestTags.DETAIL_BOOKMARK)
    val calls = fakes.animals.getAnimalCalls.size

    runOnIdle {
        fakes.animals.results.value = start.copy(
            items = listOf(flowAnimal("A")),
            hidden = listOf(deadFlowAnimal("B"))
        )
    }
    awaitCondition("상세 칸 숨김 문장") {
        onAllNodes(hasText(hidden)).fetchSemanticsNodes().isNotEmpty()
    }
    awaitCondition("카드 B가 빠짐") {
        onAllNodes(hasTestTag(TestTags.card("B"))).fetchSemanticsNodes().isEmpty()
    }
    awaitTag(TestTags.card("A"))
    assertEquals(calls, fakes.animals.getAnimalCalls.size)
}

/**
 * T23(5d): 필터 화면 — 상태 전체에서 체크 줄 "사망으로 끝난 공고도 보기"는 꺼져 있고 누를 수 있다 → 공고중이면 비활성 +
 * 안내 글 → 전체 → 켬 → [검색하기] → 마지막 검색 조건에 켬, 목록 칩 "사망 포함".
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.includeDeathsFilterFlow(fakes: FlowFakes) {
    fakes.animals.results.value = listOf3()
    val label = getString(Res.string.filter_include_deaths)
    val disabledHint = getString(Res.string.filter_include_deaths_disabled)
    val chip = getString(Res.string.chip_include_deaths)
    val row = hasText(label) and SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState)

    awaitTag(TestTags.OPEN_FILTER).performClick()
    awaitCondition("체크 줄 $label") { onAllNodes(row).fetchSemanticsNodes().isNotEmpty() }
    onNode(row).performScrollTo().assertIsOff().assertIsEnabled()

    onNodeWithTag(TestTags.choice("state", NoticeState.NOTICE)).performScrollTo().performClick()
    awaitCondition("비활성 안내 $disabledHint") {
        onAllNodes(hasText(disabledHint)).fetchSemanticsNodes().isNotEmpty()
    }
    onNode(row).assertIsNotEnabled()

    onNodeWithTag(TestTags.choice("state", NoticeState.ALL)).performScrollTo().performClick()
    awaitCondition("안내가 사라짐") { onAllNodes(hasText(disabledHint)).fetchSemanticsNodes().isEmpty() }
    onNode(row).assertIsEnabled().performScrollTo().performClick()
    awaitCondition("체크 켜짐") {
        onAllNodes(
            row and
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ToggleableState,
                    ToggleableState.On
                )
        )
            .fetchSemanticsNodes().isNotEmpty()
    }

    awaitTag(TestTags.FILTER_APPLY).performClick()
    awaitCondition("검색 조건 켬") {
        fakes.animals.searchCalls.lastOrNull()?.includeEndedByDeath == true
    }
    awaitCondition("목록 칩 $chip") { onAllNodes(hasText(chip)).fetchSemanticsNodes().isNotEmpty() }
}
