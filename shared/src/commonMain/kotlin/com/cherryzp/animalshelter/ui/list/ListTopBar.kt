package com.cherryzp.animalshelter.ui.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cherryzp.animalshelter.model.ListLayout
import com.cherryzp.animalshelter.model.SortOrder
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.action_bookmarks
import com.cherryzp.animalshelter.resources.action_filter
import com.cherryzp.animalshelter.resources.action_layout_grid
import com.cherryzp.animalshelter.resources.action_layout_large
import com.cherryzp.animalshelter.resources.action_more
import com.cherryzp.animalshelter.resources.action_refresh
import com.cherryzp.animalshelter.resources.action_settings
import com.cherryzp.animalshelter.resources.action_sort
import com.cherryzp.animalshelter.resources.app_name
import com.cherryzp.animalshelter.resources.ic_bookmark_border
import com.cherryzp.animalshelter.resources.ic_filter_list
import com.cherryzp.animalshelter.resources.ic_grid_view
import com.cherryzp.animalshelter.resources.ic_sort
import com.cherryzp.animalshelter.resources.ic_view_agenda
import com.cherryzp.animalshelter.resources.sort_closing_soon
import com.cherryzp.animalshelter.resources.sort_latest
import com.cherryzp.animalshelter.ui.common.TestTags
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** 바가 받은 폭이 이 이상이면 보기 전환을 바의 아이콘으로(브리핑 A6 — 360dp 폰에서 제목이 잘리지 않게). */
private val InlineLayoutToggleWidth = 480.dp

/**
 * 목록 상단 바(브리핑 A6): 조건 · 정렬(메뉴) · [넓으면 보기 전환] · 북마크 · 더 보기(보기 전환 · 새로고침 · 설정).
 * 새로고침은 모든 플랫폼의 더 보기에 둔다(웹 마우스·TalkBack 사용자의 당겨서 새로고침 대체).
 *
 * @param checkedSort 정렬 메뉴의 체크 — 전환 중이면 목표 정렬
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListTopBar(
    layout: ListLayout,
    checkedSort: SortOrder,
    onOpenFilter: () -> Unit,
    onSortSelected: (SortOrder) -> Unit,
    onToggleLayout: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    // 지금과 반대 모드의 아이콘·문구(03 2.1)
    val toggle = if (layout == ListLayout.GRID) {
        LayoutToggle(Res.drawable.ic_view_agenda, Res.string.action_layout_large)
    } else {
        LayoutToggle(Res.drawable.ic_grid_view, Res.string.action_layout_grid)
    }
    BoxWithConstraints(modifier = modifier) {
        val inlineToggle = maxWidth >= InlineLayoutToggleWidth
        TopAppBar(
            title = {
                Text(
                    stringResource(Res.string.app_name),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
            },
            scrollBehavior = scrollBehavior,
            actions = {
                IconButton(
                    onClick = onOpenFilter,
                    modifier = Modifier.testTag(TestTags.OPEN_FILTER)
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_filter_list),
                        contentDescription = stringResource(Res.string.action_filter)
                    )
                }
                SortAction(checkedSort = checkedSort, onSortSelected = onSortSelected)
                if (inlineToggle) {
                    IconButton(onClick = onToggleLayout) {
                        Icon(
                            painterResource(toggle.icon),
                            contentDescription = stringResource(toggle.label)
                        )
                    }
                }
                IconButton(
                    onClick = onOpenBookmarks,
                    modifier = Modifier.testTag(TestTags.OPEN_BOOKMARKS)
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_bookmark_border),
                        contentDescription = stringResource(Res.string.action_bookmarks)
                    )
                }
                MoreAction(
                    toggle = toggle.takeUnless { inlineToggle },
                    onToggleLayout = onToggleLayout,
                    onRefresh = onRefresh,
                    onOpenSettings = onOpenSettings
                )
            }
        )
    }
}

private data class LayoutToggle(val icon: DrawableResource, val label: StringResource)

@Composable
private fun SortAction(checkedSort: SortOrder, onSortSelected: (SortOrder) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painterResource(Res.drawable.ic_sort),
                contentDescription = stringResource(Res.string.action_sort)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                SortOrder.LATEST to Res.string.sort_latest,
                SortOrder.CLOSING_SOON to Res.string.sort_closing_soon
            )
                .forEach { (order, label) ->
                    DropdownMenuItem(
                        text = { Text(stringResource(label)) },
                        onClick = {
                            expanded = false
                            onSortSelected(order)
                        },
                        // 체크 아이콘은 장식이라 지금 정렬을 스크린 리더에 상태로 알린다(3d 점검 A1)
                        modifier = Modifier.semantics { selected = order == checkedSort },
                        trailingIcon = {
                            if (order == checkedSort) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    )
                }
        }
    }
}

@Composable
private fun MoreAction(
    toggle: LayoutToggle?,
    onToggleLayout: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(Res.string.action_more))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            toggle?.let {
                DropdownMenuItem(
                    text = { Text(stringResource(it.label)) },
                    leadingIcon = { Icon(painterResource(it.icon), contentDescription = null) },
                    onClick = {
                        expanded = false
                        onToggleLayout()
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.action_refresh)) },
                leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                onClick = {
                    expanded = false
                    onRefresh()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.action_settings)) },
                leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                onClick = {
                    expanded = false
                    onOpenSettings()
                }
            )
        }
    }
}
