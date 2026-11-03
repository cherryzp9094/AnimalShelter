package com.cherryzp.animalshelter.ui.placeholder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.action_back
import com.cherryzp.animalshelter.resources.detail_placeholder
import com.cherryzp.animalshelter.resources.title_settings
import com.cherryzp.animalshelter.ui.theme.AnimalShelterTheme
import org.jetbrains.compose.resources.stringResource

// Phase 1 자리표시 화면. Phase 3에서 실제 화면(ui/list, ui/settings …)으로 바꾼다.

/** 두 칸에서 상세가 비었을 때 상세 자리. */
@Composable
fun DetailPlaceholderPane(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(Res.string.detail_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** 설정 자리: 제목 + 뒤로. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPlaceholderScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.title_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding))
    }
}

/** 아직 만들지 않은 화면. 키 이름만 보여 준다. */
@Composable
fun PlaceholderScreen(keyName: String, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(text = keyName, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Preview
@Composable
private fun SettingsPlaceholderScreenPreview() {
    AnimalShelterTheme { SettingsPlaceholderScreen(onBack = {}) }
}
