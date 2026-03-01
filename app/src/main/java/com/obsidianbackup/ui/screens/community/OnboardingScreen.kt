package com.obsidianbackup.ui.screens.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.obsidianbackup.community.OnboardingManager
import com.obsidianbackup.community.OnboardingStep
import com.obsidianbackup.ui.theme.Spacing
import com.obsidianbackup.ui.theme.Elevation
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val steps = viewModel.onboardingSteps
    val pagerState = rememberPagerState(pageCount = { steps.size })
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                viewModel.skipOnboarding()
                                onComplete()
                            }
                        }
                    ) {
                        Text("Skip")
                    }
                }
            )
        },
        bottomBar = {
            OnboardingBottomBar(
                currentPage = pagerState.currentPage,
                totalPages = steps.size,
                onNext = {
                    scope.launch {
                        if (pagerState.currentPage < steps.size - 1) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        } else {
                            viewModel.completeOnboarding()
                            onComplete()
                        }
                    }
                },
                onPrevious = {
                    scope.launch {
                        if (pagerState.currentPage > 0) {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                }
            )
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { page ->
            OnboardingPage(steps[page])
        }
    }
}

@Composable
fun OnboardingPage(step: OnboardingStep) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = when (step.illustration) {
                "welcome" -> Icons.Default.WavingHand
                "storage" -> Icons.Default.CloudQueue
                "security" -> Icons.Default.Lock
                "automation" -> Icons.Default.AutoMode
                "control" -> Icons.Default.Dashboard
                else -> Icons.Default.Info
            },
            contentDescription = "Step illustration for ${step.title}",
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(Spacing.lg))
        
        Text(
            text = step.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(Spacing.md))
        
        Text(
            text = step.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun OnboardingBottomBar(
    currentPage: Int,
    totalPages: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    Surface(
        tonalElevation = Elevation.low
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentPage > 0) {
                TextButton(onClick = onPrevious) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Navigate to previous step")
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text("Previous")
                }
            } else {
                Spacer(modifier = Modifier.width(Spacing.xxs))
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                repeat(totalPages) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == currentPage) 12.dp else 8.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (index == currentPage) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
            
            Button(onClick = onNext) {
                Text(if (currentPage < totalPages - 1) "Next" else "Get Started")
                if (currentPage < totalPages - 1) {
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Navigate to next step")
                }
            }
        }
    }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class OnboardingViewModel @javax.inject.Inject constructor(
    private val onboardingManager: OnboardingManager
) : androidx.lifecycle.ViewModel() {
    
    val onboardingSteps = onboardingManager.getOnboardingSteps()
    
    suspend fun completeOnboarding() {
        onboardingManager.completeOnboarding()
    }
    
    suspend fun skipOnboarding() {
        onboardingManager.skipOnboarding()
    }
}
