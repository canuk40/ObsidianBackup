package com.obsidianbackup.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.unit.dp

// -------------------------------
// Obsidian Neon-Shield Color Scheme
// Dark cybersecurity surfaces with glowing cyan-purple neon accents
// Matches the app icon: neon shield + lock on dark background
// -------------------------------

private val ObsidianColorScheme = darkColorScheme(
    primary = ObsidianColors.MoltenOrange,         // Neon cyan
    onPrimary = ObsidianColors.TextOnMolten,
    primaryContainer = Color(0xFF0D2933),           // Deep cyan container
    onPrimaryContainer = ObsidianColors.MoltenOrange,

    secondary = ObsidianColors.MoltenAmber,         // Neon purple
    onSecondary = ObsidianColors.TextOnMolten,
    secondaryContainer = Color(0xFF1A1030),         // Deep purple container
    onSecondaryContainer = ObsidianColors.MoltenAmber,

    tertiary = ObsidianColors.MoltenGold,           // Mint/teal
    onTertiary = ObsidianColors.TextOnMolten,
    tertiaryContainer = ObsidianColors.SurfaceVariant,
    onTertiaryContainer = ObsidianColors.MoltenGold,

    error = ObsidianColors.Error,
    onError = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = ObsidianColors.Background,
    onBackground = ObsidianColors.TextPrimary,

    surface = ObsidianColors.Surface,
    onSurface = ObsidianColors.TextPrimary,
    surfaceVariant = ObsidianColors.SurfaceVariant,
    onSurfaceVariant = ObsidianColors.TextSecondary,

    outline = ObsidianColors.Border,
    outlineVariant = ObsidianColors.BorderSubtle,

    inverseSurface = ObsidianColors.TextPrimary,
    inverseOnSurface = ObsidianColors.Background,
    inversePrimary = ObsidianColors.MoltenOrange,

    surfaceTint = ObsidianColors.MoltenGlowSubtle,
    scrim = Color.Black.copy(alpha = 0.7f)
)

// Light scheme (cyan-purple variant)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0097A7),        // Deep Cyan
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2EBF2),
    onPrimaryContainer = Color.Black,

    secondary = Color(0xFF7C4DFF),       // Deep Purple
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1C4E9),
    onSecondaryContainer = Color.Black,

    tertiary = Color(0xFF00897B),        // Teal
    onTertiary = Color.White,

    error = Color(0xFFB00020),
    onError = Color.White,

    background = Color(0xFFF8F9FC),
    onBackground = Color(0xFF1C1B1F),

    surface = Color(0xFFF8F9FC),
    onSurface = Color(0xFF1C1B1F)
)

// -------------------------------
// High Contrast Theme - WCAG AAA Compliant (7:1 ratio minimum)
// -------------------------------

private val HighContrastDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00FFFF),         // Bright cyan on black
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF00E5FF),
    onPrimaryContainer = Color(0xFF000000),

    secondary = Color(0xFFCE93D8),        // Light purple
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFFBB86FC),
    onSecondaryContainer = Color(0xFF000000),

    tertiary = Color(0xFF80CBC4),         // Light teal
    onTertiary = Color(0xFF000000),

    error = Color(0xFFFF6666),
    onError = Color(0xFF000000),

    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),

    surface = Color(0xFF000000),
    onSurface = Color(0xFFFFFFFF),
    
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFFFFFFFF)
)

private val HighContrastLightColorScheme = lightColorScheme(
    primary = Color(0xFF006064),          // Dark cyan
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF0097A7),
    onPrimaryContainer = Color(0xFFFFFFFF),

    secondary = Color(0xFF4A148C),        // Dark purple
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF7C4DFF),
    onSecondaryContainer = Color(0xFFFFFFFF),

    tertiary = Color(0xFF004D40),         // Dark teal
    onTertiary = Color(0xFFFFFFFF),

    error = Color(0xFFCC0000),
    onError = Color(0xFFFFFFFF),

    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF000000)
)

// -------------------------------
// Material You 3.0 Shapes
// -------------------------------

val MaterialYouShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

// Material 3 Typography adapter using ObsidianTypography
private val ObsidianMaterialTypography = Typography(
    displayLarge = ObsidianTypography.displayLarge,
    displayMedium = ObsidianTypography.displayMedium,
    displaySmall = ObsidianTypography.displaySmall,
    headlineLarge = ObsidianTypography.headlineLarge,
    headlineMedium = ObsidianTypography.headlineMedium,
    headlineSmall = ObsidianTypography.headlineSmall,
    titleLarge = ObsidianTypography.titleLarge,
    titleMedium = ObsidianTypography.titleMedium,
    titleSmall = ObsidianTypography.titleSmall,
    bodyLarge = ObsidianTypography.bodyLarge,
    bodyMedium = ObsidianTypography.bodyMedium,
    bodySmall = ObsidianTypography.bodySmall,
    labelLarge = ObsidianTypography.labelLarge,
    labelMedium = ObsidianTypography.labelMedium,
    labelSmall = ObsidianTypography.labelSmall
)

// -------------------------------
// Extended Theme Colors for Custom Components
// -------------------------------

data class ExtendedColors(
    val moltenOrange: Color,
    val moltenAmber: Color,
    val moltenGold: Color,
    val moltenRed: Color,
    val emberGlow: Color,
    val moltenGlowSubtle: Color,
    val success: Color,
    val warning: Color,
    val info: Color,
    val accentGold: Color,
    val terminalBackground: Color,
    val terminalCursor: Color,
    val rootGranted: Color,
    val rootDenied: Color,
    val rootUnavailable: Color,
    val accentBlue: Color,
    val accentCyan: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        moltenOrange = ObsidianColors.MoltenOrange,
        moltenAmber = ObsidianColors.MoltenAmber,
        moltenGold = ObsidianColors.MoltenGold,
        moltenRed = ObsidianColors.MoltenRed,
        emberGlow = ObsidianColors.EmberGlow,
        moltenGlowSubtle = ObsidianColors.MoltenGlowSubtle,
        success = ObsidianColors.Success,
        warning = ObsidianColors.Warning,
        info = ObsidianColors.Info,
        accentGold = ObsidianColors.AccentGold,
        terminalBackground = ObsidianColors.TerminalBackground,
        terminalCursor = ObsidianColors.TerminalCursor,
        rootGranted = ObsidianColors.RootGranted,
        rootDenied = ObsidianColors.RootDenied,
        rootUnavailable = ObsidianColors.RootUnavailable,
        accentBlue = ObsidianColors.AccentBlue,
        accentCyan = ObsidianColors.AccentCyan
    )
}

// -------------------------------
// ObsidianBackup Theme Wrapper
// -------------------------------

@Composable
fun ObsidianBackupTheme(
    darkTheme: Boolean = true,  // Default to dark (Obsidian theme)
    dynamicColor: Boolean = false,  // Default to Obsidian colors, not dynamic
    highContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        highContrast && darkTheme -> HighContrastDarkColorScheme
        highContrast && !darkTheme -> HighContrastLightColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> ObsidianColorScheme  // Use Obsidian theme by default
        else -> LightColorScheme
    }

    val extendedColors = ExtendedColors(
        moltenOrange = ObsidianColors.MoltenOrange,
        moltenAmber = ObsidianColors.MoltenAmber,
        moltenGold = ObsidianColors.MoltenGold,
        moltenRed = ObsidianColors.MoltenRed,
        emberGlow = ObsidianColors.EmberGlow,
        moltenGlowSubtle = ObsidianColors.MoltenGlowSubtle,
        success = ObsidianColors.Success,
        warning = ObsidianColors.Warning,
        info = ObsidianColors.Info,
        accentGold = ObsidianColors.AccentGold,
        terminalBackground = ObsidianColors.TerminalBackground,
        terminalCursor = ObsidianColors.TerminalCursor,
        rootGranted = ObsidianColors.RootGranted,
        rootDenied = ObsidianColors.RootDenied,
        rootUnavailable = ObsidianColors.RootUnavailable,
        accentBlue = ObsidianColors.AccentBlue,
        accentCyan = ObsidianColors.AccentCyan
    )

    // Status bar theming
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ObsidianMaterialTypography,
            shapes = MaterialYouShapes,
            content = content
        )
    }
}

// Extension property for easy access to extended colors
object ObsidianTheme {
    val extendedColors: ExtendedColors
        @Composable
        get() = LocalExtendedColors.current
}
