// ui/ObsidianBackupApp.kt
package com.obsidianbackup.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.obsidianbackup.R
import com.obsidianbackup.accessibility.rememberAccessibilityState
import com.obsidianbackup.features.FeatureFlagManager
import com.obsidianbackup.model.PermissionMode
import com.obsidianbackup.navigation.Screen
import android.content.Context.MODE_PRIVATE
import com.obsidianbackup.permissions.PermissionManager
import com.obsidianbackup.scanner.AppScanner
import com.obsidianbackup.ui.navigation.NavigationHost
import com.obsidianbackup.ui.theme.ObsidianColors
import com.obsidianbackup.ui.utils.AnimationSpecs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObsidianBackupApp(
    permissionManager: PermissionManager,
    appScanner: AppScanner,
    featureFlagManager: FeatureFlagManager? = null
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route ?: "dashboard"

    val currentMode by permissionManager.currentMode.collectAsState()
    val accessibilityState = rememberAccessibilityState()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Safe Screen initialization — catches class init failures
    val screenItems = remember {
        try { Screen.drawerItems.filterNotNull() } catch (_: Throwable) { emptyList() }
    }
    val currentScreen = remember(currentRoute) {
        try {
            screenItems.find { it.route == currentRoute } ?: Screen.Dashboard
        } catch (_: Throwable) { null }
    }
    // Sub-screen route → display title map (routes not in drawerItems)
    val subScreenTitles = remember {
        mapOf(
            "cloud_providers" to "Cloud Providers",
            "cloud_provider_config/{providerId}" to "Provider Setup",
            "rclone_provider_selection" to "Rclone Provider",
            "filecoin" to "Decentralized Storage",
            "zero_knowledge" to "Zero-Knowledge Encryption",
            "simplified_mode" to "Simplified Mode",
            "smart_scheduling" to "Smart Scheduling",
            "health_privacy" to "Health Privacy",
            "version_info" to "Version Info",
            "licenses" to "Open Source Licenses",
            "feedback" to "Feedback",
            "changelog" to "Changelog",
            "tips" to "Tips & Tricks",
            "onboarding" to "Welcome",
            "feature_flags" to "Feature Flags"
        )
    }
    val currentTitle = remember(currentRoute) {
        subScreenTitles.entries.firstOrNull { currentRoute?.startsWith(it.key.substringBefore("{")) == true }?.value
            ?: currentScreen?.title
            ?: "ObsidianBackup"
    }

    // Section grouping for drawer
    val coreItems = remember { screenItems.filter { it.route in listOf("dashboard", "apps", "backups") } }
    val toolItems = remember { screenItems.filter { it.route in listOf("automation", "gaming", "health", "plugins") } }
    val systemItems = remember { screenItems.filter { it.route in listOf("logs", "settings", "community") } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = ObsidianColors.Surface,
                drawerContentColor = ObsidianColors.TextPrimary
            ) {
                // Branded header with gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0D1B2A),
                                    ObsidianColors.MoltenRed.copy(alpha = 0.15f),
                                    ObsidianColors.MoltenOrange.copy(alpha = 0.08f),
                                    ObsidianColors.Surface
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // App icon
                        Image(
                            painter = painterResource(id = R.mipmap.ic_launcher),
                            contentDescription = null,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                        )
                        Column {
                            Text(
                                "ObsidianBackup",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianColors.TextPrimary
                            )
                            Text(
                                "Root Backup Engine",
                                style = MaterialTheme.typography.bodySmall,
                                color = ObsidianColors.MoltenOrange.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                HorizontalDivider(color = ObsidianColors.Border)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // Core section
                    DrawerSectionLabel("Core")
                    coreItems.forEach { screen ->
                        DrawerNavItem(screen, currentScreen, navController, scope, drawerState)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(
                        color = ObsidianColors.DividerEmber,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Tools section
                    DrawerSectionLabel("Tools")
                    toolItems.forEach { screen ->
                        DrawerNavItem(screen, currentScreen, navController, scope, drawerState)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(
                        color = ObsidianColors.DividerEmber,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // System section
                    DrawerSectionLabel("System")
                    systemItems.forEach { screen ->
                        DrawerNavItem(screen, currentScreen, navController, scope, drawerState)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            currentTitle,
                            modifier = Modifier.semantics { heading() }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        PermissionModeChip(mode = currentMode)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ObsidianColors.Background,
                        titleContentColor = ObsidianColors.TextPrimary,
                        navigationIconContentColor = ObsidianColors.MoltenOrange
                    )
                )
            },
            containerColor = ObsidianColors.Background
        ) { paddingValues ->
        NavigationHost(
            navController = navController,
            permissionManager = permissionManager,
            appScanner = appScanner,
            featureFlagManager = featureFlagManager,
            startDestination = "dashboard",
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
    }
}

@Composable
private fun DrawerSectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = ObsidianColors.TextTertiary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp),
        letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing * 1.5f
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawerNavItem(
    screen: Screen,
    currentScreen: Screen?,
    navController: androidx.navigation.NavHostController,
    scope: kotlinx.coroutines.CoroutineScope,
    drawerState: DrawerState
) {
    val isSelected = currentScreen == screen

    NavigationDrawerItem(
        icon = {
            Icon(
                screen.icon,
                contentDescription = null,
                tint = if (isSelected) ObsidianColors.MoltenOrange
                       else ObsidianColors.TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        },
        label = {
            Text(
                screen.title,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        selected = isSelected,
        onClick = {
            navController.navigate(screen.route) {
                popUpTo("dashboard") { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            scope.launch { drawerState.close() }
        },
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = ObsidianColors.MoltenOrange.copy(alpha = 0.12f),
            unselectedContainerColor = Color.Transparent,
            selectedIconColor = ObsidianColors.MoltenOrange,
            unselectedIconColor = ObsidianColors.TextSecondary,
            selectedTextColor = ObsidianColors.MoltenOrange,
            unselectedTextColor = ObsidianColors.TextPrimary
        ),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 1.dp)
    )
}

@Composable
fun PermissionModeChip(mode: PermissionMode) {
    val (backgroundColor, icon) = when (mode) {
        PermissionMode.ROOT -> Pair(ObsidianColors.RootGranted, Icons.Default.Shield)
        PermissionMode.SHIZUKU -> Pair(ObsidianColors.MoltenAmber, Icons.Default.Shield)
        PermissionMode.ADB -> Pair(ObsidianColors.AccentBlue, Icons.Default.Shield)
        PermissionMode.SAF -> Pair(ObsidianColors.Warning, Icons.Default.Shield)
    }

    val context = LocalContext.current
    val contentDesc = stringResource(
        R.string.cd_permission_mode_indicator,
        mode.displayName
    )

    Surface(
        color = backgroundColor.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .semantics { 
                this.contentDescription = contentDesc
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = backgroundColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = mode.displayName,
                color = backgroundColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
