package com.cherryzp.animalshelter.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.detail_placeholder
import com.cherryzp.animalshelter.resources.ic_pets
import com.cherryzp.animalshelter.ui.theme.AnimalShelterTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** 두 칸에서 상세가 비었을 때 상세 자리(브리핑 B14): `ic_pets` + "목록에서 동물을 골라 주세요." */
@Composable
fun DetailPlaceholder(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(24.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_pets),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = stringResource(Res.string.detail_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview
@Composable
private fun DetailPlaceholderPreview() {
    AnimalShelterTheme { DetailPlaceholder() }
}
