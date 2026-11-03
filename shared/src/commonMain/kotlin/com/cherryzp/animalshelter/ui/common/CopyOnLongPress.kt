package com.cherryzp.animalshelter.ui.common

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.action_copy_address
import org.jetbrains.compose.resources.stringResource

/**
 * 길게 눌러 주소 복사(상세·보호소, 03 2.3·2.6). 줄 전체 폭, 터치 48dp 이상(C2), 스크린 리더에는 길게 누르기 동작 이름
 * `action_copy_address`를 알린다. 짧게 누르기는 아무것도 하지 않는다.
 */
@Composable
fun Modifier.copyOnLongPress(onCopy: () -> Unit): Modifier {
    val label = stringResource(Res.string.action_copy_address)
    return fillMaxWidth()
        .heightIn(min = 48.dp)
        .combinedClickable(onLongClickLabel = label, onLongClick = onCopy, onClick = {})
}
