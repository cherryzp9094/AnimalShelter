package com.cherryzp.animalshelter.ui.common

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
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
 */
@Composable
fun AnimalCard(
    ui: AnimalCardUi,
    layout: ListLayout,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier,
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

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(),
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = a11y }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val photoHeight = if (large) min(maxWidth, LargePhotoMaxHeight) else maxWidth
            AnimalImage(
                url = ui.photoUrl,
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
                modifier = Modifier.align(Alignment.TopEnd)
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
