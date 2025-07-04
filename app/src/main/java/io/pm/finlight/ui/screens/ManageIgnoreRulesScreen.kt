// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/ManageIgnoreRulesScreen.kt
// REASON: NEW FILE - This screen provides the user interface for managing the
// SMS parser's ignore list. It allows users to view all existing ignore phrases,
// add new ones via a text field, and delete unwanted phrases from the list.
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
            "Add phrases that, if found in an SMS, will cause the message to be ignored by the parser. This is useful for filtering out non-transactional messages like OTPs or ads.",
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
                label = { Text("Phrase to ignore") },
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
        HorizontalDivider()

        if (rules.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No ignore rules defined yet.")
            }
        } else {
            LazyColumn {
                items(rules, key = { it.id }) { rule ->
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
