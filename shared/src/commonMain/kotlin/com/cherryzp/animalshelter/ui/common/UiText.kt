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

/**
 * 스낵바 한 번(브리핑 P4). 보여 준 뒤 화면이 ViewModel의 `onMessageShown(id)`를 부른다.
 * @property id 안내마다 다른 번호([MessageIds]) — 같은 문구가 연달아 와도 값이 달라 화면 효과가 다시 시작된다
 *   (중간 검토 F3). 한 번만 나오는 고정 안내(북마크 복구)는 0이어도 된다
 */
@Immutable
data class UiMessage(val text: UiText, val action: MessageAction? = null, val id: Long = 0)

/** ViewModel마다 하나: 낼 때마다 1씩 늘린 [UiMessage.id]를 붙인다(1부터). */
class MessageIds {
    private var last = 0L

    fun create(text: UiText, action: MessageAction? = null): UiMessage =
        UiMessage(text, action, ++last)
}

/** 보여 준 안내([id])가 지금 안내와 같을 때만 지운다 — 뒤에 온 안내를 지우지 않게. */
fun UiMessage?.clearIfShown(id: Long): UiMessage? = if (this?.id == id) null else this

/** 스낵바 동작 버튼. */
enum class MessageAction { RETRY, UNDO, VIEW_BOOKMARKS }
