package com.example.orrery.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ColorScheme

private val OrreryColorScheme = ColorScheme()

@Composable
fun OrreryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OrreryColorScheme,
        content = content
    )
}
