// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/GoalScreen.kt
// REASON: NEW FILE - Implements the UI for the Savings Goals feature. It follows
// the "Project Aurora" design system, using GlassPanel components to display
// each goal with a progress bar and summary details. It includes dialogs for
// adding, editing, and deleting goals.
// FIX: Refactored the dialog management to be sequential. The main screen now
// controls the visibility of the Add/Edit dialog and the Date Picker dialog
// separately to prevent window conflicts, ensuring the Date Picker appears correctly.
// ANIMATION - Added `animateItemPlacement()` to the GoalItem in the LazyColumn.
// This makes the list fluidly animate changes when goals are added or removed.
// ANIMATION - The duration of the `tween` animation for the goal progress bar
// has been reduced from 1000ms to a much snappier 400ms.
// =================================================================================
package io.pm.finlight.ui.screens

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.pm.finlight.*
import io.pm.finlight.ui.components.GlassPanel
import io.pm.finlight.ui.theme.PopupSurfaceDark
import io.pm.finlight.ui.theme.PopupSurfaceLight
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

private fun Color.isDark() = (red * 0.299 + green * 0.587 + blue * 0.114) < 0.5

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GoalScreen(
    navController: NavController,
    goalViewModel: GoalViewModel = viewModel(),
    transactionViewModel: TransactionViewModel = viewModel()
) {
    val goals by goalViewModel.allGoals.collectAsState()
    val accounts by transactionViewModel.allAccounts.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // --- STATE HOISTING: All state for the dialog is now managed here ---
    var goalToEdit by remember { mutableStateOf<GoalWithAccountName?>(null) }
    var goalName by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var savedAmount by remember { mutableStateOf("") }
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var targetDate by remember { mutableStateOf<Long?>(null) }
    // --- END STATE HOISTING ---

    fun openDialogForNew() {
        goalToEdit = null
        goalName = ""
        targetAmount = ""
        savedAmount = ""
        selectedAccount = accounts.firstOrNull()
        targetDate = null
        showAddEditDialog = true
    }

    fun openDialogForEdit(goal: GoalWithAccountName) {
        goalToEdit = goal
        goalName = goal.name
        targetAmount = goal.targetAmount.toString()
        savedAmount = goal.savedAmount.toString()
        selectedAccount = accounts.find { it.id == goal.accountId }
        targetDate = goal.targetDate
        showAddEditDialog = true
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { openDialogForNew() }) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal")
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        if (goals.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No savings goals yet. Tap '+' to add one!",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(goals, key = { it.id }) { goal ->
                    GoalItem(
                        modifier = Modifier.animateItemPlacement(),
                        goal = goal,
                        onEdit = { openDialogForEdit(goal) },
                        onDelete = {
                            goalToEdit = goal
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showAddEditDialog) {
        AddEditGoalDialog(
            goalName = goalName,
            onGoalNameChange = { goalName = it },
            targetAmount = targetAmount,
            onTargetAmountChange = { targetAmount = it },
            savedAmount = savedAmount,
            onSavedAmountChange = { savedAmount = it },
            selectedAccount = selectedAccount,
            onAccountSelected = { selectedAccount = it },
            accounts = accounts,
            targetDate = targetDate,
            onDismiss = { showAddEditDialog = false },
            onConfirm = {
                val target = targetAmount.toDoubleOrNull()
                val saved = savedAmount.toDoubleOrNull() ?: 0.0
                if (goalName.isNotBlank() && target != null && selectedAccount != null) {
                    goalViewModel.saveGoal(goalToEdit?.id, goalName, target, saved, targetDate, selectedAccount!!.id)
                    showAddEditDialog = false
                }
            },
            onLaunchDatePicker = {
                showAddEditDialog = false
                showDatePicker = true
            }
        )
    }

    if (showDatePicker) {
        GoalDatePickerDialog(
            initialDate = targetDate,
            onDismiss = {
                showDatePicker = false
                showAddEditDialog = true // Re-show the goal dialog
            },
            onDateSelected = { newDate ->
                targetDate = newDate
                showDatePicker = false
                showAddEditDialog = true // Re-show the goal dialog
            }
        )
    }

    if (showDeleteDialog && goalToEdit != null) {
        DeleteGoalDialog(
            goalName = goalToEdit!!.name,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                goalViewModel.deleteGoal(
                    Goal(
                        id = goalToEdit!!.id,
                        name = goalToEdit!!.name,
                        targetAmount = goalToEdit!!.targetAmount,
                        savedAmount = goalToEdit!!.savedAmount,
                        targetDate = goalToEdit!!.targetDate,
                        accountId = goalToEdit!!.accountId
                    )
                )
                showDeleteDialog = false
            }
        )
    }
}

@Composable
private fun GoalItem(
    modifier: Modifier = Modifier,
    goal: GoalWithAccountName,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 400, easing = EaseOutCubic),
        label = "GoalProgress"
    )
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val dateFormat = remember { SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()) }

    GlassPanel(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Linked to: ${goal.accountName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Goal", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Goal", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                strokeCap = StrokeCap.Round
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(progress * 100).roundToInt()}% Complete",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${currencyFormat.format(goal.savedAmount)} / ${currencyFormat.format(goal.targetAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            goal.targetDate?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Target Date: ${dateFormat.format(Date(it))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditGoalDialog(
    goalName: String,
    onGoalNameChange: (String) -> Unit,
    targetAmount: String,
    onTargetAmountChange: (String) -> Unit,
    savedAmount: String,
    onSavedAmountChange: (String) -> Unit,
    selectedAccount: Account?,
    onAccountSelected: (Account) -> Unit,
    accounts: List<Account>,
    targetDate: Long?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onLaunchDatePicker: () -> Unit
) {
    var accountExpanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val isThemeDark = MaterialTheme.colorScheme.surface.isDark()
    val popupContainerColor = if (isThemeDark) PopupSurfaceDark else PopupSurfaceLight

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = popupContainerColor,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                Text(
                    text = if (goalName.isBlank()) "New Savings Goal" else "Edit Savings Goal",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    LazyColumn {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                OutlinedTextField(
                                    value = goalName,
                                    onValueChange = onGoalNameChange,
                                    label = { Text("Goal Name (e.g., New Phone)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = targetAmount,
                                    onValueChange = { onTargetAmountChange(it.filter { c -> c.isDigit() || c == '.' }) },
                                    label = { Text("Target Amount") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    leadingIcon = { Text("₹") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = savedAmount,
                                    onValueChange = { onSavedAmountChange(it.filter { c -> c.isDigit() || c == '.' }) },
                                    label = { Text("Already Saved (Optional)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    leadingIcon = { Text("₹") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                ExposedDropdownMenuBox(expanded = accountExpanded, onExpandedChange = { accountExpanded = !accountExpanded }) {
                                    OutlinedTextField(
                                        value = selectedAccount?.name ?: "Select Account",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Link to Account") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                                        modifier = Modifier.fillMaxWidth().menuAnchor()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = accountExpanded,
                                        onDismissRequest = { accountExpanded = false },
                                        modifier = Modifier.background(if (isSystemInDarkTheme()) PopupSurfaceDark else PopupSurfaceLight)
                                    ) {
                                        accounts.forEach { account ->
                                            DropdownMenuItem(
                                                text = { Text(account.name) },
                                                onClick = {
                                                    onAccountSelected(account)
                                                    accountExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = targetDate?.let { dateFormat.format(Date(it)) } ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Target Date (Optional)") },
                                    modifier = Modifier.fillMaxWidth().clickable { onLaunchDatePicker() },
                                    trailingIcon = { Icon(Icons.Default.DateRange, "Select Date") }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        enabled = goalName.isNotBlank() && targetAmount.isNotBlank() && selectedAccount != null
                    ) { Text(if (goalName.isBlank()) "Create" else "Save") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalDatePickerDialog(
    initialDate: Long?,
    onDismiss: () -> Unit,
    onDateSelected: (Long?) -> Unit
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate ?: System.currentTimeMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}


@Composable
private fun DeleteGoalDialog(
    goalName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val isThemeDark = MaterialTheme.colorScheme.surface.isDark()
    val popupContainerColor = if (isThemeDark) PopupSurfaceDark else PopupSurfaceLight

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = popupContainerColor,
        title = { Text("Delete Goal?") },
        text = { Text("Are you sure you want to delete the goal '$goalName'?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
