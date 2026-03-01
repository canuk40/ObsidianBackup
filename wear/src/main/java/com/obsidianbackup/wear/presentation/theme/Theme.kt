package com.obsidianbackup.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme

@Composable
fun WearAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = wearColorScheme,
        typography = Typography,
        content = content
    )
}
