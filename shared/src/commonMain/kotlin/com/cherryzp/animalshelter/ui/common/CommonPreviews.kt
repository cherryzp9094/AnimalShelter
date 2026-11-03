package com.cherryzp.animalshelter.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cherryzp.animalshelter.model.ListLayout
import com.cherryzp.animalshelter.ui.preview.PreviewData
import com.cherryzp.animalshelter.ui.theme.AnimalShelterTheme

@Preview
@Composable
private fun StatusBadgesPreview() {
    AnimalShelterTheme {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PreviewData.cards.forEach { StatusBadge(it.status) }
            }
        }
    }
}

@Preview
@Composable
private fun StatusBadgesDarkPreview() {
    AnimalShelterTheme(darkTheme = true) {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PreviewData.cards.forEach { StatusBadge(it.status) }
            }
        }
    }
}

@Preview
@Composable
private fun AnimalCardGridPreview() {
    AnimalShelterTheme {
        AnimalCard(
            ui = PreviewData.cards[1],
            layout = ListLayout.GRID,
            onClick = {},
            onBookmarkClick = {},
            modifier = Modifier.width(172.dp)
        )
    }
}

@Preview
@Composable
private fun AnimalCardLargePreview() {
    AnimalShelterTheme {
        AnimalCard(
            ui = PreviewData.cards[0],
            layout = ListLayout.LARGE,
            onClick = {},
            onBookmarkClick = {},
            modifier = Modifier.width(360.dp)
        )
    }
}
