package com.cherryzp.animalshelter.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.pretendard_regular
import com.cherryzp.animalshelter.resources.pretendard_semibold
import org.jetbrains.compose.resources.Font

/** 숫자 폭 고정(D-day·마리 수·날짜·체중·전화, 06 3.3). `TextStyle(fontFeatureSettings = TABULAR_NUMBERS)`. */
const val TABULAR_NUMBERS = "tnum"

/**
 * Pretendard(OFL, 공식 릴리스 v1.3.9 OTF). Regular = W400, SemiBold = W600이고 M3 스케일의 Medium(W500)과
 * Bold(W700) 자리도 SemiBold 파일로 그린다(Phase 0 B안, 브리핑 P7). 글꼴 참조는 이 파일 한곳에만 둔다.
 */
@Composable
fun pretendardFontFamily(): FontFamily {
    val regular = Font(Res.font.pretendard_regular, FontWeight.W400)
    val semiBoldW500 = Font(Res.font.pretendard_semibold, FontWeight.W500)
    val semiBold = Font(Res.font.pretendard_semibold, FontWeight.W600)
    val semiBoldW700 = Font(Res.font.pretendard_semibold, FontWeight.W700)
    return remember(regular, semiBoldW500, semiBold, semiBoldW700) {
        FontFamily(regular, semiBoldW500, semiBold, semiBoldW700)
    }
}

/**
 * 앱 글꼴 한곳(06 3절). M3 기본 스케일에 글꼴만 바꾸고 `bodyMedium` 줄 간격을 21sp(1.5배)로 둔다.
 * 최소 12sp(`labelSmall`은 기본 11sp → 12sp). 고정 높이 텍스트 상자는 쓰지 않는다.
 */
@Composable
fun appTypography(): Typography {
    val family = pretendardFontFamily()
    return remember(family) {
        val base = Typography()
        fun TextStyle.withFamily() = copy(fontFamily = family)
        Typography(
            displayLarge = base.displayLarge.withFamily(),
            displayMedium = base.displayMedium.withFamily(),
            displaySmall = base.displaySmall.withFamily(),
            headlineLarge = base.headlineLarge.withFamily(),
            headlineMedium = base.headlineMedium.withFamily(),
            headlineSmall = base.headlineSmall.withFamily(),
            titleLarge = base.titleLarge.withFamily(),
            titleMedium = base.titleMedium.withFamily(),
            titleSmall = base.titleSmall.withFamily(),
            bodyLarge = base.bodyLarge.withFamily(),
            bodyMedium = base.bodyMedium.withFamily().copy(lineHeight = 21.sp),
            bodySmall = base.bodySmall.withFamily(),
            labelLarge = base.labelLarge.withFamily(),
            labelMedium = base.labelMedium.withFamily(),
            labelSmall = base.labelSmall.withFamily().copy(fontSize = 12.sp, lineHeight = 16.sp)
        )
    }
}
