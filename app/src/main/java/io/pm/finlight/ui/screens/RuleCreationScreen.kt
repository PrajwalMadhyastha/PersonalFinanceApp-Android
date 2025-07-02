// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/RuleCreationScreen.kt
// REASON: CRASH FIX - The call to `viewModel.saveRule` is updated to pass the
// full original SMS message. This provides the necessary context for the
// `generateRegex` function in the ViewModel and resolves the
// StringIndexOutOfBoundsException crash.
// =================================================================================
package io.pm.finlight.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.gson.Gson
import io.pm.finlight.PotentialTransaction
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
    potentialTransactionJson: String
) {
    val context = LocalContext.current.applicationContext as Application
    val viewModel: RuleCreationViewModel = viewModel(factory = RuleCreationViewModelFactory(context))

    // Deserialize the JSON string back into a PotentialTransaction object
    val potentialTxn = remember { Gson().fromJson(potentialTransactionJson, PotentialTransaction::class.java) }

    val uiState by viewModel.uiState.collectAsState()
    var textFieldValue by remember { mutableStateOf(TextFieldValue(potentialTxn.originalMessage)) }
    val scope = rememberCoroutineScope()

    // Initialize the ViewModel's state with the pre-parsed data
    LaunchedEffect(key1 = potentialTxn) {
        viewModel.initializeState(potentialTxn)
    }


    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- NEW: Instructional Text ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = "Info")
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Long-press text to select it, then tap a 'Mark as...' button below. Manage your rules later in Settings.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }


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
                            // --- FIX: Pass the full original message to the save function ---
                            viewModel.saveRule(potentialTxn.originalMessage) {
                                if (potentialTxn.sourceSmsId != -1L) { // Check if we came from a real transaction
                                    navController.previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("reparse_needed", true)
                                }
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
