// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/RuleCreationScreen.kt
// REASON: ARCHITECTURAL REFACTOR - The UI is updated for the trigger-based
// system. A "Mark as Trigger" button has been added, and the summary view now
// displays the selected trigger. The "Save Rule" button is now enabled only
// when a trigger and at least one other field (merchant or amount) are marked.
// =================================================================================
package io.pm.finlight.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.pm.finlight.RuleCreationViewModel
import io.pm.finlight.RuleSelection
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

// Factory for creating RuleCreationViewModel with Application context
class RuleCreationViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RuleCreationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RuleCreationViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * A screen where users can define custom parsing rules for SMS messages.
 */
@Composable
fun RuleCreationScreen(
    navController: NavController,
    smsText: String
) {
    val context = LocalContext.current.applicationContext as Application
    val viewModel: RuleCreationViewModel = viewModel(factory = RuleCreationViewModelFactory(context))

    val uiState by viewModel.uiState.collectAsState()
    var textFieldValue by remember { mutableStateOf(TextFieldValue(smsText)) }
    val scope = rememberCoroutineScope()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Full SMS Message", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { textFieldValue = it },
                        readOnly = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            val selection = textFieldValue.selection
            val isSelectionActive = !selection.collapsed

            // --- NEW: Button to mark the trigger phrase ---
            Button(
                onClick = {
                    val start = min(selection.start, selection.end)
                    val end = max(selection.start, selection.end)
                    val selectedText = textFieldValue.text.substring(start, end)
                    viewModel.onMarkAsTrigger(RuleSelection(selectedText, start, end))
                },
                enabled = isSelectionActive,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mark as Trigger Phrase")
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val start = min(selection.start, selection.end)
                        val end = max(selection.start, selection.end)
                        val selectedText = textFieldValue.text.substring(start, end)
                        viewModel.onMarkAsMerchant(RuleSelection(selectedText, start, end))
                    },
                    enabled = isSelectionActive,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Mark as Merchant")
                }
                Button(
                    onClick = {
                        val start = min(selection.start, selection.end)
                        val end = max(selection.start, selection.end)
                        val selectedText = textFieldValue.text.substring(start, end)
                        viewModel.onMarkAsAmount(RuleSelection(selectedText, start, end))
                    },
                    enabled = isSelectionActive,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Mark as Amount")
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Defined Rule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    // --- NEW: Display the selected trigger phrase ---
                    RuleSummaryItem(
                        icon = Icons.Default.Flag,
                        label = "Trigger",
                        value = uiState.triggerSelection.selectedText.ifBlank { "Not set (Required)" },
                        isError = uiState.triggerSelection.selectedText.isBlank()
                    )
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
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                val isSaveEnabled = uiState.triggerSelection.selectedText.isNotBlank() &&
                        (uiState.merchantSelection.selectedText.isNotBlank() || uiState.amountSelection.selectedText.isNotBlank())

                Button(
                    onClick = {
                        scope.launch {
                            viewModel.saveRule(smsText) {
                                navController.popBackStack()
                            }
                        }
                    },
                    enabled = isSaveEnabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Rule")
                }
            }
        }
    }
}

@Composable
private fun RuleSummaryItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isError: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        Text("$label:", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = if (isError) MaterialTheme.colorScheme.error else LocalContentColor.current)
    }
}
