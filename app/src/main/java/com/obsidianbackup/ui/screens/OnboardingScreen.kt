package com.obsidianbackup.ui.screens

import androidx.compose.foundation.layout.*
import com.obsidianbackup.ui.theme.Spacing
import com.obsidianbackup.ui.theme.IconSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.res.stringResource
import com.obsidianbackup.R
import com.obsidianbackup.ui.components.EnhancedButton
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val steps = listOf(
        OnboardingStep(
            icon = Icons.Default.Security,
            title = stringResource(R.string.onboarding_step1_title),
            description = stringResource(R.string.onboarding_step1_description)
        ),
        OnboardingStep(
            icon = Icons.Default.Speed,
            title = stringResource(R.string.onboarding_step2_title),
            description = stringResource(R.string.onboarding_step2_description)
        ),
        OnboardingStep(
            icon = Icons.Default.Extension,
            title = stringResource(R.string.onboarding_step3_title),
            description = stringResource(R.string.onboarding_step3_description)
        )
    )
    
    val pagerState = rememberPagerState(pageCount = { steps.size })
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.onboarding_title)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // HorizontalPager for steps
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPage(step = steps[page])
            }
            
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Page indicators
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(steps.size) { index ->
                        PageIndicator(
                            isActive = index == pagerState.currentPage,
                            modifier = Modifier.padding(horizontal = Spacing.xxs)
                        )
                    }
                }
                
                // Navigation buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (pagerState.currentPage > 0) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            Spacer(modifier = Modifier.width(Spacing.xxs))
                            Text(stringResource(R.string.action_back))
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    
                    if (pagerState.currentPage < steps.size - 1) {
                        EnhancedButton(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        ) {
                            Text(stringResource(R.string.action_next))
                            Spacer(modifier = Modifier.width(Spacing.xxs))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    } else {
                        EnhancedButton(
                            onClick = onComplete
                        ) {
                            Text(stringResource(R.string.action_get_started))
                            Spacer(modifier = Modifier.width(Spacing.xxs))
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingPage(step: OnboardingStep) {
    // Animated icon scale
    val infiniteTransition = rememberInfiniteTransition(label = "icon_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_scale"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // Animated icon
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .size(IconSize.hero)
                .scale(scale)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    step.icon,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.xlarge),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(Spacing.xl))
        
        Text(
            step.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(Spacing.md))
        
        Text(
            step.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PageIndicator(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val width by animateDpAsState(
        targetValue = if (isActive) 24.dp else 8.dp,
        animationSpec = tween(300),
        label = "indicator_width"
    )
    
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (isActive) 
            MaterialTheme.colorScheme.primary 
        else 
            MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .width(width)
            .height(8.dp)
    ) {}
}

data class OnboardingStep(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String
)
