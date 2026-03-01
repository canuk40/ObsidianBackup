package com.obsidianbackup.ui.screens.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.obsidianbackup.community.*
import com.obsidianbackup.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    viewModel: FeedbackViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    var showSubmitDialog by remember { mutableStateOf(false) }
    val submissionStatus by viewModel.submissionStatus.collectAsState()
    val feedbackList by viewModel.feedbackList.collectAsState()
    
    // Handle submission status
    LaunchedEffect(submissionStatus) {
        when (submissionStatus) {
            is SubmissionStatus.Success -> {
                showSubmitDialog = false
                viewModel.resetSubmissionStatus()
            }
            else -> {}
        }
    }
    
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showSubmitDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add feedback") },
                text = { Text("Send Feedback") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.md)
                    ) {
                        Text(
                            text = "We Value Your Input",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            text = "Share your ideas, report bugs, or suggest improvements. Your feedback helps make ObsidianBackup better!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            if (feedbackList.isNotEmpty()) {
                item {
                    Text(
                        text = "Your Submissions",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = Spacing.sm)
                    )
                }
                
                items(feedbackList) { feedback ->
                    FeedbackItemCard(feedback)
                }
            }
        }
    }
    
    if (showSubmitDialog) {
        FeedbackDialog(
            onDismiss = { showSubmitDialog = false },
            onSubmit = { type, title, description, email, attachLogs ->
                viewModel.submitFeedback(type, title, description, email, attachLogs)
            },
            submissionStatus = submissionStatus
        )
    }
}

@Composable
fun FeedbackItemCard(feedback: FeedbackItem) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = feedback.title,
                    style = MaterialTheme.typography.titleSmall
                )
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = feedback.type.name.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = feedback.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = "ID: ${feedback.id}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackDialog(
    onDismiss: () -> Unit,
    onSubmit: (FeedbackType, String, String, String?, Boolean) -> Unit,
    submissionStatus: SubmissionStatus
) {
    var selectedType by remember { mutableStateOf(FeedbackType.FEATURE_REQUEST) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var attachLogs by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send Feedback") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                // Type Selector
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedType.name.replace("_", " "),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        FeedbackType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name.replace("_", " ")) },
                                onClick = {
                                    selectedType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = attachLogs,
                        onCheckedChange = { attachLogs = it }
                    )
                    Text("Attach logs for debugging")
                }
                
                if (submissionStatus is SubmissionStatus.Submitting) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                
                if (submissionStatus is SubmissionStatus.Error) {
                    Text(
                        text = submissionStatus.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        selectedType,
                        title,
                        description,
                        email.ifBlank { null },
                        attachLogs
                    )
                },
                enabled = title.isNotBlank() && description.isNotBlank() && submissionStatus !is SubmissionStatus.Submitting
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
