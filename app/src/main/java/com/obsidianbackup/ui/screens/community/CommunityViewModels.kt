package com.obsidianbackup.ui.screens.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obsidianbackup.community.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val communityForumManager: CommunityForumManager,
    private val betaProgramManager: BetaProgramManager,
    private val analyticsManager: AnalyticsManager,
    private val feedbackManager: FeedbackManager,
    private val changelogManager: ChangelogManager,
    private val tipsManager: TipsManager
) : ViewModel() {
    
    val communityLinks = MutableStateFlow(communityForumManager.getCommunityLinks())
    val betaEnrolled = betaProgramManager.betaEnrolled.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )
    val analyticsEnabled = analyticsManager.analyticsEnabled.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )
    
    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()
    
    fun openCommunityLink(link: CommunityLink) {
        when (link.icon) {
            "discord" -> communityForumManager.openDiscord()
            "reddit" -> communityForumManager.openReddit()
            "github" -> communityForumManager.openGitHub()
            "docs" -> communityForumManager.openDocumentation()
            "support" -> communityForumManager.openSupportPage()
        }
    }
    
    fun enrollInBeta() {
        viewModelScope.launch {
            betaProgramManager.enrollInBeta()
        }
    }
    
    fun leaveBeta() {
        viewModelScope.launch {
            betaProgramManager.leaveBeta()
        }
    }
    
    fun setAnalyticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            analyticsManager.setAnalyticsEnabled(enabled)
        }
    }
    
    fun navigateToFeedback() {
        viewModelScope.launch {
            _navigationEvent.emit(NavigationEvent.NavigateToFeedback)
        }
    }
    
    fun navigateToChangelog() {
        viewModelScope.launch {
            _navigationEvent.emit(NavigationEvent.NavigateToChangelog)
        }
    }
    
    fun navigateToTips() {
        viewModelScope.launch {
            _navigationEvent.emit(NavigationEvent.NavigateToTips)
        }
    }
}

sealed class NavigationEvent {
    object NavigateToFeedback : NavigationEvent()
    object NavigateToChangelog : NavigationEvent()
    object NavigateToTips : NavigationEvent()
}

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val feedbackManager: FeedbackManager
) : ViewModel() {
    
    val feedbackList = feedbackManager.feedbackList.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    
    val submissionStatus = feedbackManager.submissionStatus.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SubmissionStatus.Idle
    )
    
    fun submitFeedback(
        type: FeedbackType,
        title: String,
        description: String,
        email: String?,
        attachLogs: Boolean
    ) {
        viewModelScope.launch {
            feedbackManager.submitFeedback(
                type = type,
                title = title,
                description = description,
                email = email,
                attachLogs = attachLogs
            )
        }
    }
    
    fun resetSubmissionStatus() {
        feedbackManager.resetSubmissionStatus()
    }
}

@HiltViewModel
class ChangelogViewModel @Inject constructor(
    private val changelogManager: ChangelogManager
) : ViewModel() {
    
    val changelog = changelogManager.changelog.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    
    fun getLatestChanges() = changelogManager.getLatestChanges()
}

@HiltViewModel
class TipsViewModel @Inject constructor(
    private val tipsManager: TipsManager
) : ViewModel() {
    
    private val _tips = MutableStateFlow(tipsManager.getAllTips())
    val tips = _tips.asStateFlow()
    
    val dismissedTips = tipsManager.dismissedTips.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptySet()
    )
    
    private val _tipOfTheDay = MutableStateFlow<Tip?>(null)
    val tipOfTheDay = _tipOfTheDay.asStateFlow()
    
    init {
        loadTipOfTheDay()
    }
    
    private fun loadTipOfTheDay() {
        viewModelScope.launch {
            _tipOfTheDay.value = tipsManager.getTipOfTheDay()
        }
    }
    
    fun dismissTip(tipId: String) {
        viewModelScope.launch {
            tipsManager.dismissTip(tipId)
            loadTipOfTheDay()
        }
    }
    
    fun resetTips() {
        viewModelScope.launch {
            tipsManager.resetTips()
            loadTipOfTheDay()
        }
    }
}
