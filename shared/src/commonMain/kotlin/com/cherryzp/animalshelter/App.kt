package com.cherryzp.animalshelter

import androidx.compose.runtime.Composable
import com.cherryzp.animalshelter.navigation.AppNavDisplay
import com.cherryzp.animalshelter.ui.theme.AnimalShelterTheme

/** 앱 진입 Composable. Android `MainActivity`와 웹 `main()`이 같은 것을 띄운다. */
@Composable
fun App() {
    AnimalShelterTheme {
        AppNavDisplay()
    }
}
