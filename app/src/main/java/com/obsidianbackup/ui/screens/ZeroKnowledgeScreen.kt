// ui/screens/ZeroKnowledgeScreen.kt
package com.obsidianbackup.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.obsidianbackup.crypto.ZeroKnowledgeManager
import com.obsidianbackup.crypto.PrivacyAuditResult
import kotlinx.coroutines.launch

/**
 * Zero-Knowledge Encryption Settings Screen
 * 
 * Allows users to:
 * - Enable/configure zero-knowledge mode
 * - Export/import key backups
 * - Configure local-only mode
 * - Run privacy audits
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZeroKnowledgeScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val zkManager = remember { ZeroKnowledgeManager(context) }
    
    var zkEnabled by remember { mutableStateOf(false) }
    var zkUnlocked by remember { mutableStateOf(false) }
    var zkConfig by remember { mutableStateOf<com.obsidianbackup.crypto.ZeroKnowledgeConfig?>(null) }
    var auditResult by remember { mutableStateOf<PrivacyAuditResult?>(null) }
    
    var showSetupDialog by remember { mutableStateOf(false) }
    var showUnlockDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showWarningDialog by remember { mutableStateOf(false) }
    
    // Load configuration
    LaunchedEffect(Unit) {
        zkEnabled = zkManager.isEnabled()
        zkUnlocked = zkManager.isUnlocked()
        zkConfig = zkManager.getConfig()
        
        if (zkEnabled) {
            auditResult = zkManager.getLastAuditResult()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Card
        ZeroKnowledgeStatusCard(
            enabled = zkEnabled,
            unlocked = zkUnlocked,
            config = zkConfig
        )
        
        if (!zkEnabled) {
            // Setup Section
            ZeroKnowledgeSetupSection(
                onSetup = { showSetupDialog = true },
                onImport = { showImportDialog = true },
                onShowWarning = { showWarningDialog = true }
            )
        } else {
            // Configuration Section
            if (!zkUnlocked) {
                UnlockSection(onUnlock = { showUnlockDialog = true })
            } else {
                ConfigurationSection(
                    config = zkConfig,
                    auditResult = auditResult,
                    onExportKey = { showExportDialog = true },
                    onLock = {
                        zkManager.lock()
                        zkUnlocked = false
                    },
                    onToggleLocalOnly = { enabled ->
                        scope.launch {
                            zkManager.setLocalOnlyMode(enabled)
                            zkConfig = zkManager.getConfig()
                        }
                    },
                    onToggleSearchIndex = { enabled ->
                        scope.launch {
                            zkManager.setSearchIndexEnabled(enabled)
                            zkConfig = zkManager.getConfig()
                        }
                    },
                    onRunAudit = {
                        scope.launch {
                            auditResult = zkManager.performPrivacyAudit()
                        }
                    }
                )
            }
        }
    }
    
    // Dialogs
    if (showSetupDialog) {
        SetupDialog(
            onDismiss = { showSetupDialog = false },
            onConfirm = { passphrase ->
                scope.launch {
                    zkManager.initializeZeroKnowledge(passphrase)
                        .onSuccess {
                            zkEnabled = true
                            zkUnlocked = true
                            zkConfig = zkManager.getConfig()
                            showSetupDialog = false
                        }
                        .onFailure {
                            // Show error
                        }
                }
            }
        )
    }
    
    if (showUnlockDialog) {
        UnlockDialog(
            onDismiss = { showUnlockDialog = false },
            onConfirm = { passphrase ->
                scope.launch {
                    zkManager.unlockWithPassphrase(passphrase)
                        .onSuccess {
                            zkUnlocked = true
                            showUnlockDialog = false
                        }
                        .onFailure {
                            // Show error
                        }
                }
            }
        )
    }
    
    if (showExportDialog) {
        ExportKeyDialog(
            onDismiss = { showExportDialog = false },
            onExport = { backupPassphrase ->
                scope.launch {
                    zkManager.exportKeyBackup(backupPassphrase)
                        .onSuccess { backup ->
                            // Show backup string to user
                            showExportDialog = false
                        }
                        .onFailure {
                            // Show error
                        }
                }
            }
        )
    }
    
    if (showImportDialog) {
        ImportKeyDialog(
            onDismiss = { showImportDialog = false },
            onImport = { backupData, backupPassphrase ->
                scope.launch {
                    zkManager.importKeyBackup(backupData, backupPassphrase)
                        .onSuccess {
                            zkEnabled = true
                            zkUnlocked = true
                            zkConfig = zkManager.getConfig()
                            showImportDialog = false
                        }
                        .onFailure {
                            // Show error
                        }
                }
            }
        )
    }
    
    if (showWarningDialog) {
        KeyLossWarningDialog(
            onDismiss = { showWarningDialog = false },
            onAccept = {
                showWarningDialog = false
                showSetupDialog = true
            }
        )
    }
}

@Composable
private fun ZeroKnowledgeStatusCard(
    enabled: Boolean,
    unlocked: Boolean,
    config: com.obsidianbackup.crypto.ZeroKnowledgeConfig?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled && unlocked) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (enabled && unlocked) {
                        Icons.Default.Lock
                    } else {
                        Icons.Default.LockOpen
                    },
                    contentDescription = null,
                    tint = if (enabled && unlocked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                Text(
                    text = when {
                        !enabled -> "Zero-Knowledge: Disabled"
                        !unlocked -> "Zero-Knowledge: Locked"
                        else -> "Zero-Knowledge: Active"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (enabled && config != null) {
                HorizontalDivider()
                
                StatusRow("Backup Exported", config.keyBackupExported)
                StatusRow("Local-Only Mode", config.localOnlyMode)
                StatusRow("Search Index", config.searchIndexEnabled)
                StatusRow("Privacy Audit", config.privacyAuditEnabled)
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, enabled: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Icon(
            imageVector = if (enabled) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
        )
    }
}

@Composable
private fun ZeroKnowledgeSetupSection(
    onSetup: () -> Unit,
    onImport: () -> Unit,
    onShowWarning: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Get Started",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Enable zero-knowledge encryption for maximum privacy. " +
                      "Your encryption keys never leave your device.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Button(
                onClick = onShowWarning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Lock, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Enable Zero-Knowledge Mode")
            }
            
            OutlinedButton(
                onClick = onImport,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Upload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Import Key Backup")
            }
        }
    }
}

@Composable
private fun UnlockSection(onUnlock: () -> Unit) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Zero-Knowledge Mode Locked",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Enter your passphrase to unlock encryption",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Button(
                onClick = onUnlock,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Unlock")
            }
        }
    }
}

@Composable
private fun ConfigurationSection(
    config: com.obsidianbackup.crypto.ZeroKnowledgeConfig?,
    auditResult: PrivacyAuditResult?,
    onExportKey: () -> Unit,
    onLock: () -> Unit,
    onToggleLocalOnly: (Boolean) -> Unit,
    onToggleSearchIndex: (Boolean) -> Unit,
    onRunAudit: () -> Unit
) {
    // Key Management
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Key Management",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedButton(
                onClick = onExportKey,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Export Key Backup")
            }
            
            if (config?.keyBackupExported == false) {
                Text(
                    text = "⚠️ Backup not exported - Key loss = Data loss!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            OutlinedButton(
                onClick = onLock,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.LockOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Lock")
            }
        }
    }
    
    // Privacy Settings
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Privacy Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Local-Only Mode", fontWeight = FontWeight.Medium)
                    Text(
                        "Never sync to cloud",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = config?.localOnlyMode ?: false,
                    onCheckedChange = onToggleLocalOnly
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Searchable Encryption", fontWeight = FontWeight.Medium)
                    Text(
                        "Enable encrypted search",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = config?.searchIndexEnabled ?: true,
                    onCheckedChange = onToggleSearchIndex
                )
            }
        }
    }
    
    // Privacy Audit
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Privacy Audit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (auditResult != null) {
                AuditResultDisplay(auditResult)
            }
            
            Button(
                onClick = onRunAudit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Security, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Run Privacy Audit")
            }
        }
    }
}

@Composable
private fun AuditResultDisplay(result: PrivacyAuditResult) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = if (result.passed) "✅ Audit Passed" else "⚠️ Warnings Found",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (result.passed) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
        )
        
        result.warnings.forEach { warning ->
            Text(
                text = "• $warning",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun SetupDialog(
    onDismiss: () -> Unit,
    onConfirm: (CharArray) -> Unit
) {
    var passphrase by remember { mutableStateOf("") }
    var confirmPassphrase by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Master Passphrase") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose a strong passphrase. You'll need this to access your encrypted backups.")
                
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Master Passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = confirmPassphrase,
                    onValueChange = { confirmPassphrase = it },
                    label = { Text("Confirm Passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (passphrase == confirmPassphrase && passphrase.isNotEmpty()) {
                        onConfirm(passphrase.toCharArray())
                    }
                },
                enabled = passphrase == confirmPassphrase && passphrase.length >= 12
            ) {
                Text("Setup")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun UnlockDialog(
    onDismiss: () -> Unit,
    onConfirm: (CharArray) -> Unit
) {
    var passphrase by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unlock Zero-Knowledge Mode") },
        text = {
            OutlinedTextField(
                value = passphrase,
                onValueChange = { passphrase = it },
                label = { Text("Master Passphrase") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(passphrase.toCharArray()) },
                enabled = passphrase.isNotEmpty()
            ) {
                Text("Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ExportKeyDialog(
    onDismiss: () -> Unit,
    onExport: (CharArray) -> Unit
) {
    var backupPassphrase by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Key Backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Set a passphrase for your key backup. Store it securely!")
                
                OutlinedTextField(
                    value = backupPassphrase,
                    onValueChange = { backupPassphrase = it },
                    label = { Text("Backup Passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onExport(backupPassphrase.toCharArray()) },
                enabled = backupPassphrase.length >= 12
            ) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ImportKeyDialog(
    onDismiss: () -> Unit,
    onImport: (String, CharArray) -> Unit
) {
    var backupData by remember { mutableStateOf("") }
    var backupPassphrase by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Key Backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = backupData,
                    onValueChange = { backupData = it },
                    label = { Text("Backup Data") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                OutlinedTextField(
                    value = backupPassphrase,
                    onValueChange = { backupPassphrase = it },
                    label = { Text("Backup Passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(backupData, backupPassphrase.toCharArray()) },
                enabled = backupData.isNotEmpty() && backupPassphrase.isNotEmpty()
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun KeyLossWarningDialog(
    onDismiss: () -> Unit,
    onAccept: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null) },
        title = { Text("⚠️ Critical Warning") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Zero-Knowledge Encryption Consequences:",
                    fontWeight = FontWeight.Bold
                )
                Text("• Your encryption keys NEVER leave your device")
                Text("• ObsidianBackup CANNOT recover your keys")
                Text("• Lost passphrase = Lost data FOREVER")
                Text("• No account recovery possible")
                Text("• YOU are solely responsible for key backup")
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    text = "This is TRUE zero-knowledge security.",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("I Understand")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
