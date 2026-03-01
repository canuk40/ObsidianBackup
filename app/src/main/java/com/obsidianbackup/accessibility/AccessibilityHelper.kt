package com.obsidianbackup.accessibility

import androidx.compose.ui.graphics.Color
import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * Helper class for accessibility features and WCAG 2.2 compliance.
 * Provides utilities for screen readers, TalkBack, and accessibility services.
 */
object AccessibilityHelper {
    
    /**
     * Minimum touch target size as per WCAG 2.2 (Level AAA: 44x44 dp, Level AA: 24x24 dp)
     * We use 48x48 dp to exceed requirements.
     */
    const val MIN_TOUCH_TARGET_SIZE_DP = 48
    
    /**
     * Minimum color contrast ratio for normal text (WCAG AA)
     */
    const val MIN_CONTRAST_RATIO_NORMAL = 4.5
    
    /**
     * Minimum color contrast ratio for large text (WCAG AA)
     */
    const val MIN_CONTRAST_RATIO_LARGE = 3.0
    
    /**
     * Check if TalkBack or other accessibility services are enabled
     */
    fun isAccessibilityEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        return am?.isEnabled == true && am.isTouchExplorationEnabled
    }
    
    /**
     * Check if screen reader is active
     */
    fun isScreenReaderActive(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        return am?.isTouchExplorationEnabled == true
    }
    
    /**
     * Announce a message to screen readers
     */
    fun announceForAccessibility(context: Context, message: String) {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        if (am?.isEnabled == true) {
            // The announcement will be queued and spoken by TalkBack
            val event = android.view.accessibility.AccessibilityEvent.obtain().apply {
                eventType = android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT
                text.add(message)
            }
            am.sendAccessibilityEvent(event)
        }
    }
    
    /**
     * Calculate relative luminance for WCAG contrast calculations
     */
    fun calculateLuminance(color: Int): Double {
        val r = android.graphics.Color.red(color) / 255.0
        val g = android.graphics.Color.green(color) / 255.0
        val b = android.graphics.Color.blue(color) / 255.0
        
        val rLum = if (r <= 0.03928) r / 12.92 else Math.pow((r + 0.055) / 1.055, 2.4)
        val gLum = if (g <= 0.03928) g / 12.92 else Math.pow((g + 0.055) / 1.055, 2.4)
        val bLum = if (b <= 0.03928) b / 12.92 else Math.pow((b + 0.055) / 1.055, 2.4)
        
        return 0.2126 * rLum + 0.7152 * gLum + 0.0722 * bLum
    }
    
    /**
     * Calculate contrast ratio between two colors
     * Returns a ratio where 21:1 is maximum contrast (black/white)
     */
    fun calculateContrastRatio(foreground: Int, background: Int): Double {
        val lum1 = calculateLuminance(foreground)
        val lum2 = calculateLuminance(background)
        val lighter = maxOf(lum1, lum2)
        val darker = minOf(lum1, lum2)
        return (lighter + 0.05) / (darker + 0.05)
    }
    
    /**
     * Check if contrast ratio meets WCAG AA requirements
     */
    fun meetsWCAGAA(foreground: Int, background: Int, isLargeText: Boolean = false): Boolean {
        val ratio = calculateContrastRatio(foreground, background)
        val required = if (isLargeText) MIN_CONTRAST_RATIO_LARGE else MIN_CONTRAST_RATIO_NORMAL
        return ratio >= required
    }
    
    /**
     * Check if contrast ratio meets WCAG AAA requirements
     */
    fun meetsWCAGAAA(foreground: Int, background: Int, isLargeText: Boolean = false): Boolean {
        val ratio = calculateContrastRatio(foreground, background)
        val required = if (isLargeText) 4.5 else 7.0
        return ratio >= required
    }
}

/**
 * Composable to remember accessibility state
 */
@Composable
fun rememberAccessibilityState(): AccessibilityState {
    val context = LocalContext.current
    
    return remember(context) {
        AccessibilityState(
            isEnabled = AccessibilityHelper.isAccessibilityEnabled(context),
            isScreenReaderActive = AccessibilityHelper.isScreenReaderActive(context)
        )
    }
}

data class AccessibilityState(
    val isEnabled: Boolean,
    val isScreenReaderActive: Boolean
)
