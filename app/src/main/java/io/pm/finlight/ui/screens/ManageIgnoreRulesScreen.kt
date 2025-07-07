// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/ManageIgnoreRulesScreen.kt
// REASON: MAJOR REFACTOR - The screen has been fully redesigned to align with the
// "Project Aurora" vision. All list items and input fields are now housed in
// GlassPanel components.
// BUG FIX: All text and component colors have been explicitly set using
// MaterialTheme.colorScheme to ensure high contrast and legibility in dark
// mode, resolving the visibility issues.
// =================================================================================
package io.pm.finlight.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.pm.finlight.IgnoreRule
import io.pm.finlight.ManageIgnoreRulesViewModel
import io.pm.finlight.ui.components.GlassPanel
import io.pm.finlight.ui.theme.PopupSurfaceDark
import io.pm.finlight.ui.theme.PopupSurfaceLight

@Composable
fun ManageIgnoreRulesScreen(
    viewModel: ManageIgnoreRulesViewModel = viewModel()
) {
    val rules by viewModel.allRules.collectAsState()
    var newPhrase by remember { mutableStateOf("") }
    var ruleToDelete by remember { mutableStateOf<IgnoreRule?>(null) }

    val (defaultRules, customRules) = rules.partition { it.isDefault }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    "Manage Ignore Phrases",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Add phrases to ignore messages from the SMS parser. You can also toggle the app's default rules.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            GlassPanel {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newPhrase,
                        onValueChange = { newPhrase = it },
                        label = { Text("Add custom phrase") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                        )
                    )
                    Button(
                        onClick = {
                            viewModel.addIgnoreRule(newPhrase)
                            newPhrase = "" // Clear input
                        },
                        enabled = newPhrase.isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Phrase")
                    }
                }
            }
        }

        if (customRules.isNotEmpty()) {
            item {
                Text(
                    "Your Custom Rules",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(customRules, key = { "custom-${it.id}" }) { rule ->
                GlassPanel {
                    ListItem(
                        headlineContent = { Text(rule.phrase, color = MaterialTheme.colorScheme.onSurface) },
                        trailingContent = {
                            IconButton(onClick = { ruleToDelete = rule }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete rule",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }

        if (defaultRules.isNotEmpty()) {
            item {
                Text(
                    "Default App Rules",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(defaultRules, key = { "default-${it.id}" }) { rule ->
                GlassPanel {
                    ListItem(
                        headlineContent = { Text(rule.phrase, color = MaterialTheme.colorScheme.onSurface) },
                        trailingContent = {
                            Switch(
                                checked = rule.isEnabled,
                                onCheckedChange = { isEnabled ->
                                    viewModel.updateIgnoreRule(rule.copy(isEnabled = isEnabled))
                                }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }

    if (ruleToDelete != null) {
        AlertDialog(
            onDismissRequest = { ruleToDelete = null },
            title = { Text("Delete Ignore Phrase?") },
            text = { Text("Are you sure you want to delete the phrase \"${ruleToDelete!!.phrase}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteIgnoreRule(ruleToDelete!!)
                        ruleToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { ruleToDelete = null }) { Text("Cancel") }
            },
            containerColor = if (isSystemInDarkTheme()) PopupSurfaceDark else PopupSurfaceLight
        )
    }
}
