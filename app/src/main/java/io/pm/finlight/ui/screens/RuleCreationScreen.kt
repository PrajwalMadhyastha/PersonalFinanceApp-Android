package io.pm.finlight.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NewLabel
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.pm.finlight.RuleCreationViewModel
import io.pm.finlight.RuleSelection

/**
 * A screen where users can define custom parsing rules for SMS messages.
 * This screen displays the full SMS text and provides tools for the user
 * to mark different parts of the text (e.g., merchant name, amount).
 *
 * @param navController The NavController for handling navigation.
 * @param smsText The full body of the SMS message to create a rule for.
 * @param viewModel The ViewModel to manage the state of the rule creation process.
 */
@Composable
fun RuleCreationScreen(
    navController: NavController,
    smsText: String,
    viewModel: RuleCreationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var textFieldValue by remember { mutableStateOf(TextFieldValue(smsText)) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section to display the full SMS text with selection enabled
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Full SMS Message",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    // --- FIX: Using a read-only BasicTextField to reliably get selection ---
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { textFieldValue = it },
                        readOnly = true, // Makes it behave like Text, but allows selection
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            // Section for action buttons, enabled when text is selected
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val selection = textFieldValue.selection
                val isSelectionActive = !selection.collapsed

                Button(
                    onClick = {
                        val selectedText = textFieldValue.text.substring(selection.start, selection.end)
                        val ruleSelection = RuleSelection(selectedText, selection.start, selection.end)
                        viewModel.onMarkAsMerchant(ruleSelection)
                    },
                    enabled = isSelectionActive,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Mark as Merchant")
                }
                Button(
                    onClick = {
                        val selectedText = textFieldValue.text.substring(selection.start, selection.end)
                        val ruleSelection = RuleSelection(selectedText, selection.start, selection.end)
                        viewModel.onMarkAsAmount(ruleSelection)
                    },
                    enabled = isSelectionActive,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Mark as Amount")
                }
            }

            // Dynamic rule summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Defined Rules",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider()
                    RuleSummaryItem(
                        icon = Icons.Default.Title,
                        label = "Merchant",
                        value = uiState.merchantSelection.selectedText.ifBlank { "Not set" }
                    )
                    RuleSummaryItem(
                        icon = Icons.Default.Pin,
                        label = "Amount",
                        value = uiState.amountSelection.selectedText.ifBlank { "Not set" }
                    )
                    RuleSummaryItem(
                        icon = Icons.Default.NewLabel,
                        label = "Transaction Type",
                        value = uiState.transactionType ?: "Not set"
                    )
                }
            }

            // Save and Cancel buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(onClick = { /* TODO: Implement save logic */ }, enabled = false, modifier = Modifier.weight(1f)) {
                    Text("Save Rule")
                }
            }
        }
    }
}

/**
 * A helper composable to display a single line in the rule summary card.
 *
 * @param icon The icon to display for the rule type.
 * @param label The label for the rule type (e.g., "Merchant").
 * @param value The value extracted or defined for this rule.
 */
@Composable
private fun RuleSummaryItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        Text("$label:", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
