package com.cherryzp.animalshelter.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.cherryzp.animalshelter.data.api.toBasicString
import com.cherryzp.animalshelter.ui.filter.FilterScreen
import com.cherryzp.animalshelter.ui.list.AnimalListScreen
import com.cherryzp.animalshelter.ui.placeholder.DetailPlaceholderPane
import com.cherryzp.animalshelter.ui.placeholder.PlaceholderScreen
import com.cherryzp.animalshelter.ui.placeholder.SettingsPlaceholderScreen
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.compose.viewmodel.koinViewModel

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

/**
 * 앱 탐색. 두 칸(목록 + 상세)은 창 폭 ≥ 840dp(expanded)부터, 그 아래는 한 칸(03-screens 0.4).
 * 목록·필터는 실제 화면(Phase 3a), 상세·사진·북마크·보호소·설정은 자리표시(3b가 바꾼다).
 * ViewModel은 entry 안에서 `koinViewModel()`(`rememberViewModelStoreNavEntryDecorator`가 entry별 범위를 준다).
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AppNavDisplay() {
    val backStack = rememberNavBackStack(AppNavSavedStateConfiguration, AnimalListKey)
    val directive = calculatePaneScaffoldDirective(currentWindowAdaptiveInfoV2())
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        sceneStrategies = listOf(listDetailStrategy),
        entryProvider = entryProvider {
            entry<AnimalListKey>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { DetailPlaceholderPane() }
                )
            ) {
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
                PlaceholderScreen(keyName = "AnimalDetailKey(${key.desertionNo})")
            }
            entry<SettingsKey> {
                SettingsPlaceholderScreen(onBack = { backStack.removeLastOrNull() })
            }
            entry<FilterKey> {
                FilterScreen(viewModel = koinViewModel(), onBack = { backStack.removeLastOrNull() })
            }
            entry<PhotoViewerKey> { PlaceholderScreen(keyName = "PhotoViewerKey") }
            entry<BookmarkListKey> { PlaceholderScreen(keyName = "BookmarkListKey") }
            entry<ShelterInfoKey> { PlaceholderScreen(keyName = "ShelterInfoKey") }
        }
    )
}
