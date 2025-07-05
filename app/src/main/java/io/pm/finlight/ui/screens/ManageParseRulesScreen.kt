// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/ManageParseRulesScreen.kt
// REASON: FEATURE - An "Edit" IconButton has been added to each `RuleItemCard`.
// When tapped, it navigates to the `rule_creation_screen`, passing the ID of
// the selected rule. This allows the user to modify existing rules, completing
// the primary UI entry point for the "Edit Rule" feature.
// =================================================================================
package io.pm.finlight.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.pm.finlight.CustomSmsRule
import io.pm.finlight.ManageParseRulesViewModel

@Composable
fun ManageParseRulesScreen(
    navController: NavController,
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
                    onEditClick = {
                        navController.navigate("rule_creation_screen?ruleId=${rule.id}")
                    },
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
private fun RuleItemCard(
    rule: CustomSmsRule,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
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
                    text = rule.triggerPhrase,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // --- NEW: Edit button ---
                IconButton(onClick = onEditClick) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Rule",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Rule",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            HorizontalDivider()
            rule.merchantNameExample?.let {
                RuleDetailRow(label = "Merchant Name", value = it)
            }
            rule.amountExample?.let {
                RuleDetailRow(label = "Amount", value = it)
            }
            rule.accountNameExample?.let {
                RuleDetailRow(label = "Account Info", value = it)
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
