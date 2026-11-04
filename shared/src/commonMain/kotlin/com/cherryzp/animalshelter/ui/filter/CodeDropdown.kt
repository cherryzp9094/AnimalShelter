package com.cherryzp.animalshelter.ui.filter

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.cherryzp.animalshelter.data.repository.CacheState
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.a11y_loading
import com.cherryzp.animalshelter.resources.action_retry
import com.cherryzp.animalshelter.resources.choice_all
import com.cherryzp.animalshelter.resources.filter_codes_error
import com.cherryzp.animalshelter.resources.filter_codes_stale
import com.cherryzp.animalshelter.ui.common.progressName
import com.cherryzp.animalshelter.ui.common.statusMessage
import org.jetbrains.compose.resources.stringResource

/**
 * 코드표 드롭다운(브리핑 A17). 시도·시군구·보호소는 읽기 전용 칸, 품종은 [query]가 있으면 입력해 거르는 칸.
 * 첫 항목은 "전체"(null). 코드표 상태 규칙은 셋·품종 모두 같다:
 * 첫 로딩 → 메뉴 안 진행 표시, 데이터 없이 실패 → `filter_codes_error` + 다시 시도,
 * 오래된 캐시 + 실패 → 칸 아래 `filter_codes_stale`(오래된 캐시도 먼저 보인다).
 * 입력 칸에서 메뉴 항목(전체 포함)을 고르면 키보드를 닫는다 — 남은 키보드가 아래쪽 [검색하기]를 가려 그 자리를
 * 누르면 키보드가 받았다(5a-live M13). 초점은 그대로 둔다(`clearFocus` 없음 — 웹 키보드 사용자가 자리를 잃지 않게).
 * 바깥 누르기·뒤로로 메뉴만 닫을 때는 키보드를 건드리지 않는다(계속 입력할 수 있다).
 *
 * @param state null이면 위를 아직 고르지 않았다(비활성)
 * @param selectedName 지금 고른 이름. null = 전체
 * @param disabledHint 비활성일 때 칸 아래 안내
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> CodeDropdown(
    label: String,
    state: CacheState<List<T>>?,
    selectedName: String?,
    itemName: (T) -> String,
    onSelected: (T?) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    disabledHint: String? = null,
    query: String? = null,
    onQueryChange: (String) -> Unit = {},
    placeholder: String? = null,
) {
    val enabled = state != null
    var expanded by remember { mutableStateOf(false) }
    val allLabel = stringResource(Res.string.choice_all)
    val data = state?.data
    val error = state?.error
    val editable = query != null
    val keyboard = LocalSoftwareKeyboardController.current
    val shown = when {
        data == null || !editable || query.isBlank() || query == selectedName -> data
        else -> data.filter { itemName(it).contains(query.trim(), ignoreCase = true) }
    }

    fun close() {
        expanded = false
        if (editable) onQueryChange(selectedName.orEmpty())
    }

    fun pick(item: T?) {
        onSelected(item)
        expanded = false
        if (editable) keyboard?.hide()
    }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = if (editable) query.orEmpty() else selectedName ?: allLabel,
            onValueChange = {
                if (editable) {
                    onQueryChange(it)
                    expanded = true
                }
            },
            readOnly = !editable,
            enabled = enabled,
            singleLine = true,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled)
            },
            supportingText = when {
                !enabled && disabledHint != null -> {
                    { Text(disabledHint) }
                }

                data == null && error != null -> {
                    {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(Res.string.filter_codes_error),
                                modifier = Modifier.weight(1f).statusMessage()
                            )
                            TextButton(onClick = onRetry) {
                                Text(stringResource(Res.string.action_retry))
                            }
                        }
                    }
                }

                data != null && error != null -> {
                    {
                        Text(
                            stringResource(Res.string.filter_codes_stale),
                            modifier = Modifier.statusMessage()
                        )
                    }
                }

                else -> null
            },
            isError = data == null && error != null,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(
                    if (editable) {
                        ExposedDropdownMenuAnchorType.PrimaryEditable
                    } else {
                        ExposedDropdownMenuAnchorType.PrimaryNotEditable
                    },
                    enabled
                )
        )
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = ::close) {
            when {
                shown == null && error == null -> DropdownMenuItem(
                    text = {
                        CircularProgressIndicator(
                            Modifier.size(
                                24.dp
                            ).progressName(stringResource(Res.string.a11y_loading)),
                            strokeWidth = 2.dp
                        )
                    },
                    onClick = {},
                    enabled = false
                )

                shown == null -> {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.filter_codes_error)) },
                        onClick = {},
                        enabled = false
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.action_retry)) },
                        onClick = {
                            expanded = false
                            onRetry()
                        }
                    )
                }

                else -> {
                    DropdownMenuItem(text = { Text(allLabel) }, onClick = { pick(null) })
                    shown.forEach { item ->
                        DropdownMenuItem(text = { Text(itemName(item)) }, onClick = { pick(item) })
                    }
                }
            }
        }
    }
}
