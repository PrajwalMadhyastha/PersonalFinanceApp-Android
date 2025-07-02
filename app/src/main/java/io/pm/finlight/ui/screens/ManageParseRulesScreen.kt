// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/ManageParseRulesScreen.kt
// REASON: UX IMPROVEMENT - The UI is updated to display the user-friendly example
// text (e.g., "Merchant Name: STARBUCKS") instead of the complex regex pattern.
// This makes the screen much more intuitive and understandable for all users.
// =================================================================================
package io.pm.finlight.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.pm.finlight.CustomSmsRule
import io.pm.finlight.ManageParseRulesViewModel

@Composable
fun ManageParseRulesScreen(
    viewModel: ManageParseRulesViewModel = viewModel()
) {
    val rules by viewModel.allRules.collectAsState()
    var ruleToDelete by remember { mutableStateOf<CustomSmsRule?>(null) }

    if (rules.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No custom parsing rules have been created yet.")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(rules) { rule ->
                RuleItemCard(
                    rule = rule,
                    onDeleteClick = { ruleToDelete = rule }
                )
            }
        }
    }

    if (ruleToDelete != null) {
        AlertDialog(
            onDismissRequest = { ruleToDelete = null },
            title = { Text("Delete Rule?") },
            text = { Text("Are you sure you want to delete this parsing rule? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRule(ruleToDelete!!)
                        ruleToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { ruleToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RuleItemCard(rule: CustomSmsRule, onDeleteClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rule #${rule.id}",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Rule",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            HorizontalDivider()
            RuleDetailRow(label = "Trigger Phrase", value = rule.triggerPhrase)
            // --- UPDATED: Display user-friendly examples instead of regex ---
            rule.merchantNameExample?.let {
                RuleDetailRow(label = "Merchant Name", value = it)
            }
            rule.amountExample?.let {
                RuleDetailRow(label = "Amount", value = it)
            }
        }
    }
}

@Composable
private fun RuleDetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
