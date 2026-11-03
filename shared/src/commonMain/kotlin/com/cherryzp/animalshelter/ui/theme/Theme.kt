package com.cherryzp.animalshelter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * 앱 테마. 시드 #0A81AB에서 생성한 정적 스킴(Color.kt)을 쓴다.
 * 동적 색상은 쓰지 않는다(플랫폼 간 같은 색). 다크 테마는 시스템을 따른다(설정 고정은 Phase 3).
 */
@Composable
fun AnimalShelterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = AppTypography,
        content = content
    )
}
