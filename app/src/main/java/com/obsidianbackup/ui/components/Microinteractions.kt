package com.obsidianbackup.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.obsidianbackup.ui.components.animations.PullToRefreshAnimation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

/**
 * Pull to refresh with animated indicator
 */
@Composable
fun PullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    indicatorHeight: Dp = 80.dp,
    content: @Composable () -> Unit
) {
    var pullDistance by remember { mutableStateOf(0f) }
    var isRefreshTriggered by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    val maxPullDistance = with(androidx.compose.ui.platform.LocalDensity.current) {
        indicatorHeight.toPx() * 1.5f
    }
    
    val pullProgress = min(pullDistance / maxPullDistance, 1f)
    
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing && isRefreshTriggered) {
            // Animate back to 0
            animate(
                initialValue = pullDistance,
                targetValue = 0f,
                animationSpec = tween(300)
            ) { value, _ ->
                pullDistance = value
            }
            isRefreshTriggered = false
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (pullDistance >= maxPullDistance && !isRefreshing) {
                            isRefreshTriggered = true
                            onRefresh()
                        } else {
                            scope.launch {
                                animate(
                                    initialValue = pullDistance,
                                    targetValue = 0f,
                                    animationSpec = tween(300)
                                ) { value, _ ->
                                    pullDistance = value
                                }
                            }
                        }
                    }
                ) { change, dragAmount ->
                    if (!isRefreshing) {
                        val newPullDistance = pullDistance + dragAmount
                        if (newPullDistance >= 0) {
                            pullDistance = min(newPullDistance, maxPullDistance * 1.2f)
                            change.consume()
                        }
                    }
                }
            }
    ) {
        Box(modifier = Modifier.offset(y = (pullDistance * 0.5f).dp)) {
            content()
        }
        
        // Pull indicator
        if (pullDistance > 0 || isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(indicatorHeight)
                    .align(Alignment.TopCenter),
                contentAlignment = Alignment.Center
            ) {
                PullToRefreshAnimation(
                    pullProgress = pullProgress,
                    isRefreshing = isRefreshing
                )
            }
        }
    }
}

/**
 * Material You circular progress indicator with custom animation
 */
@Composable
fun AnimatedCircularProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 4.dp,
    size: Dp = 48.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = 600,
            easing = FastOutSlowInEasing
        ),
        label = "progress"
    )
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sweepAngle = animatedProgress * 360f
            
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

/**
 * Success checkmark with animation
 */
@Composable
fun AnimatedSuccessCheckmark(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }
    
    val progress by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 600,
            easing = FastOutSlowInEasing
        ),
        label = "checkmark"
    )
    
    Canvas(modifier = modifier.size(48.dp)) {
        val checkmarkPath = listOf(
            Offset(size.width * 0.2f, size.height * 0.5f),
            Offset(size.width * 0.4f, size.height * 0.7f),
            Offset(size.width * 0.8f, size.height * 0.3f)
        )
        
        if (progress > 0f) {
            val currentEnd = (checkmarkPath.size * progress).toInt()
            
            for (i in 0 until minOf(currentEnd, checkmarkPath.size - 1)) {
                drawLine(
                    color = color,
                    start = checkmarkPath[i],
                    end = checkmarkPath[i + 1],
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Ripple effect for button press
 */
@Composable
fun rememberRippleEffect(): (Offset) -> Unit {
    var rippleOffset by remember { mutableStateOf<Offset?>(null) }
    var rippleRadius by remember { mutableStateOf(0f) }
    
    return { offset ->
        rippleOffset = offset
        rippleRadius = 100f
    }
}

/**
 * Animated badge with pulse
 */
@Composable
fun PulsatingBadge(
    count: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.error
) {
    if (count > 0) {
        val infiniteTransition = rememberInfiniteTransition(label = "badge")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        
        Box(
            modifier = modifier
                .size((20 * scale).dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = color,
                    radius = size.minDimension / 2
                )
            }
        }
    }
}

/**
 * Breathing animation for important elements
 */
@Composable
fun BreathingEffect(
    content: @Composable (scale: Float) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    content(scale)
}
