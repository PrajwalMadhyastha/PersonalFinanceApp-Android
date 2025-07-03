// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/AddTransactionScreen.kt
// REASON: UX REFINEMENT - The "Expense"/"Income" switch has been moved from its
// own separate card into the "Account" row within the DetailsCard. This provides
// a more integrated and contextually relevant UI, consistent with the changes
// made to the TransactionDetailScreen.
// =================================================================================
package io.pm.finlight.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.pm.finlight.*
import io.pm.finlight.ui.components.CreateAccountDialog
import io.pm.finlight.ui.components.CreateCategoryDialog
import io.pm.finlight.ui.components.TimePickerDialog
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.*

private sealed class AddSheetContent {
    object Account : AddSheetContent()
    object Category : AddSheetContent()
    object Tags : AddSheetContent()
    object Notes : AddSheetContent()
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
    // region State Variables
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("expense") }
    var notes by remember { mutableStateOf("") }
    var attachedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        attachedImageUris = attachedImageUris + uris
    }

    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    val categories by viewModel.allCategories.collectAsState(initial = emptyList())
    val allTags by viewModel.allTags.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()
    val defaultAccount by viewModel.defaultAccount.collectAsState()
    val validationError by viewModel.validationError.collectAsState()

    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    val selectedDateTime by remember { mutableStateOf(Calendar.getInstance()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var activeSheetContent by remember { mutableStateOf<AddSheetContent?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showCreateAccountDialog by remember { mutableStateOf(false) }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var showTypeDropdown by remember { mutableStateOf(false) }

    val amountFocusRequester = remember { FocusRequester() }

    val isSaveEnabled = amount.isNotBlank() && description.isNotBlank() && selectedAccount != null && selectedCategory != null
    // endregion

    // region Helper Functions & Effects
    fun resetAllState() {
        amount = ""
        description = ""
        transactionType = "expense"
        notes = ""
        attachedImageUris = emptyList()
        selectedCategory = null
        selectedDateTime.timeInMillis = System.currentTimeMillis()
        viewModel.clearAddTransactionState()
        // Keep selected account for convenience
    }

    LaunchedEffect(Unit) {
        amountFocusRequester.requestFocus()
        viewModel.clearAddTransactionState()
    }

    LaunchedEffect(defaultAccount, isCsvEdit) {
        if (!isCsvEdit && selectedAccount == null) {
            selectedAccount = defaultAccount
        }
    }

    // Handle initial data for CSV editing
    LaunchedEffect(initialDataJson, accounts, categories) {
        if (isCsvEdit && initialDataJson != null) {
            try {
                val gson = Gson()
                val initialData: List<String> = gson.fromJson(URLDecoder.decode(initialDataJson, "UTF-8"), object : TypeToken<List<String>>() {}.type)
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

                selectedCategory = categories.find { it.name.equals(categoryName, ignoreCase = true) }
                selectedAccount = accounts.find { it.name.equals(accountName, ignoreCase = true) }

            } catch (e: Exception) {
                Toast.makeText(context, "Error loading row data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Display validation errors
    LaunchedEffect(validationError) {
        validationError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }
    // endregion

    Scaffold(
        topBar = {
            AddTransactionTopBar(
                transactionType = transactionType,
                onTypeChange = { transactionType = it },
                onClose = { navController.popBackStack() }
            )
        },
        bottomBar = {
            AddTransactionBottomBar(
                isSaveEnabled = isSaveEnabled,
                onSave = {
                    scope.launch {
                        val success = viewModel.addTransaction(
                            description = description,
                            categoryId = selectedCategory?.id,
                            amountStr = amount,
                            accountId = selectedAccount!!.id,
                            notes = notes.takeIf { it.isNotBlank() },
                            date = selectedDateTime.timeInMillis,
                            transactionType = transactionType,
                            imageUris = attachedImageUris
                        )
                        if (success) {
                            navController.popBackStack()
                        }
                    }
                },
                onSaveAndAddAnother = {
                    scope.launch {
                        val success = viewModel.addTransaction(
                            description = description,
                            categoryId = selectedCategory?.id,
                            amountStr = amount,
                            accountId = selectedAccount!!.id,
                            notes = notes.takeIf { it.isNotBlank() },
                            date = selectedDateTime.timeInMillis,
                            transactionType = transactionType,
                            imageUris = attachedImageUris
                        )
                        if (success) {
                            resetAllState()
                            amountFocusRequester.requestFocus()
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        // region Main Layout
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AmountInput(
                    amount = amount,
                    onAmountChange = { amount = it },
                    focusRequester = amountFocusRequester
                )
            }

            item {
                DescriptionDateTimeCard(
                    description = description,
                    onDescriptionChange = { description = it },
                    dateTime = selectedDateTime.time,
                    onDateTimeClick = { showDatePicker = true }
                )
            }

            item {
                AccountTypeCard(
                    selectedAccount = selectedAccount,
                    transactionType = transactionType,
                    onAccountClick = { activeSheetContent = AddSheetContent.Account },
                    onTypeChange = { newType -> transactionType = newType }
                )
            }

            item {
                DetailActionRow(
                    icon = Icons.Default.Category,
                    label = "Category",
                    value = selectedCategory?.name,
                    onClick = { activeSheetContent = AddSheetContent.Category }
                )
            }

            item {
                DetailActionRow(
                    icon = Icons.AutoMirrored.Filled.Notes,
                    label = "Notes",
                    value = notes.ifBlank { null },
                    onClick = { activeSheetContent = AddSheetContent.Notes }
                )
            }

            item {
                DetailActionRow(
                    icon = Icons.Default.NewLabel,
                    label = "Tags",
                    value = if (selectedTags.isEmpty()) null else selectedTags.joinToString { it.name },
                    onClick = { activeSheetContent = AddSheetContent.Tags }
                )
            }

            item {
                DetailActionRow(
                    icon = Icons.Default.Attachment,
                    label = "Attachment",
                    value = if (attachedImageUris.isEmpty()) "Photo of a receipt/warranty" else "${attachedImageUris.size} image(s)",
                    onClick = {
                        imagePickerLauncher.launch("image/*")
                    }
                )
            }
        }
        // endregion

        // region Modals and Dialogs
        if (activeSheetContent != null) {
            ModalBottomSheet(
                onDismissRequest = { activeSheetContent = null },
                sheetState = sheetState
            ) {
                when (val sheet = activeSheetContent) {
                    is AddSheetContent.Account -> AddAccountPickerSheet(
                        items = accounts,
                        onItemSelected = { selectedAccount = it; activeSheetContent = null },
                        onAddNew = { showCreateAccountDialog = true; activeSheetContent = null },
                        onDismiss = { activeSheetContent = null }
                    )
                    is AddSheetContent.Category -> AddCategoryPickerSheet(
                        items = categories,
                        onItemSelected = { selectedCategory = it; activeSheetContent = null },
                        onAddNew = { showCreateCategoryDialog = true; activeSheetContent = null },
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
                    is AddSheetContent.Notes -> NotesInputSheet(
                        initialValue = notes,
                        onConfirm = { notes = it; activeSheetContent = null },
                        onDismiss = { activeSheetContent = null }
                    )
                    null -> { /* This case is added to make the 'when' exhaustive */ }
                }
            }
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
        // endregion
    }
}

// region New UI Components
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionTopBar(
    transactionType: String,
    onTypeChange: (String) -> Unit,
    onClose: () -> Unit
) {
    var showDropdown by remember { mutableStateOf(false) }
    val icon = if (transactionType == "expense") Icons.Default.NorthEast else Icons.Default.SouthWest

    CenterAlignedTopAppBar(
        title = {
            Box {
                Row(
                    modifier = Modifier.clickable { showDropdown = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(
                        text = transactionType.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Change transaction type")
                }
                DropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Expense") },
                        onClick = { onTypeChange("expense"); showDropdown = false },
                        leadingIcon = { Icon(Icons.Default.NorthEast, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Income") },
                        onClick = { onTypeChange("income"); showDropdown = false },
                        leadingIcon = { Icon(Icons.Default.SouthWest, contentDescription = null) }
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
private fun AmountInput(
    amount: String,
    onAmountChange: (String) -> Unit,
    focusRequester: FocusRequester
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Amount spent", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = amount,
            onValueChange = onAmountChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            textStyle = MaterialTheme.typography.displayMedium.copy(
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            ),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
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
                                "Enter amount",
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        innerTextField()
                    }
                    // Spacer for the right side to keep it centered
                    Spacer(Modifier.width(32.dp))
                }
            }
        )
        HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun DescriptionDateTimeCard(
    description: String,
    onDescriptionChange: (String) -> Unit,
    dateTime: Date,
    onDateTimeClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Date & time", modifier = Modifier.weight(1f))
                Text(
                    text = dateFormatter.format(dateTime),
                    modifier = Modifier.clickable(onClick = onDateTimeClick),
                    fontWeight = FontWeight.SemiBold
                )
            }
            HorizontalDivider()
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Paid to") },
                placeholder = { Text("Enter the name or place") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
private fun AccountTypeCard(
    selectedAccount: Account?,
    transactionType: String,
    onAccountClick: () -> Unit,
    onTypeChange: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = onAccountClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Account")
            Spacer(Modifier.width(16.dp))
            Text(
                text = selectedAccount?.name ?: "Select Account",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = transactionType.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 8.dp)
            )
            Switch(
                checked = transactionType == "expense",
                onCheckedChange = { isChecked ->
                    onTypeChange(if (isChecked) "expense" else "income")
                },
                thumbContent = {
                    Icon(
                        if (transactionType == "expense") Icons.Default.NorthEast else Icons.Default.SouthWest,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            )
        }
    }
}

@Composable
private fun DetailActionRow(
    icon: ImageVector,
    label: String,
    value: String?,
    onClick: () -> Unit
) {
    val hasValue = !value.isNullOrBlank()
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = if (hasValue) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyLarge,
                    color = if (hasValue) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                if (hasValue) {
                    Text(
                        text = value!!,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Icon(Icons.Default.Add, contentDescription = "Add $label")
        }
    }
}


@Composable
private fun AddTransactionBottomBar(
    isSaveEnabled: Boolean,
    onSave: () -> Unit,
    onSaveAndAddAnother: () -> Unit
) {
    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onSaveAndAddAnother,
                modifier = Modifier.weight(1f),
                enabled = isSaveEnabled
            ) {
                Text("Save & add another")
            }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = isSaveEnabled
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun NotesInputSheet(
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Add Notes", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Your notes") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { onConfirm(text) }) { Text("Done") }
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
