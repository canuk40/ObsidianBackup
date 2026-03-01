package com.obsidianbackup.ui.utils

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

/**
 * Content animation utilities for list items, loading states, and other UI components
 */

/**
 * Staggered animation for LazyColumn/LazyRow items
 * Each item animates in with a slight delay after the previous one
 * 
 * Usage:
 * ```
 * LazyColumn {
 *     itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
 *         Box(modifier = Modifier.animateItemPlacement()) {
 *             ItemContent(item, modifier = Modifier.staggeredListItemAnimation(index))
 *         }
 *     }
 * }
 * ```
 */
fun Modifier.staggeredListItemAnimation(
    index: Int,
    staggerDelayMillis: Int = 50,
    durationMillis: Int = AnimationSpecs.NORMAL
): Modifier = composed {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay((index * staggerDelayMillis).toLong())
        visible = true
    }
    
    this.graphicsLayer {
        if (!visible) {
            alpha = 0f
            translationY = 50f
        } else {
            alpha = 1f
            translationY = 0f
        }
    }
}

/**
 * Simple fade-in animation for list items
 * 
 * Usage in LazyColumn:
 * ```
 * items(items, key = { it.id }) { item ->
 *     ItemContent(item, modifier = Modifier.fadeInListItem())
 * }
 * ```
 */
fun Modifier.fadeInListItem(
    durationMillis: Int = AnimationSpecs.NORMAL
): Modifier = composed {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis, easing = AnimationSpecs.EaseOut),
        label = "list_item_fade"
    )
    
    this.graphicsLayer { this.alpha = alpha }
}

/**
 * Enter animation spec for list items
 */
val listItemEnterAnimation = fadeIn(
    animationSpec = tween(
        durationMillis = AnimationSpecs.NORMAL,
        easing = AnimationSpecs.StandardEasing
    )
) + slideInVertically(
    initialOffsetY = { it / 2 },
    animationSpec = tween(
        durationMillis = AnimationSpecs.NORMAL,
        easing = AnimationSpecs.EmphasizedEasing
    )
) + expandVertically(
    animationSpec = tween(
        durationMillis = AnimationSpecs.NORMAL,
        easing = AnimationSpecs.StandardEasing
    )
)

/**
 * Exit animation spec for list items
 */
val listItemExitAnimation = fadeOut(
    animationSpec = tween(
        durationMillis = AnimationSpecs.FAST,
        easing = AnimationSpecs.StandardEasing
    )
) + slideOutVertically(
    targetOffsetY = { -it / 2 },
    animationSpec = tween(
        durationMillis = AnimationSpecs.FAST,
        easing = AnimationSpecs.EmphasizedEasing
    )
) + shrinkVertically(
    animationSpec = tween(
        durationMillis = AnimationSpecs.FAST,
        easing = AnimationSpecs.StandardEasing
    )
)

/**
 * Loading state crossfade
 * 
 * Usage:
 * ```
 * LoadingCrossfade(
 *     isLoading = state.isLoading,
 *     loadingContent = { CircularProgressIndicator() },
 *     content = { MainContent() }
 * )
 * ```
 */
@Composable
fun LoadingCrossfade(
    isLoading: Boolean,
    loadingContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = isLoading,
        transitionSpec = {
            fadeIn(
                animationSpec = tween(
                    durationMillis = AnimationSpecs.NORMAL,
                    easing = AnimationSpecs.StandardEasing
                )
            ) togetherWith fadeOut(
                animationSpec = tween(
                    durationMillis = AnimationSpecs.FAST,
                    easing = AnimationSpecs.StandardEasing
                )
            )
        },
        modifier = modifier,
        label = "loading_crossfade"
    ) { loading ->
        if (loading) {
            loadingContent()
        } else {
            content()
        }
    }
}

/**
 * Error/Empty state animation
 * 
 * Usage:
 * ```
 * EmptyStateAnimation(
 *     visible = items.isEmpty(),
 *     content = { EmptyStateMessage() }
 * )
 * ```
 */
@Composable
fun EmptyStateAnimation(
    visible: Boolean,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = AnimationSpecs.NORMAL,
                delayMillis = AnimationSpecs.FAST,
                easing = AnimationSpecs.StandardEasing
            )
        ) + scaleIn(
            initialScale = 0.9f,
            animationSpec = tween(
                durationMillis = AnimationSpecs.NORMAL,
                delayMillis = AnimationSpecs.FAST,
                easing = AnimationSpecs.EmphasizedEasing
            )
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = AnimationSpecs.FAST,
                easing = AnimationSpecs.StandardEasing
            )
        ) + scaleOut(
            targetScale = 0.9f,
            animationSpec = tween(
                durationMillis = AnimationSpecs.FAST,
                easing = AnimationSpecs.StandardEasing
            )
        ),
        modifier = modifier,
        label = "empty_state"
    ) {
        content()
    }
}

/**
 * FAB visibility animation with scale and fade
 * 
 * Usage:
 * ```
 * FabAnimation(
 *     visible = !scrollBehavior.state.isScrolledAway,
 *     content = {
 *         FloatingActionButton(onClick = {}) {
 *             Icon(Icons.Default.Add, contentDescription = "Add")
 *         }
 *     }
 * )
 * ```
 */
@Composable
fun FabAnimation(
    visible: Boolean,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.7f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = AnimationSpecs.FAST,
                easing = AnimationSpecs.StandardEasing
            )
        ),
        exit = scaleOut(
            targetScale = 0.7f,
            animationSpec = tween(
                durationMillis = AnimationSpecs.FAST,
                easing = AnimationSpecs.StandardEasing
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = AnimationSpecs.FAST,
                easing = AnimationSpecs.StandardEasing
            )
        ),
        modifier = modifier,
        label = "fab_animation"
    ) {
        content()
    }
}

/**
 * Card expansion animation
 * 
 * Usage:
 * ```
 * CardExpansion(
 *     expanded = isExpanded,
 *     content = { ExpandedContent() }
 * )
 * ```
 */
@Composable
fun CardExpansion(
    expanded: Boolean,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = AnimationSpecs.NORMAL,
                easing = AnimationSpecs.StandardEasing
            )
        ),
        exit = shrinkVertically(
            animationSpec = tween(
                durationMillis = AnimationSpecs.NORMAL,
                easing = AnimationSpecs.StandardEasing
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = AnimationSpecs.FAST,
                easing = AnimationSpecs.StandardEasing
            )
        ),
        modifier = modifier,
        label = "card_expansion"
    ) {
        content()
    }
}

/**
 * Success animation - scale with bounce
 */
@Composable
fun SuccessAnimation(
    visible: Boolean,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.5f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = AnimationSpecs.NORMAL,
                easing = AnimationSpecs.StandardEasing
            )
        ),
        exit = scaleOut(
            targetScale = 1.2f,
            animationSpec = tween(
                durationMillis = AnimationSpecs.FAST,
                easing = AnimationSpecs.StandardEasing
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = AnimationSpecs.FAST,
                easing = AnimationSpecs.StandardEasing
            )
        ),
        modifier = modifier,
        label = "success_animation"
    ) {
        content()
    }
}

/**
 * Error shake animation
 */
fun Modifier.shakeOnError(shouldShake: Boolean): Modifier = composed {
    val offsetX by animateFloatAsState(
        targetValue = if (shouldShake) 1f else 0f,
        animationSpec = keyframes {
            durationMillis = 400
            0f at 0
            -10f at 50
            10f at 100
            -10f at 150
            10f at 200
            -5f at 250
            5f at 300
            0f at 400
        },
        label = "shake_animation"
    )
    
    this.graphicsLayer { translationX = offsetX }
}

/**
 * Pulse animation for notifications/badges
 */
fun Modifier.pulseAnimation(enabled: Boolean): Modifier = composed {
    val scale by rememberInfiniteTransition(label = "pulse_transition").animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    if (enabled) {
        this.graphicsLayer { scaleX = scale; scaleY = scale }
    } else {
        this
    }
}
