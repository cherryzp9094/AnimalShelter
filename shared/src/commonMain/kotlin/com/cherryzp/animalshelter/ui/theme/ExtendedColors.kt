package com.cherryzp.animalshelter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * M3 색 역할 밖의 확장 색(06 2.3, 브리핑 P8). 값은 `tools/gen_theme.mjs`가 `Color.kt`에 생성한다
 * ([LightExtendedColors]·[DarkExtendedColors]).
 */
@Immutable
data class ExtendedColors(
    /** 마감 임박 배지 배경(공고중 D-3 이하). */
    val closingSoonContainer: Color,
    /** 마감 임박 배지 글자. */
    val onClosingSoonContainer: Color,
)

/** 테마가 제공하는 확장 색. [AnimalShelterTheme] 밖에서는 라이트 값. */
val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/** `MaterialTheme.extendedColors.closingSoonContainer`처럼 쓴다. */
val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current
