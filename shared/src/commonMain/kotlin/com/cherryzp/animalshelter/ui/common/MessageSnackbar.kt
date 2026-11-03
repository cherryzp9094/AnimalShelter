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
 * (그때는 [onShown]을 부르지 않는다 — ViewModel의 `onMessageAction`이 지운다). 버튼이 있으면 길게 보인다.
 */
@Composable
fun MessageSnackbarEffect(
    message: UiMessage?,
    snackbarHostState: SnackbarHostState,
    onShown: () -> Unit,
    onAction: (MessageAction) -> Unit,
) {
    if (message == null) return
    val text = message.text.resolve()
    val actionLabel = message.action?.let { stringResource(it.labelRes()) }
    val currentShown by rememberUpdatedState(onShown)
    val currentAction by rememberUpdatedState(onAction)
    LaunchedEffect(message) {
        val result = snackbarHostState.showSnackbar(
            message = text,
            actionLabel = actionLabel,
            duration = if (actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
        )
        val action = message.action
        if (result == SnackbarResult.ActionPerformed && action != null) {
            currentAction(action)
        } else {
            currentShown()
        }
    }
}

/** 스낵바 버튼 문구. */
fun MessageAction.labelRes(): StringResource = when (this) {
    MessageAction.RETRY -> Res.string.action_retry
    MessageAction.UNDO -> Res.string.action_undo
    MessageAction.VIEW_BOOKMARKS -> Res.string.action_view_bookmarks
}
