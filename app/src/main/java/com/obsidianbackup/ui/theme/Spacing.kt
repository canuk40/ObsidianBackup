package com.obsidianbackup.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standardized spacing tokens for consistent layout
 * Based on 4dp grid system
 * 
 * Usage:
 * ```
 * import com.obsidianbackup.ui.theme.Spacing
 * 
 * modifier = Modifier.padding(Spacing.md)
 * verticalArrangement = Arrangement.spacedBy(Spacing.xs)
 * ```
 */
object Spacing {
    // Micro spacing
    val xxxs: Dp = 2.dp    // Tight internal gaps (dividers, borders)
    val xxs: Dp = 4.dp     // Minimal element spacing
    
    // Small spacing
    val xs: Dp = 8.dp      // Small gaps between related items
    val sm: Dp = 12.dp     // Medium gaps (less common)
    
    // Standard spacing
    val md: Dp = 16.dp     // Default padding & margins (PRIMARY - most common)
    val lg: Dp = 24.dp     // Section/card spacing
    
    // Large spacing
    val xl: Dp = 32.dp     // Page margins, large gutters
    val xxl: Dp = 48.dp    // Hero spacing, screen padding
    val xxxl: Dp = 64.dp   // Extra large breathing room
}

/**
 * Standard elevation values for cards and surfaces
 */
object Elevation {
    val none: Dp = 0.dp       // Flat surfaces
    val subtle: Dp = 1.dp     // Barely raised (dividers)
    val low: Dp = 2.dp        // Subtle depth (inactive cards)
    val medium: Dp = 4.dp     // Standard cards (DEFAULT)
    val high: Dp = 8.dp       // Prominent elements (active cards, FAB)
    val highest: Dp = 12.dp   // Modals, dialogs
}

/**
 * Standard icon sizes
 */
object IconSize {
    val small: Dp = 16.dp     // Inline icons
    val medium: Dp = 24.dp    // Standard icons (DEFAULT)
    val large: Dp = 48.dp     // List leading icons
    val xlarge: Dp = 64.dp    // Feature icons
    val hero: Dp = 120.dp     // Empty state icons
}

/**
 * Corner radius beyond Material shapes
 */
object CornerRadius {
    val small: Dp = 8.dp
    val medium: Dp = 12.dp
    val large: Dp = 16.dp
    val xlarge: Dp = 24.dp
}
