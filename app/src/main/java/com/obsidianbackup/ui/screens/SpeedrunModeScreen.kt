// ui/screens/SpeedrunModeScreen.kt
package com.obsidianbackup.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.obsidianbackup.gaming.models.SaveState
import com.obsidianbackup.gaming.models.SpeedrunProfile
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedrunModeScreen(
    viewModel: SpeedrunModeViewModel
) {
    val profiles by viewModel.profiles.collectAsState()
    val currentProfile by viewModel.currentProfile.collectAsState()
    val scope = rememberCoroutineScope()
    
    var showCreateProfileDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Speedrun Mode") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            if (currentProfile != null) {
                FloatingActionButton(
                    onClick = { 
                        scope.launch { viewModel.createQuickSave() }
                    }
                ) {
                    Icon(Icons.Default.Save, "Quick Save")
                }
            } else {
                ExtendedFloatingActionButton(
                    onClick = { showCreateProfileDialog = true },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("New Profile") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (currentProfile == null) {
                // Profile selection
                if (profiles.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Speedrun Mode",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                "Create a profile to start quick-saving",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = { showCreateProfileDialog = true }) {
                                Icon(Icons.Default.Add, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Create Profile")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                "Select a profile",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                        items(profiles, key = { it.hashCode() }) { profile ->
                            ProfileCard(
                                profile = profile,
                                onClick = { viewModel.selectProfile(profile) }
                            )
                        }
                    }
                }
            } else {
                // Active profile view
                Column(modifier = Modifier.fillMaxSize()) {
                    // Profile header
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    currentProfile!!.gameName,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${currentProfile!!.saveStates.size}/${currentProfile!!.maxSaveStates} save states",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            IconButton(onClick = { viewModel.closeProfile() }) {
                                Icon(Icons.Default.Close, "Close profile")
                            }
                        }
                    }
                    
                    // Quick action buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { scope.launch { viewModel.createQuickSave() } },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Quick Save")
                        }
                        
                        OutlinedButton(
                            onClick = { /* Export */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Upload, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Export")
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Save states list
                    Text(
                        "Save States",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(currentProfile!!.saveStates) { saveState ->
                            SaveStateCard(
                                saveState = saveState,
                                onLoad = { scope.launch { viewModel.loadSaveState(saveState) } },
                                onDelete = { scope.launch { viewModel.deleteSaveState(saveState) } }
                            )
                        }
                        
                        if (currentProfile!!.saveStates.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "No save states yet. Tap the + button to create one!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showCreateProfileDialog) {
        CreateProfileDialog(
            onDismiss = { showCreateProfileDialog = false },
            onConfirm = { gameName, maxStates ->
                scope.launch {
                    viewModel.createProfile(gameName, maxStates)
                    showCreateProfileDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCard(
    profile: SpeedrunProfile,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    profile.gameName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${profile.saveStates.size} save states",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Last used: ${dateFormat.format(Date(profile.lastUsed))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SaveStateCard(
    saveState: SaveState,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    saveState.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    dateFormat.format(Date(saveState.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onLoad) {
                    Icon(
                        Icons.Default.PlayArrow,
                        "Load",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun CreateProfileDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var gameName by remember { mutableStateOf("") }
    var maxStates by remember { mutableStateOf(10) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Speedrun Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = gameName,
                    onValueChange = { gameName = it },
                    label = { Text("Game Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Text("Max Save States: $maxStates")
                Slider(
                    value = maxStates.toFloat(),
                    onValueChange = { maxStates = it.toInt() },
                    valueRange = 5f..50f,
                    steps = 8
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(gameName, maxStates) },
                enabled = gameName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ViewModel interface
interface SpeedrunModeViewModel {
    val profiles: StateFlow<List<SpeedrunProfile>>
    val currentProfile: StateFlow<SpeedrunProfile?>
    suspend fun createProfile(gameName: String, maxStates: Int)
    fun selectProfile(profile: SpeedrunProfile)
    fun closeProfile()
    suspend fun createQuickSave()
    suspend fun loadSaveState(saveState: SaveState)
    suspend fun deleteSaveState(saveState: SaveState)
}
