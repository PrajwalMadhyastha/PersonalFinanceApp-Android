// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/AddTransactionScreen.kt
// REASON: FEATURE - Added a new Switch component to this screen, allowing users
// to mark a new transaction as included or excluded from calculations right at
// the time of creation. This brings consistency with the TransactionDetailScreen.
// The state for this switch (`isIncluded`) is now passed to the ViewModel.
// =================================================================================
package io.pm.finlight.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.pm.finlight.*
import io.pm.finlight.ui.components.CreateAccountDialog
import io.pm.finlight.ui.components.CreateCategoryDialog
import io.pm.finlight.ui.components.TimePickerDialog
import java.text.SimpleDateFormat
import java.util.*

private sealed class AddSheetContent {
    object Account : AddSheetContent()
    object Category : AddSheetContent()
    object Tags : AddSheetContent()
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionScreen(
    navController: NavController,
    viewModel: TransactionViewModel,
    isCsvEdit: Boolean = false,
    csvLineNumber: Int = -1,
    initialDataJson: String? = null
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("expense") }
    var isIncluded by remember { mutableStateOf(true) } // State for the new switch

    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    var selectedAccount by remember { mutableStateOf<Account?>(null) }

    val categories by viewModel.allCategories.collectAsState(initial = emptyList())
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val selectedDateTime by remember { mutableStateOf(Calendar.getInstance()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val validationError by viewModel.validationError.collectAsState()

    var attachedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        attachedImageUris = attachedImageUris + uris
    }

    var activeSheetContent by remember { mutableStateOf<AddSheetContent?>(null) }
    val sheetState = rememberModalBottomSheetState()

    var showCreateAccountDialog by remember { mutableStateOf(false) }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }

    val isSaveEnabled = (description.isNotBlank() && amount.isNotBlank() && selectedAccount != null && selectedCategory != null)

    val allTags by viewModel.allTags.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()

    val defaultAccount by viewModel.defaultAccount.collectAsState()

    LaunchedEffect(defaultAccount, isCsvEdit) {
        if (!isCsvEdit && selectedAccount == null) {
            selectedAccount = defaultAccount
        }
    }

    LaunchedEffect(initialDataJson, accounts, categories) {
        if (isCsvEdit && initialDataJson != null) {
            try {
                val gson = Gson()
                val initialData: List<String> = gson.fromJson(initialDataJson, object : TypeToken<List<String>>() {}.type)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                initialData.getOrNull(0)?.let {
                    try {
                        selectedDateTime.time = dateFormat.parse(it) ?: Date()
                    } catch (e: Exception) { /* Keep default date on parse error */ }
                }
                description = initialData.getOrElse(1) { "" }
                amount = initialData.getOrElse(2) { "" }
                transactionType = initialData.getOrElse(3) { "expense" }
                val categoryName = initialData.getOrElse(4) { "" }
                val accountName = initialData.getOrElse(5) { "" }
                notes = initialData.getOrElse(6) { "" }
                // Assuming isExcluded is the 7th column if it exists
                isIncluded = initialData.getOrNull(7)?.toBooleanStrictOrNull()?.not() ?: true


                selectedCategory = categories.find { it.name.equals(categoryName, ignoreCase = true) }
                selectedAccount = accounts.find { it.name.equals(accountName, ignoreCase = true) }

            } catch (e: Exception) {
                // Log error or show a toast if JSON parsing fails
            }
        }
    }

    DisposableEffect(Unit) { onDispose { viewModel.clearSelectedTags() } }

    LaunchedEffect(validationError) { validationError?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SegmentedButton(
                    options = listOf("Expense", "Income"),
                    selectedOption = transactionType.replaceFirstChar { it.uppercase() },
                    onOptionSelected = {
                        transactionType = it.lowercase(Locale.getDefault())
                    }
                )
            }

            item {
                PrimaryInfoCard(
                    amount = amount,
                    onAmountChange = { amount = it },
                    description = description,
                    onDescriptionChange = { description = it }
                )
            }

            item {
                DetailsCard(
                    selectedAccount = selectedAccount,
                    onAccountClick = { activeSheetContent = AddSheetContent.Account },
                    selectedCategory = selectedCategory,
                    onCategoryClick = { activeSheetContent = AddSheetContent.Category },
                    isCategoryVisible = true,
                    selectedDate = selectedDateTime.time,
                    onDateClick = { showDatePicker = true },
                    tags = selectedTags,
                    onTagsClick = { activeSheetContent = AddSheetContent.Tags },
                    notes = notes,
                    onNotesChange = { notes = it },
                    attachmentsCount = attachedImageUris.size,
                    onAttachmentsClick = { imagePickerLauncher.launch("image/*") }
                )
            }

            // --- NEW: Added the switch for including/excluding the transaction ---
            item {
                Card(elevation = CardDefaults.cardElevation(2.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = transactionType.replaceFirstChar { it.titlecase(Locale.getDefault()) },
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = isIncluded,
                            onCheckedChange = { isIncluded = it }
                        )
                    }
                }
            }


            if (attachedImageUris.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(attachedImageUris) { uri ->
                            Box {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Selected image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                IconButton(
                                    onClick = { attachedImageUris = attachedImageUris - uri },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(24.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove attachment",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (isCsvEdit) {
                                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                val correctedData = listOf(
                                    dateFormat.format(selectedDateTime.time),
                                    description,
                                    amount,
                                    transactionType,
                                    selectedCategory?.name ?: "",
                                    selectedAccount?.name ?: "",
                                    notes
                                )
                                val gson = Gson()
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("corrected_row", gson.toJson(correctedData))
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("corrected_row_line", csvLineNumber)
                                navController.popBackStack()
                            } else {
                                val success = viewModel.addTransaction(
                                    description = description,
                                    categoryId = selectedCategory!!.id, // It can't be null here due to isSaveEnabled
                                    amountStr = amount,
                                    accountId = selectedAccount!!.id,
                                    notes = notes.takeIf { it.isNotBlank() },
                                    date = selectedDateTime.timeInMillis,
                                    transactionType = transactionType,
                                    isIncluded = isIncluded, // Pass the switch state
                                    sourceSmsId = null,
                                    sourceSmsHash = null,
                                    imageUris = attachedImageUris
                                )
                                if (success) navController.popBackStack()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isSaveEnabled
                    ) {
                        Text(if (isCsvEdit) "Update Row" else "Save")
                    }
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
                is AddSheetContent.Account -> AddAccountPickerSheet(
                    items = accounts,
                    onItemSelected = { selectedAccount = it; activeSheetContent = null },
                    onAddNew = {
                        activeSheetContent = null
                        showCreateAccountDialog = true
                    },
                    onDismiss = { activeSheetContent = null }
                )
                is AddSheetContent.Category -> AddCategoryPickerSheet(
                    items = categories,
                    onItemSelected = { selectedCategory = it; activeSheetContent = null },
                    onAddNew = {
                        activeSheetContent = null
                        showCreateCategoryDialog = true
                    },
                    onDismiss = { activeSheetContent = null }
                )
                is AddSheetContent.Tags -> AddTagPickerSheet(
                    allTags = allTags,
                    selectedTags = selectedTags,
                    onTagSelected = viewModel::onTagSelected,
                    onAddNewTag = viewModel::addTagOnTheGo,
                    onConfirm = { activeSheetContent = null },
                    onDismiss = { activeSheetContent = null }
                )
                else -> {}
            }
        }
    }

    if (showCreateAccountDialog) {
        CreateAccountDialog(
            onDismiss = { showCreateAccountDialog = false },
            onConfirm = { name, type ->
                viewModel.createAccount(name, type) { newAccount ->
                    selectedAccount = newAccount
                }
                showCreateAccountDialog = false
            }
        )
    }

    if (showCreateCategoryDialog) {
        CreateCategoryDialog(
            onDismiss = { showCreateCategoryDialog = false },
            onConfirm = { name, iconKey, colorKey ->
                viewModel.createCategory(name, iconKey, colorKey) { newCategory ->
                    selectedCategory = newCategory
                }
                showCreateCategoryDialog = false
            }
        )
    }


    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateTime.timeInMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val cal = Calendar.getInstance().apply { timeInMillis = it }
                        selectedDateTime.set(Calendar.YEAR, cal.get(Calendar.YEAR))
                        selectedDateTime.set(Calendar.MONTH, cal.get(Calendar.MONTH))
                        selectedDateTime.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH))
                    }
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedDateTime.get(Calendar.HOUR_OF_DAY),
            initialMinute = selectedDateTime.get(Calendar.MINUTE)
        )
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            onConfirm = {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                selectedDateTime.set(Calendar.MINUTE, timePickerState.minute)
                showTimePicker = false
            }
        ) { TimePicker(state = timePickerState) }
    }
}

@Composable
fun PrimaryInfoCard(
    amount: String,
    onAmountChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    Card(elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            BasicTextField(
                value = amount,
                onValueChange = onAmountChange,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                textStyle = MaterialTheme.typography.displayMedium.copy(
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "₹",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (amount.isEmpty()) {
                                Text(
                                    "0.00",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.End
                                )
                            }
                            innerTextField()
                        }
                    }
                }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth().testTag("description_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                ),
                placeholder = { Text("What did you spend on?") }
            )
        }
    }
}

@Composable
fun DetailsCard(
    selectedAccount: Account?,
    onAccountClick: () -> Unit,
    selectedCategory: Category?,
    onCategoryClick: () -> Unit,
    isCategoryVisible: Boolean,
    selectedDate: Date,
    onDateClick: () -> Unit,
    tags: Set<Tag>,
    onTagsClick: () -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    attachmentsCount: Int,
    onAttachmentsClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("EEE, dd MMMMyyyy", Locale.getDefault()) }

    Card(elevation = CardDefaults.cardElevation(2.dp)) {
        Column {
            DetailRow(
                icon = Icons.Default.AccountBalanceWallet,
                label = "Account",
                value = selectedAccount?.name ?: "Select account",
                onClick = onAccountClick,
                valueColor = if (selectedAccount == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            if (isCategoryVisible) {
                HorizontalDivider()
                DetailRow(
                    icon = Icons.Default.Category,
                    label = "Category",
                    value = selectedCategory?.name ?: "Select category",
                    onClick = onCategoryClick,
                    valueColor = if (selectedCategory == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    leadingIcon = { selectedCategory?.let { CategoryIcon(it, Modifier.size(24.dp)) } }
                )
            }
            HorizontalDivider()
            DetailRow(
                icon = Icons.Default.DateRange,
                label = "Date",
                value = dateFormatter.format(selectedDate),
                onClick = onDateClick
            )
            HorizontalDivider()
            DetailRow(
                icon = Icons.Default.NewLabel,
                label = "Tags",
                value = if (tags.isEmpty()) "Add tags" else tags.joinToString { it.name },
                onClick = onTagsClick
            )
            HorizontalDivider()
            DetailRow(
                icon = Icons.Default.Attachment,
                label = "Attach Photo",
                value = if (attachmentsCount > 0) "$attachmentsCount image(s)" else "Add receipt",
                onClick = onAttachmentsClick
            )
            HorizontalDivider()
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
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


@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


@Composable
private fun SegmentedButton(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        options.forEach { option ->
            Button(
                onClick = { onOptionSelected(option) },
                modifier = Modifier.weight(1f),
                shape = CircleShape,
                colors = if (option == selectedOption) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                } else {
                    ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface)
                },
                elevation = if (option == selectedOption) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null
            ) {
                Text(option)
            }
        }
    }
}

@Composable
private fun AddAccountPickerSheet(
    items: List<Account>,
    onItemSelected: (Account) -> Unit,
    onAddNew: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.navigationBarsPadding()) {
        Text(
            "Select Account",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )
        LazyColumn {
            items(items) { item ->
                ListItem(
                    headlineContent = { Text(item.name) },
                    modifier = Modifier.clickable { onItemSelected(item) }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("+ Create New Account") },
                    leadingContent = { Icon(Icons.Default.Add, contentDescription = "Create New Account") },
                    modifier = Modifier.clickable(onClick = onAddNew)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AddCategoryPickerSheet(
    items: List<Category>,
    onItemSelected: (Category) -> Unit,
    onAddNew: () -> Unit,
    onDismiss: () -> Unit
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
            item {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onAddNew)
                        .padding(vertical = 12.dp)
                        .height(76.dp), // Match height of other items
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Create New", modifier = Modifier.size(48.dp))
                    Text("New", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddTagPickerSheet(
    allTags: List<Tag>,
    selectedTags: Set<Tag>,
    onTagSelected: (Tag) -> Unit,
    onAddNewTag: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
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
            TextButton(onClick = onDismiss) { Text("Cancel") }
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
