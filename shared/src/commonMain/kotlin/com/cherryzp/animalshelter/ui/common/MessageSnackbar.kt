package com.cherryzp.animalshelter.ui.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.action_retry
import com.cherryzp.animalshelter.resources.action_undo
import com.cherryzp.animalshelter.resources.action_view_bookmarks
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * 상태의 [UiMessage]를 스낵바로 한 번 보인다(브리핑 P3·P4). 보여 준 뒤 [onShown], 버튼을 누르면 [onAction]
 * (그때는 [onShown]을 부르지 않는다 — ViewModel의 `onMessageAction`이 지운다). 둘 다 그 안내의 `id`를 넘긴다.
 * 버튼이 있으면 길게 보인다.
 *
 * 효과 키는 안내 + 푼 문구(중간 검토 F3): 웹 `stringResource`는 처음 쓰는 문구에 ""를 먼저 돌려주고 비동기로
 * 채운다(CMP `ResourceState.web.kt`). 문구가 비었거나 버튼 문구가 비었으면 띄우지 않고 채워질 때를 기다린다.
 */
@Composable
fun MessageSnackbarEffect(
    message: UiMessage?,
    snackbarHostState: SnackbarHostState,
    onShown: (id: Long) -> Unit,
    onAction: (action: MessageAction, id: Long) -> Unit,
) {
    if (message == null) return
    val text = message.text.resolve()
    val actionLabel = message.action?.let { stringResource(it.labelRes()) }
    val currentShown by rememberUpdatedState(onShown)
    val currentAction by rememberUpdatedState(onAction)
    LaunchedEffect(message, text, actionLabel) {
        if (text.isEmpty() || (message.action != null && actionLabel.isNullOrEmpty())) {
            return@LaunchedEffect
        }
        val result = snackbarHostState.showSnackbar(
            message = text,
            actionLabel = actionLabel,
            duration = if (actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
        )
        val action = message.action
        if (result == SnackbarResult.ActionPerformed && action != null) {
            currentAction(action, message.id)
        } else {
            currentShown(message.id)
        }
    }
}

/** 스낵바 버튼 문구. */
fun MessageAction.labelRes(): StringResource = when (this) {
    MessageAction.RETRY -> Res.string.action_retry
    MessageAction.UNDO -> Res.string.action_undo
    MessageAction.VIEW_BOOKMARKS -> Res.string.action_view_bookmarks
}
