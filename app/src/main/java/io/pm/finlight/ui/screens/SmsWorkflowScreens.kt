// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/SmsWorkflowScreens.kt
// REASON: BUG FIX - The call to save a merchant name has been updated from the
// obsolete `saveMerchantMapping` to the new `saveMerchantRenameRule`. This
// aligns the approval screen with the new intelligent renaming feature, passing
// the originally parsed name and the user's final description to create a rule.
// FEATURE - A new "Link to Existing" button has been added to the
// `PotentialTransactionItem`. This button navigates to the new
// `link_transaction_screen`, passing the details of the parsed SMS to find
// potential matches.
// =================================================================================
package io.pm.finlight.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.gson.Gson
import io.pm.finlight.*
import kotlinx.coroutines.launch
import java.net.URLEncoder

private sealed class ApproveSheetContent {
    object Category : ApproveSheetContent()
    object Tags : ApproveSheetContent()
}


@Composable
fun ReviewSmsScreen(
    navController: NavController,
    viewModel: SettingsViewModel,
) {
    val potentialTransactions by viewModel.potentialTransactions.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    var hasLoadedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(isScanning, potentialTransactions) {
        if (!isScanning) {
            hasLoadedOnce = true
        }
        if (hasLoadedOnce && potentialTransactions.isEmpty()) {
            navController.popBackStack()
        }
    }

    if (isScanning && !hasLoadedOnce) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Scanning for transactions...", style = MaterialTheme.typography.titleMedium)
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "${potentialTransactions.size} potential transactions found.",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(potentialTransactions, key = { it.sourceSmsId }) { pt ->
                PotentialTransactionItem(
                    transaction = pt,
                    onDismiss = { viewModel.dismissPotentialTransaction(it) },
                    onApprove = { transaction ->
                        val encodedPotentialTxn = URLEncoder.encode(Gson().toJson(transaction), "UTF-8")
                        val route = "approve_transaction_screen?potentialTxnJson=$encodedPotentialTxn"
                        navController.navigate(route)
                    },
                    onCreateRule = { transaction ->
                        val json = Gson().toJson(transaction)
                        val encodedJson = URLEncoder.encode(json, "UTF-8")
                        navController.navigate("rule_creation_screen/$encodedJson")
                    },
                    onLink = { transaction ->
                        val json = Gson().toJson(transaction)
                        val encodedJson = URLEncoder.encode(json, "UTF-8")
                        navController.navigate("link_transaction_screen/$encodedJson")
                    }
                )
            }
        }
    }
}

@Composable
fun PotentialTransactionItem(
    transaction: PotentialTransaction,
    onDismiss: (PotentialTransaction) -> Unit,
    onApprove: (PotentialTransaction) -> Unit,
    onCreateRule: (PotentialTransaction) -> Unit,
    onLink: (PotentialTransaction) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            val amountColor = if (transaction.transactionType == "expense") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = transaction.merchantName ?: "Unknown Merchant",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "₹${"%.2f".format(transaction.amount)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = amountColor,
                    fontWeight = FontWeight.Bold,
                )
            }
            transaction.potentialAccount?.let {
                Text(
                    text = "Account: ${it.formattedName} (${it.accountType})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Type: ${transaction.transactionType.replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Original Message: ${transaction.originalMessage}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onLink(transaction) }) {
                    Text("Link to Existing")
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { onDismiss(transaction) }) { Text("Dismiss") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onApprove(transaction) }) { Text("Approve") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ApproveTransactionScreen(
    navController: NavController,
    transactionViewModel: TransactionViewModel,
    settingsViewModel: SettingsViewModel,
    potentialTxn: PotentialTransaction,
) {
    var description by remember { mutableStateOf(potentialTxn.merchantName ?: "") }
    var notes by remember { mutableStateOf("") }
    var selectedTransactionType by remember(potentialTxn.transactionType) { mutableStateOf(potentialTxn.transactionType) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val categories by transactionViewModel.allCategories.collectAsState(initial = emptyList())
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    val allTags by transactionViewModel.allTags.collectAsState()
    val selectedTags by transactionViewModel.selectedTags.collectAsState()

    var activeSheetContent by remember { mutableStateOf<ApproveSheetContent?>(null) }
    val sheetState = rememberModalBottomSheetState()


    val isSaveEnabled = description.isNotBlank() && selectedCategory != null

    DisposableEffect(Unit) {
        onDispose {
            transactionViewModel.clearSelectedTags()
        }
    }

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "₹${"%,.2f".format(potentialTxn.amount)}",
                            style = MaterialTheme.typography.displaySmall,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                            fontWeight = FontWeight.Bold
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description / Merchant") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent
                            ),
                            placeholder = { Text("What was this for?") }
                        )
                    }
                }
            }
            item {
                TabRow(selectedTabIndex = if (selectedTransactionType == "expense") 0 else 1) {
                    listOf("Expense", "Income").forEachIndexed { index, title ->
                        Tab(selected = (if (selectedTransactionType == "expense") 0 else 1) == index, onClick = {
                            selectedTransactionType = if (index == 0) "expense" else "income"
                        }, text = { Text(title) })
                    }
                }
            }

            item {
                Card(elevation = CardDefaults.cardElevation(2.dp)) {
                    Column {
                        DetailRow(
                            icon = Icons.Default.AccountBalanceWallet,
                            label = "Account",
                            value = potentialTxn.potentialAccount?.formattedName ?: "Unknown Account",
                            onClick = null // Not editable
                        )
                        HorizontalDivider()
                        DetailRow(
                            icon = Icons.Default.Category,
                            label = "Category",
                            value = selectedCategory?.name ?: "Select category",
                            onClick = { activeSheetContent = ApproveSheetContent.Category },
                            valueColor = if (selectedCategory == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            leadingIcon = { selectedCategory?.let { CategoryIcon(it, Modifier.size(24.dp)) } }
                        )
                        HorizontalDivider()
                        DetailRow(
                            icon = Icons.Default.NewLabel,
                            label = "Tags",
                            value = if (selectedTags.isEmpty()) "Add tags" else selectedTags.joinToString { it.name },
                            onClick = { activeSheetContent = ApproveSheetContent.Tags }
                        )
                        HorizontalDivider()
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Add notes...") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = "Notes") },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                            )
                        )
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                val success = transactionViewModel.approveSmsTransaction(
                                    potentialTxn = potentialTxn,
                                    description = description,
                                    categoryId = selectedCategory?.id,
                                    notes = notes.takeIf { it.isNotBlank() },
                                    tags = selectedTags
                                )
                                if (success) {
                                    settingsViewModel.onTransactionApproved(potentialTxn.sourceSmsId)
                                    potentialTxn.merchantName?.let { originalName ->
                                        settingsViewModel.saveMerchantRenameRule(originalName, description)
                                    }
                                    val notificationManager = NotificationManagerCompat.from(context)
                                    notificationManager.cancel(potentialTxn.sourceSmsId.toInt())
                                    navController.popBackStack()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isSaveEnabled,
                    ) { Text("Save Transaction") }
                }
            }
        }
    }

    if (activeSheetContent != null) {
        ModalBottomSheet(
            onDismissRequest = { activeSheetContent = null },
            sheetState = sheetState
        ) {
            when (activeSheetContent) {
                is ApproveSheetContent.Category -> ApproveCategoryPickerSheet(
                    items = categories,
                    onItemSelected = { selectedCategory = it; activeSheetContent = null }
                )
                is ApproveSheetContent.Tags -> ApproveTagPickerSheet(
                    allTags = allTags,
                    selectedTags = selectedTags,
                    onTagSelected = transactionViewModel::onTagSelected,
                    onAddNewTag = transactionViewModel::addTagOnTheGo,
                    onConfirm = { activeSheetContent = null }
                )
                else -> {}
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: (() -> Unit)?,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            leadingIcon()
        } else {
            Icon(icon, contentDescription = label)
        }
        Spacer(Modifier.width(16.dp))
        Text(label, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontWeight = FontWeight.SemiBold)
        if (onClick != null) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ApproveCategoryPickerSheet(
    items: List<Category>,
    onItemSelected: (Category) -> Unit
) {
    Column(modifier = Modifier.navigationBarsPadding()) {
        Text(
            "Select Category",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items) { category ->
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onItemSelected(category) }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryIcon(category, Modifier.size(48.dp))
                    Text(category.name, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ApproveTagPickerSheet(
    allTags: List<Tag>,
    selectedTags: Set<Tag>,
    onTagSelected: (Tag) -> Unit,
    onAddNewTag: (String) -> Unit,
    onConfirm: () -> Unit
) {
    var newTagName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Manage Tags", style = MaterialTheme.typography.titleLarge)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            allTags.forEach { tag ->
                FilterChip(
                    selected = tag in selectedTags,
                    onClick = { onTagSelected(tag) },
                    label = { Text(tag.name) }
                )
            }
        }
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newTagName,
                onValueChange = { newTagName = it },
                label = { Text("New Tag Name") },
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    onAddNewTag(newTagName)
                    newTagName = ""
                },
                enabled = newTagName.isNotBlank()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Tag")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onConfirm) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (newTagName.isNotBlank()) {
                    onAddNewTag(newTagName)
                }
                onConfirm()
            }) { Text("Done") }
        }
    }
}

@Composable
private fun CategoryIcon(category: Category, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(CategoryIconHelper.getIconBackgroundColor(category.colorKey)),
        contentAlignment = Alignment.Center
    ) {
        if (category.iconKey == "letter_default") {
            Text(
                text = category.name.firstOrNull()?.uppercase() ?: "?",
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontSize = 22.sp
            )
        } else {
            Icon(
                imageVector = CategoryIconHelper.getIcon(category.iconKey),
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
