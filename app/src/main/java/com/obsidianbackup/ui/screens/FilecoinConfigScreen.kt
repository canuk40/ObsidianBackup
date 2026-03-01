// ui/screens/FilecoinConfigScreen.kt
package com.obsidianbackup.ui.screens

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.obsidianbackup.cloud.FilecoinCloudProvider
import com.obsidianbackup.cloud.FilecoinConfig
import com.obsidianbackup.logging.ObsidianLogger
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilecoinConfigScreen(
    logger: ObsidianLogger,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { 
        context.getSharedPreferences("filecoin_config", Context.MODE_PRIVATE) 
    }
    
    var apiToken by remember { 
        mutableStateOf(prefs.getString("web3_storage_token", "") ?: "") 
    }
    var showToken by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf<ConnectionTestResult?>(null) }
    var showStorageCost by remember { mutableStateOf(false) }
    var estimatedCost by remember { mutableStateOf<StorageCostInfo?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("IPFS/Filecoin Setup") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                            Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            "Decentralized Storage",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "Censorship-resistant backups using IPFS and Filecoin",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Benefits Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Why Decentralized?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    BenefitItem(
                        Icons.Default.Security,
                        "Censorship Resistant",
                        "Content-addressed storage that can't be blocked"
                    )
                    BenefitItem(
                        Icons.Default.LockOpen,
                        "No Single Point of Failure",
                        "Data replicated across distributed network"
                    )
                    BenefitItem(
                        Icons.Default.Verified,
                        "Content Integrity",
                        "Cryptographic verification via CID hashes"
                    )
                    BenefitItem(
                        Icons.Default.Public,
                        "Permanent Storage",
                        "Filecoin storage deals ensure long-term persistence"
                    )
                }
            }

            // API Configuration
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "web3.storage Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        "Get your free API token at web3.storage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = apiToken,
                        onValueChange = { apiToken = it },
                        label = { Text("API Token") },
                        leadingIcon = { 
                            Icon(Icons.Default.Key, "API Token") 
                        },
                        trailingIcon = {
                            IconButton(onClick = { showToken = !showToken }) {
                                Icon(
                                    if (showToken) Icons.Default.Visibility 
                                    else Icons.Default.VisibilityOff,
                                    "Toggle visibility"
                                )
                            }
                        },
                        visualTransformation = if (showToken) 
                            VisualTransformation.None 
                        else 
                            PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                // Save token
                                prefs.edit()
                                    .putString("web3_storage_token", apiToken)
                                    .apply()
                                connectionStatus = ConnectionTestResult(
                                    success = true,
                                    message = "Token saved successfully"
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, "Save")
                            Spacer(Modifier.width(4.dp))
                            Text("Save Token")
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isTestingConnection = true
                                    connectionStatus = null
                                    
                                    try {
                                        val config = FilecoinConfig(apiToken)
                                        val provider = FilecoinCloudProvider(context, logger, config)
                                        val result = provider.testConnection()
                                        
                                        connectionStatus = when (result) {
                                            is com.obsidianbackup.cloud.CloudResult.Success -> 
                                                ConnectionTestResult(
                                                    success = true,
                                                    message = "Connected! Latency: ${result.data.latencyMs}ms"
                                                )
                                            is com.obsidianbackup.cloud.CloudResult.Error -> 
                                                ConnectionTestResult(
                                                    success = false,
                                                    message = result.error.message
                                                )
                                        }
                                    } catch (e: Exception) {
                                        connectionStatus = ConnectionTestResult(
                                            success = false,
                                            message = "Test failed: ${e.message}"
                                        )
                                    } finally {
                                        isTestingConnection = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isTestingConnection && apiToken.isNotBlank()
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.CloudSync, "Test")
                            }
                            Spacer(Modifier.width(4.dp))
                            Text("Test")
                        }
                    }

                    // Connection status
                    connectionStatus?.let { status ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (status.success) 
                                    MaterialTheme.colorScheme.primaryContainer
                                else 
                                    MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (status.success) Icons.Default.CheckCircle 
                                    else Icons.Default.Error,
                                    contentDescription = null
                                )
                                Text(status.message)
                            }
                        }
                    }
                }
            }

            // Storage Cost Estimator
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Storage Cost Estimator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Filecoin storage costs are extremely low compared to traditional cloud providers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (showStorageCost) {
                        estimatedCost?.let { cost ->
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CostRow("Storage Size", cost.sizeLabel)
                                CostRow("FIL Cost", "≈${String.format("%.8f", cost.filAmount)} FIL")
                                CostRow("USD Equivalent", "≈$${String.format("%.4f", cost.usdAmount)}")
                                CostRow("Duration", "Permanent")
                                
                                HorizontalDivider()
                                
                                Text(
                                    "💡 Traditional cloud: ~$0.02/GB/month",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "💾 Filecoin: ~$0.000001/GB (one-time)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            showStorageCost = !showStorageCost
                            if (showStorageCost && estimatedCost == null) {
                                // Calculate for 1GB as example
                                val sizeBytes = 1024L * 1024L * 1024L
                                scope.launch {
                                    try {
                                        val config = FilecoinConfig(apiToken)
                                        val provider = FilecoinCloudProvider(context, logger, config)
                                        val result = provider.getStorageCost(sizeBytes)
                                        if (result is com.obsidianbackup.cloud.CloudResult.Success) {
                                            estimatedCost = StorageCostInfo(
                                                sizeLabel = "1 GB",
                                                filAmount = result.data.filAmount,
                                                usdAmount = result.data.usdAmount
                                            )
                                        }
                                    } catch (e: Exception) {
                                        logger.e("FilecoinConfig", "Cost calculation failed", e)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            if (showStorageCost) Icons.Default.ExpandLess 
                            else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (showStorageCost) "Hide Costs" else "Show Costs")
                    }
                }
            }

            // Help Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Help, null)
                        Text(
                            "How to get started",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Text(
                        "1. Visit web3.storage and create a free account\n" +
                        "2. Generate an API token from your dashboard\n" +
                        "3. Paste the token above and test connection\n" +
                        "4. Your backups will be stored on IPFS and Filecoin",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Technical Details
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Technical Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    TechnicalItem("Protocol", "IPFS (InterPlanetary File System)")
                    TechnicalItem("Storage", "Filecoin Network")
                    TechnicalItem("Addressing", "Content-based (CID)")
                    TechnicalItem("Retrieval", "Multi-gateway fallback")
                    TechnicalItem("Verification", "Cryptographic checksums")
                }
            }
        }
    }
}

@Composable
private fun BenefitItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CostRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TechnicalItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

private data class ConnectionTestResult(
    val success: Boolean,
    val message: String
)

private data class StorageCostInfo(
    val sizeLabel: String,
    val filAmount: Double,
    val usdAmount: Double
)
