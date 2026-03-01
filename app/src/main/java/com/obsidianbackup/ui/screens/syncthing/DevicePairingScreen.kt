// ui/screens/syncthing/DevicePairingScreen.kt
package com.obsidianbackup.ui.screens.syncthing

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.obsidianbackup.sync.SyncthingManager
import kotlinx.coroutines.launch

/**
 * Device pairing screen with QR code and manual device ID entry
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicePairingScreen(
    syncthingManager: SyncthingManager,
    onNavigateBack: () -> Unit
) {
    var showQRCode by remember { mutableStateOf(true) }
    var deviceId by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf("") }
    var isAdding by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pair Device") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tab selector
            TabRow(selectedTabIndex = if (showQRCode) 0 else 1) {
                Tab(
                    selected = showQRCode,
                    onClick = { showQRCode = true },
                    text = { Text("Show QR Code") }
                )
                Tab(
                    selected = !showQRCode,
                    onClick = { showQRCode = false },
                    text = { Text("Manual Entry") }
                )
            }
            
            if (showQRCode) {
                // QR Code Display
                QRCodeSection(syncthingManager)
            } else {
                // Manual Entry
                ManualEntrySection(
                    deviceId = deviceId,
                    deviceName = deviceName,
                    onDeviceIdChange = { deviceId = it },
                    onDeviceNameChange = { deviceName = it },
                    isAdding = isAdding,
                    errorMessage = errorMessage,
                    onAddDevice = {
                        scope.launch {
                            isAdding = true
                            errorMessage = null
                            
                            val result = syncthingManager.addDevice(
                                deviceId = deviceId.trim(),
                                name = deviceName.trim()
                            )
                            
                            result.onSuccess {
                                onNavigateBack()
                            }.onFailure { e ->
                                errorMessage = e.message ?: "Failed to add device"
                            }
                            
                            isAdding = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun QRCodeSection(syncthingManager: SyncthingManager) {
    val qrCodeData = remember {
        try {
            syncthingManager.generatePairingQRCode()
        } catch (e: Exception) {
            ""
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Scan this QR code from another device",
                style = MaterialTheme.typography.titleMedium
            )
            
            if (qrCodeData.isNotEmpty()) {
                val bitmap = remember(qrCodeData) {
                    generateQRCode(qrCodeData, 512)
                }
                
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "QR Code for device pairing",
                        modifier = Modifier
                            .size(300.dp)
                            .padding(8.dp)
                    )
                }
            } else {
                Text(
                    text = "QR code generation failed. Please use manual entry.",
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            HorizontalDivider()
            
            Text(
                text = "Or have the other device scan YOUR QR code",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ManualEntrySection(
    deviceId: String,
    deviceName: String,
    onDeviceIdChange: (String) -> Unit,
    onDeviceNameChange: (String) -> Unit,
    isAdding: Boolean,
    errorMessage: String?,
    onAddDevice: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Enter Device Information",
            style = MaterialTheme.typography.titleMedium
        )
        
        OutlinedTextField(
            value = deviceId,
            onValueChange = onDeviceIdChange,
            label = { Text("Device ID") },
            placeholder = { Text("7-character device ID") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isAdding,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            singleLine = true
        )
        
        OutlinedTextField(
            value = deviceName,
            onValueChange = onDeviceNameChange,
            label = { Text("Device Name") },
            placeholder = { Text("My Phone") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isAdding,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { onAddDevice() }
            ),
            singleLine = true
        )
        
        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        
        Button(
            onClick = onAddDevice,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isAdding && deviceId.isNotBlank() && deviceName.isNotBlank()
        ) {
            if (isAdding) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Device")
            }
        }
        
        HorizontalDivider()
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "How to find Device ID",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "On the other device, open Syncthing settings and look for the Device ID. It's a 7-character code (e.g., ABCD123).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Generate QR code bitmap
 */
private fun generateQRCode(content: String, size: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(
                    x, y,
                    if (bitMatrix.get(x, y)) android.graphics.Color.BLACK
                    else android.graphics.Color.WHITE
                )
            }
        }
        
        bitmap
    } catch (e: Exception) {
        null
    }
}
