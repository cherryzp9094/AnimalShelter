package com.cherryzp.animalshelter.ui.filter

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.action_cancel
import com.cherryzp.animalshelter.resources.action_ok
import com.cherryzp.animalshelter.resources.date_clear
import com.cherryzp.animalshelter.resources.date_default
import com.cherryzp.animalshelter.resources.date_last_30
import com.cherryzp.animalshelter.resources.date_last_7
import com.cherryzp.animalshelter.resources.date_picker_title
import com.cherryzp.animalshelter.resources.filter_date_from
import com.cherryzp.animalshelter.resources.filter_date_to
import com.cherryzp.animalshelter.ui.common.QuickDate
import com.cherryzp.animalshelter.ui.common.UiText
import com.cherryzp.animalshelter.ui.common.resolve
import com.cherryzp.animalshelter.ui.theme.TABULAR_NUMBERS
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

/** 고를 수 있는 가장 이른 날(브리핑 A18). */
val MIN_PICKABLE_DATE = LocalDate(2000, 1, 1)

/** `DatePickerState`의 millis는 UTC 자정이다(브리핑 A18). */
fun LocalDate.toPickerMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

fun pickerMillisToDate(millis: Long): LocalDate =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date

private enum class DateField { FROM, TO }

/**
 * 구조 날짜(03 2.2, 브리핑 A18): 빠른 칩(최근 7일·30일·지우기) + 시작·종료 읽기 전용 칸(누르면 날짜 선택).
 * 칸 아래에 인라인 오류, 비어 있으면 "최근 1개월"(API 기본 창).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DateRangeFields(
    from: LocalDate?,
    to: LocalDate?,
    today: LocalDate,
    quickDate: QuickDate?,
    error: UiText?,
    onQuickDate: (QuickDate?) -> Unit,
    onFromPicked: (LocalDate) -> Unit,
    onToPicked: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf<DateField?>(null) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = quickDate == QuickDate.LAST_7,
                onClick = { onQuickDate(QuickDate.LAST_7) },
                label = { Text(stringResource(Res.string.date_last_7)) }
            )
            FilterChip(
                selected = quickDate == QuickDate.LAST_30,
                onClick = { onQuickDate(QuickDate.LAST_30) },
                label = { Text(stringResource(Res.string.date_last_30)) }
            )
            FilterChip(
                selected = false,
                onClick = { onQuickDate(null) },
                label = { Text(stringResource(Res.string.date_clear)) }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateField(
                label = stringResource(Res.string.filter_date_from),
                date = from,
                isError = error != null,
                onClick = { picking = DateField.FROM },
                modifier = Modifier.weight(1f)
            )
            DateField(
                label = stringResource(Res.string.filter_date_to),
                date = to,
                isError = error != null,
                onClick = { picking = DateField.TO },
                modifier = Modifier.weight(1f)
            )
        }
        when {
            error != null -> Text(
                text = error.resolve(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            from == null && to == null -> Text(
                text = stringResource(Res.string.date_default),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }

    picking?.let { field ->
        DatePickerDialogFor(
            initial = if (field == DateField.FROM) from else to,
            today = today,
            onDismiss = { picking = null },
            onPicked = { date ->
                picking = null
                if (field == DateField.FROM) onFromPicked(date) else onToPicked(date)
            }
        )
    }
}

/** 읽기 전용 날짜 칸. 칸을 누르거나 달력 아이콘을 누르면 [onClick]. */
@Composable
private fun DateField(
    label: String,
    date: LocalDate?,
    isError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val currentOnClick by rememberUpdatedState(onClick)
    LaunchedEffect(interaction) {
        interaction.interactions.collect { if (it is PressInteraction.Release) currentOnClick() }
    }
    OutlinedTextField(
        value = date?.toString().orEmpty(),
        onValueChange = {},
        readOnly = true,
        singleLine = true,
        isError = isError,
        label = { Text(label) },
        textStyle = dateTextStyle(),
        trailingIcon = {
            IconButton(onClick = onClick) {
                Icon(Icons.Filled.DateRange, contentDescription = label)
            }
        },
        interactionSource = interaction,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun dateTextStyle(): TextStyle =
    MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TABULAR_NUMBERS)

/**
 * M3 날짜 선택(2000-01-01 ~ 오늘). 제목은 우리 문구(로케일에 따라 영어로 나오지 않게).
 * CMP material3 1.9.0에서 `DatePickerDialog`는 아직 `@ExperimentalMaterial3Api`다(`DatePicker`는 안정).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialogFor(
    initial: LocalDate?,
    today: LocalDate,
    onDismiss: () -> Unit,
    onPicked: (LocalDate) -> Unit,
) {
    val selectable = remember(today) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                pickerMillisToDate(utcTimeMillis) in MIN_PICKABLE_DATE..today

            override fun isSelectableYear(year: Int): Boolean =
                year in MIN_PICKABLE_DATE.year..today.year
        }
    }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (initial ?: today).toPickerMillis(),
        yearRange = MIN_PICKABLE_DATE.year..today.year,
        selectableDates = selectable
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { onPicked(pickerMillisToDate(it)) }
                        ?: onDismiss()
                }
            ) { Text(stringResource(Res.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        }
    ) {
        DatePicker(
            state = state,
            title = {
                Text(
                    text = stringResource(Res.string.date_picker_title),
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                )
            }
        )
    }
}
