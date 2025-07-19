// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/SplitTransactionScreen.kt
// REASON: NEW FILE - This screen provides the dedicated UI for splitting a
// transaction. It displays the total amount, the remaining unallocated amount,
// and a list of editable split items. It enforces the rule that the sum of
// splits must equal the total before saving.
// FIX - Added the missing private `isDark()` helper function to resolve a
// build error when determining the background color for the bottom sheet.
// =================================================================================
package io.pm.finlight.ui.screens

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.pm.finlight.*
import io.pm.finlight.ui.components.GlassPanel
import io.pm.finlight.ui.theme.PopupSurfaceDark
import io.pm.finlight.ui.theme.PopupSurfaceLight
import java.text.NumberFormat
import java.util.*

// Helper to detect perceived luminance.
private fun Color.isDark() = (red * 0.299 + green * 0.587 + blue * 0.114) < 0.5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitTransactionScreen(
    navController: NavController,
    transactionId: Int
) {
    val application = LocalContext.current.applicationContext as Application
    val factory = SplitTransactionViewModelFactory(application, transactionId)
    val viewModel: SplitTransactionViewModel = viewModel(factory = factory)
    // We also need the TransactionViewModel to perform the final save operation
    val transactionViewModel: TransactionViewModel = viewModel()

    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categoryRepository.allCategories.collectAsState(initial = emptyList())
    val context = LocalContext.current

    var activeSheetTarget by remember { mutableStateOf<SplitItem?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isSaveEnabled = uiState.remainingAmount == 0.0 && uiState.splitItems.all { it.category != null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Split Transaction") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SplitHeader(
                        totalAmount = uiState.parentTransaction?.amount ?: 0.0,
                        remainingAmount = uiState.remainingAmount
                    )
                }

                items(uiState.splitItems, key = { it.id }) { item ->
                    SplitItemRow(
                        item = item,
                        onAmountChange = { newAmount -> viewModel.updateSplitAmount(item, newAmount) },
                        onCategoryClick = { activeSheetTarget = item },
                        onDeleteClick = { viewModel.removeSplitItem(item) }
                    )
                }

                item {
                    OutlinedButton(
                        onClick = { viewModel.addSplitItem() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Split")
                        Spacer(Modifier.width(8.dp))
                        Text("Add Item")
                    }
                }
            }

            // Bottom Save Bar
            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            transactionViewModel.saveTransactionSplits(transactionId, uiState.splitItems) {
                                Toast.makeText(context, "Transaction split saved!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isSaveEnabled
                    ) { Text("Save Splits") }
                }
            }
        }
    }

    if (activeSheetTarget != null) {
        val isThemeDark = MaterialTheme.colorScheme.surface.isDark()
        val popupContainerColor = if (isThemeDark) PopupSurfaceDark else PopupSurfaceLight

        ModalBottomSheet(
            onDismissRequest = { activeSheetTarget = null },
            sheetState = sheetState,
            containerColor = popupContainerColor
        ) {
            SplitCategoryPickerSheet(
                categories = categories,
                onCategorySelected = { category ->
                    activeSheetTarget?.let {
                        viewModel.updateSplitCategory(it, category)
                    }
                    activeSheetTarget = null
                }
            )
        }
    }
}

@Composable
private fun SplitHeader(totalAmount: Double, remainingAmount: Double) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val remainingColor = when {
        remainingAmount > 0 -> MaterialTheme.colorScheme.error
        remainingAmount < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    GlassPanel {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Total Amount",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                currencyFormat.format(totalAmount),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Remaining: ${currencyFormat.format(remainingAmount)}",
                style = MaterialTheme.typography.titleMedium,
                color = remainingColor
            )
        }
    }
}

@Composable
private fun SplitItemRow(
    item: SplitItem,
    onAmountChange: (String) -> Unit,
    onCategoryClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    GlassPanel {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onCategoryClick)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val icon = item.category?.let { CategoryIconHelper.getIcon(it.iconKey) } ?: Icons.Default.Add
                Icon(
                    imageVector = icon,
                    contentDescription = "Category",
                    tint = if (item.category != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    item.category?.name ?: "Set",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (item.category != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = item.amount,
                onValueChange = onAmountChange,
                modifier = Modifier.weight(1f),
                label = { Text("Amount") },
                leadingIcon = { Text("₹") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Split", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun SplitCategoryPickerSheet(
    categories: List<Category>,
    onCategorySelected: (Category) -> Unit
) {
    Column(modifier = Modifier.navigationBarsPadding()) {
        Text(
            "Select Category for Split",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(categories) { category ->
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onCategorySelected(category) }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(CategoryIconHelper.getIconBackgroundColor(category.colorKey)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = CategoryIconHelper.getIcon(category.iconKey),
                            contentDescription = category.name,
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        category.name,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
