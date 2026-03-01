package com.obsidianbackup.deeplink

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.obsidianbackup.logging.ObsidianLogger

/**
 * Example integration showing how to handle deep link extras in MainActivity
 * 
 * This demonstrates how to process deep link actions that arrive via intents
 * from the DeepLinkActivity after routing.
 */
object DeepLinkIntegration {
    
    /**
     * Process deep link extras from intent
     * Call this from MainActivity.onCreate() or onNewIntent()
     */
    fun processDeepLinkExtras(
        activity: ComponentActivity,
        intent: Intent,
        logger: ObsidianLogger,
        onBackup: (packages: List<String>?, includeData: Boolean, includeApk: Boolean) -> Unit,
        onRestore: (snapshotId: String, packages: List<String>?) -> Unit,
        onNavigate: (route: String) -> Unit
    ) {
        val actionType = intent.getStringExtra(DeepLinkRouter.EXTRA_ACTION_TYPE) ?: return
        val navigationRoute = intent.getStringExtra(DeepLinkRouter.EXTRA_NAVIGATION_ROUTE)
        
        logger.i("DeepLinkIntegration", "Processing action: $actionType")
        
        when (actionType) {
            DeepLinkRouter.ACTION_BACKUP -> handleBackupAction(intent, onBackup)
            DeepLinkRouter.ACTION_RESTORE -> handleRestoreAction(intent, onRestore)
            DeepLinkRouter.ACTION_NAVIGATE -> navigationRoute?.let { onNavigate(it) }
            "cloud_connect" -> handleCloudConnectAction(intent, onNavigate)
            else -> logger.w("DeepLinkIntegration", "Unknown action type: $actionType")
        }
    }
    
    private fun handleBackupAction(
        intent: Intent,
        onBackup: (packages: List<String>?, includeData: Boolean, includeApk: Boolean) -> Unit
    ) {
        val packages = intent.getStringArrayExtra("packages")?.toList()
        val includeData = intent.getBooleanExtra("includeData", true)
        val includeApk = intent.getBooleanExtra("includeApk", true)
        
        onBackup(packages, includeData, includeApk)
    }
    
    private fun handleRestoreAction(
        intent: Intent,
        onRestore: (snapshotId: String, packages: List<String>?) -> Unit
    ) {
        val snapshotId = intent.getStringExtra("snapshotId") ?: return
        val packages = intent.getStringArrayExtra("packages")?.toList()
        
        onRestore(snapshotId, packages)
    }
    
    private fun handleCloudConnectAction(
        intent: Intent,
        onNavigate: (route: String) -> Unit
    ) {
        val provider = intent.getStringExtra("provider")
        val autoConnect = intent.getBooleanExtra("autoConnect", false)
        
        // Navigate to cloud settings with provider pre-selected
        onNavigate("settings/cloud")
        
        // In a real implementation, you would also pass the provider and autoConnect
        // to your cloud settings screen via navigation arguments
    }
    
    private fun handleGamingBackupAction(
        intent: Intent,
        onNavigate: (route: String) -> Unit
    ) {
        val emulator = intent.getStringExtra("emulator")
        
        // Navigate to gaming screen with emulator pre-selected
        onNavigate("gaming")
    }
    
    private fun handleHealthExportAction(
        intent: Intent,
        onNavigate: (route: String) -> Unit
    ) {
        val startDate = intent.getStringExtra("startDate")
        val endDate = intent.getStringExtra("endDate")
        
        // Navigate to health screen and trigger export
        onNavigate("health")
    }
    
    private fun handlePluginInstallAction(
        intent: Intent,
        onNavigate: (route: String) -> Unit
    ) {
        val pluginId = intent.getStringExtra("pluginId")
        
        // Navigate to plugins screen
        onNavigate("plugins")
    }
}

/**
 * Example MainActivity implementation showing deep link integration
 * 
 * Required imports:
 * import androidx.navigation.NavHostController
 * import androidx.navigation.compose.rememberNavController
 */
/*
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var logger: ObsidianLogger
    
    @Inject
    lateinit var backupManager: BackupManager
    
    @Inject
    lateinit var restoreManager: RestoreManager
    
    private lateinit var navController: NavHostController
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ObsidianBackupTheme {
                navController = rememberNavController()
                
                // Your app content
                ObsidianBackupApp(navController)
            }
        }
        
        // Handle deep link extras
        handleDeepLinkIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleDeepLinkIntent(it) }
    }
    
    private fun handleDeepLinkIntent(intent: Intent) {
        DeepLinkIntegration.processDeepLinkExtras(
            activity = this,
            intent = intent,
            logger = logger,
            onBackup = { packages, includeData, includeApk ->
                // Trigger backup
                lifecycleScope.launch {
                    backupManager.startBackup(
                        packageNames = packages,
                        includeData = includeData,
                        includeApk = includeApk
                    )
                }
            },
            onRestore = { snapshotId, packages ->
                // Trigger restore
                lifecycleScope.launch {
                    restoreManager.restoreSnapshot(
                        snapshotId = snapshotId,
                        packageNames = packages
                    )
                }
            },
            onNavigate = { route ->
                // Navigate to screen
                navController.navigate(route)
            }
        )
    }
}
*/

/**
 * Example: Using deep links to share backup actions
 */
/*
@Composable
fun BackupShareButton(packages: List<String>) {
    val context = LocalContext.current
    
    Button(
        onClick = {
            val action = DeepLinkAction.StartBackup(
                packageNames = packages,
                includeData = true,
                includeApk = true
            )
            
            // Option 1: Copy to clipboard
            action.copyToClipboard(context)
            
            // Option 2: Share via Android share sheet
            action.share(context)
            
            // Option 3: Generate QR code
            val qrData = action.toDeepLink()
            // Use qrData to generate QR code image
        }
    ) {
        Text("Share Backup Link")
    }
}
*/

/**
 * Example: Creating a notification with deep link action
 */
/*
fun createBackupNotification(context: Context): Notification {
    val deepLinkIntent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("obsidianbackup://backup")
    }
    
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        deepLinkIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    
    return NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle("Backup Reminder")
        .setContentText("Tap to start backup")
        .setSmallIcon(R.drawable.ic_backup)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()
}
*/

/**
 * Example: Creating a widget with deep link action
 */
/*
class BackupWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("obsidianbackup://backup")
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
            
            val views = RemoteViews(context.packageName, R.layout.widget_backup).apply {
                setOnClickPendingIntent(R.id.backup_button, pendingIntent)
            }
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
*/

/**
 * Example: Generating deep links for automation
 */
/*
fun generateTaskerCommands(): List<String> {
    return listOf(
        // Daily backup
        DeepLinkGenerator.generateBackupLink(),
        
        // Backup specific apps
        DeepLinkGenerator.generateBackupLink(
            packages = listOf("com.whatsapp", "com.telegram")
        ),
        
        // Open settings
        DeepLinkGenerator.generateSettingsLink(SettingsScreen.AUTOMATION),
        
        // Connect to cloud
        DeepLinkGenerator.generateCloudConnectLink(CloudProvider.WEBDAV)
    )
}
*/

/**
 * Example: Email template with deep links
 */
/*
fun generateBackupReminderEmail(snapshotId: String): String {
    val backupLink = DeepLinkGenerator.generateBackupLink()
    val restoreLink = DeepLinkGenerator.generateRestoreLink(snapshotId)
    
    return """
        <html>
        <body>
            <h2>ObsidianBackup Reminder</h2>
            <p>Your backup is ready!</p>
            
            <p>
                <a href="$backupLink">Start New Backup</a>
            </p>
            
            <p>
                <a href="$restoreLink">Restore from Snapshot $snapshotId</a>
            </p>
            
            <p>
                <small>
                    Direct link: $backupLink<br>
                    Web link: ${DeepLinkGenerator.toAppLink(backupLink)}
                </small>
            </p>
        </body>
        </html>
    """.trimIndent()
}
*/
