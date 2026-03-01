package com.obsidianbackup.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obsidianbackup.community.FeedbackManager
import com.obsidianbackup.community.FeedbackType
import com.obsidianbackup.community.SubmissionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val feedbackManager: FeedbackManager
) : ViewModel() {
    
    val submissionStatus: StateFlow<SubmissionStatus> = feedbackManager.submissionStatus
    
    fun submitFeedback(
        title: String,
        description: String,
        type: FeedbackType = FeedbackType.OTHER,
        attachLogs: Boolean = false
    ) {
        viewModelScope.launch {
            feedbackManager.submitFeedback(
                type = type,
                title = title,
                description = description,
                attachLogs = attachLogs
            )
        }
    }
    
    fun resetStatus() {
        feedbackManager.resetSubmissionStatus()
    }
}
