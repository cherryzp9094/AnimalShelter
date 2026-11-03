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
import com.cherryzp.animalshelter.ui.placeholder.DetailPlaceholderPane
import com.cherryzp.animalshelter.ui.placeholder.ListPlaceholderScreen
import com.cherryzp.animalshelter.ui.placeholder.PlaceholderScreen
import com.cherryzp.animalshelter.ui.placeholder.SettingsPlaceholderScreen
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

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
 * Phase 1은 목록·설정 자리표시만 등록하고 나머지 키는 [PlaceholderScreen]으로 묶는다.
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
                ListPlaceholderScreen(onOpenSettings = { backStack.add(SettingsKey) })
            }
            entry<AnimalDetailKey>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
                PlaceholderScreen(keyName = "AnimalDetailKey(${key.desertionNo})")
            }
            entry<SettingsKey> {
                SettingsPlaceholderScreen(onBack = { backStack.removeLastOrNull() })
            }
            entry<FilterKey> { PlaceholderScreen(keyName = "FilterKey") }
            entry<PhotoViewerKey> { PlaceholderScreen(keyName = "PhotoViewerKey") }
            entry<BookmarkListKey> { PlaceholderScreen(keyName = "BookmarkListKey") }
            entry<ShelterInfoKey> { PlaceholderScreen(keyName = "ShelterInfoKey") }
        }
    )
}
