package com.obsidianbackup.ui.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * Haptic feedback utility for providing tactile responses to user actions.
 * Supports modern Android haptic patterns and falls back gracefully on older devices.
 */
class HapticFeedback(private val context: Context, private val view: View? = null) {
    
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * Light tap feedback for button presses and selections
     */
    fun light() {
        view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            )
        }
    }

    /**
     * Medium feedback for important actions
     */
    fun medium() {
        view?.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            )
        }
    }

    /**
     * Heavy feedback for critical actions
     */
    fun heavy() {
        view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            )
        }
    }

    /**
     * Success feedback - double tap pattern
     */
    fun success() {
        view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val timings = longArrayOf(0, 50, 50, 50)
            val amplitudes = intArrayOf(0, 100, 0, 150)
            vibrator?.vibrate(
                VibrationEffect.createWaveform(timings, amplitudes, -1)
            )
        }
    }

    /**
     * Error feedback - sharp double pulse
     */
    fun error() {
        view?.performHapticFeedback(HapticFeedbackConstants.REJECT)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val timings = longArrayOf(0, 30, 80, 30)
            val amplitudes = intArrayOf(0, 200, 0, 200)
            vibrator?.vibrate(
                VibrationEffect.createWaveform(timings, amplitudes, -1)
            )
        }
    }

    /**
     * Long press feedback
     */
    fun longPress() {
        view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        }
    }

    /**
     * Keyboard tap feedback
     */
    fun keyboardTap() {
        view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /**
     * Virtual key feedback
     */
    fun virtualKey() {
        view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    /**
     * Gesture start feedback (Android 10+)
     */
    fun gestureStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view?.performHapticFeedback(HapticFeedbackConstants.GESTURE_START)
        } else {
            light()
        }
    }

    /**
     * Gesture end feedback (Android 10+)
     */
    fun gestureEnd() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view?.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
        } else {
            medium()
        }
    }
}

/**
 * Remember haptic feedback utility in Composable
 */
@Composable
fun rememberHapticFeedback(): HapticFeedback {
    val context = LocalContext.current
    val view = LocalView.current
    return remember(context, view) {
        HapticFeedback(context, view)
    }
}
