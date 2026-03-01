package com.obsidianbackup.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.obsidianbackup.features.FeatureFlagManager
import com.obsidianbackup.permissions.PermissionManager
import com.obsidianbackup.scanner.AppScanner
import com.obsidianbackup.ui.screens.*
import com.obsidianbackup.ui.screens.community.CommunityScreen
import com.obsidianbackup.ui.screens.community.FeedbackScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.obsidianbackup.presentation.dashboard.DashboardViewModel
import com.obsidianbackup.ui.utils.AnimationSpecs

// Pre-computed route strings to avoid sealed class initialization issues in transition lambdas
private val MAIN_ROUTES = setOf("dashboard", "apps", "backups", "automation", "logs", "settings")

/**
 * Navigation host with comprehensive animations for all screens
 * 
 * Features:
 * - Smooth slide transitions for hierarchical navigation
 * - Crossfade for lateral navigation (bottom nav)
 * - Modal animations for dialog-style screens
 * - Respects accessibility preferences for reduced motion
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NavigationHost(
    navController: NavHostController,
    permissionManager: PermissionManager,
    appScanner: AppScanner,
    featureFlagManager: FeatureFlagManager?,
    modifier: Modifier = Modifier,
    startDestination: String = "dashboard"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        // Default forward navigation - slide from right with fade
        enterTransition = {
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
        },
        // Default forward navigation exit
        exitTransition = {
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
        },
        // Back navigation - slide from left with fade
        popEnterTransition = {
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
        },
        // Back navigation exit
        popExitTransition = {
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
    ) {
        // Main navigation screens - use crossfade for bottom nav switches
        composable(
            route = "dashboard",
            enterTransition = { 
                if (initialState.destination.route in MAIN_ROUTES) {
                    // Lateral navigation (bottom nav) - crossfade only
                    fadeIn(tween(AnimationSpecs.NORMAL)) + scaleIn(
                        initialScale = 0.95f,
                        animationSpec = tween(AnimationSpecs.NORMAL)
                    )
                } else {
                    // Deep navigation - use default slide
                    null
                }
            },
            exitTransition = {
                if (targetState.destination.route in MAIN_ROUTES) {
                    fadeOut(tween(AnimationSpecs.FAST)) + scaleOut(
                        targetScale = 0.95f,
                        animationSpec = tween(AnimationSpecs.FAST)
                    )
                } else {
                    null
                }
            }
        ) {
            DashboardScreen(
                permissionManager = permissionManager,
                onNavigate = { screen -> navController.navigate(screen.route) },
                onNavigateToOnboarding = { navController.navigate("onboarding") }
            )
        }

        composable(
            route = "apps",
            enterTransition = { 
                if (initialState.destination.route in MAIN_ROUTES) {
                    fadeIn(tween(AnimationSpecs.NORMAL)) + scaleIn(
                        initialScale = 0.95f,
                        animationSpec = tween(AnimationSpecs.NORMAL)
                    )
                } else null
            },
            exitTransition = {
                if (targetState.destination.route in MAIN_ROUTES) {
                    fadeOut(tween(AnimationSpecs.FAST)) + scaleOut(
                        targetScale = 0.95f,
                        animationSpec = tween(AnimationSpecs.FAST)
                    )
                } else null
            }
        ) {
            AppsScreen(permissionManager, appScanner)
        }

        composable(
            route = "backups",
            enterTransition = { 
                if (initialState.destination.route in MAIN_ROUTES) {
                    fadeIn(tween(AnimationSpecs.NORMAL)) + scaleIn(
                        initialScale = 0.95f,
                        animationSpec = tween(AnimationSpecs.NORMAL)
                    )
                } else null
            },
            exitTransition = {
                if (targetState.destination.route in MAIN_ROUTES) {
                    fadeOut(tween(AnimationSpecs.FAST)) + scaleOut(
                        targetScale = 0.95f,
                        animationSpec = tween(AnimationSpecs.FAST)
                    )
                } else null
            }
        ) {
            BackupsScreen()
        }

        composable(
            route = "automation",
            enterTransition = { 
                if (initialState.destination.route in MAIN_ROUTES) {
                    fadeIn(tween(AnimationSpecs.NORMAL)) + scaleIn(
                        initialScale = 0.95f,
                        animationSpec = tween(AnimationSpecs.NORMAL)
                    )
                } else null
            },
            exitTransition = {
                if (targetState.destination.route in MAIN_ROUTES) {
                    fadeOut(tween(AnimationSpecs.FAST)) + scaleOut(
                        targetScale = 0.95f,
                        animationSpec = tween(AnimationSpecs.FAST)
                    )
                } else null
            }
        ) {
            AutomationScreen(permissionManager)
        }

        composable(
            route = "logs",
            enterTransition = { 
                if (initialState.destination.route in MAIN_ROUTES) {
                    fadeIn(tween(AnimationSpecs.NORMAL)) + scaleIn(
                        initialScale = 0.95f,
                        animationSpec = tween(AnimationSpecs.NORMAL)
                    )
                } else null
            },
            exitTransition = {
                if (targetState.destination.route in MAIN_ROUTES) {
                    fadeOut(tween(AnimationSpecs.FAST)) + scaleOut(
                        targetScale = 0.95f,
                        animationSpec = tween(AnimationSpecs.FAST)
                    )
                } else null
            }
        ) {
            LogsScreen()
        }

        composable(
            route = "settings",
            enterTransition = { 
                if (initialState.destination.route in MAIN_ROUTES) {
                    fadeIn(tween(AnimationSpecs.NORMAL)) + scaleIn(
                        initialScale = 0.95f,
                        animationSpec = tween(AnimationSpecs.NORMAL)
                    )
                } else null
            },
            exitTransition = {
                if (targetState.destination.route in MAIN_ROUTES) {
                    fadeOut(tween(AnimationSpecs.FAST)) + scaleOut(
                        targetScale = 0.95f,
                        animationSpec = tween(AnimationSpecs.FAST)
                    )
                } else null
            }
        ) {
            SettingsScreen(
                permissionManager = permissionManager,
                onNavigateToFeatureFlags = { navController.navigate("feature_flags") },
                onNavigateToZeroKnowledge = { navController.navigate("zero_knowledge") },
                onNavigateToCloudProviders = { navController.navigate("cloud_providers") },
                onNavigateToFilecoin = { navController.navigate("filecoin") },
                onNavigateToGaming = { navController.navigate("gaming") },
                onNavigateToHealth = { navController.navigate("health") },
                onNavigateToAutomation = { navController.navigate("automation") },
                onNavigateToPlugins = { navController.navigate("plugins") },
                onNavigateToSmartScheduling = { navController.navigate("smart_scheduling") },
                onNavigateToLicenses = { navController.navigate("licenses") },   // H-1 / M-7
                onNavigateToVersionInfo = { navController.navigate("version_info") },
                onNavigateToSimplifiedMode = { navController.navigate("simplified_mode") },
                onNavigateToBusyBox = { navController.navigate("busybox_options") },
                onNavigateToPermissionMode = { navController.navigate("permission_mode") },
                onNavigateToRetentionPolicies = { navController.navigate("retention_policies") },
                onNavigateToStorageLimits = { navController.navigate("storage_limits") }
            )
        }

        // Smart Scheduling Screen
        composable(route = "smart_scheduling") {
            SmartSchedulingScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Detail/Settings screens - standard slide
        composable("gaming") {
            GamingScreen()
        }

        composable("health") {
            HealthScreen(
                onNavigateToHealthPrivacy = { navController.navigate("health_privacy") }
            )
        }

        composable("plugins") {
            PluginsScreen()
        }

        composable("feature_flags") {
            if (featureFlagManager != null) {
                FeatureFlagsScreen(
                    featureFlags = featureFlagManager,
                    onBack = { navController.popBackStack() }
                )
            } else {
                // FeatureFlagManager requires Firebase — show fallback
                FeatureFlagsUnavailableScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // Modal-style screens - slide up from bottom
        composable(
            route = "zero_knowledge",
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(tween(AnimationSpecs.FAST)) + scaleIn(
                    initialScale = 0.92f,
                    animationSpec = tween(AnimationSpecs.NORMAL)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(AnimationSpecs.NORMAL)
                ) + fadeOut(tween(AnimationSpecs.FAST)) + scaleOut(
                    targetScale = 0.92f,
                    animationSpec = tween(AnimationSpecs.NORMAL)
                )
            }
        ) {
            ZeroKnowledgeScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "cloud_providers",
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(tween(AnimationSpecs.FAST)) + scaleIn(
                    initialScale = 0.92f,
                    animationSpec = tween(AnimationSpecs.NORMAL)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(AnimationSpecs.NORMAL)
                ) + fadeOut(tween(AnimationSpecs.FAST)) + scaleOut(
                    targetScale = 0.92f,
                    animationSpec = tween(AnimationSpecs.NORMAL)
                )
            }
        ) {
            CloudProvidersScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProvider = { providerKey ->
                    when (providerKey) {
                        "filecoin" -> navController.navigate("filecoin")
                        "oracle_cloud" -> navController.navigate("cloud_provider_config")
                        else -> navController.navigate("cloud_provider_setup/$providerKey")
                    }
                }
            )
        }

        composable("cloud_provider_setup/{providerKey}") { backStackEntry ->
            val providerKey = backStackEntry.arguments?.getString("providerKey") ?: ""
            val providerType = when (providerKey) {
                "google_drive", "google_drive" -> com.obsidianbackup.cloud.rclone.RcloneProviderFactory.ProviderType.GOOGLE_DRIVE
                "dropbox"      -> com.obsidianbackup.cloud.rclone.RcloneProviderFactory.ProviderType.DROPBOX
                "onedrive"     -> com.obsidianbackup.cloud.rclone.RcloneProviderFactory.ProviderType.ONEDRIVE
                "aws_s3", "s3" -> com.obsidianbackup.cloud.rclone.RcloneProviderFactory.ProviderType.S3
                "backblaze_b2" -> com.obsidianbackup.cloud.rclone.RcloneProviderFactory.ProviderType.BACKBLAZE_B2
                "webdav"       -> com.obsidianbackup.cloud.rclone.RcloneProviderFactory.ProviderType.WEBDAV
                "sftp"         -> com.obsidianbackup.cloud.rclone.RcloneProviderFactory.ProviderType.SFTP
                else           -> null
            }
            if (providerType != null) {
                com.obsidianbackup.ui.cloud.RcloneProviderConfigScreen(
                    providerType = providerType,
                    onSave = { navController.popBackStack() },
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                navController.popBackStack()
            }
        }

        composable("rclone_provider_selection") {
            com.obsidianbackup.ui.cloud.RcloneProviderSelectionScreen(
                onProviderSelected = { providerType ->
                    navController.navigate("cloud_provider_setup/${providerType.name.lowercase()}")
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("cloud_provider_config") {
            com.obsidianbackup.ui.cloud.CloudProviderConfigScreen(
                viewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("filecoin") {
            FilecoinConfigScreen(
                logger = com.obsidianbackup.logging.ObsidianLogger(),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Community/Info screens - fade only (no strong directionality)
        composable(
            route = "community",
            enterTransition = {
                fadeIn(tween(AnimationSpecs.NORMAL)) + scaleIn(
                    initialScale = 0.95f,
                    animationSpec = tween(AnimationSpecs.NORMAL, easing = AnimationSpecs.EmphasizedEasing)
                )
            },
            exitTransition = {
                fadeOut(tween(AnimationSpecs.FAST)) + scaleOut(
                    targetScale = 0.95f,
                    animationSpec = tween(AnimationSpecs.FAST)
                )
            }
        ) {
            CommunityScreen(
                onNavigateToFeedback = { navController.navigate("feedback") },
                onNavigateToChangelog = { navController.navigate("changelog") },
                onNavigateToTips = { navController.navigate("tips") }
            )
        }

        composable(
            route = "feedback",
            enterTransition = {
                fadeIn(tween(AnimationSpecs.NORMAL)) + scaleIn(
                    initialScale = 0.95f,
                    animationSpec = tween(AnimationSpecs.NORMAL, easing = AnimationSpecs.EmphasizedEasing)
                )
            },
            exitTransition = {
                fadeOut(tween(AnimationSpecs.FAST)) + scaleOut(
                    targetScale = 0.95f,
                    animationSpec = tween(AnimationSpecs.FAST)
                )
            }
        ) {
            FeedbackScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "changelog",
            enterTransition = {
                fadeIn(tween(AnimationSpecs.NORMAL)) + scaleIn(
                    initialScale = 0.95f,
                    animationSpec = tween(AnimationSpecs.NORMAL, easing = AnimationSpecs.EmphasizedEasing)
                )
            },
            exitTransition = {
                fadeOut(tween(AnimationSpecs.FAST)) + scaleOut(
                    targetScale = 0.95f,
                    animationSpec = tween(AnimationSpecs.FAST)
                )
            }
        ) {
            ChangelogScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "tips",
            enterTransition = {
                fadeIn(tween(AnimationSpecs.NORMAL)) + scaleIn(
                    initialScale = 0.95f,
                    animationSpec = tween(AnimationSpecs.NORMAL, easing = AnimationSpecs.EmphasizedEasing)
                )
            },
            exitTransition = {
                fadeOut(tween(AnimationSpecs.FAST)) + scaleOut(
                    targetScale = 0.95f,
                    animationSpec = tween(AnimationSpecs.FAST)
                )
            }
        ) {
            TipsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Onboarding - special case, fade in only (no back navigation expected)
        composable(
            route = "onboarding",
            enterTransition = {
                fadeIn(
                    animationSpec = tween(
                        durationMillis = AnimationSpecs.SLOW,
                        easing = AnimationSpecs.EmphasizedEasing
                    )
                )
            },
            exitTransition = {
                fadeOut(
                    animationSpec = tween(
                        durationMillis = AnimationSpecs.NORMAL,
                        easing = AnimationSpecs.EmphasizedEasing
                    )
                )
            }
        ) {
            val dashboardViewModel: DashboardViewModel = hiltViewModel()
            OnboardingScreen(
                onComplete = {
                    dashboardViewModel.completeOnboarding()
                    navController.navigate("dashboard") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        // H-1 / M-7: Open Source Licenses screen — was unreachable (showComingSoonDialog)
        composable(route = "licenses") {
            LicensesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // M-003: SimplifiedModeScreen route
        composable(route = "simplified_mode") {
            com.obsidianbackup.ui.screens.SimplifiedModeScreen()
        }

        composable(route = "busybox_options") {
            com.obsidianbackup.ui.screens.BusyBoxOptionsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = "permission_mode") {
            com.obsidianbackup.ui.screens.PermissionModeScreen(
                permissionManager = permissionManager,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = "retention_policies") {
            com.obsidianbackup.ui.screens.RetentionPoliciesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = "storage_limits") {
            com.obsidianbackup.ui.screens.StorageLimitsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // M-002: HealthPrivacyScreen route
        composable(route = "health_privacy") {
            com.obsidianbackup.health.HealthPrivacyScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // H-001 partial: Version Info screen (replaces Coming Soon)
        composable(route = "version_info") {
            com.obsidianbackup.ui.screens.VersionInfoScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun FeatureFlagsUnavailableScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            androidx.compose.material3.Text(
                text = "Feature Flags",
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
            )
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.padding(vertical = androidx.compose.ui.unit.Dp(8f))
            )
            androidx.compose.material3.Text(
                text = "Requires Firebase Remote Config to be configured."
            )
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.padding(vertical = androidx.compose.ui.unit.Dp(16f))
            )
            androidx.compose.material3.TextButton(onClick = onBack) {
                androidx.compose.material3.Text("Go Back")
            }
        }
    }
}
