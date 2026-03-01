package com.obsidianbackup.tv.navigation

import android.view.KeyEvent
import android.view.View

/**
 * Handles D-pad and remote control navigation for TV interface
 */
object TVNavigationHandler {
    
    /**
     * Handle key events from D-pad and remote control
     */
    fun handleKeyEvent(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                // Let the system handle D-pad navigation
                false
            }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                // Handle select/enter action
                false
            }
            KeyEvent.KEYCODE_BACK -> {
                // Handle back button
                false
            }
            KeyEvent.KEYCODE_MENU -> {
                // Handle menu button
                true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                // Handle media controls
                true
            }
            else -> false
        }
    }

    /**
     * Set up focus listeners for TV navigation
     */
    fun setupFocusListener(view: View, onFocusChanged: (Boolean) -> Unit) {
        view.setOnFocusChangeListener { _, hasFocus ->
            onFocusChanged(hasFocus)
        }
    }

    /**
     * Request focus on the first focusable view
     */
    fun requestInitialFocus(rootView: View) {
        rootView.post {
            val firstFocusable = findFirstFocusable(rootView)
            firstFocusable?.requestFocus()
        }
    }

    private fun findFirstFocusable(view: View): View? {
        if (view.isFocusable) return view
        
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                val focusable = findFirstFocusable(child)
                if (focusable != null) return focusable
            }
        }
        
        return null
    }

    /**
     * Configure view for TV focus and selection
     */
    fun configureTVView(view: View) {
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        view.isClickable = true
    }
}
