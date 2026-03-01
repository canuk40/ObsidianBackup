// ui/screens/FeatureFlagsScreen.kt
package com.obsidianbackup.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.obsidianbackup.features.Feature
import com.obsidianbackup.features.FeatureFlagManager
import com.obsidianbackup.features.description
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureFlagsScreen(
    featureFlags: FeatureFlagManager,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val featureStates = remember { mutableStateMapOf<Feature, Boolean>() }

    // Load current feature states
    LaunchedEffect(Unit) {
        Feature.values().forEach { feature ->
            val enabled = featureFlags.isEnabled(feature)
            featureStates[feature] = enabled
        }
    }

    LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Experimental Features",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "These features are experimental and may not work correctly. Use at your own risk.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Feature.values().forEach { feature ->
                item {
                    FeatureFlagItem(
                        feature = feature,
                        enabled = featureStates[feature] ?: false,
                        onToggle = { enabled ->
                            scope.launch {
                                featureFlags.setLocalOverride(feature, enabled)
                                featureStates[feature] = enabled
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            Feature.values().forEach { feature ->
                                featureFlags.clearLocalOverride(feature)
                                val defaultEnabled = featureFlags.isEnabled(feature)
                                featureStates[feature] = defaultEnabled
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset to Defaults")
                }
            }
        }
}

@Composable
fun FeatureFlagItem(
    feature: Feature,
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feature.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = feature.description(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle
            )
        }
    }
}
