// FILE: app/src/main/java/io/pm/finlight/ui/screens/TagManagementScreen.kt

package io.pm.finlight.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.pm.finlight.Tag
import io.pm.finlight.TagViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagementScreen(viewModel: TagViewModel = viewModel()) {
    val tags by viewModel.allTags.collectAsState()
    var newTagName by remember { mutableStateOf("") }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedTag by remember { mutableStateOf<Tag?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // This listens for one-time events from the ViewModel
    LaunchedEffect(key1 = viewModel.uiEvent) {
        viewModel.uiEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    label = { Text("New Tag Name") },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        viewModel.addTag(newTagName)
                        newTagName = "" // Clear input field
                    },
                    enabled = newTagName.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Tag")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()

            if (tags.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tags created yet. Add one above!")
                }
            } else {
                LazyColumn {
                    items(tags) { tag ->
                        ListItem(
                            headlineContent = { Text(tag.name) },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = {
                                        selectedTag = tag
                                        showEditDialog = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Tag")
                                    }
                                    IconButton(onClick = {
                                        selectedTag = tag
                                        showDeleteDialog = true
                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Tag",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    // --- DIALOGS ---

    if (showEditDialog && selectedTag != null) {
        EditTagDialog(
            tag = selectedTag!!,
            onDismiss = { showEditDialog = false },
            onConfirm = { updatedTag ->
                viewModel.updateTag(updatedTag)
                showEditDialog = false
            }
        )
    }

    if (showDeleteDialog && selectedTag != null) {
        DeleteTagDialog(
            tag = selectedTag!!,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                viewModel.deleteTag(it)
                showDeleteDialog = false
            }
        )
    }
}


@Composable
private fun EditTagDialog(
    tag: Tag,
    onDismiss: () -> Unit,
    onConfirm: (Tag) -> Unit
) {
    var tagName by remember(tag) { mutableStateOf(tag.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Tag") },
        text = {
            OutlinedTextField(
                value = tagName,
                onValueChange = { tagName = it },
                label = { Text("Tag Name") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(tag.copy(name = tagName)) },
                enabled = tagName.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DeleteTagDialog(
    tag: Tag,
    onDismiss: () -> Unit,
    onConfirm: (Tag) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Tag?") },
        text = { Text("Are you sure you want to delete the tag '${tag.name}'?") },
        confirmButton = {
            Button(
                onClick = { onConfirm(tag) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

