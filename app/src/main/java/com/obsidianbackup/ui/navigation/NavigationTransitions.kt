package com.obsidianbackup.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.navigation.NavBackStackEntry
import com.obsidianbackup.ui.utils.AnimationSpecs

/**
 * Navigation transition animations for Material You 3.0
 */
object NavigationTransitions {
    
    /**
     * Standard horizontal slide transition
     */
    fun horizontalSlide(
        initialOffsetX: (Int) -> Int = { it },
        targetOffsetX: (Int) -> Int = { -it }
    ): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition {
        return {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(
                    durationMillis = AnimationSpecs.NORMAL,
                    easing = AnimationSpecs.EmphasizedEasing
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = AnimationSpecs.FAST,
                    easing = LinearEasing
                )
            )
        }
    }
    
    /**
     * Standard horizontal exit transition
     */
    fun horizontalExit(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition {
        return {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(
                    durationMillis = AnimationSpecs.NORMAL,
                    easing = AnimationSpecs.EmphasizedEasing
                )
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = AnimationSpecs.FAST,
                    easing = LinearEasing
                )
            )
        }
    }
    
    /**
     * Pop enter transition (back navigation)
     */
    fun popEnter(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition {
        return {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(
                    durationMillis = AnimationSpecs.NORMAL,
                    easing = AnimationSpecs.EmphasizedEasing
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = AnimationSpecs.FAST,
                    easing = LinearEasing
                )
            )
        }
    }
    
    /**
     * Pop exit transition (back navigation)
     */
    fun popExit(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition {
        return {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(
                    durationMillis = AnimationSpecs.NORMAL,
                    easing = AnimationSpecs.EmphasizedEasing
                )
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = AnimationSpecs.FAST,
                    easing = LinearEasing
                )
            )
        }
    }
    
    /**
     * Fade through transition (for bottom navigation)
     */
    fun fadeThrough(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition {
        return {
            fadeIn(
                animationSpec = tween(
                    durationMillis = AnimationSpecs.NORMAL,
                    delayMillis = 50,
                    easing = AnimationSpecs.StandardEasing
                )
            ) + scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(
                    durationMillis = AnimationSpecs.NORMAL,
                    easing = AnimationSpecs.StandardEasing
                )
            )
        }
    }
    
    /**
     * Fade through exit
     */
    fun fadeThroughExit(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition {
        return {
            fadeOut(
                animationSpec = tween(
                    durationMillis = AnimationSpecs.FAST,
                    easing = AnimationSpecs.StandardEasing
                )
            ) + scaleOut(
                targetScale = 0.92f,
                animationSpec = tween(
                    durationMillis = AnimationSpecs.FAST,
                    easing = AnimationSpecs.StandardEasing
                )
            )
        }
    }
    
    /**
     * Shared axis Z transition (hierarchical)
     */
    fun sharedAxisZ(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition {
        return {
            fadeIn(
                animationSpec = tween(
                    durationMillis = AnimationSpecs.NORMAL,
                    easing = AnimationSpecs.EmphasizedEasing
                )
            ) + scaleIn(
                initialScale = 0.8f,
                animationSpec = tween(
                    durationMillis = AnimationSpecs.NORMAL,
                    easing = AnimationSpecs.EmphasizedEasing
                )
            )
        }
    }
    
    /**
     * Shared axis Z exit
     */
    fun sharedAxisZExit(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition {
        return {
            fadeOut(
                animationSpec = tween(
                    durationMillis = AnimationSpecs.FAST,
                    easing = AnimationSpecs.EmphasizedEasing
                )
            ) + scaleOut(
                targetScale = 1.1f,
                animationSpec = tween(
                    durationMillis = AnimationSpecs.FAST,
                    easing = AnimationSpecs.EmphasizedEasing
                )
            )
        }
    }
    
    /**
     * Vertical slide for modal/dialog style
     */
    fun modalSlideUp(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition {
        return {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Up,
                animationSpec = tween(
                    durationMillis = AnimationSpecs.NORMAL,
                    easing = AnimationSpecs.EmphasizedEasing
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = AnimationSpecs.FAST
                )
            )
        }
    }
    
    /**
     * Modal slide down exit
     */
    fun modalSlideDown(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition {
        return {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Down,
                animationSpec = tween(
                    durationMillis = AnimationSpecs.NORMAL,
                    easing = AnimationSpecs.EmphasizedEasing
                )
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = AnimationSpecs.FAST
                )
            )
        }
    }
}

/**
 * Extension for default navigation transitions
 */
@OptIn(ExperimentalAnimationApi::class)
fun defaultEnterTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
    NavigationTransitions.horizontalSlide()

@OptIn(ExperimentalAnimationApi::class)
fun defaultExitTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
    NavigationTransitions.horizontalExit()

@OptIn(ExperimentalAnimationApi::class)
fun defaultPopEnterTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
    NavigationTransitions.popEnter()

@OptIn(ExperimentalAnimationApi::class)
fun defaultPopExitTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
    NavigationTransitions.popExit()
