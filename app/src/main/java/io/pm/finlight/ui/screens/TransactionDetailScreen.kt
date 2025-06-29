package io.pm.finlight.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import io.pm.finlight.*
import io.pm.finlight.ui.components.TimePickerDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionDetailScreen(
    navController: NavController,
    transactionId: Int,
    viewModel: TransactionViewModel = viewModel()
) {
    val transactionDetails by viewModel.getTransactionDetailsById(transactionId).collectAsState(initial = null)
    val accounts by viewModel.allAccounts.collectAsState()
    val categories by viewModel.allCategories.collectAsState(initial = emptyList())
    val allTags by viewModel.allTags.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()
    val attachedImages by viewModel.transactionImages.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var showDescriptionDialog by remember { mutableStateOf(false) }
    var showAmountDialog by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf<Uri?>(null) }
    var showImageDeleteDialog by remember { mutableStateOf<TransactionImage?>(null) }


    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.attachPhotoToTransaction(transactionId, it)
        }
    }

    LaunchedEffect(transactionId) {
        viewModel.loadTagsForTransaction(transactionId)
        viewModel.loadImagesForTransaction(transactionId)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearSelectedTags()
        }
    }

    val title = when (transactionDetails?.transaction?.transactionType) {
        "expense" -> "Debit transaction"
        "income" -> "Credit transaction"
        else -> "Transaction Details"
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
        transactionDetails?.let { details ->
            val calendar = remember { Calendar.getInstance().apply { timeInMillis = details.transaction.date } }

            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
            ) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(vertical=16.dp)){
                    item {
                        TransactionHeaderCard(
                            details = details,
                            onDescriptionClick = { showDescriptionDialog = true },
                            onAmountClick = { showAmountDialog = true },
                            onCategoryClick = { showCategoryPicker = true },
                            onDateTimeClick = { showDatePicker = true }
                        )
                    }
                    item {
                        InfoCard(
                            icon = Icons.Default.AccountBalanceWallet,
                            label = "Account",
                            value = details.accountName ?: "N/A",
                            onClick = { showAccountPicker = true }
                        )
                    }
                    item {
                        InfoCard(
                            icon = Icons.AutoMirrored.Filled.Notes,
                            label = "Notes",
                            value = details.transaction.notes ?: "Tap to add",
                            onClick = { showNotesDialog = true }
                        )
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(2.dp),
                            onClick = { showTagPicker = true }
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
                            value = if(attachedImages.isEmpty()) "Tap to add a receipt or photo" else "${attachedImages.size} image(s) attached",
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
                }
            }


            if (showTagPicker) {
                TagPickerDialog(
                    allTags = allTags,
                    selectedTags = selectedTags,
                    onDismiss = { showTagPicker = false },
                    onTagSelected = viewModel::onTagSelected,
                    onAddNewTag = viewModel::addTagOnTheGo,
                    onConfirm = {
                        viewModel.updateTagsForTransaction(transactionId)
                        showTagPicker = false
                    }
                )
            }
            if (showDescriptionDialog) {
                EditDialog(
                    title = "Edit Description",
                    initialValue = details.transaction.description,
                    onDismiss = { showDescriptionDialog = false },
                    onConfirm = { viewModel.updateTransactionDescription(transactionId, it) }
                )
            }
            if (showAmountDialog) {
                EditDialog(
                    title = "Edit Amount",
                    initialValue = "%.2f".format(details.transaction.amount),
                    keyboardType = KeyboardType.Number,
                    onDismiss = { showAmountDialog = false },
                    onConfirm = { viewModel.updateTransactionAmount(transactionId, it) }
                )
            }
            if (showNotesDialog) {
                EditDialog(
                    title = "Edit Notes",
                    initialValue = details.transaction.notes ?: "",
                    onDismiss = { showNotesDialog = false },
                    onConfirm = { viewModel.updateTransactionNotes(transactionId, it) }
                )
            }
            if (showAccountPicker) {
                Picker_Dialog(
                    title = "Select Account",
                    items = accounts,
                    onDismiss = { showAccountPicker = false },
                    onItemSelected = { account -> viewModel.updateTransactionAccount(transactionId, (account as Account).id) },
                    getItemName = { (it as Account).name }
                )
            }
            if (showCategoryPicker) {
                Picker_Dialog(
                    title = "Select Category",
                    items = categories,
                    onDismiss = { showCategoryPicker = false },
                    onItemSelected = { category -> viewModel.updateTransactionCategory(transactionId, (category as Category).id) },
                    getItemName = { (it as Category).name }
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
                                navController.popBackStack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("Delete") }
                    },
                    dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
                )
            }

            if(showImageViewer != null){
                Dialog(onDismissRequest = { showImageViewer = null }) {
                    AsyncImage(
                        model = showImageViewer,
                        contentDescription = "Full screen image",
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
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


        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun TransactionHeaderCard(
    details: TransactionDetails,
    onDescriptionClick: () -> Unit,
    onAmountClick: () -> Unit,
    onCategoryClick: () -> Unit,
    onDateTimeClick: () -> Unit,
) {
    val dateFormatter = remember { SimpleDateFormat("EEE, dd MMM yyyy, h:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = details.transaction.description,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable(onClick = onDescriptionClick)
            )

            Text(
                text = "₹${"%,.2f".format(details.transaction.amount)}",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable(onClick = onAmountClick)
            )

            ChipWithIcon(
                text = details.categoryName ?: "Uncategorized",
                icon = CategoryIconHelper.getIcon(details.categoryIconKey ?: "category"),
                colorKey = details.categoryColorKey ?: "gray_light",
                onClick = onCategoryClick
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormatter.format(Date(details.transaction.date)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(onClick = onDateTimeClick)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Transaction Source",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = details.transaction.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ChipWithIcon(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    colorKey: String,
    onClick: () -> Unit
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(20.dp)
        )
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
            Column {
                Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun EditDialog(
    title: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    var text by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Value") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    capitalization = if (keyboardType == KeyboardType.Text) KeyboardCapitalization.Sentences else KeyboardCapitalization.None
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(text)
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun <T> Picker_Dialog(
    title: String,
    items: List<T>,
    onDismiss: () -> Unit,
    onItemSelected: (T) -> Unit,
    getItemName: (T) -> String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
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
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagPickerDialog(
    allTags: List<Tag>,
    selectedTags: Set<Tag>,
    onDismiss: () -> Unit,
    onTagSelected: (Tag) -> Unit,
    onAddNewTag: (String) -> Unit,
    onConfirm: () -> Unit
) {
    var newTagName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Tags") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
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
            }
        },
        confirmButton = {
            Button(onClick = {
                if (newTagName.isNotBlank()) {
                    onAddNewTag(newTagName)
                }
                onConfirm()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
