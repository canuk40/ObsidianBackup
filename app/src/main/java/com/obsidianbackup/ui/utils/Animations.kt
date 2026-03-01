package com.obsidianbackup.ui.utils

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.unit.IntOffset

/**
 * Centralized animation specifications for consistent motion design
 */
object Animations {
    // Standard durations (milliseconds)
    const val DURATION_SHORT = 150
    const val DURATION_MEDIUM = 300
    const val DURATION_LONG = 500
    
    // Easing curves
    private val standardEasing = FastOutSlowInEasing
    private val emphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    
    // ====================
    // FAB Animations
    // ====================
    
    val fabEnterAnimation = scaleIn(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    ) + fadeIn(
        animationSpec = tween(DURATION_MEDIUM)
    )
    
    val fabExitAnimation = scaleOut(
        animationSpec = tween(DURATION_SHORT)
    ) + fadeOut(
        animationSpec = tween(DURATION_SHORT)
    )
    
    // ====================
    // List Item Animations
    // ====================
    
    val listItemEnterAnimation = expandVertically(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    ) + fadeIn(
        animationSpec = tween(DURATION_MEDIUM)
    )
    
    val listItemExitAnimation = shrinkVertically(
        animationSpec = tween(DURATION_MEDIUM)
    ) + fadeOut(
        animationSpec = tween(DURATION_SHORT)
    )
    
    // ====================
    // Screen Transitions
    // ====================
    
    /**
     * Slide in from right (forward navigation)
     */
    fun slideInFromRight(): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(DURATION_MEDIUM, easing = standardEasing)
        ) + fadeIn(
            animationSpec = tween(DURATION_MEDIUM)
        )
    }
    
    /**
     * Slide out to left (forward navigation)
     */
    fun slideOutToLeft(): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { fullWidth -> -fullWidth / 3 },
            animationSpec = tween(DURATION_MEDIUM, easing = standardEasing)
        ) + fadeOut(
            animationSpec = tween(DURATION_MEDIUM)
        )
    }
    
    /**
     * Slide in from left (back navigation)
     */
    fun slideInFromLeft(): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { fullWidth -> -fullWidth / 3 },
            animationSpec = tween(DURATION_MEDIUM, easing = standardEasing)
        ) + fadeIn(
            animationSpec = tween(DURATION_MEDIUM)
        )
    }
    
    /**
     * Slide out to right (back navigation)
     */
    fun slideOutToRight(): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(DURATION_MEDIUM, easing = standardEasing)
        ) + fadeOut(
            animationSpec = tween(DURATION_MEDIUM)
        )
    }
    
    // ====================
    // State Change Animations
    // ====================
    
    /**
     * Crossfade for loading/content transitions
     */
    val crossfadeSpec = tween<Float>(
        durationMillis = DURATION_MEDIUM,
        easing = standardEasing
    )
    
    /**
     * Simple fade in/out
     */
    val fadeInAnimation = fadeIn(
        animationSpec = tween(DURATION_MEDIUM)
    )
    
    val fadeOutAnimation = fadeOut(
        animationSpec = tween(DURATION_SHORT)
    )
    
    // ====================
    // Onboarding Step Animations
    // ====================
    
    /**
     * Forward step transition (swipe left)
     */
    fun stepForward(): ContentTransform {
        return slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(DURATION_MEDIUM, easing = emphasizedEasing)
        ) + fadeIn(
            animationSpec = tween(DURATION_MEDIUM)
        ) togetherWith slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(DURATION_MEDIUM, easing = emphasizedEasing)
        ) + fadeOut(
            animationSpec = tween(DURATION_MEDIUM)
        )
    }
    
    /**
     * Backward step transition (swipe right)
     */
    fun stepBackward(): ContentTransform {
        return slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(DURATION_MEDIUM, easing = emphasizedEasing)
        ) + fadeIn(
            animationSpec = tween(DURATION_MEDIUM)
        ) togetherWith slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(DURATION_MEDIUM, easing = emphasizedEasing)
        ) + fadeOut(
            animationSpec = tween(DURATION_MEDIUM)
        )
    }
    
    // ====================
    // Card Animations
    // ====================
    
    /**
     * Card expansion animation
     */
    val cardExpandAnimation = expandVertically(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    )
    
    /**
     * Card collapse animation
     */
    val cardCollapseAnimation = shrinkVertically(
        animationSpec = tween(DURATION_MEDIUM, easing = standardEasing)
    )
}
