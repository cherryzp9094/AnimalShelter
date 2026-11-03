package com.cherryzp.animalshelter.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * 화면 문구(브리핑 P4). ViewModel은 문자열 리소스를 읽지 않고 이 값을 상태에 싣는다 — Composable이 [resolve]로 푼다.
 * 인자에 [UiText]를 넣으면 먼저 풀어서 넘긴다.
 */
@Immutable
sealed interface UiText {
    data class Res(val id: StringResource, val args: List<Any> = emptyList()) : UiText

    data class Raw(val value: String) : UiText
}

/** 리소스 문구 줄임. */
fun uiText(id: StringResource, vararg args: Any): UiText = UiText.Res(id, args.toList())

@Composable
fun UiText.resolve(): String = when (this) {
    is UiText.Raw -> value

    is UiText.Res -> {
        val resolved = args.map { if (it is UiText) it.resolve() else it }
        stringResource(id, *resolved.toTypedArray())
    }
}

/** 스낵바 한 번(브리핑 P4). 보여 준 뒤 화면이 ViewModel의 `onMessageShown()`을 부른다. */
@Immutable
data class UiMessage(val text: UiText, val action: MessageAction? = null)

/** 스낵바 동작 버튼. */
enum class MessageAction { RETRY, UNDO, VIEW_BOOKMARKS }
