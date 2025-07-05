// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/RetrospectiveUpdateScreen.kt
// REASON: NEW FILE - This screen provides the UI for the Retrospective Update
// feature. It displays a list of transactions similar to the one the user just
// edited. Each item has a checkbox, allowing the user to select which
// transactions should be included in the batch update. It includes "Select All"
// functionality and a confirmation button to apply the changes.
// =================================================================================
package io.pm.finlight.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.pm.finlight.RetrospectiveUpdateViewModel
import io.pm.finlight.RetrospectiveUpdateViewModelFactory
import io.pm.finlight.Transaction
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetrospectiveUpdateScreen(
    navController: NavController,
    transactionId: Int,
    originalDescription: String,
    newDescription: String?,
    newCategoryId: Int?
) {
    val application = LocalContext.current.applicationContext as Application
    val factory = RetrospectiveUpdateViewModelFactory(application, originalDescription, transactionId)
    val viewModel: RetrospectiveUpdateViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Update Similar Transactions") })
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.performBatchUpdate(newDescription, newCategoryId)
                                navController.popBackStack()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.selectedIds.isNotEmpty()
                    ) {
                        Text("Update ${uiState.selectedIds.size} Items")
                    }
                }
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.similarTransactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("No other similar transactions found.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Column {
                        Text("Found ${uiState.similarTransactions.size} similar transaction(s) for '${originalDescription}'.")
                        Spacer(Modifier.height(8.dp))
                        Text("Select which ones you'd like to update.")
                        Spacer(Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val allSelected = uiState.selectedIds.size == uiState.similarTransactions.size
                            Checkbox(
                                checked = allSelected,
                                onCheckedChange = { viewModel.toggleSelectAll() }
                            )
                            Text(if (allSelected) "Deselect All" else "Select All")
                        }
                        HorizontalDivider()
                    }
                }
                items(uiState.similarTransactions, key = { it.id }) { transaction ->
                    SelectableTransactionItem(
                        transaction = transaction,
                        isSelected = transaction.id in uiState.selectedIds,
                        onToggle = { viewModel.toggleSelection(transaction.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectableTransactionItem(
    transaction: Transaction,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.description, fontWeight = FontWeight.SemiBold)
            Text(dateFormatter.format(Date(transaction.date)), style = MaterialTheme.typography.bodySmall)
        }
        Text("₹${"%,.2f".format(transaction.amount)}")
    }
}
