// health/HealthPrivacyScreen.kt
package com.obsidianbackup.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthPrivacyScreen(
    onNavigateBack: () -> Unit,
    viewModel: HealthPrivacyViewModel = hiltViewModel()
) {
    val privacySettings by viewModel.privacySettings.collectAsState()
    val backupState by viewModel.backupState.collectAsState()
    val statistics by viewModel.statistics.collectAsState()
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Data Privacy") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Privacy Overview
            item {
                PrivacyOverviewCard(privacySettings)
            }
            
            // Backup Statistics
            item {
                statistics?.let {
                    BackupStatisticsCard(it)
                }
            }
            
            // Data Type Controls
            item {
                Text(
                    text = "Data Types to Backup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(HealthDataType.entries) { dataType ->
                DataTypeControl(
                    dataType = dataType,
                    enabled = privacySettings.isDataTypeEnabled(dataType),
                    onToggle = { enabled ->
                        viewModel.toggleDataType(dataType, enabled)
                    }
                )
            }
            
            // Privacy Settings
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Privacy Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                PrivacySettingsCard(
                    settings = privacySettings,
                    onSettingsChange = { viewModel.updatePrivacySettings(it) }
                )
            }
            
            // Backup Actions
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                BackupActionsCard(
                    backupState = backupState,
                    onBackupAll = {
                        scope.launch {
                            viewModel.backupAllHealthData()
                        }
                    },
                    onExportJSON = {
                        scope.launch {
                            viewModel.exportToJSON()
                        }
                    },
                    onExportCSV = {
                        scope.launch {
                            viewModel.exportToCSV()
                        }
                    },
                    onDeleteAll = {
                        scope.launch {
                            viewModel.deleteAllBackups()
                        }
                    }
                )
            }
            
            // Warning Notice
            item {
                PrivacyNoticeCard()
            }
        }
    }
}

@Composable
fun PrivacyOverviewCard(settings: HealthPrivacySettings) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Privacy Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = "${settings.enabledDataTypes.size} of ${HealthDataType.entries.size} data types enabled",
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (settings.anonymizeData) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Data anonymization enabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            if (settings.retentionDays > 0) {
                Text(
                    text = "Data retention: ${settings.retentionDays} days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BackupStatisticsCard(statistics: HealthBackupStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
                    Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Backup Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Last Backup:", style = MaterialTheme.typography.bodyMedium)
                Text(
                    statistics.lastBackupTime?.toString() ?: "Never",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Size:", style = MaterialTheme.typography.bodyMedium)
                Text(
                    formatBytes(statistics.totalBackupSize),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Records:", style = MaterialTheme.typography.bodyMedium)
                Text(
                    statistics.recordCountsByType.values.sum().toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun DataTypeControl(
    dataType: HealthDataType,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    getDataTypeIcon(dataType),
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column {
                    Text(
                        text = getDataTypeName(dataType),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = getDataTypeDescription(dataType),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Switch(
                checked = enabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
fun PrivacySettingsCard(
    settings: HealthPrivacySettings,
    onSettingsChange: (HealthPrivacySettings) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Anonymize Data",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Remove identifiable information",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.anonymizeData,
                    onCheckedChange = { 
                        onSettingsChange(settings.copy(anonymizeData = it))
                    }
                )
            }
            
            HorizontalDivider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Exclude Sensitive Data",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Skip potentially sensitive health info",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.excludeSensitiveData,
                    onCheckedChange = { 
                        onSettingsChange(settings.copy(excludeSensitiveData = it))
                    }
                )
            }
            
            HorizontalDivider()
            
            Column {
                Text(
                    text = "Data Retention (days)",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (settings.retentionDays == 0) "Keep forever" else "${settings.retentionDays} days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = settings.retentionDays.toFloat(),
                    onValueChange = { 
                        onSettingsChange(settings.copy(retentionDays = it.toInt()))
                    },
                    valueRange = 0f..365f,
                    steps = 11
                )
            }
        }
    }
}

@Composable
fun BackupActionsCard(
    backupState: HealthBackupState,
    onBackupAll: () -> Unit,
    onExportJSON: () -> Unit,
    onExportCSV: () -> Unit,
    onDeleteAll: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (backupState) {
                is HealthBackupState.InProgress -> {
                    LinearProgressIndicator(
                        progress = { backupState.progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Backup in progress: ${backupState.progress}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is HealthBackupState.Error -> {
                    Text(
                        text = "Error: ${backupState.exception.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {}
            }
            
            Button(
                onClick = onBackupAll,
                modifier = Modifier.fillMaxWidth(),
                enabled = backupState !is HealthBackupState.InProgress
            ) {
                Icon(Icons.Default.Backup, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Backup All Health Data")
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onExportJSON,
                    modifier = Modifier.weight(1f),
                    enabled = backupState !is HealthBackupState.InProgress
                ) {
                    Text("Export JSON")
                }
                OutlinedButton(
                    onClick = onExportCSV,
                    modifier = Modifier.weight(1f),
                    enabled = backupState !is HealthBackupState.InProgress
                ) {
                    Text("Export CSV")
                }
            }
            
            OutlinedButton(
                onClick = onDeleteAll,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                enabled = backupState !is HealthBackupState.InProgress
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Delete All Backups")
            }
        }
    }
}

@Composable
fun PrivacyNoticeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "Your health data is stored locally and encrypted. Only you have access to this information. No data is shared with third parties without your explicit consent.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

// Helper Functions
fun getDataTypeIcon(dataType: HealthDataType) = when (dataType) {
    HealthDataType.STEPS -> Icons.AutoMirrored.Filled.DirectionsWalk
    HealthDataType.HEART_RATE -> Icons.Default.Favorite
    HealthDataType.SLEEP -> Icons.Default.Bedtime
    HealthDataType.WORKOUTS -> Icons.Default.FitnessCenter
    HealthDataType.NUTRITION -> Icons.Default.Restaurant
    HealthDataType.BODY_MEASUREMENTS -> Icons.Default.MonitorWeight
}

fun getDataTypeName(dataType: HealthDataType) = when (dataType) {
    HealthDataType.STEPS -> "Steps"
    HealthDataType.HEART_RATE -> "Heart Rate"
    HealthDataType.SLEEP -> "Sleep"
    HealthDataType.WORKOUTS -> "Workouts"
    HealthDataType.NUTRITION -> "Nutrition"
    HealthDataType.BODY_MEASUREMENTS -> "Body Measurements"
}

fun getDataTypeDescription(dataType: HealthDataType) = when (dataType) {
    HealthDataType.STEPS -> "Daily step count and walking activity"
    HealthDataType.HEART_RATE -> "Heart rate measurements and trends"
    HealthDataType.SLEEP -> "Sleep sessions and quality data"
    HealthDataType.WORKOUTS -> "Exercise sessions and activity records"
    HealthDataType.NUTRITION -> "Meal logs and nutritional information"
    HealthDataType.BODY_MEASUREMENTS -> "Weight, height, and body composition"
}

fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    
    return when {
        gb >= 1 -> "%.2f GB".format(gb)
        mb >= 1 -> "%.2f MB".format(mb)
        kb >= 1 -> "%.2f KB".format(kb)
        else -> "$bytes B"
    }
}
