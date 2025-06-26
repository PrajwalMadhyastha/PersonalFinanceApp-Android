package io.pm.finlight.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.pm.finlight.Budget
import io.pm.finlight.BudgetViewModel

@Composable
fun BudgetScreen(
    navController: NavController,
    viewModel: BudgetViewModel = viewModel(),
) {
    val categoryBudgets by viewModel.budgetsForCurrentMonth.collectAsState(initial = emptyList())
    val overallBudget by viewModel.overallBudget.collectAsState()
    val monthYear = viewModel.getCurrentMonthYearString()
    val context = LocalContext.current

    var overallBudgetInput by remember(overallBudget) {
        mutableStateOf(if (overallBudget > 0) "%.0f".format(overallBudget) else "")
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var budgetToDelete by remember { mutableStateOf<Budget?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(
                        modifier =
                            Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                    ) {
                        Text("Overall Monthly Budget", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = overallBudgetInput,
                            onValueChange = { overallBudgetInput = it },
                            label = { Text("Total Budget Amount") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Text("₹") },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                viewModel.saveOverallBudget(overallBudgetInput)
                                Toast.makeText(context, "Overall Budget Saved!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text("Save Overall Budget")
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Category Budgets", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = { navController.navigate("add_budget") }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Category Budget")
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }

            if (categoryBudgets.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("No category budgets set. Tap the '+' button to add one.")
                    }
                }
            } else {
                items(categoryBudgets) { budget ->
                    SimpleBudgetItem(
                        budget = budget,
                        onEdit = { navController.navigate("edit_budget/${budget.id}") },
                        onDelete = {
                            budgetToDelete = budget
                            showDeleteDialog = true
                        },
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Budget?") },
            text = { Text("Are you sure you want to delete the budget for '${budgetToDelete?.categoryName}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        budgetToDelete?.let { viewModel.deleteBudget(it) }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
fun SimpleBudgetItem(
    budget: Budget,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(budget.categoryName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "Budget: ₹${"%.0f".format(budget.amount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit Budget")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Budget", tint = MaterialTheme.colorScheme.error)
        }
    }
    HorizontalDivider()
}
