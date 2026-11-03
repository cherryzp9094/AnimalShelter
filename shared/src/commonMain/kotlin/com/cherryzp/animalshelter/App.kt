package com.cherryzp.animalshelter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cherryzp.animalshelter.data.repository.SettingsRepository
import com.cherryzp.animalshelter.model.ThemeMode
import com.cherryzp.animalshelter.navigation.AppNavDisplay
import com.cherryzp.animalshelter.ui.theme.AnimalShelterTheme
import com.cherryzp.animalshelter.ui.theme.isDarkTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

/**
 * 앱 진입 Composable. Android `MainActivity`와 웹 `main()`이 같은 것을 띄운다.
 * 설정의 테마를 읽기 전에는 시스템 테마의 배경만 그린다(잘못된 테마가 번쩍이지 않게, 브리핑 A1).
 */
@Composable
fun App() {
    val settingsRepository = koinInject<SettingsRepository>()
    val themeMode by remember(settingsRepository) {
        settingsRepository.getSettingsStream().map { it.theme }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)

    val mode: ThemeMode? = themeMode
    if (mode == null) {
        AnimalShelterTheme {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }
        return
    }
    AnimalShelterTheme(darkTheme = mode.isDarkTheme()) {
        AppNavDisplay()
    }
}
