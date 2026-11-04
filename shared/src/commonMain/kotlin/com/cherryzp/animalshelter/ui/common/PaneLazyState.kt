package com.cherryzp.animalshelter.ui.common

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.LocalListDetailSceneScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.Snapshot

// 칸(listPane·detailPane·extraPane) entry 안의 lazy 상태(4a-fixes 검토 G1, reports/phase4a.md 11.3).
//
// 두 칸 장면(`ListDetailSceneStrategy` → `ThreePaneScaffoldScene`)의 칸은 `ThreePaneScaffold`의 `LookaheadScope`
// 안이다(adaptive-layout 1.3.0 `ThreePaneScaffold.kt:155`). lazy 상태는 lookahead 측정을 한 번 받으면
// `hasLookaheadOccurred`가 참이 되고 풀리지 않는다(foundation 1.12.1 `LazyGridState.kt:191`·`PagerState.kt:171`).
// 창이 한 칸이 되면 전략이 장면을 내지 않아(`ListDetailSceneStrategy.kt:193`) `SinglePaneScene`이 그 entry를 그리는데,
// Nav3가 entry를 `movableContentOf`로 옮기므로(navigation3-ui 1.2.0 `SceneSetupNavEntryDecorator.kt:75`) 같은 상태
// 객체가 lookahead가 없는 곳으로 간다. 그 뒤 측정 결과는 `approachLayoutInfo`에만 들어가 `layoutInfo`·
// `canScrollForward/Backward`·첫 보이는 항목이 굳고(`LazyGridState.kt:657`), 사용자 스크롤 대신
// `scrollDeltaBetweenPasses`로 측정한다(`LazyGrid.kt:384-389`) — 웹에서 두 칸 → 한 칸으로 창을 줄이면 스크롤·다음 쪽·
// 사진 넘기기·상단 바 복귀가 멈췄다. Android도 크기만 바뀌면(방향·밀도·테마 그대로 — 멀티 윈도 경계 끌기·데스크톱 창
// 조절) Activity를 다시 만들지 않아 같은 경로이고, 이 도우미가 함께 막는다(4a-fixes2 검토 12.3 ①). release APK도
// 넓힐 때는 그 자리에서 바뀌지만, 화면 크기 등급이 작아지면(짧은 변 약 720·480dp 아래로) Activity를 다시 만든다 —
// 그때 위치는 저장·복원이 지킨다(`03-screens.md` 0.4, `reports/phase5a.md` 3절 3).
//
// 그래서 "두 칸 장면 안"(`LocalListDetailSceneScope.current != null`)이 바뀌면 상태를 같은 위치로 새로 만든다.
// 저장·복원은 그 경계와 무관한 `rememberSaveable` 하나가 맡고, 저장할 때 그 순간의 상태에서 위치를 읽는다 —
// `key(경계) { remember…State() }`처럼 묶으면 저장 키가 장면을 타서 Android 재생성(두 칸 → 한 칸) 뒤 위치가 사라진다.
//
// 라이브러리가 lookahead 밖으로 옮겨진 상태를 고치면 걷어낸다 — 버전 갱신 단계에서 T12(`UiFlowTest.t12*`)로 확인.

/** 지금 두 칸 장면(`ThreePaneScaffoldScene`)의 칸 안인가. `PaneWidths.kt`의 칸 판정이 읽는 것과 같은 CompositionLocal. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun isInPaneScene(): Boolean = LocalListDetailSceneScope.current != null

/**
 * 그리드 위치 보관함. [current]가 있으면 저장할 때 그 상태의 지금 위치를, 없으면(복원 직후 첫 상태를 만들기 전)
 * 되살린 [index]·[offset]을 쓴다.
 */
private class GridPosition(val index: Int = 0, val offset: Int = 0) {
    var current: LazyGridState? = null
}

private val GridPositionSaver: Saver<GridPosition, *> = listSaver(
    save = { position ->
        val state = position.current
        if (state != null) {
            listOf(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
        } else {
            listOf(position.index, position.offset)
        }
    },
    restore = { GridPosition(index = it[0], offset = it[1]) }
)

/**
 * 칸 entry 안의 그리드 상태 — `rememberLazyGridState()` 대신(목록·북마크 목록). 두 칸 장면 안팎이 바뀌면 직전
 * 상태의 첫 보이는 항목·오프셋으로 새 상태를 만들고, 위치 저장(Android 재생성·다른 화면에서 돌아올 때)은 경계와
 * 무관하게 한 곳에서 한다. 원인은 이 파일 머리 주석.
 */
@Composable
fun rememberPaneLazyGridState(): LazyGridState {
    val position = rememberSaveable(saver = GridPositionSaver) { GridPosition() }
    val inPane = isInPaneScene()
    val state = remember(position, inPane) {
        // 직전 상태의 위치는 읽기 추적 없이 — 아니면 스크롤할 때마다 이 화면 전체가 다시 그려진다
        Snapshot.withoutReadObservation {
            val previous = position.current
            LazyGridState(
                firstVisibleItemIndex = previous?.firstVisibleItemIndex ?: position.index,
                firstVisibleItemScrollOffset =
                    previous?.firstVisibleItemScrollOffset ?: position.offset
            )
        }
    }
    SideEffect { position.current = state }
    return state
}

/** 페이저 위치 보관함([GridPosition]과 같은 방식). */
private class PagerPosition(val page: Int = 0, val offsetFraction: Float = 0f) {
    var current: PagerState? = null
}

private val PagerPositionSaver: Saver<PagerPosition, *> = listSaver(
    save = { position ->
        val state = position.current
        if (state != null) {
            listOf<Any>(state.currentPage, state.currentPageOffsetFraction)
        } else {
            listOf<Any>(position.page, position.offsetFraction)
        }
    },
    restore = { PagerPosition(page = it[0] as Int, offsetFraction = it[1] as Float) }
)

/**
 * 칸 entry 안의 페이저 상태 — `rememberPagerState { … }` 대신(상세 사진). 두 칸 장면 안팎이 바뀌면 직전 상태의
 * 쪽·쪽 안 위치로 새 상태를 만든다. [pageCount]는 매번 최신 람다를 읽는다(`rememberPagerState`와 같다).
 */
@Composable
fun rememberPanePagerState(pageCount: () -> Int): PagerState {
    val currentPageCount by rememberUpdatedState(pageCount)
    val position = rememberSaveable(saver = PagerPositionSaver) { PagerPosition() }
    val inPane = isInPaneScene()
    val state = remember(position, inPane) {
        Snapshot.withoutReadObservation {
            val previous = position.current
            // 쪽 안 위치는 생성자가 받는 [-0.5, 0.5]로 자른다 — 라이브러리 `DefaultPagerState.Saver`와 같다(5a K2)
            PagerState(
                currentPage = previous?.currentPage ?: position.page,
                currentPageOffsetFraction =
                    (previous?.currentPageOffsetFraction ?: position.offsetFraction)
                        .coerceIn(-0.5f, 0.5f)
            ) { currentPageCount() }
        }
    }
    SideEffect { position.current = state }
    return state
}
