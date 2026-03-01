package com.obsidianbackup.ui.utils

import android.os.Build
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

/**
 * Predictive back gesture support for Android 14+
 * Provides visual feedback during back gesture
 */
@Composable
fun PredictiveBackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit,
    content: @Composable (backProgress: Float) -> Unit
) {
    val context = LocalContext.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    
    var backProgress by remember { mutableStateOf(0f) }
    
    DisposableEffect(enabled, onBack) {
        val callback = object : OnBackPressedCallback(enabled) {
            override fun handleOnBackPressed() {
                onBack()
            }
        }
        
        backDispatcher?.addCallback(callback)
        
        // Android 14+ predictive back gesture
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val activity = context as? ComponentActivity
            activity?.let {
                setupPredictiveBackGesture(
                    activity = it,
                    onBackProgress = { progress ->
                        backProgress = progress
                    },
                    onBack = onBack
                )
            }
        }
        
        onDispose {
            callback.remove()
        }
    }
    
    content(backProgress)
}

/**
 * Setup predictive back gesture for Android 14+
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private fun setupPredictiveBackGesture(
    activity: ComponentActivity,
    onBackProgress: (Float) -> Unit,
    onBack: () -> Unit
) {
    val callback = object : OnBackInvokedCallback {
        override fun onBackInvoked() {
            onBack()
        }
    }
    
    activity.onBackInvokedDispatcher.registerOnBackInvokedCallback(
        OnBackInvokedDispatcher.PRIORITY_DEFAULT,
        callback
    )
}

/**
 * Animated content with predictive back gesture effect
 */
@Composable
fun PredictiveBackContent(
    backProgress: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scale = 1f - (backProgress * 0.1f) // Scale down to 90%
    val translationX = backProgress * 50f // Slide right 50dp max
    val alpha = 1f - (backProgress * 0.3f) // Fade to 70%
    
    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.translationX = translationX
            this.alpha = alpha
        }
    ) {
        content()
    }
}

/**
 * Screen with predictive back gesture animation
 */
@Composable
fun PredictiveBackScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    PredictiveBackHandler(
        enabled = true,
        onBack = onBack
    ) { backProgress ->
        PredictiveBackContent(
            backProgress = backProgress,
            modifier = modifier
        ) {
            content()
        }
    }
}

/**
 * Material You predictive back animation
 * Scales and translates content during back gesture
 */
@Composable
fun MaterialYouBackAnimation(
    backProgress: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Material You specs for predictive back
    val scale = lerp(1f, 0.9f, backProgress)
    val translationX = lerp(0f, 100f, backProgress)
    val cornerRadius = lerp(0f, 28f, backProgress)
    
    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.translationX = translationX
            shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius.toInt())
            clip = true
        }
    ) {
        content()
    }
}

/**
 * Linear interpolation helper
 */
private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}
