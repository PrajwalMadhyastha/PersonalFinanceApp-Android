// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/AddEditBudgetScreen.kt
// REASON: REFACTOR - The dialog has been updated to use GlassPanel components
// and align with the Project Aurora aesthetic, ensuring a consistent and modern
// look for adding and editing budgets.
// =================================================================================
package io.pm.finlight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.pm.finlight.Budget
import io.pm.finlight.BudgetViewModel
import io.pm.finlight.Category
import io.pm.finlight.ui.components.GlassPanel
import io.pm.finlight.ui.theme.PopupSurfaceDark
import io.pm.finlight.ui.theme.PopupSurfaceLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBudgetScreen(
    navController: NavController,
    viewModel: BudgetViewModel,
    budgetId: Int?,
) {
    val isEditMode = budgetId != null
    val buttonText = if (isEditMode) "Update Budget" else "Save Budget"
    val titleText = if (isEditMode) "Edit Budget" else "Add Budget"

    var amount by remember { mutableStateOf("") }
    val availableCategories by viewModel.availableCategoriesForNewBudget.collectAsState(initial = emptyList())
    val allCategories by viewModel.allCategories.collectAsState(initial = emptyList())
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    val budgetToEdit by if (isEditMode) {
        viewModel.getBudgetById(budgetId!!).collectAsState(initial = null)
    } else {
        remember { mutableStateOf<Budget?>(null) }
    }

    LaunchedEffect(budgetToEdit, allCategories) {
        if (isEditMode) {
            budgetToEdit?.let { budget ->
                amount = "%.0f".format(budget.amount)
                selectedCategory = allCategories.find { it.name == budget.categoryName }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(titleText, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)

        GlassPanel {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val isDropdownEnabled = !isEditMode && availableCategories.isNotEmpty()

                ExposedDropdownMenuBox(
                    expanded = isCategoryDropdownExpanded && isDropdownEnabled,
                    onExpandedChange = { if (isDropdownEnabled) isCategoryDropdownExpanded = !isCategoryDropdownExpanded },
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "Select Category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded && isDropdownEnabled) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = isDropdownEnabled,
                    )
                    ExposedDropdownMenu(
                        expanded = isCategoryDropdownExpanded && isDropdownEnabled,
                        onDismissRequest = { isCategoryDropdownExpanded = false },
                        modifier = Modifier.background(if (isSystemInDarkTheme()) PopupSurfaceDark else PopupSurfaceLight)
                    ) {
                        availableCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategory = category
                                    isCategoryDropdownExpanded = false
                                },
                            )
                        }
                    }
                }

                if (availableCategories.isEmpty() && !isEditMode) {
                    Text(
                        text = "All categories already have a budget for this month. You can edit existing budgets from the previous screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Budget Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Text("₹") },
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull()
                    if (selectedCategory != null && amountDouble != null && amountDouble > 0) {
                        if (isEditMode) {
                            budgetToEdit?.let { currentBudget ->
                                viewModel.updateBudget(currentBudget.copy(amount = amountDouble))
                            }
                        } else {
                            viewModel.addCategoryBudget(selectedCategory!!.name, amount)
                        }
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = selectedCategory != null && amount.isNotBlank(),
            ) {
                Text(buttonText)
            }
        }
    }
}
