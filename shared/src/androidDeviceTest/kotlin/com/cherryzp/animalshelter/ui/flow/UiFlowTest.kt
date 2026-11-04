package com.cherryzp.animalshelter.ui.flow

import androidx.compose.ui.test.ExperimentalTestApi
import com.cherryzp.animalshelter.PlatformHooks
import com.cherryzp.animalshelter.model.ListLayout
import kotlin.test.Test

/**
 * UI 흐름 테스트 래퍼(브리핑 C6). 본문은 commonTest의 `Flows.kt`. 이름에 공백을 쓰지 않는다 — 기기 테스트 APK의
 * D8이 DEX 040(API 30) 미만에서 공백 이름을 받지 않는다(minSdk 26).
 */
@OptIn(ExperimentalTestApi::class)
class UiFlowTest {
    /** T1 목록 → 상세 → 북마크 추가 → 북마크 목록에 그 카드. */
    @Test
    fun t1ListDetailBookmarkThenBookmarkList() = runFlowTest { listToBookmarksFlow(it) }

    /** T2 필터 고양이 + 공고중 → 목록 칩과 마지막 검색 조건. */
    @Test
    fun t2FilterCatNoticeThenChipsAndQuery() = runFlowTest { filterFlow(it) }

    /** T3 2열 가운데 카드 북마크 되돌리기는 같은 자리. */
    @Test
    fun t3GridUndoRestoresSamePlace() = runFlowTest { bookmarkUndoFlow(it, ListLayout.GRID) }

    /** T3 1열 첫 카드 되돌리기는 같은 자리(화면 밖 복원 회귀). */
    @Test
    fun t3LargeFirstCardUndoRestoresSamePlace() = runFlowTest {
        bookmarkUndoFlow(it, ListLayout.LARGE)
    }

    /** T4 상세 바 제목은 품종명 줄이 바 아래로 지나간 뒤에만(3d D6). */
    @Test
    fun t4DetailTitleAppearsAfterNameScrollsUnderBar() = runFlowTest { detailTitleFlow(it) }

    /** T4 두 단 상세는 조금 스크롤하면 바 제목, 맨 위면 없다(3e E2). */
    @Test
    fun t4TwoColumnDetailTitleFollowsBarScrollColor() = runFlowTest(
        widthDp = WIDE_WIDTH_DP,
        heightDp = WIDE_SHORT_HEIGHT_DP
    ) { detailTitleTwoColumnFlow(it) }

    /** T5 두 칸에서 상세에 열린 카드만 selected(3d D7). */
    @Test
    fun t5WideOpenedCardIsMarked() = runFlowTest(
        widthDp = WIDE_WIDTH_DP,
        heightDp = WIDE_HEIGHT_DP
    ) { openedCardMarkFlow(it) }

    /** T5 한 칸에서는 열린 카드 표시가 없다(3d D7). */
    @Test
    fun t5PhoneNoOpenedCardMark() = runFlowTest { noMarkOnPhoneFlow(it) }

    /** T6 정렬 메뉴는 지금 정렬을 selected로 알린다(3d 점검 A1). */
    @Test
    fun t6SortMenuMarksCurrentOrder() = runFlowTest { sortMenuSelectedFlow(it) }

    /** T8 창 높이가 compact(폰 가로)면 폭과 무관하게 한 칸(3e E3). */
    @Test
    fun t8LandscapePhoneIsSinglePane() = runFlowTest(
        widthDp = LANDSCAPE_WIDTH_DP,
        heightDp = LANDSCAPE_HEIGHT_DP
    ) { landscapeSinglePaneFlow(it) }

    /** T7 폰 창 상세 요약의 둘째 열이 같은 자리(3e E1). */
    @Test
    fun t7PhoneSummarySecondColumnAligned() = runFlowTest { summaryGridFlow(it) }

    /** T7 두 칸 창 2단 상세 요약의 둘째 열이 같은 자리(3e E1). */
    @Test
    fun t7WideSummarySecondColumnAligned() = runFlowTest(
        widthDp = WIDE_WIDTH_DP,
        heightDp = WIDE_HEIGHT_DP
    ) { summaryGridFlow(it) }

    /** T9 요약 칩의 클릭 동작 이름과 동작 하나(3e E8). */
    @Test
    fun t9SummaryChipsHaveActionLabels() = runFlowTest { summaryChipActionLabelFlow(it) }

    /** T10 폰 가로 목록 상단 바는 끌면 숨고 올리면 나오며, 조건이 바뀌면 펼쳐진다(4a N4). */
    @Test
    fun t10LandscapeListTopBarHidesOnScroll() = runFlowTest(
        widthDp = LANDSCAPE_WIDTH_DP,
        heightDp = LANDSCAPE_HEIGHT_DP
    ) { landscapeTopBarHidesFlow(it) }

    /** T10 폰 가로 북마크 목록 상단 바도 끌면 숨는다(4a N4). */
    @Test
    fun t10LandscapeBookmarksTopBarHidesOnScroll() = runFlowTest(
        widthDp = LANDSCAPE_WIDTH_DP,
        heightDp = LANDSCAPE_HEIGHT_DP
    ) { landscapeBookmarksTopBarHidesFlow(it) }

    /** T10 폰 세로 목록 상단 바는 pinned 그대로(4a N4). */
    @Test
    fun t10PortraitListTopBarStaysPinned() = runFlowTest { portraitTopBarStaysFlow(it) }

    /** T10 폰 가로 북마크 목록이 줄거나 비면 접힌 바가 돌아온다(4a 검토 F2). */
    @Test
    fun t10LandscapeBookmarksBarReturnsWhenGridStopsScrolling() = runFlowTest(
        widthDp = LANDSCAPE_WIDTH_DP,
        heightDp = LANDSCAPE_HEIGHT_DP
    ) { landscapeBookmarksBarReturnsFlow(it) }

    /** T10 폰 가로 목록 결과가 줄면 접힌 바가 돌아온다(4a 검토 F2). */
    @Test
    fun t10LandscapeListBarReturnsWhenResultsShrink() = runFlowTest(
        widthDp = LANDSCAPE_WIDTH_DP,
        heightDp = LANDSCAPE_HEIGHT_DP
    ) { landscapeListBarReturnsFlow(it) }

    /** T11 모션 줄이기면 화면 전환 없이 바로 상세(4a 검토 F4). */
    @Test
    fun t11ReducedMotionSwitchesScreensWithoutTransition() = runFlowTest(
        hooks = PlatformHooks(reducedMotion = true)
    ) { screenTransitionFlow(it, reducedMotion = true) }

    /** T11 기본 모션이면 전환 중에 목록과 상세가 함께 있다(4a 검토 F4). */
    @Test
    fun t11DefaultMotionKeepsBothScreensDuringTransition() = runFlowTest {
        screenTransitionFlow(it, reducedMotion = false)
    }

    /** T12 두 칸 → 한 칸으로 줄여도 목록 위치가 유지되고 계속 스크롤·다음 쪽(4a-fixes 검토 G1). */
    @Test
    fun t12ListKeepsScrollingAfterPaneShrink() = runFlowTest(
        widthDp = WIDE_WIDTH_DP,
        heightDp = WIDE_HEIGHT_DP
    ) { listKeepsScrollingAfterPaneShrinkFlow(it) }

    /** T12 두 칸 → 한 칸으로 줄여도 상세 사진이 넘어간다(4a-fixes 검토 G1). */
    @Test
    fun t12PhotoPagerKeepsPagingAfterPaneShrink() = runFlowTest(
        widthDp = PAGER_WIDE_WIDTH_DP,
        heightDp = PAGER_HEIGHT_DP
    ) { photoPagerKeepsPagingAfterPaneShrinkFlow(it) }

    /** T12 두 칸 → 폰 가로로 줄인 북마크 목록이 줄면 접힌 바가 돌아온다(4a-fixes 검토 G1). */
    @Test
    fun t12BookmarksBarReturnsAfterPaneShrink() = runFlowTest(
        widthDp = BOOKMARKS_WIDE_DP,
        heightDp = BOOKMARKS_WIDE_DP
    ) { bookmarksBarReturnsAfterPaneShrinkFlow(it) }

    /** T13 상세를 연 채 창이 바뀌어 1단 ↔ 2단이 되어도 보던 사진 쪽이 남는다(5a K1). */
    @Test
    fun t13PhotoPageKeptAcrossColumnChange() = runFlowTest(
        widthDp = COLUMN_WIDE_WIDTH_DP,
        heightDp = COLUMN_HEIGHT_DP
    ) { photoPageKeptAcrossColumnChangeFlow(it) }

    /** T15 로딩 뒤 나타나는 전체 화면·끝 행 문장은 polite 라이브 영역(5a Y1). */
    @Test
    fun t15StatusMessagesArePoliteLiveRegions() = runFlowTest { statusMessagesLiveRegionFlow(it) }

    /** T15 진행 표시에 이름 a11y_loading(5a Y2). */
    @Test
    fun t15LoadingIndicatorsHaveName() = runFlowTest { loadingIndicatorNameFlow(it) }

    /** T14 상세 사진 아래 이전·다음 단추로 한 쪽씩, 끝 쪽에서는 비활성(5a U1). */
    @Test
    fun t14DetailPhotoButtonsPage() = runFlowTest { detailPhotoButtonsFlow(it) }

    /** T14 사진 뷰어 양옆 이전·다음 단추로 한 쪽씩, 끝 쪽에서는 없다(5a U2). */
    @Test
    fun t14ViewerPhotoButtonsPage() = runFlowTest { viewerPhotoButtonsFlow(it) }

    /** T16 설정 > 오픈소스 라이선스 → 묶음·Apache 전문 → 뒤로 설정(5a O1). */
    @Test
    fun t16LicensesFromSettings() = runFlowTest { licensesFlow(it) }
}
