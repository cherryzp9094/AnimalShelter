package com.cherryzp.animalshelter.ui.common

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.action_bookmark_add
import com.cherryzp.animalshelter.resources.action_bookmark_remove
import com.cherryzp.animalshelter.resources.ic_bookmark
import com.cherryzp.animalshelter.resources.ic_bookmark_border
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** 상단 바의 북마크 토글(상세). 켜짐 `ic_bookmark` + `primary`, 꺼짐 `ic_bookmark_border`. */
@Composable
fun BookmarkToggleButton(bookmarked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
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
            tint = if (bookmarked) MaterialTheme.colorScheme.primary else LocalContentColor.current
        )
    }
}
