package com.obsidianbackup

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.obsidianbackup.deeplink.DeepLinkAction
import com.obsidianbackup.deeplink.DeepLinkRouter
import com.obsidianbackup.features.FeatureFlagManager
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.permissions.PermissionManager
import com.obsidianbackup.scanner.AppScanner
import com.obsidianbackup.ui.ObsidianBackupApp
import com.obsidianbackup.ui.theme.ObsidianBackupTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var permissionManager: PermissionManager
    
    @Inject
    lateinit var appScanner: AppScanner
    
    @Inject
    lateinit var logger: ObsidianLogger

    @Inject
    lateinit var deepLinkRouter: DeepLinkRouter

    @Inject
    lateinit var featureFlagManager: FeatureFlagManager

    // Launcher must be registered before onCreate
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied — app works either way, notifications just won't appear if denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request POST_NOTIFICATIONS on Android 13+ (non-blocking — app works either way)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Detect best permission mode on startup
        CoroutineScope(Dispatchers.IO).launch {
            permissionManager.detectBestMode()
        }
        
        // Handle deep link extras if present
        handleDeepLinkExtras(intent)

        setContent {
            ObsidianBackupTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ObsidianBackupApp(
                        permissionManager = permissionManager,
                        appScanner = appScanner,
                        featureFlagManager = featureFlagManager
                    )
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLinkExtras(intent)
    }
    
    private fun handleDeepLinkExtras(intent: Intent) {
        val actionType = intent.getStringExtra(DeepLinkRouter.EXTRA_ACTION_TYPE)
        val navigationRoute = intent.getStringExtra(DeepLinkRouter.EXTRA_NAVIGATION_ROUTE)

        if (actionType != null) {
            logger.i("MainActivity", "Deep link received: action=$actionType, route=$navigationRoute")

            // Build the appropriate DeepLinkAction and call DeepLinkRouter.route()
            // DeepLinkRouter is already fully implemented — just call it
            val action: DeepLinkAction? = when (actionType) {
                DeepLinkRouter.ACTION_BACKUP -> DeepLinkAction.StartBackup()
                DeepLinkRouter.ACTION_RESTORE -> {
                    val snapshotId = navigationRoute ?: return
                    DeepLinkAction.RestoreSnapshot(snapshotId = snapshotId)
                }
                DeepLinkRouter.ACTION_NAVIGATE -> DeepLinkAction.OpenDashboard
                else -> null
            }

            if (action != null) {
                deepLinkRouter.route(action, this)
            }
        }
    }
}
