package com.cherryzp.animalshelter.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.cherryzp.animalshelter.model.ListLayout
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.a11y_bookmarked
import com.cherryzp.animalshelter.resources.action_bookmark_add
import com.cherryzp.animalshelter.resources.action_bookmark_remove
import com.cherryzp.animalshelter.resources.ic_bookmark
import com.cherryzp.animalshelter.resources.ic_bookmark_border
import com.cherryzp.animalshelter.ui.theme.TABULAR_NUMBERS
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** 1열 카드 사진의 최대 높이(03 2.1). */
private val LargePhotoMaxHeight = 480.dp

/** 이 글자 배율부터 2열 카드의 1·2행(품종·성별 나이 체중)을 2줄까지(03 0.4, 중간 검토 F1). */
private const val TWO_LINE_FONT_SCALE = 1.3f

/**
 * 카드 하나가 두 형태를 그린다(03 2.1 표, 브리핑 A10, 중간 검토 F1). 글자는 두 형태 모두 1행 품종 / 2행 성별 · 나이 ·
 * 체중 / 3행 보호소 줄. [ListLayout.LARGE] 1열(확대형)만 4행 색상 · 특징(2줄)과 5행 발견 장소를 더한다 — 2열은
 * 글자 폭이 136~160dp라 그 문장을 넣지 않는다(상세에서 본다). 카드 전체가 접근성 한 문장(P10), 북마크 버튼은 따로
 * 초점을 받는다.
 *
 * @param selected 두 칸 이상에서 이 카드가 상세 칸에 열려 있다(3d D7) — 2dp `primary` 테두리 + `selected` 의미
 */
@Composable
fun AnimalCard(
    ui: AnimalCardUi,
    layout: ListLayout,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val large = layout == ListLayout.LARGE
    val typography = MaterialTheme.typography
    val facts = ui.facts.map { it.resolve() }
    val appearance = ui.appearance.map { it.resolve() }
    val a11y = cardA11ySentence(
        title = ui.titleParts.map { it.resolve() },
        shelterLine = ui.shelterLine,
        status = ui.status.a11y.resolve(),
        bookmarked = if (ui.isBookmarked) stringResource(Res.string.a11y_bookmarked) else null,
        happenPlace = if (large) ui.happenPlace else null
    )
    val twoLines = !large && LocalDensity.current.fontScale >= TWO_LINE_FONT_SCALE
    // 키보드 초점 링(4a M1): 키보드 입력 모드에서 카드에 초점이 오면 바깥 3dp secondary 테두리를 2dp 띄워 그린다
    // (Material Web 초점 링). 선택 표시(안쪽 2dp primary, D7)와 같이 보일 수 있다
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val keyboardMode = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val ringColor = MaterialTheme.colorScheme.secondary

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier
            .focusRing(visible = focused && keyboardMode, color = ringColor)
            .testTag(TestTags.card(ui.desertionNo))
            .semantics(mergeDescendants = true) {
                contentDescription = a11y
                if (selected) this.selected = true
            }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val photoHeight = if (large) min(maxWidth, LargePhotoMaxHeight) else maxWidth
            AnimalImage(
                url = ui.photoUrl,
                frame = PhotoFrame.CARD,
                modifier = Modifier.fillMaxWidth().height(photoHeight).clearAndSetSemantics {}
            )
            StatusBadge(
                status = ui.status,
                textStyle = if (large) typography.labelMedium else typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clearAndSetSemantics {}
            )
            BookmarkButton(
                bookmarked = ui.isBookmarked,
                onClick = onBookmarkClick,
                modifier = Modifier.align(
                    Alignment.TopEnd
                ).testTag(TestTags.cardBookmark(ui.desertionNo))
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(if (large) 16.dp else 12.dp).clearAndSetSemantics {}
        ) {
            ui.visibleRows(layout).forEach { row ->
                when (row) {
                    CardRow.BREED -> Text(
                        text = ui.breed.orEmpty(),
                        style = if (large) typography.titleLarge else typography.titleMedium,
                        maxLines = if (twoLines) 2 else 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    CardRow.FACTS -> Text(
                        text = facts.joinToString(PART_SEPARATOR),
                        style = (if (large) typography.bodyMedium else typography.bodySmall)
                            .copy(fontFeatureSettings = TABULAR_NUMBERS),
                        maxLines = if (twoLines) 2 else 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    CardRow.SHELTER -> Text(
                        text = ui.shelterLine.orEmpty(),
                        style = if (large) typography.bodyMedium else typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    CardRow.APPEARANCE -> Text(
                        text = appearance.joinToString(PART_SEPARATOR),
                        style = typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    CardRow.HAPPEN_PLACE -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = ui.happenPlace.orEmpty(),
                            style = typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * 사진 위 북마크 버튼: 터치 48dp, 아이콘 24dp, 지름 32dp 원 배경(`surface` 90% — 사진 위에서도 보이게).
 * 켜짐 `ic_bookmark` + `primary`, 꺼짐 `ic_bookmark_border` + `onSurfaceVariant`.
 * 누르면 scale 1 → 0.9 → 1(B8).
 */
@Composable
fun BookmarkButton(bookmarked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bounce = rememberPressBounce()
    IconButton(
        onClick = {
            bounce.play()
            onClick()
        },
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .bounce(bounce)
                .size(32.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape)
        ) {
            Icon(
                painter = painterResource(
                    if (bookmarked) Res.drawable.ic_bookmark else Res.drawable.ic_bookmark_border
                ),
                contentDescription = stringResource(
                    if (bookmarked) {
                        Res.string.action_bookmark_remove
                    } else {
                        Res.string.action_bookmark_add
                    }
                ),
                tint = if (bookmarked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/** 카드 모서리(12dp)에 맞춘 바깥 초점 링: 두께 [FocusRingWidth], 카드에서 [FocusRingGap] 띄운다(M1). */
private val CardCorner = 12.dp
private val FocusRingWidth = 3.dp
private val FocusRingGap = 2.dp

private fun Modifier.focusRing(visible: Boolean, color: Color): Modifier = drawWithContent {
    drawContent()
    if (!visible) return@drawWithContent
    val stroke = FocusRingWidth.toPx()
    val inset = FocusRingGap.toPx() + stroke / 2f // 선의 가운데까지
    drawRoundRect(
        color = color,
        topLeft = Offset(-inset, -inset),
        size = Size(size.width + inset * 2f, size.height + inset * 2f),
        cornerRadius = CornerRadius(CardCorner.toPx() + inset),
        style = Stroke(width = stroke)
    )
}
