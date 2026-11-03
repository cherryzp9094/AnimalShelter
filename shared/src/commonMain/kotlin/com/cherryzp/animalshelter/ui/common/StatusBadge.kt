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
import androidx.compose.ui.semantics.contentDescription
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
        modifier = modifier.clearAndSetSemantics { contentDescription = a11y },
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
