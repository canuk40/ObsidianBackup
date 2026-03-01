package com.obsidianbackup.wear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.obsidianbackup.wear.presentation.screens.BackupScreen
import com.obsidianbackup.wear.presentation.screens.ProgressScreen
import com.obsidianbackup.wear.presentation.screens.StatusScreen
import com.obsidianbackup.wear.presentation.theme.WearAppTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity for Wear OS app
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            WearApp()
        }
    }
}

@Composable
fun WearApp() {
    WearAppTheme {
        val navController = rememberSwipeDismissableNavController()
        
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = "home"
        ) {
            composable("home") {
                BackupScreen(
                    onNavigateToStatus = { navController.navigate("status") },
                    onNavigateToProgress = { navController.navigate("progress") }
                )
            }
            
            composable("status") {
                StatusScreen()
            }
            
            composable("progress") {
                ProgressScreen()
            }
        }
    }
}
