package com.cherryzp.animalshelter.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 하단 고정 바(상세 전화·필터 초기화/검색하기)의 버튼 최소 높이 — 06 6절 48dp(M3 기본 40dp, 3d 점검 A4). */
val BottomBarButtonHeight = 48.dp

/**
 * 하단 고정 바의 inset(Phase 3 검토 R6): `safeDrawing`에서 IME를 뺀 가로·아래. 시스템 바·카메라 컷아웃은 피하고
 * 키보드 위로는 떠오르지 않는다(키보드가 열리면 바는 그 뒤에 있고 본문이 `imePadding`으로 스크롤된다).
 * 하단 바는 모두 [BottomBar]로 이 inset을 쓴다.
 */
val WindowInsets.Companion.bottomBar: WindowInsets
    @Composable get() = WindowInsets.safeDrawing
        .exclude(WindowInsets.ime)
        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)

/**
 * 하단 고정 바(상세 전화 바·필터 하단 바, 4a 공유 버튼도 여기): `surfaceContainer`, inset [WindowInsets.Companion.bottomBar],
 * 안쪽 여백 가로 16dp·세로 12dp, 버튼 사이 8dp. 버튼은 `Modifier.weight(1f).heightIn(min = BottomBarButtonHeight)`.
 */
@Composable
fun BottomBar(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.bottomBar)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            content = content
        )
    }
}
