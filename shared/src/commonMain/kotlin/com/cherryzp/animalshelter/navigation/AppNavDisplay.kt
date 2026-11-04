package com.cherryzp.animalshelter.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.defaultPopTransitionSpec
import androidx.navigation3.ui.defaultPredictivePopTransitionSpec
import androidx.navigation3.ui.defaultTransitionSpec
import androidx.savedstate.serialization.SavedStateConfiguration
import com.cherryzp.animalshelter.data.api.toBasicString
import com.cherryzp.animalshelter.ui.bookmarks.BookmarkListScreen
import com.cherryzp.animalshelter.ui.common.LocalReducedMotion
import com.cherryzp.animalshelter.ui.detail.AnimalDetailScreen
import com.cherryzp.animalshelter.ui.detail.DetailPlaceholder
import com.cherryzp.animalshelter.ui.filter.FilterScreen
import com.cherryzp.animalshelter.ui.list.AnimalListScreen
import com.cherryzp.animalshelter.ui.photo.PhotoViewerScreen
import com.cherryzp.animalshelter.ui.settings.SettingsScreen
import com.cherryzp.animalshelter.ui.shelter.ShelterInfoScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** back stack 저장 설정. 비JVM(wasmJs)은 리플렉션 직렬화가 없어서 키를 전부 등록한다(CMP Nav3 문서 — [AppNavSerializersModule]). */
internal val AppNavSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = AppNavSerializersModule
}

/** 앱 전체 최대 폭. 넘치면 가운데, 바깥은 `background`(03 0.4 extra-large). */
private val AppMaxWidth = 1600.dp

/** 보호소 정보(extraPane) 칸 폭. */
private val ShelterPaneWidth = 360.dp

/**
 * 앱 탐색(03 0.3·0.4, 브리핑 B14). 지시는 `calculatePaneScaffoldDirective`(expanded 2칸, large 이상 3칸)에
 * 칸 수 상한 [maxPaneCount](창 높이 compact면 1칸)를 덮어쓴다.
 * - 목록·북마크 목록 = `listPane` + 상세 자리표시. 폭(창 폭 클래스마다 400·440·480dp)은 지시의
 *   `defaultPanePreferredWidth`로 — 창 크기가 바뀌면 바로 따라간다
 * - 상세 = `detailPane`, 보호소 정보 = `extraPane`(360dp — large 이상에서 세 번째 칸)
 * - 필터·설정·사진 뷰어는 칸 메타데이터 없이 전체 화면
 * ViewModel은 entry 안에서 `koinViewModel()`(`rememberViewModelStoreNavEntryDecorator`가 entry별 범위를 준다).
 * 칸이 바뀌어도 그리드·페이저·스크롤 상태는 entry 안의 `remember…State`/`rememberSaveable`이 지킨다 — 칸 entry의
 * 그리드·페이저는 `ui/common/PaneLazyState.kt`의 도우미로(두 칸 → 한 칸 장면에서 굳지 않게, 4a-fixes 검토 G1).
 * [navigation]: 처음 back stack(저장된 상태가 없을 때)과 플랫폼 연결(웹 브라우저 기록 W3, Android App Links L3).
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AppNavDisplay(navigation: NavigationBinding = NavigationBinding.None) {
    val backStack = rememberNavBackStack(
        AppNavSavedStateConfiguration,
        *navigation.initialStack.ifEmpty { listOf(AnimalListKey) }.toTypedArray()
    )
    navigation.Bind(backStack)
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    // 목록 칸 폭은 지시의 기본 폭으로 준다 — entry 메타데이터는 entry가 처음 만들어질 때 고정돼
    // 웹에서 창 크기만 바꾸면(재생성 없음) 예전 폭이 남는다. 상세는 남는 폭, 보호소는 메타데이터 360dp.
    // 창 높이가 compact(폰 가로)면 폭과 무관하게 한 칸(N1) — 기본 지시는 폭만 본다.
    val sizeClass = adaptiveInfo.windowSizeClass
    val listPaneWidth = listPaneWidthDp(sizeClass.minWidthDp)
    val directive = calculatePaneScaffoldDirective(adaptiveInfo).copy(
        maxHorizontalPartitions = maxPaneCount(sizeClass.minWidthDp, sizeClass.minHeightDp),
        defaultPanePreferredWidth = listPaneWidth.dp
    )
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)
    val listPaneMetadata = remember {
        ListDetailSceneStrategy.listPane(detailPlaceholder = { DetailPlaceholder() })
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        AppNavDisplay(backStack, listDetailStrategy, listPaneMetadata)
    }
}

/** 목록·북마크 칸 옆 상세에 열린 카드 번호(3d D7). 목록 칸과 상세 칸이 함께 보일 때만, 아니면 null. */
@Composable
private fun openedDetailBeside(backStack: List<NavKey>, listKey: NavKey): String? =
    if (isListBesideDetail()) backStack.openedDetailAfter(listKey) else null

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun AppNavDisplay(
    backStack: NavBackStack<NavKey>,
    listDetailStrategy: SceneStrategy<NavKey>,
    listPaneMetadata: Map<String, Any>,
) {
    // 모션 줄이기(M2)면 화면 전환 없음, 아니면 Nav3 기본 전환(P13) — Android는 페이드(700ms), 웹은 CMP 기본이 전환
    // 없음(navigation3-ui `NavDisplay.web.kt`의 `EnterTransition.None`/`ExitTransition.None`)이라 이 분기와 무관하게 바로 바뀐다
    val reducedMotion = LocalReducedMotion.current
    NavDisplay(
        modifier = Modifier.widthIn(max = AppMaxWidth).fillMaxHeight(),
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        transitionSpec = if (reducedMotion) noTransition() else defaultTransitionSpec(),
        popTransitionSpec = if (reducedMotion) noTransition() else defaultPopTransitionSpec(),
        predictivePopTransitionSpec = if (reducedMotion) {
            { _ -> EnterTransition.None togetherWith ExitTransition.None }
        } else {
            defaultPredictivePopTransitionSpec()
        },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        sceneStrategies = listOf(listDetailStrategy),
        entryProvider = entryProvider {
            entry<AnimalListKey>(metadata = listPaneMetadata) {
                AnimalListScreen(
                    viewModel = koinViewModel(),
                    onOpenFilter = { backStack.add(FilterKey) },
                    onOpenAnimal = { card ->
                        backStack.addDetail(
                            AnimalDetailKey(card.desertionNo, card.happenDate?.toBasicString())
                        )
                    },
                    onOpenBookmarks = { backStack.add(BookmarkListKey) },
                    onOpenSettings = { backStack.add(SettingsKey) },
                    selectedDesertionNo = openedDetailBeside(backStack, AnimalListKey)
                )
            }
            entry<AnimalDetailKey>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
                AnimalDetailScreen(
                    viewModel = koinViewModel { parametersOf(key) },
                    showBack = !isDetailBesideOtherPane(),
                    onBack = { backStack.removeLastOrNull() },
                    onOpenPhoto = { backStack.add(it) },
                    onOpenShelter = { backStack.add(it) },
                    onGoToList = { backStack.popToList() }
                )
            }
            entry<SettingsKey> {
                SettingsScreen(viewModel = koinViewModel(), onBack = {
                    backStack.removeLastOrNull()
                })
            }
            entry<FilterKey> {
                FilterScreen(viewModel = koinViewModel(), onBack = { backStack.removeLastOrNull() })
            }
            entry<PhotoViewerKey> { key ->
                PhotoViewerScreen(
                    viewModel = koinViewModel { parametersOf(key) },
                    initialIndex = key.index,
                    onClose = { backStack.removeLastOrNull() }
                )
            }
            entry<BookmarkListKey>(metadata = listPaneMetadata) {
                BookmarkListScreen(
                    viewModel = koinViewModel(),
                    onBack = { backStack.removeLastOrNull() },
                    onOpenAnimal = { card ->
                        backStack.addDetail(
                            AnimalDetailKey(
                                card.desertionNo,
                                card.happenDate?.toBasicString(),
                                fromBookmarks = true
                            )
                        )
                    },
                    selectedDesertionNo = openedDetailBeside(backStack, BookmarkListKey)
                )
            }
            entry<ShelterInfoKey>(
                metadata = ListDetailSceneStrategy.extraPane() +
                    ListDetailSceneStrategy.preferredPaneSize(width = ShelterPaneWidth)
            ) { key ->
                ShelterInfoScreen(
                    viewModel = koinViewModel { parametersOf(key) },
                    onBack = { backStack.removeLastOrNull() },
                    onShowNotices = { backStack.popToList() }
                )
            }
        }
    )
}

/** 전환 없음(모션 줄이기, M2). */
private fun noTransition(): AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform =
    { EnterTransition.None togetherWith ExitTransition.None }
