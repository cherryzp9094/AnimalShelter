package com.cherryzp.animalshelter.navigation

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
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.cherryzp.animalshelter.data.api.toBasicString
import com.cherryzp.animalshelter.ui.bookmarks.BookmarkListScreen
import com.cherryzp.animalshelter.ui.detail.AnimalDetailScreen
import com.cherryzp.animalshelter.ui.detail.DetailPlaceholder
import com.cherryzp.animalshelter.ui.filter.FilterScreen
import com.cherryzp.animalshelter.ui.list.AnimalListScreen
import com.cherryzp.animalshelter.ui.photo.PhotoViewerScreen
import com.cherryzp.animalshelter.ui.settings.SettingsScreen
import com.cherryzp.animalshelter.ui.shelter.ShelterInfoScreen
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * back stack 저장 설정. 비JVM(wasmJs)은 리플렉션 직렬화가 없어서 키를 전부 등록한다(CMP Nav3 문서).
 * 키를 새로 만들면 여기에도 넣는다.
 */
internal val AppNavSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(AnimalListKey::class)
            subclass(FilterKey::class)
            subclass(AnimalDetailKey::class)
            subclass(PhotoViewerKey::class)
            subclass(BookmarkListKey::class)
            subclass(ShelterInfoKey::class)
            subclass(SettingsKey::class)
        }
    }
}

/** 앱 전체 최대 폭. 넘치면 가운데, 바깥은 `background`(03 0.4 extra-large). */
private val AppMaxWidth = 1600.dp

/** 보호소 정보(extraPane) 칸 폭. */
private val ShelterPaneWidth = 360.dp

/**
 * 앱 탐색(03 0.3·0.4, 브리핑 B14). 지시는 `calculatePaneScaffoldDirective`(expanded 2칸, large 이상 3칸).
 * - 목록·북마크 목록 = `listPane`(창 폭 클래스마다 400·440·480dp) + 상세 자리표시
 * - 상세 = `detailPane`, 보호소 정보 = `extraPane`(360dp — large 이상에서 세 번째 칸)
 * - 필터·설정·사진 뷰어는 칸 메타데이터 없이 전체 화면
 * ViewModel은 entry 안에서 `koinViewModel()`(`rememberViewModelStoreNavEntryDecorator`가 entry별 범위를 준다).
 * 칸이 바뀌어도 그리드·페이저·스크롤 상태는 entry 안의 `remember…State`/`rememberSaveable`이 지킨다.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AppNavDisplay() {
    val backStack = rememberNavBackStack(AppNavSavedStateConfiguration, AnimalListKey)
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val directive = calculatePaneScaffoldDirective(adaptiveInfo)
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)
    val listPaneWidth = listPaneWidthDp(adaptiveInfo.windowSizeClass.minWidthDp)
    val listPaneMetadata = remember(listPaneWidth) {
        ListDetailSceneStrategy.listPane(detailPlaceholder = { DetailPlaceholder() }) +
            ListDetailSceneStrategy.preferredPaneSize(width = listPaneWidth.dp)
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        AppNavDisplay(backStack, listDetailStrategy, listPaneMetadata)
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun AppNavDisplay(
    backStack: NavBackStack<NavKey>,
    listDetailStrategy: SceneStrategy<NavKey>,
    listPaneMetadata: Map<String, Any>,
) {
    NavDisplay(
        modifier = Modifier.widthIn(max = AppMaxWidth).fillMaxHeight(),
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
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
                    onOpenSettings = { backStack.add(SettingsKey) }
                )
            }
            entry<AnimalDetailKey>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
                AnimalDetailScreen(
                    viewModel = koinViewModel { parametersOf(key) },
                    showBack = !isListPaneExpanded(),
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
                    }
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
