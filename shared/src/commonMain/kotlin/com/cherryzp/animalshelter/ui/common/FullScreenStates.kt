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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.ic_pets
import org.jetbrains.compose.resources.painterResource

// 빈 상태·오류·첫 로딩(03 0.2, 06 4.4): 일러스트 없이 아이콘 하나 + 한 문장 + 버튼.

/** 화면 가운데 진행 표시(문구 없음). */
@Composable
fun CenteredLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
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
                textAlign = TextAlign.Center
            )
            primaryLabel?.let { Button(onClick = onPrimary) { Text(it) } }
            secondaryLabel?.let { OutlinedButton(onClick = onSecondary) { Text(it) } }
        }
    }
}
