package com.obsidianbackup.wear.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme

val Purple200 = Color(0xFFBB86FC)
val Purple500 = Color(0xFF6200EE)
val Purple700 = Color(0xFF3700B3)
val Teal200 = Color(0xFF03DAC5)
val Red400 = Color(0xFFCF6679)

internal val wearColorScheme: ColorScheme = ColorScheme(
    primary = Purple200,
    primaryDim = Purple700,
    primaryContainer = Purple700,
    onPrimary = Color.Black,
    onPrimaryContainer = Color.White,
    secondary = Teal200,
    secondaryDim = Teal200,
    secondaryContainer = Teal200,
    onSecondary = Color.Black,
    onSecondaryContainer = Color.White,
    tertiary = Purple500,
    tertiaryDim = Purple500,
    tertiaryContainer = Purple500,
    onTertiary = Color.Black,
    onTertiaryContainer = Color.White,
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerLow = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF2C2C2C),
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFCACACA),
    background = Color(0xFF000000),
    onBackground = Color.White,
    error = Red400,
    onError = Color.Black
)
