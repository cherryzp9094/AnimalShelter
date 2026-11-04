package com.cherryzp.animalshelter.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cherryzp.animalshelter.ui.theme.TABULAR_NUMBERS
import com.cherryzp.animalshelter.ui.theme.extendedColors

/** 배지 배경·글자 색(06 2.3, 브리핑 P9). */
@Composable
fun StatusKind.badgeColors(): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        StatusKind.NOTICING -> scheme.primaryContainer to scheme.onPrimaryContainer

        StatusKind.CLOSING_SOON -> MaterialTheme.extendedColors.let {
            it.closingSoonContainer to it.onClosingSoonContainer
        }

        StatusKind.PROTECTING -> scheme.tertiaryContainer to scheme.onTertiaryContainer

        StatusKind.ENDED, StatusKind.UNKNOWN ->
            scheme.surfaceContainerHigh to
                scheme.onSurfaceVariant
    }
}

/**
 * 상태 배지: 불투명 배경, 모서리 8dp, 글자 필수(색만으로 구분하지 않는다), 애니메이션 없음.
 * 사진 위에 얹는다(06 2.3 — 사진 위에는 배지만). 스크린 리더에는 "D-4" 대신 상태 문장(`a11y_days_left` 등, C2②).
 * 문장은 설명이 아니라 글 의미로 준다(5a-live M5): CMP 1.12.2 웹은 설명을 역할 없는 `div`의 `aria-label`로 내는데
 * 브라우저가 그것을 이름으로 쓰지 않아 배지가 접근성 트리에서 빠졌다. 글 의미는 `innerText`로 나간다.
 * Android 낭독은 그대로다. 카드 안의 배지는 카드가 의미를 한 문장으로 덮는다.
 */
@Composable
fun StatusBadge(
    status: StatusUi,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall,
) {
    val (container, content) = status.kind.badgeColors()
    val a11y = status.a11y.resolve()
    Surface(
        modifier = modifier.clearAndSetSemantics { text = AnnotatedString(a11y) },
        shape = RoundedCornerShape(8.dp),
        color = container,
        contentColor = content
    ) {
        Text(
            text = status.label.resolve(),
            style = textStyle.copy(fontFeatureSettings = TABULAR_NUMBERS),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
