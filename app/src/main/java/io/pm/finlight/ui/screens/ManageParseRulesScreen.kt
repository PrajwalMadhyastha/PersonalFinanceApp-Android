// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/ManageParseRulesScreen.kt
// REASON: MAJOR REFACTOR - The screen has been redesigned to align with the
// "Project Aurora" vision. The standard Card has been replaced with a GlassPanel
// component, and all text colors have been updated to be theme-aware, ensuring
// a consistent, high-contrast experience.
// =================================================================================
package io.pm.finlight.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.pm.finlight.CustomSmsRule
import io.pm.finlight.ManageParseRulesViewModel
import io.pm.finlight.ui.components.GlassPanel
import io.pm.finlight.ui.theme.PopupSurfaceDark
import io.pm.finlight.ui.theme.PopupSurfaceLight

@Composable
fun ManageParseRulesScreen(
    navController: NavController,
    viewModel: ManageParseRulesViewModel = viewModel()
) {
    val rules by viewModel.allRules.collectAsState()
    var ruleToDelete by remember { mutableStateOf<CustomSmsRule?>(null) }

    if (rules.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No custom parsing rules have been created yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(rules, key = { it.id }) { rule ->
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
            },
            containerColor = if (isSystemInDarkTheme()) PopupSurfaceDark else PopupSurfaceLight
        )
    }
}

@Composable
private fun RuleItemCard(
    rule: CustomSmsRule,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rule.triggerPhrase,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onEditClick) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Rule",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
}

@Composable
private fun RuleDetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
