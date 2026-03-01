// ui/components/PermissionGate.kt
package com.obsidianbackup.ui.components

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.obsidianbackup.ui.theme.ObsidianColors
import com.obsidianbackup.ui.theme.Spacing

/**
 * Gates content behind one or more runtime permissions.
 *
 * - If all [permissions] are already granted, [content] is shown immediately.
 * - Otherwise a rationale card is shown with a "Grant Permission" button.
 * - After a permanent denial the card switches to an "Open Settings" button.
 *
 * Usage:
 * ```
 * PermissionGate(
 *     permissions = listOf(Manifest.permission.READ_CONTACTS),
 *     rationaleTitle = "Contacts Access",
 *     rationaleMessage = "Required to back up your contacts."
 * ) {
 *     ContactsBackupContent()
 * }
 * ```
 */
@Composable
fun PermissionGate(
    permissions: List<String>,
    rationaleTitle: String,
    rationaleMessage: String,
    rationaleIcon: ImageVector = Icons.Default.Lock,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    fun checkAllGranted() = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    var allGranted by remember { mutableStateOf(checkAllGranted()) }
    var permanentlyDenied by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        allGranted = results.values.all { it }
        if (!allGranted) {
            // If any result comes back false, the user either denied or permanently denied.
            // We can't distinguish without checking shouldShowRequestPermissionRationale,
            // but for simplicity we show "Open Settings" after any denial.
            permanentlyDenied = results.any { (_, granted) -> !granted }
        }
    }

    if (allGranted) {
        content()
    } else {
        PermissionRationaleCard(
            icon = rationaleIcon,
            title = rationaleTitle,
            message = rationaleMessage,
            permanentlyDenied = permanentlyDenied,
            onGrant = { launcher.launch(permissions.toTypedArray()) },
            onOpenSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )
    }
}

@Composable
private fun PermissionRationaleCard(
    icon: ImageVector,
    title: String,
    message: String,
    permanentlyDenied: Boolean,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.md),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = ObsidianColors.MoltenOrange
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (permanentlyDenied) {
                    OutlinedButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(Spacing.xs))
                        Text("Open App Settings")
                    }
                    Text(
                        text = "Permission permanently denied. Enable it in App Settings to continue.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Button(onClick = onGrant) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(Spacing.xs))
                        Text("Grant Permission")
                    }
                }
            }
        }
    }
}
