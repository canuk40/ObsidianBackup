package com.obsidianbackup.ui.utils

import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized animation specifications following Material Design 3 motion principles
 */
object AnimationSpecs {
    
    // Duration constants (in milliseconds)
    const val FAST = 150
    const val NORMAL = 300
    const val SLOW = 500
    const val VERY_SLOW = 700
    
    // Standard easing curves
    val EaseInOut = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    val EaseOut = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val EaseIn = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    val StandardEasing = EaseInOut
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    
    // Spring specifications
    val FastSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    val MediumSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )
    
    val SlowSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessVeryLow
    )
    
    // Standard tween animations
    fun <T> fastTween() = tween<T>(
        durationMillis = FAST,
        easing = StandardEasing
    )
    
    fun <T> normalTween() = tween<T>(
        durationMillis = NORMAL,
        easing = StandardEasing
    )
    
    fun <T> slowTween() = tween<T>(
        durationMillis = SLOW,
        easing = EmphasizedEasing
    )
    
    // Emphasized animations for important transitions
    fun <T> emphasizedTween() = tween<T>(
        durationMillis = NORMAL,
        easing = EmphasizedEasing
    )
    
    // Fade animations
    val fadeInSpec = fadeIn(animationSpec = fastTween())
    val fadeOutSpec = fadeOut(animationSpec = fastTween())
    
    // Slide animations with fade
    fun slideInFromRight() = slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = normalTween()
    ) + fadeIn(animationSpec = fastTween())
    
    fun slideOutToLeft() = slideOutHorizontally(
        targetOffsetX = { -it / 2 },
        animationSpec = normalTween()
    ) + fadeOut(animationSpec = fastTween())
    
    fun slideInFromLeft() = slideInHorizontally(
        initialOffsetX = { -it },
        animationSpec = normalTween()
    ) + fadeIn(animationSpec = fastTween())
    
    fun slideOutToRight() = slideOutHorizontally(
        targetOffsetX = { it / 2 },
        animationSpec = normalTween()
    ) + fadeOut(animationSpec = fastTween())
    
    // Elevation animations
    fun elevationSpec() = tween<Dp>(
        durationMillis = FAST,
        easing = StandardEasing
    )
    
    // Scale animations
    val scaleInSpec = tween<Float>(
        durationMillis = FAST,
        easing = EaseOut
    )
    
    val scaleOutSpec = tween<Float>(
        durationMillis = FAST,
        easing = EaseIn
    )
    
    // Rotation animations
    val rotationSpec = tween<Float>(
        durationMillis = NORMAL,
        easing = StandardEasing
    )
    
    // Infinite pulse animation for progress indicators
    val pulseSpec = infiniteRepeatable<Float>(
        animation = tween(1000, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
    )
    
    // Shake animation for errors
    val shakeSpec = keyframes<Float> {
        durationMillis = 400
        0f at 0
        -10f at 50
        10f at 100
        -10f at 150
        10f at 200
        -5f at 250
        5f at 300
        0f at 400
    }
    
    // Bounce animation for success
    @Suppress("DEPRECATION")
    val bounceSpec = keyframes<Float> {
        durationMillis = 600
        0f at 0
        1.1f at 200 with FastOutLinearInEasing
        0.9f at 400 with LinearOutSlowInEasing
        1f at 600
    }
}

/**
 * Extension functions for common animation patterns
 */
fun <T> repeatingAnimation(
    durationMillis: Int = AnimationSpecs.NORMAL,
    repeatMode: RepeatMode = RepeatMode.Restart
): InfiniteRepeatableSpec<T> = infiniteRepeatable(
    animation = tween(durationMillis),
    repeatMode = repeatMode
)
