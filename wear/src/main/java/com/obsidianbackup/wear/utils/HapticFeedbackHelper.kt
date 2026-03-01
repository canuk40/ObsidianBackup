package com.obsidianbackup.wear.utils

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.getSystemService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides haptic feedback for user interactions
 */
@Singleton
class HapticFeedbackHelper @Inject constructor(
    private val context: Context
) {
    private val vibrator: Vibrator? by lazy {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            context.getSystemService<VibratorManager>()?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun backupStarted() {
        vibrate(SHORT_PULSE)
    }

    fun backupCompleted() {
        vibrate(DOUBLE_PULSE)
    }

    fun backupFailed() {
        vibrate(ERROR_PATTERN)
    }

    fun buttonClick() {
        vibrate(LIGHT_CLICK)
    }

    private fun vibrate(pattern: LongArray) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(pattern, -1)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, -1)
        }
    }

    companion object {
        private val SHORT_PULSE = longArrayOf(0, 50)
        private val DOUBLE_PULSE = longArrayOf(0, 50, 100, 50)
        private val ERROR_PATTERN = longArrayOf(0, 100, 50, 100, 50, 100)
        private val LIGHT_CLICK = longArrayOf(0, 10)
    }
}
