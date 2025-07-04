// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/RuleCreationScreen.kt
// REASON: FEATURE - The screen now supports both "create" and "edit" modes. It
// accepts an optional `ruleId` from navigation. If the ID is present, it calls
// the ViewModel to load the rule's data, including the `sourceSmsBody`, and
// pre-populates the UI, enabling the user to modify and update existing rules.
// =================================================================================
package io.pm.finlight.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.google.gson.Gson
import io.pm.finlight.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class RuleCreationViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RuleCreationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RuleCreationViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleCreationScreen(
    navController: NavController,
    potentialTransactionJson: String?,
    ruleId: Int?
) {
    val context = LocalContext.current.applicationContext as Application
    val viewModel: RuleCreationViewModel = viewModel(factory = RuleCreationViewModelFactory(context))

    val uiState by viewModel.uiState.collectAsState()
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    val scope = rememberCoroutineScope()
    val isEditMode = ruleId != null

    LaunchedEffect(key1 = ruleId, key2 = potentialTransactionJson) {
        if (isEditMode) {
            viewModel.loadRuleForEditing(ruleId!!)
            // Fetch the rule and set its body to the text field
            val rule = AppDatabase.getInstance(context).customSmsRuleDao().getRuleById(ruleId).firstOrNull()
            if (rule != null) {
                textFieldValue = TextFieldValue(rule.sourceSmsBody)
            }
        } else if (potentialTransactionJson != null) {
            val potentialTxn = Gson().fromJson(potentialTransactionJson, PotentialTransaction::class.java)
            viewModel.initializeStateForCreation(potentialTxn)
            textFieldValue = TextFieldValue(potentialTxn.originalMessage)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Parsing Rule" else "Create Parsing Rule") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Button(
                    onClick = {
                        val start = min(selection.start, selection.end)
                        val end = max(selection.start, selection.end)
                        val selectedText = textFieldValue.text.substring(start, end)
                        viewModel.onMarkAsAccount(RuleSelection(selectedText, start, end))
                    },
                    enabled = isSelectionActive,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mark as Account Info")
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
                    RuleSummaryItem(
                        icon = Icons.Default.AccountBalanceWallet,
                        label = "Account",
                        value = uiState.accountSelection.selectedText.ifBlank { "Not set" }
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                val isSaveEnabled = uiState.triggerSelection.selectedText.isNotBlank() &&
                        (uiState.merchantSelection.selectedText.isNotBlank() || uiState.amountSelection.selectedText.isNotBlank() || uiState.accountSelection.selectedText.isNotBlank())

                Button(
                    onClick = {
                        scope.launch {
                            viewModel.saveRule(textFieldValue.text) {
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("reparse_needed", true)
                                navController.popBackStack()
                            }
                        }
                    },
                    enabled = isSaveEnabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isEditMode) "Update Rule" else "Save Rule")
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
