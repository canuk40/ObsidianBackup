package com.obsidianbackup.accessibility

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Voice control handler for accessibility support.
 * Enables voice commands like "backup my apps", "restore my apps", etc.
 * Complies with WCAG 2.2 guideline 2.5.6 (Concurrent Input Mechanisms).
 */
@Singleton
class VoiceControlHandler @Inject constructor(
    @ApplicationContext private val context: Context
) : RecognitionListener {
    
    private var speechRecognizer: SpeechRecognizer? = null
    private val _voiceCommandState = MutableLiveData<VoiceCommandState>()
    val voiceCommandState: LiveData<VoiceCommandState> = _voiceCommandState
    
    private val _lastCommand = MutableLiveData<VoiceCommand?>()
    val lastCommand: LiveData<VoiceCommand?> = _lastCommand
    
    /**
     * Initialize speech recognizer
     */
    fun initialize() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@VoiceControlHandler)
            }
            _voiceCommandState.value = VoiceCommandState.Ready
        } else {
            _voiceCommandState.value = VoiceCommandState.NotAvailable
        }
    }
    
    /**
     * Start listening for voice commands
     */
    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }
        
        speechRecognizer?.startListening(intent)
        _voiceCommandState.value = VoiceCommandState.Listening
    }
    
    /**
     * Stop listening for voice commands
     */
    fun stopListening() {
        speechRecognizer?.stopListening()
        _voiceCommandState.value = VoiceCommandState.Ready
    }
    
    /**
     * Release resources
     */
    fun release() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        _voiceCommandState.value = VoiceCommandState.NotAvailable
    }
    
    override fun onReadyForSpeech(params: Bundle?) {
        _voiceCommandState.value = VoiceCommandState.Listening
    }
    
    override fun onBeginningOfSpeech() {
        _voiceCommandState.value = VoiceCommandState.Processing
    }
    
    override fun onRmsChanged(rmsdB: Float) {
        // Can be used for visual feedback
    }
    
    override fun onBufferReceived(buffer: ByteArray?) {
        // Not used
    }
    
    override fun onEndOfSpeech() {
        _voiceCommandState.value = VoiceCommandState.Processing
    }
    
    override fun onError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
            else -> "Unknown error"
        }
        _voiceCommandState.value = VoiceCommandState.Error(errorMessage)
    }
    
    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (matches != null && matches.isNotEmpty()) {
            val command = parseVoiceCommand(matches[0])
            _lastCommand.value = command
            _voiceCommandState.value = VoiceCommandState.CommandRecognized(command)
        } else {
            _voiceCommandState.value = VoiceCommandState.Ready
        }
    }
    
    override fun onPartialResults(partialResults: Bundle?) {
        // Can be used for real-time feedback
    }
    
    override fun onEvent(eventType: Int, params: Bundle?) {
        // Not used
    }
    
    /**
     * Parse voice input into a command
     */
    private fun parseVoiceCommand(input: String): VoiceCommand {
        val normalized = input.lowercase().trim()
        
        return when {
            normalized.contains("backup") && (normalized.contains("app") || normalized.contains("all")) ->
                VoiceCommand.BackupApps
            
            normalized.contains("restore") && (normalized.contains("app") || normalized.contains("all")) ->
                VoiceCommand.RestoreApps
            
            normalized.contains("status") || normalized.contains("progress") ->
                VoiceCommand.CheckStatus
            
            normalized.contains("settings") || normalized.contains("setting") ->
                VoiceCommand.OpenSettings
            
            normalized.contains("cancel") || normalized.contains("stop") ->
                VoiceCommand.CancelOperation
            
            normalized.contains("help") ->
                VoiceCommand.ShowHelp
            
            else -> VoiceCommand.Unknown(input)
        }
    }
}

/**
 * Voice command state
 */
sealed class VoiceCommandState {
    object NotAvailable : VoiceCommandState()
    object Ready : VoiceCommandState()
    object Listening : VoiceCommandState()
    object Processing : VoiceCommandState()
    data class CommandRecognized(val command: VoiceCommand) : VoiceCommandState()
    data class Error(val message: String) : VoiceCommandState()
}

/**
 * Supported voice commands
 */
sealed class VoiceCommand {
    object BackupApps : VoiceCommand()
    object RestoreApps : VoiceCommand()
    object CheckStatus : VoiceCommand()
    object OpenSettings : VoiceCommand()
    object CancelOperation : VoiceCommand()
    object ShowHelp : VoiceCommand()
    data class Unknown(val input: String) : VoiceCommand()
}
