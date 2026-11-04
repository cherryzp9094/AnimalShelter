package com.cherryzp.animalshelter.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.about_data_source
import com.cherryzp.animalshelter.resources.action_call
import com.cherryzp.animalshelter.resources.action_open_map
import com.cherryzp.animalshelter.resources.action_shelter_info
import com.cherryzp.animalshelter.resources.disclaimer
import com.cherryzp.animalshelter.resources.label_org_name
import com.cherryzp.animalshelter.resources.label_shelter_address
import com.cherryzp.animalshelter.resources.label_shelter_tel
import com.cherryzp.animalshelter.resources.promo_condition
import com.cherryzp.animalshelter.resources.promo_image_a11y
import com.cherryzp.animalshelter.resources.section_found
import com.cherryzp.animalshelter.resources.section_health
import com.cherryzp.animalshelter.resources.section_notice
import com.cherryzp.animalshelter.resources.section_promotions
import com.cherryzp.animalshelter.resources.section_shelter
import com.cherryzp.animalshelter.resources.section_source
import com.cherryzp.animalshelter.resources.section_traits
import com.cherryzp.animalshelter.ui.common.PART_SEPARATOR
import com.cherryzp.animalshelter.ui.common.StatusBadge
import com.cherryzp.animalshelter.ui.common.TestTags
import com.cherryzp.animalshelter.ui.common.UiText
import com.cherryzp.animalshelter.ui.common.copyOnLongPress
import com.cherryzp.animalshelter.ui.common.rememberImageModel
import com.cherryzp.animalshelter.ui.common.resolve
import com.cherryzp.animalshelter.ui.common.statusMessage
import com.cherryzp.animalshelter.ui.theme.TABULAR_NUMBERS
import org.jetbrains.compose.resources.stringResource

// 상세 섹션(03 2.3 섹션 1~8, 06 4.2). 라벨(labelMedium·onSurfaceVariant) 위에 값(bodyLarge) — 표를 쓰지 않는다.

/** 섹션 제목(titleMedium + heading) 아래 내용. */
@Composable
fun DetailSection(title: String?, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }
            )
        }
        content()
    }
}

/** 라벨 위에 값. 칩이 있으면 칩을 나열한다(누르지 않는 표시용). */
@Composable
fun DetailRowView(row: DetailRow, modifier: Modifier = Modifier) {
    LabeledValue(
        label = row.label.resolve(),
        modifier = modifier
    ) {
        if (row.chips.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.chips.forEach { InfoChip(it) }
            }
        } else {
            Text(
                text = row.values.map { it.resolve() }.joinToString(PART_SEPARATOR),
                style = MaterialTheme.typography.bodyLarge.let {
                    if (row.numeric) it.copy(fontFeatureSettings = TABULAR_NUMBERS) else it
                }
            )
        }
    }
}

/** 라벨 위에 내용(값 하나 또는 칩 등). */
@Composable
fun LabeledValue(label: String, modifier: Modifier = Modifier, value: @Composable () -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        value()
    }
}

/** 누르지 않는 칩(백신·건강 검사). 버튼처럼 읽히지 않게 `AssistChip` 대신 테두리 상자로 그린다. */
@Composable
private fun InfoChip(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/** 행 목록(세로). */
@Composable
private fun DetailRows(rows: List<DetailRow>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { DetailRowView(it) }
    }
}

/**
 * 섹션 1 요약: 품종명(titleLarge) + 축종, 성별 | 중성화, 나이 | 체중, 색상 — 두 칸 격자(3e E1).
 * @param nameLineModifier 품종명 줄에 붙인다 — 상세 바 제목이 이 줄의 위치를 잰다(3d D6)
 */
@Composable
fun SummarySection(
    detail: AnimalDetailUi,
    modifier: Modifier = Modifier,
    nameLineModifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            // 품종명이 없으면 제목 묶음(축종 보조 제목)이 그 줄을 대신한다(D6).
            modifier = if (detail.name == null) nameLineModifier else Modifier
        ) {
            detail.name?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = nameLineModifier.semantics { heading() }
                )
            }
            detail.kindLabel?.let {
                Text(
                    text = it.resolve(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (detail.summary.isNotEmpty()) {
            // 칸 폭과 무관하게 한 줄에 둘, 같은 폭(R1) — 둘째 열이 줄마다 같은 자리. 마지막 홀수 항목은 줄 전체.
            FlowRow(
                maxItemsInEachRow = 2,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                detail.summary.forEachIndexed { index, row ->
                    DetailRowView(row, Modifier.weight(1f).testTag(TestTags.summaryItem(index)))
                }
            }
        }
    }
}

/** 오른쪽(또는 1단 아래) 섹션 2~8. 빈 섹션은 숨긴다. */
@Composable
fun DetailSectionsBody(
    detail: AnimalDetailUi,
    shelterToday: List<UiText>,
    hasMap: Boolean,
    onDial: (String) -> Unit,
    onCopyAddress: (String) -> Unit,
    onOpenMap: () -> Unit,
    onOpenShelter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(32.dp)) {
        DetailSection(stringResource(Res.string.section_notice)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusBadge(status = detail.status, textStyle = MaterialTheme.typography.labelLarge)
                detail.statusHint?.let {
                    Text(text = it.resolve(), style = MaterialTheme.typography.bodyMedium)
                }
            }
            DetailRows(detail.notice)
        }
        detail.traits?.let { traits ->
            DetailSection(stringResource(Res.string.section_traits)) {
                Text(text = traits, style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (detail.found.isNotEmpty()) {
            DetailSection(stringResource(Res.string.section_found)) { DetailRows(detail.found) }
        }
        if (detail.health.isNotEmpty()) {
            DetailSection(stringResource(Res.string.section_health)) { DetailRows(detail.health) }
        }
        detail.shelter?.let { shelter ->
            DetailSection(stringResource(Res.string.section_shelter)) {
                ShelterCard(
                    shelter = shelter,
                    today = shelterToday,
                    hasMap = hasMap,
                    onDial = onDial,
                    onCopyAddress = onCopyAddress,
                    onOpenMap = onOpenMap,
                    onOpenShelter = onOpenShelter
                )
            }
        }
        if (detail.promotions.isNotEmpty()) {
            DetailSection(stringResource(Res.string.section_promotions)) {
                detail.promotions.forEach { PromotionCard(it) }
            }
        }
        DetailSection(stringResource(Res.string.section_source)) {
            SourceText()
        }
    }
}

/** 출처·면책(상세·보호소·설정 공용). */
@Composable
fun SourceText(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.about_data_source),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(Res.string.disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 섹션 6 보호소 카드: 이름, 전화(탭 → 다이얼, 목록 값), 주소(길게 눌러 복사), 관할 기관, 오늘 운영시간(B7),
 * 버튼 지도(좌표 있을 때)·보호소 정보(등록번호 있을 때).
 */
@Composable
private fun ShelterCard(
    shelter: DetailShelterUi,
    today: List<UiText>,
    hasMap: Boolean,
    onDial: (String) -> Unit,
    onCopyAddress: (String) -> Unit,
    onOpenMap: () -> Unit,
    onOpenShelter: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            shelter.name?.let { Text(text = it, style = MaterialTheme.typography.titleMedium) }
            if (today.isNotEmpty()) {
                Text(
                    text = today.map { it.resolve() }.joinToString(PART_SEPARATOR),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFeatureSettings = TABULAR_NUMBERS
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            shelter.tel?.let { tel ->
                val callLabel = stringResource(Res.string.action_call)
                LabeledValue(
                    label = stringResource(Res.string.label_shelter_tel),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable(onClickLabel = callLabel) { onDial(tel) }
                ) {
                    Text(
                        text = tel,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFeatureSettings = TABULAR_NUMBERS
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            shelter.address?.let { address ->
                LabeledValue(
                    label = stringResource(Res.string.label_shelter_address),
                    modifier = Modifier.copyOnLongPress { onCopyAddress(address) }
                ) {
                    Text(text = address, style = MaterialTheme.typography.bodyLarge)
                }
            }
            shelter.orgName?.let {
                LabeledValue(label = stringResource(Res.string.label_org_name)) {
                    Text(text = it, style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (hasMap || shelter.regNo != null) {
                // 줄을 넘기면(큰 글자) 버튼끼리 붙지 않게 세로도 8dp(3d 점검 A7)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hasMap) {
                        OutlinedButton(onClick = onOpenMap) {
                            Text(stringResource(Res.string.action_open_map))
                        }
                    }
                    if (shelter.regNo != null) {
                        OutlinedButton(onClick = onOpenShelter) {
                            Text(stringResource(Res.string.action_shelter_info))
                        }
                    }
                }
            }
        }
    }
}

/** 섹션 7 보호소 안내 카드 하나. */
@Composable
private fun PromotionCard(promo: PromotionUi) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            val typeText = promo.type.resolve()
            // 종류 라벨은 행동이 아니다 → onSurfaceVariant(R2 — primary 글자는 전화번호 링크에만)
            Text(
                text = typeText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            promo.title?.let { Text(text = it, style = MaterialTheme.typography.titleSmall) }
            promo.period?.let {
                Text(
                    text = it.resolve(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFeatureSettings = TABULAR_NUMBERS
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            promo.condition?.let {
                LabeledValue(label = stringResource(Res.string.promo_condition)) {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                }
            }
            promo.text?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
            promo.imageUrl?.let {
                AsyncImage(
                    model = rememberImageModel(it),
                    // 안내 제목, 없으면 종류 문구(R5)
                    contentDescription = stringResource(
                        Res.string.promo_image_a11y,
                        promo.title ?: typeText
                    ),
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
                )
            }
        }
    }
}

/** 상단 배너(재조회 0건 등): 아이콘 + 한 문장, `secondaryContainer`. */
@Composable
fun NoticeBanner(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.statusMessage()
            )
        }
    }
}
