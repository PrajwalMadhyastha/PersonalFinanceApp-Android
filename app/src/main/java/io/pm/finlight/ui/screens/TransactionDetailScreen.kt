// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/TransactionDetailScreen.kt
// REASON: FIX - The logic for creating a rename rule has been made more robust.
// 1. The "Always rename" checkbox is now always visible when editing a description.
// 2. The `originalNameForRule` is now correctly determined by using the
//    `originalDescription` if it exists, otherwise falling back to the current
//    `description` (the value before the edit). This ensures that rules can be
//    created for any transaction, not just imported ones.
// FEATURE - The `TransactionHeaderCard` has been redesigned to display a large,
// semi-transparent background image corresponding to the transaction's category,
// creating a more visually engaging and contextual header.
// BUG FIX - The `TransactionHeaderCard` has been refactored to use a layered
// Box layout. This ensures the background image always crops and fills the
// available space uniformly, fixing inconsistent sizing issues. The image alpha
// and text colors have also been adjusted for better visibility.
// VISUAL - Increased the minimum height of the TransactionHeaderCard and adjusted
// the background image alpha for a more prominent and visually appealing look.
// VISUAL - The content layout within the `TransactionHeaderCard` has been updated
// to spread the elements vertically, making better use of the larger card size.
// BUG FIX - Replaced the unresolved `ChipDefaults.LeadingIconSize` with a hardcoded
// size of `18.dp` to resolve a compilation error and ensure consistent icon sizing.
// BUG FIX - Added a `LaunchedEffect` to separately load the visit count when
// the transaction details are available, fixing an infinite recomposition loop.
// BUG FIX - The `LaunchedEffect` for loading the visit count now correctly
// passes the `originalDescription` to ensure an accurate count for renamed
// merchants.
// =================================================================================
package io.pm.finlight.ui.screens

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.gson.Gson
import io.pm.finlight.*
import io.pm.finlight.ui.components.CreateAccountDialog
import io.pm.finlight.ui.components.CreateCategoryDialog
import io.pm.finlight.ui.components.TimePickerDialog
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "DetailScreenDebug"

private sealed class SheetContent {
    object Description : SheetContent()
    object Amount : SheetContent()
    object Notes : SheetContent()
    object Account : SheetContent()
    object Category : SheetContent()
    object Tags : SheetContent()
}

private sealed interface DetailScreenState {
    object Loading : DetailScreenState
    data class Success(val details: TransactionDetails) : DetailScreenState
    object Exit : DetailScreenState
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionDetailScreen(
    navController: NavController,
    transactionId: Int,
    viewModel: TransactionViewModel = viewModel(),
    onSaveRenameRule: (originalName: String, newName: String) -> Unit
) {
    Log.d(TAG, "Composing TransactionDetailScreen for transactionId: $transactionId")

    val screenState by produceState<DetailScreenState>(initialValue = DetailScreenState.Loading, transactionId) {
        viewModel.findTransactionDetailsById(transactionId).collect { details ->
            value = if (details != null) {
                DetailScreenState.Success(details)
            } else {
                DetailScreenState.Exit
            }
        }
    }

    val reparseResult = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<Boolean>("reparse_needed")
        ?.observeAsState()

    LaunchedEffect(reparseResult?.value) {
        if (reparseResult?.value == true) {
            Log.d("DetailScreen", "Reparse needed signal received for txn ID: $transactionId")
            viewModel.reparseTransactionFromSms(transactionId)
            navController.currentBackStackEntry?.savedStateHandle?.set("reparse_needed", false)
        }
    }


    val accounts by viewModel.allAccounts.collectAsState()
    val categories by viewModel.allCategories.collectAsState(initial = emptyList())
    val allTags by viewModel.allTags.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()
    val attachedImages by viewModel.transactionImages.collectAsState()
    val originalSms by viewModel.originalSmsText.collectAsState()
    val visitCount by viewModel.visitCount.collectAsState()
    val scope = rememberCoroutineScope()

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf<Uri?>(null) }
    var showImageDeleteDialog by remember { mutableStateOf<TransactionImage?>(null) }

    var activeSheetContent by remember { mutableStateOf<SheetContent?>(null) }
    val sheetState = rememberModalBottomSheetState()

    var showCreateAccountDialog by remember { mutableStateOf(false) }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.attachPhotoToTransaction(transactionId, it)
        }
    }

    val context = LocalContext.current
    LaunchedEffect(transactionId) {
        NotificationManagerCompat.from(context).cancel(transactionId)
        viewModel.loadTagsForTransaction(transactionId)
        viewModel.loadImagesForTransaction(transactionId)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearSelectedTags()
            viewModel.clearOriginalSms()
        }
    }

    when (val state = screenState) {
        is DetailScreenState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is DetailScreenState.Exit -> {
            LaunchedEffect(Unit) {
                navController.popBackStack()
            }
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }
        is DetailScreenState.Success -> {
            val details = state.details
            val title = when (details.transaction.transactionType) {
                "expense" -> "Debit transaction"
                "income" -> "Credit transaction"
                else -> "Transaction Details"
            }
            val calendar = remember { Calendar.getInstance().apply { timeInMillis = details.transaction.date } }

            LaunchedEffect(details.transaction.originalDescription, details.transaction.description) {
                viewModel.loadVisitCount(details.transaction.originalDescription, details.transaction.description)
            }

            LaunchedEffect(details.transaction.sourceSmsId) {
                viewModel.loadOriginalSms(details.transaction.sourceSmsId)
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(title) },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        showMenu = false
                                        showDeleteDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp)
                ) {
                    item {
                        TransactionHeaderCard(
                            details = details,
                            visitCount = visitCount,
                            onDescriptionClick = { activeSheetContent = SheetContent.Description },
                            onAmountClick = { activeSheetContent = SheetContent.Amount },
                            onCategoryClick = { activeSheetContent = SheetContent.Category },
                            onDateTimeClick = { showDatePicker = true }
                        )
                    }
                    item {
                        AccountCardWithSwitch(
                            details = details,
                            onAccountClick = { activeSheetContent = SheetContent.Account },
                            onExcludeToggled = { isExcluded ->
                                viewModel.updateTransactionExclusion(details.transaction.id, isExcluded)
                            }
                        )
                    }
                    item {
                        InfoCard(
                            icon = Icons.AutoMirrored.Filled.Notes,
                            label = "Notes",
                            value = details.transaction.notes ?: "Tap to add",
                            onClick = { activeSheetContent = SheetContent.Notes }
                        )
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(2.dp),
                            onClick = { activeSheetContent = SheetContent.Tags }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.NewLabel, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Tags", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (selectedTags.isEmpty()) {
                                        Text("Tap to add tags")
                                    } else {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            selectedTags.forEach { tag ->
                                                AssistChip(onClick = {}, label = { Text(tag.name) })
                                            }
                                        }
                                    }
                                }
                                Icon(Icons.Default.Add, contentDescription = "Add Tag")
                            }
                        }
                    }

                    item {
                        InfoCard(
                            icon = Icons.Default.Attachment,
                            label = "Attachments",
                            value = if (attachedImages.isEmpty()) "Tap to add a receipt or photo" else "${attachedImages.size} image(s) attached",
                            onClick = { imagePickerLauncher.launch("image/*") }
                        )
                    }

                    if (attachedImages.isNotEmpty()) {
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(attachedImages) { image ->
                                    Box {
                                        AsyncImage(
                                            model = File(image.imageUri),
                                            contentDescription = "Transaction Attachment",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { showImageViewer = File(image.imageUri).toUri() }
                                        )
                                        IconButton(
                                            onClick = { showImageDeleteDialog = image },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(24.dp)
                                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Delete Attachment",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (details.transaction.sourceSmsId != null) {
                        item {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val smsMessage = viewModel.getOriginalSmsMessage(details.transaction.sourceSmsId!!)
                                        if (smsMessage != null) {
                                            val potentialTxn = PotentialTransaction(
                                                sourceSmsId = smsMessage.id,
                                                smsSender = smsMessage.sender,
                                                amount = details.transaction.amount,
                                                transactionType = details.transaction.transactionType,
                                                merchantName = details.transaction.description,
                                                originalMessage = smsMessage.body,
                                                sourceSmsHash = details.transaction.sourceSmsHash
                                            )
                                            val json = Gson().toJson(potentialTxn)
                                            val encodedJson = URLEncoder.encode(json, "UTF-8")
                                            navController.navigate("rule_creation_screen/$encodedJson")
                                        } else {
                                            Toast.makeText(context, "Original SMS not found.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Build, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Fix Parsing")
                            }
                        }
                    }

                    if (!originalSms.isNullOrBlank()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(2.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Message,
                                            contentDescription = "Original SMS",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "Original SMS Message",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = originalSms!!,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 20.sp
                                    )
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
                        TransactionEditSheetContent(
                            sheetContent = activeSheetContent!!,
                            details = details,
                            viewModel = viewModel,
                            onSaveRenameRule = onSaveRenameRule,
                            accounts = accounts,
                            categories = categories,
                            allTags = allTags,
                            selectedTags = selectedTags,
                            onDismiss = { activeSheetContent = null },
                            onAddNewAccount = {
                                activeSheetContent = null
                                showCreateAccountDialog = true
                            },
                            onAddNewCategory = {
                                activeSheetContent = null
                                showCreateCategoryDialog = true
                            }
                        )
                    }
                }

                if (showCreateAccountDialog) {
                    CreateAccountDialog(
                        onDismiss = { showCreateAccountDialog = false },
                        onConfirm = { name, type ->
                            viewModel.createAccount(name, type) { newAccount ->
                                viewModel.updateTransactionAccount(transactionId, newAccount.id)
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
                                viewModel.updateTransactionCategory(transactionId, newCategory.id)
                            }
                            showCreateCategoryDialog = false
                        }
                    )
                }

                if (showDatePicker) {
                    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = calendar.timeInMillis)
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let {
                                    calendar.timeInMillis = it
                                }
                                showDatePicker = false
                                showTimePicker = true
                            }) { Text("OK") }
                        }
                    ) { DatePicker(state = datePickerState) }
                }
                if (showTimePicker) {
                    val timePickerState = rememberTimePickerState(initialHour = calendar.get(Calendar.HOUR_OF_DAY), initialMinute = calendar.get(Calendar.MINUTE))
                    TimePickerDialog(
                        onDismissRequest = { showTimePicker = false },
                        onConfirm = {
                            calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            calendar.set(Calendar.MINUTE, timePickerState.minute)
                            viewModel.updateTransactionDate(transactionId, calendar.timeInMillis)
                            showTimePicker = false
                        }
                    ) { TimePicker(state = timePickerState) }
                }

                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Delete Transaction?") },
                        text = { Text("Are you sure you want to permanently delete this transaction? This action cannot be undone.") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.deleteTransaction(details.transaction)
                                    showDeleteDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) { Text("Delete") }
                        },
                        dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
                    )
                }

                if (showImageViewer != null) {
                    Dialog(onDismissRequest = { showImageViewer = null }) {
                        AsyncImage(
                            model = showImageViewer,
                            contentDescription = "Full screen image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                }

                if (showImageDeleteDialog != null) {
                    AlertDialog(
                        onDismissRequest = { showImageDeleteDialog = null },
                        title = { Text("Delete Attachment?") },
                        text = { Text("Are you sure you want to delete this attachment? This action cannot be undone.") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.deleteTransactionImage(showImageDeleteDialog!!)
                                    showImageDeleteDialog = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) { Text("Delete") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showImageDeleteDialog = null }) { Text("Cancel") }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionEditSheetContent(
    sheetContent: SheetContent,
    details: TransactionDetails,
    viewModel: TransactionViewModel,
    onSaveRenameRule: (originalName: String, newName: String) -> Unit,
    accounts: List<Account>,
    categories: List<Category>,
    allTags: List<Tag>,
    selectedTags: Set<Tag>,
    onDismiss: () -> Unit,
    onAddNewAccount: () -> Unit,
    onAddNewCategory: () -> Unit
) {
    val transactionId = details.transaction.id
    val scope = rememberCoroutineScope()

    when (sheetContent) {
        is SheetContent.Description -> {
            var saveForFuture by remember { mutableStateOf(false) }
            EditTextFieldSheet(
                title = "Edit Description",
                initialValue = details.transaction.description,
                onConfirm = { newDescription ->
                    scope.launch {
                        val originalNameForRule = details.transaction.originalDescription ?: details.transaction.description
                        viewModel.updateTransactionDescription(transactionId, newDescription)
                        if (saveForFuture) {
                            if (originalNameForRule.isNotBlank() && newDescription.isNotBlank()) {
                                onSaveRenameRule(originalNameForRule, newDescription)
                            }
                        }
                    }
                },
                onDismiss = onDismiss
            ) {
                val originalNameForRule = details.transaction.originalDescription ?: details.transaction.description
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { saveForFuture = !saveForFuture }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = saveForFuture, onCheckedChange = { saveForFuture = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Always rename '$originalNameForRule' to this")
                }
            }
        }
        is SheetContent.Amount -> {
            EditTextFieldSheet(
                title = "Edit Amount",
                initialValue = "%.2f".format(details.transaction.amount),
                keyboardType = KeyboardType.Number,
                onConfirm = { viewModel.updateTransactionAmount(transactionId, it) },
                onDismiss = onDismiss
            )
        }
        is SheetContent.Notes -> {
            EditTextFieldSheet(
                title = "Edit Notes",
                initialValue = details.transaction.notes ?: "",
                onConfirm = { viewModel.updateTransactionNotes(transactionId, it) },
                onDismiss = onDismiss
            )
        }
        is SheetContent.Account -> {
            PickerSheet(
                title = "Select Account",
                items = accounts,
                getItemName = { (it as Account).name },
                onItemSelected = { viewModel.updateTransactionAccount(transactionId, (it as Account).id) },
                onDismiss = onDismiss,
                onAddNew = onAddNewAccount
            )
        }
        is SheetContent.Category -> {
            CategoryPickerSheet(
                title = "Select Category",
                items = categories,
                onItemSelected = { viewModel.updateTransactionCategory(transactionId, it.id) },
                onDismiss = onDismiss,
                onAddNew = onAddNewCategory
            )
        }
        is SheetContent.Tags -> {
            TagPickerSheet(
                allTags = allTags,
                selectedTags = selectedTags,
                onTagSelected = viewModel::onTagSelected,
                onAddNewTag = viewModel::addTagOnTheGo,
                onConfirm = {
                    viewModel.updateTagsForTransaction(transactionId)
                    onDismiss()
                },
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun EditTextFieldSheet(
    title: String,
    initialValue: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    additionalContent: @Composable (() -> Unit)? = null
) {
    var text by remember { mutableStateOf(initialValue) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Value") },
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                capitalization = if (keyboardType == KeyboardType.Text) KeyboardCapitalization.Sentences else KeyboardCapitalization.None
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("value_input")
        )
        additionalContent?.invoke()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                onConfirm(text)
                onDismiss()
            }) { Text("Save") }
        }
    }
}

@Composable
private fun <T> PickerSheet(
    title: String,
    items: List<T>,
    getItemName: (T) -> String,
    onItemSelected: (T) -> Unit,
    onDismiss: () -> Unit,
    onAddNew: (() -> Unit)? = null
) {
    Column(modifier = Modifier.navigationBarsPadding()) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )
        LazyColumn {
            items(items) { item ->
                ListItem(
                    headlineContent = { Text(getItemName(item)) },
                    modifier = Modifier.clickable {
                        onItemSelected(item)
                        onDismiss()
                    }
                )
            }
            if (onAddNew != null) {
                item {
                    ListItem(
                        headlineContent = { Text("Create New") },
                        leadingContent = { Icon(Icons.Default.Add, contentDescription = "Create New") },
                        modifier = Modifier.clickable(onClick = onAddNew)
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun CategoryPickerSheet(
    title: String,
    items: List<Category>,
    onItemSelected: (Category) -> Unit,
    onDismiss: () -> Unit,
    onAddNew: (() -> Unit)? = null
) {
    Column(modifier = Modifier.navigationBarsPadding()) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items) { category ->
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onItemSelected(category)
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryIconDisplay(category)
                    Text(category.name, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (onAddNew != null) {
                item {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onAddNew)
                            .padding(vertical = 12.dp)
                            .height(80.dp), // Match height of other items
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Create New", modifier = Modifier.size(48.dp))
                        Text("New", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagPickerSheet(
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
            .padding(16.dp),
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
            }) { Text("Save") }
        }
    }
}

@Composable
private fun CategoryIconDisplay(category: Category) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(CategoryIconHelper.getIconBackgroundColor(category.colorKey)),
        contentAlignment = Alignment.Center
    ) {
        if (category.iconKey == "letter_default") {
            Text(
                text = category.name.firstOrNull()?.uppercase() ?: "?",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
        } else {
            Icon(
                imageVector = CategoryIconHelper.getIcon(category.iconKey),
                contentDescription = category.name,
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


@Composable
private fun TransactionHeaderCard(
    details: TransactionDetails,
    visitCount: Int,
    onDescriptionClick: () -> Unit,
    onAmountClick: () -> Unit,
    onCategoryClick: () -> Unit,
    onDateTimeClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("EEE, dd MMMM yy, h:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 400.dp),
        ) {
            // Layer 1: The Background Image
            Image(
                painter = painterResource(id = CategoryIconHelper.getCategoryBackground(details.categoryIconKey)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
                alpha = 0.5f
            )

            // Layer 2: A darkening scrim
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            // Layer 3: The actual content, using Box alignments
            // Top Content (Description and Visit Count)
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = details.transaction.description,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    modifier = Modifier.clickable(onClick = onDescriptionClick)
                )
                if (visitCount > 1) {
                    AssistChip(
                        onClick = { /* No action needed */ },
                        label = { Text("$visitCount visits") },
                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            labelColor = Color.White,
                            leadingIconContentColor = Color.White
                        )
                    )
                }
            }


            // Center Content (Amount and Category)
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "₹${"%,.2f".format(details.transaction.amount)}",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.clickable(onClick = onAmountClick)
                )
                Spacer(Modifier.height(12.dp))
                ChipWithIcon(
                    text = details.categoryName ?: "Uncategorized",
                    icon = CategoryIconHelper.getIcon(details.categoryIconKey ?: "category"),
                    colorKey = details.categoryColorKey ?: "gray_light",
                    onClick = onCategoryClick,
                    category = details.toCategory()
                )
            }

            // Bottom Content (Date and Source)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormatter.format(Date(details.transaction.date)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.clickable(onClick = onDateTimeClick)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Transaction Source",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = details.transaction.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountCardWithSwitch(
    details: TransactionDetails,
    onAccountClick: () -> Unit,
    onExcludeToggled: (Boolean) -> Unit
) {
    val switchLabel = details.transaction.transactionType.replaceFirstChar { it.titlecase(Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AccountBalanceWallet,
                contentDescription = "Account",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(16.dp))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onAccountClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(details.accountName ?: "N/A", style = MaterialTheme.typography.bodyLarge)
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Account",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = switchLabel,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 8.dp)
            )
            Switch(
                checked = !details.transaction.isExcluded,
                onCheckedChange = { isChecked ->
                    onExcludeToggled(!isChecked)
                }
            )
        }
    }
}


private fun TransactionDetails.toCategory(): Category {
    return Category(
        id = this.transaction.categoryId ?: 0,
        name = this.categoryName ?: "Uncategorized",
        iconKey = this.categoryIconKey ?: "category",
        colorKey = this.categoryColorKey ?: "gray_light"
    )
}

@Composable
private fun ChipWithIcon(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    colorKey: String,
    onClick: () -> Unit,
    category: Category
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(CategoryIconHelper.getIconBackgroundColor(colorKey).copy(alpha = 0.9f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (category.iconKey == "letter_default") {
            Text(
                text = category.name.firstOrNull()?.uppercase() ?: "?",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun InfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick = onClick,
        enabled = onClick != {}
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyLarge)
            }
            Icon(Icons.Default.Edit, contentDescription = "Edit")
        }
    }
}
