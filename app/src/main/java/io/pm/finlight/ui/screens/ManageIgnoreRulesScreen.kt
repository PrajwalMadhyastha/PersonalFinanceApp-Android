// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/ManageIgnoreRulesScreen.kt
// REASON: FEATURE - The UI has been enhanced to differentiate between default
// and user-added ignore rules. Default rules are now displayed with a toggle
// Switch to enable/disable them, while user-added rules have a delete button,
// providing a more intuitive and powerful management interface.
// BUG FIX - Corrected the function call to `updateIgnoreRule` to match the
// function name in the ViewModel, resolving the persistent compilation error.
// =================================================================================
package io.pm.finlight.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.pm.finlight.IgnoreRule
import io.pm.finlight.ManageIgnoreRulesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageIgnoreRulesScreen(
    viewModel: ManageIgnoreRulesViewModel = viewModel()
) {
    val rules by viewModel.allRules.collectAsState()
    var newPhrase by remember { mutableStateOf("") }
    var ruleToDelete by remember { mutableStateOf<IgnoreRule?>(null) }

    val (defaultRules, customRules) = rules.partition { it.isDefault }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Manage Ignore Phrases",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            "Add phrases that, if found in an SMS, will cause the message to be ignored by the parser. You can also toggle the app's default ignore rules.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newPhrase,
                onValueChange = { newPhrase = it },
                label = { Text("Add custom phrase to ignore") },
                modifier = Modifier.weight(1f)
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

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            if (customRules.isNotEmpty()) {
                item {
                    Text("Your Custom Rules", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
                }
                items(customRules, key = { "custom-${it.id}" }) { rule ->
                    ListItem(
                        headlineContent = { Text(rule.phrase) },
                        trailingContent = {
                            IconButton(onClick = { ruleToDelete = rule }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete rule",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }

            if (defaultRules.isNotEmpty()) {
                item {
                    Text("Default App Rules", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
                }
                items(defaultRules, key = { "default-${it.id}" }) { rule ->
                    ListItem(
                        headlineContent = { Text(rule.phrase) },
                        trailingContent = {
                            Switch(
                                checked = rule.isEnabled,
                                onCheckedChange = { isEnabled ->
                                    viewModel.updateIgnoreRule(rule.copy(isEnabled = isEnabled))
                                }
                            )
                        }
                    )
                    HorizontalDivider()
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
            }
        )
    }
}
