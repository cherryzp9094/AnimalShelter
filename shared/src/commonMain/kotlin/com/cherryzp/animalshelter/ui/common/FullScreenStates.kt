package com.cherryzp.animalshelter.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.a11y_loading
import com.cherryzp.animalshelter.resources.ic_pets
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// 빈 상태·오류·첫 로딩(03 0.2, 06 4.4): 일러스트 없이 아이콘 하나 + 한 문장 + 버튼.

/**
 * 로딩 뒤 나타나는 안내 문장(5a Y1, WCAG 4.1.3 상태 메시지): polite 라이브 영역 — 화면 읽기가 나타나거나 바뀔 때
 * 저절로 읽는다(Android TalkBack — M3 스낵바와 같은 방식). 문장 `Text`에만 단다(버튼·아이콘·묶음에는 달지 않는다,
 * `Assertive`는 쓰지 않는다).
 */
fun Modifier.statusMessage(): Modifier = semantics { liveRegion = LiveRegionMode.Polite }

/**
 * 진행 표시의 이름(5a Y2): [name]은 `stringResource(Res.string.a11y_loading)`. 라이브 영역은 달지 않는다 — 열 때 바로
 * 보이고, 끝나면 [statusMessage] 문장이 알린다.
 */
fun Modifier.progressName(name: String): Modifier = semantics { contentDescription = name }

/** 화면 가운데 진행 표시(보이는 문구 없음, 이름 `a11y_loading`). */
@Composable
fun CenteredLoading(modifier: Modifier = Modifier) {
    val loading = stringResource(Res.string.a11y_loading)
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.progressName(loading))
    }
}

/**
 * 결과가 비었을 때의 한 화면. [primaryLabel]은 채운 버튼, [secondaryLabel]은 외곽선 버튼(없으면 숨김).
 * 큰 글자에서도 잘리지 않게 세로로 스크롤된다.
 */
@Composable
fun FullScreenMessage(
    text: String,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
    primaryLabel: String? = null,
    onPrimary: () -> Unit = {},
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {},
) {
    Box(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp).widthIn(max = 480.dp)
        ) {
            if (showIcon) {
                Icon(
                    painter = painterResource(Res.drawable.ic_pets),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.statusMessage()
            )
            primaryLabel?.let { Button(onClick = onPrimary) { Text(it) } }
            secondaryLabel?.let { OutlinedButton(onClick = onSecondary) { Text(it) } }
        }
    }
}
