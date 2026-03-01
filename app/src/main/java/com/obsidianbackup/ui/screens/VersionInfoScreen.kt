package com.obsidianbackup.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.obsidianbackup.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionInfoScreen(onNavigateBack: () -> Unit) {
    Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text("Version", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(BuildConfig.VERSION_NAME, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(12.dp))

            Text("Version Code", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(BuildConfig.VERSION_CODE.toString(), style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(12.dp))

            Text("Build Type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(BuildConfig.BUILD_TYPE, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(12.dp))

            Text("Application ID", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(BuildConfig.APPLICATION_ID, style = MaterialTheme.typography.bodyLarge)
        }
}
