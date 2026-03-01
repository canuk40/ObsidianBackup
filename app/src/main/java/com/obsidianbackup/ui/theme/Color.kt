package com.obsidianbackup.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Obsidian Neon-Shield Theme Colors
 * A dark cybersecurity aesthetic with glowing cyan-to-purple neon accents
 * Matches the app icon: neon shield with cyan lock on dark background
 */
object ObsidianColors {
    // =========================================================================
    // Core Surfaces - Dark Obsidian Aesthetic
    // =========================================================================
    val Background = Color(0xFF08080C)           // Deep obsidian black
    val BackgroundElevated = Color(0xFF0C0C12)   // Slightly elevated
    val Surface = Color(0xFF101018)              // Card/container surface
    val SurfaceVariant = Color(0xFF181822)       // Variant surface
    val SurfaceElevated = Color(0xFF1C1C28)      // Elevated containers
    val SurfaceContainer = Color(0xFF141420)     // Inner containers
    val SurfaceForged = Color(0xFF1A1A26)        // Deep metal look

    // =========================================================================
    // Neon Accents - Cyan to Purple Shield Glow
    // =========================================================================
    val MoltenOrange = Color(0xFF00E5FF)         // Primary neon cyan (renamed for compat)
    val MoltenAmber = Color(0xFFBB86FC)          // Neon purple secondary
    val MoltenGold = Color(0xFF64FFDA)           // Mint/teal highlight
    val MoltenRed = Color(0xFF7C4DFF)            // Deep purple accent
    val EmberGlow = Color(0xFF40C4FF)            // Bright cyan glow
    val EmberCore = Color(0xFF9C27B0)            // Purple core

    // Subtle glow variants (for backgrounds/overlays)
    val MoltenGlowSubtle = Color(0x1A00E5FF)     // 10% cyan
    val MoltenGlowMedium = Color(0x4D00E5FF)     // 30% cyan
    val MoltenGlowStrong = Color(0x8000E5FF)     // 50% cyan
    val MoltenGlowIntense = Color(0xB300E5FF)    // 70% cyan

    // =========================================================================
    // Dark Metal Greys (cooler blue-tinted)
    // =========================================================================
    val MetalDark = Color(0xFF1A1A24)            // Dark cool steel
    val MetalMedium = Color(0xFF282838)          // Medium steel
    val MetalLight = Color(0xFF383848)           // Light steel highlight
    val MetalShine = Color(0xFF484858)           // Metallic shine
    val MetalEdge = Color(0xFF222232)            // Edge highlight

    // =========================================================================
    // Secondary Accents
    // =========================================================================
    val AccentBlue = Color(0xFF448AFF)           // Electric blue
    val AccentCyan = Color(0xFF00E5FF)           // Neon cyan
    val AccentPurple = Color(0xFFBB86FC)         // Neon purple

    // =========================================================================
    // Text Colors
    // =========================================================================
    val TextPrimary = Color(0xFFE8E8F0)          // Primary text (cool-tinted)
    val TextSecondary = Color(0xFFB0B0C0)        // Secondary text
    val TextTertiary = Color(0xFF707088)         // Tertiary/hint text
    val TextOnMolten = Color(0xFF08080C)         // Text on neon backgrounds
    val TextEmber = Color(0xFF80DEEA)            // Cyan-tinted text

    // =========================================================================
    // State Colors
    // =========================================================================
    val Success = Color(0xFF4CAF50)
    val SuccessGlow = Color(0x334CAF50)
    val Warning = Color(0xFFFFC107)
    val WarningAmber = Color(0xFFFFB300)
    val WarningGlow = Color(0x33FFC107)
    val Error = Color(0xFFEF5350)
    val ErrorRed = Color(0xFFFF5252)
    val ErrorGlow = Color(0x33EF5350)
    val Info = Color(0xFF40C4FF)
    val InfoBlue = Info
    val InfoGlow = Color(0x3340C4FF)

    // =========================================================================
    // Borders and Dividers
    // =========================================================================
    val Border = Color(0xFF282838)
    val BorderSubtle = Color(0xFF1E1E2E)
    val BorderEmber = Color(0x6600E5FF)          // Cyan-tinted border
    val Divider = Color(0xFF222230)
    val DividerEmber = Color(0x3300E5FF)         // Cyan-tinted divider

    // =========================================================================
    // Terminal Specific
    // =========================================================================
    val TerminalBackground = Color(0xFF000000)   // Pure black
    val TerminalSurface = Color(0xFF050510)      // Very dark surface
    val TerminalCursor = Color(0xFF00E5FF)       // Neon cyan cursor
    val TerminalCursorGlow = Color(0x6600E5FF)   // Cursor glow effect
    val TerminalSelection = Color(0x4400E5FF)    // Selection highlight
    val TerminalPrompt = Color(0xFF64FFDA)       // Mint prompt color

    // =========================================================================
    // Accent Colors
    // =========================================================================
    val AccentGold = Color(0xFFFFD700)
    val AccentGoldDark = Color(0xFFB8860B)
    val AccentGoldGlow = Color(0x33FFD700)
    val AccentGradientStart = Color(0xFF00E5FF)
    val AccentGradientMid = Color(0xFFBB86FC)
    val AccentGradientEnd = Color(0xFFFFD700)

    // =========================================================================
    // Root Status Colors
    // =========================================================================
    val RootGranted = Color(0xFF64FFDA)          // Mint green for root
    val RootGrantedGlow = Color(0x3364FFDA)
    val RootGrantedBackground = Color(0x1A64FFDA)

    val RootDenied = Color(0xFFFFCA28)
    val RootDeniedGlow = Color(0x33FFCA28)
    val RootDeniedBackground = Color(0x1AFFCA28)

    val RootUnavailable = Color(0xFFEF5350)
    val RootUnavailableGlow = Color(0x33EF5350)
    val RootUnavailableBackground = Color(0x1AEF5350)

    val RootRequiredBadge = Color(0xFFEF5350)
    val RootRequiredBadgeBackground = Color(0x33EF5350)

    val RootRecommendedBadge = Color(0xFFFFCA28)
    val RootRecommendedBadgeBackground = Color(0x33FFCA28)

    // =========================================================================
    // Interactive States
    // =========================================================================
    val Pressed = Color(0xFF202030)
    val PressedEmber = Color(0x3300E5FF)
    val Hovered = Color(0xFF1C1C2C)
    val HoveredEmber = Color(0x1A00E5FF)
    val Focused = Color(0x3300E5FF)
    val FocusRing = Color(0xFF00E5FF)
    val Disabled = Color(0xFF3A3A48)
    val DisabledContent = Color(0xFF585868)

    // =========================================================================
    // Gradient Definitions
    // =========================================================================
    val MoltenGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF00E5FF), Color(0xFFBB86FC), Color(0xFF00E5FF))
    )

    val EmberGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF9C27B0), Color(0xFF00E5FF), Color(0xFFBB86FC))
    )

    val AccentGradient = Brush.horizontalGradient(
        colors = listOf(AccentGradientStart, AccentGradientMid, AccentGradientEnd)
    )

    val ForgedMetalGradient = Brush.verticalGradient(
        colors = listOf(MetalLight, MetalDark, MetalMedium)
    )

    val SurfaceGradient = Brush.verticalGradient(
        colors = listOf(SurfaceElevated, Surface, Background)
    )

    // New: Shield gradient matching the app icon
    val ShieldGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFBB86FC), Color(0xFF7C4DFF), Color(0xFF00E5FF))
    )

    val NeonGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF00E5FF), Color(0xFFBB86FC))
    )
}

// Legacy color aliases for backward compatibility
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

