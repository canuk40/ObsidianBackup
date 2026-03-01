package com.obsidianbackup.deeplink

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.obsidianbackup.ui.theme.ObsidianBackupTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Test activity for deep linking functionality
 * Used for debugging and demonstration purposes
 */
@AndroidEntryPoint
class DeepLinkTestActivity : ComponentActivity() {
    
    @Inject
    lateinit var deepLinkHandler: DeepLinkHandler
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ObsidianBackupTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DeepLinkTestScreen(deepLinkHandler)
                }
            }
        }
    }
}

@Composable
fun DeepLinkTestScreen(handler: DeepLinkHandler) {
    val context = LocalContext.current
    var validationResult by remember { mutableStateOf<DeepLinkValidationResult?>(null) }
    
    val testLinks = remember {
        listOf(
            "obsidianbackup://backup" to "Start backup (all apps)",
            "obsidianbackup://backup?packages=com.example.app1,com.example.app2" to "Backup specific apps",
            "obsidianbackup://restore?snapshot=snapshot_123" to "Restore snapshot",
            "obsidianbackup://settings" to "Open settings",
            "obsidianbackup://settings/automation" to "Open automation settings",
            "obsidianbackup://cloud/connect?provider=webdav" to "Connect WebDAV",
            "obsidianbackup://logs" to "Open logs",
            "obsidianbackup://automation" to "Open automation",
            "obsidianbackup://app?package=com.example.app" to "Open app details",
            "https://obsidianbackup.app/backup" to "App Link - Backup",
            "https://obsidianbackup.app/settings/cloud" to "App Link - Cloud Settings"
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Deep Link Testing",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "Click a link to test deep linking functionality",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(testLinks) { (uri, description) ->
                TestLinkCard(
                    uri = uri,
                    description = description,
                    onTest = {
                        validationResult = handler.validateDeepLink(Uri.parse(uri))
                    },
                    onLaunch = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                        context.startActivity(intent)
                    }
                )
            }
        }
        
        validationResult?.let { result ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (result.valid) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Validation Result",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Valid: ${result.valid}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    result.action?.let {
                        Text(
                            text = "Action: ${it.javaClass.simpleName}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = "Requires Auth: ${result.requiresAuth}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    result.errorMessage?.let {
                        Text(
                            text = "Error: $it",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TestLinkCard(
    uri: String,
    description: String,
    onTest: () -> Unit,
    onLaunch: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = description,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = uri,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onTest,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Validate")
                }
                Button(
                    onClick = onLaunch,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Launch")
                }
            }
        }
    }
}
