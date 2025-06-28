package io.pm.finlight.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.pm.finlight.Budget
import io.pm.finlight.BudgetViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GaugeChart(progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000)
    )

    Canvas(modifier = Modifier.size(150.dp)) {
        val arcSize = size.width
        val arcRadius = arcSize / 2
        val strokeWidth = 20f

        drawArc(
            color = Color.LightGray.copy(alpha = 0.3f),
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset.Zero,
            size = Size(arcSize, arcSize),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        drawArc(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF00E5FF), Color(0xFF18A0FF), Color(0xFF0052D4))
            ),
            startAngle = 135f,
            sweepAngle = 270 * animatedProgress,
            useCenter = false,
            topLeft = Offset.Zero,
            size = Size(arcSize, arcSize),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        val angle = (135 + 270 * animatedProgress).coerceIn(135f, 405f)
        val angleInRadians = Math.toRadians(angle.toDouble())
        val lineStart = center
        val lineEnd = Offset(
            x = center.x + (arcRadius * 0.8f) * cos(angleInRadians).toFloat(),
            y = center.y + (arcRadius * 0.8f) * sin(angleInRadians).toFloat()
        )
        drawLine(
            color = Color.White,
            start = lineStart,
            end = lineEnd,
            strokeWidth = 8f,
            cap = StrokeCap.Round
        )
        drawCircle(color = Color.White, radius = 15f, center = center)
        drawCircle(color = Color.Black, radius = 10f, center = center)

        // --- NEW: Draw the stars ---
        for (i in 0..4) {
            val starProgress = i / 4f
            val starAngle = 135f + (270f * starProgress)
            val starAngleRad = Math.toRadians(starAngle.toDouble())
            val starRadius = arcRadius * 0.9f
            val starCenter = Offset(
                center.x + starRadius * cos(starAngleRad).toFloat(),
                center.y + starRadius * sin(starAngleRad).toFloat()
            )
            val starColor = if(animatedProgress >= starProgress) Color(0xFF81D4FA) else Color.Gray.copy(alpha = 0.5f)

            rotate(degrees = starAngle - 90, pivot = starCenter) {
                val path = Path().apply {
                    val outerR = 12f
                    val innerR = 5f
                    moveTo(starCenter.x, starCenter.y - outerR)
                    for (j in 1..4) {
                        val outerAngle = j * (360.0 / 5.0) + -90.0
                        val innerAngle = outerAngle + (360.0 / 10.0)
                        lineTo(
                            starCenter.x + (innerR * cos(Math.toRadians(innerAngle))).toFloat(),
                            starCenter.y + (innerR * sin(Math.toRadians(innerAngle))).toFloat()
                        )
                        lineTo(
                            starCenter.x + (outerR * cos(Math.toRadians(outerAngle + 360.0/5.0))).toFloat(),
                            starCenter.y + (outerR * sin(Math.toRadians(outerAngle + 360.0/5.0))).toFloat()
                        )
                    }
                    close()
                }
                drawPath(path, color = starColor)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    navController: NavController,
    viewModel: BudgetViewModel = viewModel(),
) {
    val categoryBudgets by viewModel.budgetsForCurrentMonth.collectAsState(initial = emptyList())
    val overallBudget by viewModel.overallBudget.collectAsState()
    val totalSpending by viewModel.totalSpending.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var budgetToDelete by remember { mutableStateOf<Budget?>(null) }
    var showOverallBudgetDialog by remember { mutableStateOf(false) }

    // --- FIX: Removed the Scaffold and its bottomBar ---
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f), // Use weight to fill available space
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Set monthly budget", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Setting a budget reduces expenditures about 10% on an average.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    GaugeChart(progress = if (overallBudget > 0) (totalSpending.toFloat() / overallBudget) else 0f)
                }
            }

            item {
                // --- FIX: This card is now clickable to open the dialog ---
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { showOverallBudgetDialog = true },
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Overall Budget",
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "Overall budget",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "₹${"%,.0f".format(overallBudget)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Category wise budget",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    IconButton(onClick = { navController.navigate("add_budget") }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Category Budget")
                    }
                }
            }

            if (categoryBudgets.isEmpty()) {
                item {
                    Text(
                        "No category budgets set. Tap the '+' icon to add one.",
                        modifier = Modifier.padding(16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = Color.Gray
                    )
                }
            } else {
                items(categoryBudgets) { budget ->
                    CategoryBudgetItem(
                        budget = budget,
                        viewModel = viewModel,
                        onEdit = { navController.navigate("edit_budget/${budget.id}") },
                        onDelete = {
                            budgetToDelete = budget
                            showDeleteDialog = true
                        }
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

    if (showOverallBudgetDialog) {
        EditOverallBudgetDialog(
            currentBudget = overallBudget,
            onDismiss = { showOverallBudgetDialog = false },
            onConfirm = { newAmount ->
                viewModel.saveOverallBudget(newAmount)
                showOverallBudgetDialog = false
            }
        )
    }
}

@Composable
fun EditOverallBudgetDialog(
    currentBudget: Float,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var budgetInput by remember { mutableStateOf(if (currentBudget > 0) "%.0f".format(currentBudget) else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Overall Budget") },
        text = {
            OutlinedTextField(
                value = budgetInput,
                onValueChange = { budgetInput = it.filter { char -> char.isDigit() } },
                label = { Text("Total Monthly Budget Amount") },
                leadingIcon = { Text("₹") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(budgetInput) },
                enabled = budgetInput.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@Composable
fun CategoryBudgetItem(
    budget: Budget,
    viewModel: BudgetViewModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val spending by viewModel.getActualSpending(budget.categoryName)
        .collectAsState(initial = 0.0)
    val progress = if (budget.amount > 0) (spending / budget.amount).toFloat() else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Category,
                contentDescription = budget.categoryName,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(budget.categoryName, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f,1f) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "₹${"%,.0f".format(spending)} of ₹${"%,.0f".format(budget.amount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}
