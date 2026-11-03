package com.cherryzp.animalshelter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.cherryzp.animalshelter.model.ThemeMode

/**
 * 앱 테마. 시드 #0A81AB에서 생성한 정적 스킴과 확장 색(Color.kt) + Pretendard 글꼴(Type.kt).
 * 동적 색상은 쓰지 않는다(플랫폼 간 같은 색). 라이트/다크는 [darkTheme]로 — 설정의 [ThemeMode]는
 * [isDarkTheme]으로 푼다.
 */
@Composable
fun AnimalShelterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalExtendedColors provides if (darkTheme) DarkExtendedColors else LightExtendedColors
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = appTypography(),
            content = content
        )
    }
}

/** 설정의 테마 → 다크 여부. SYSTEM은 시스템을 따른다. */
@Composable
fun ThemeMode.isDarkTheme(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
