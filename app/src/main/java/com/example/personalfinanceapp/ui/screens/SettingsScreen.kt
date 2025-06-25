package com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.personalfinanceapp.DataExportService
import com.example.personalfinanceapp.SettingsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isScanning by viewModel.isScanning.collectAsState()

    val isAppLockEnabled by viewModel.appLockEnabled.collectAsState()
    val isWeeklySummaryEnabled by viewModel.weeklySummaryEnabled.collectAsState()
    val isDailyReminderEnabled by viewModel.dailyReminderEnabled.collectAsState()
    val isUnknownTransactionPopupEnabled by viewModel.unknownTransactionPopupEnabled.collectAsState()

    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNotificationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasSmsPermission = perms[Manifest.permission.READ_SMS] == true && perms[Manifest.permission.RECEIVE_SMS] == true
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }
    var showImportConfirmDialog by remember { mutableStateOf(false) }

    val fileSaverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    val jsonString = DataExportService.exportToJsonString(context)
                    if (jsonString != null) {
                        try {
                            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                                outputStream.write(jsonString.toByteArray())
                            }
                            Toast.makeText(context, "Data exported successfully!", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error saving file.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Error exporting data.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    )

    var showImportJsonDialog by remember { mutableStateOf(false) }
    var showImportCsvDialog by remember { mutableStateOf(false) }

    // --- ADDED: Launcher for saving the CSV file ---
    val csvFileSaverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    val csvString = DataExportService.exportToCsvString(context)
                    if (csvString != null) {
                        try {
                            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                                outputStream.write(csvString.toByteArray())
                            }
                            Toast.makeText(context, "CSV exported successfully!", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error saving CSV file.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Error exporting CSV data.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    )

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    val success = DataExportService.importDataFromJson(context, it)
                    if (success) {
                        Toast.makeText(context, "Data imported successfully! Please restart the app.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Failed to import data.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    )

    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                Log.d("SettingsScreen", "CSV file selected: $it. Starting validation.")
                viewModel.validateCsvFile(it)
                navController.navigate("csv_validation_screen")
            }
        }
    )

    val jsonImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    if (DataExportService.importDataFromJson(context, it)) {
                        Toast.makeText(context, "Data imported successfully! Please restart the app.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Failed to import data.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            item { SettingSectionHeader("App Management") }
            item {
                SettingsActionItem(
                    text = "Manage Budgets",
                    icon = Icons.Default.Savings,
                    onClick = { navController.navigate("budget_screen") }
                )
            }
            item {
                SettingsActionItem(
                    text = "Manage Categories",
                    icon = Icons.Default.Category,
                    onClick = { navController.navigate("category_list") }
                )
            }

            item { SettingSectionHeader("Security") }
            item {
                SettingsToggleItem(
                    title = "Enable App Lock",
                    subtitle = "Use biometrics or screen lock to secure the app.",
                    icon = Icons.Default.Lock,
                    checked = isAppLockEnabled,
                    onCheckedChange = { viewModel.setAppLockEnabled(it) }
                )
            }

            item { SettingSectionHeader("Notifications") }
            item {
                SettingsToggleItem(
                    title = "Daily Review Reminder",
                    subtitle = "Get a notification if you have transactions waiting for approval.",
                    icon = Icons.Default.NotificationsActive,
                    checked = isDailyReminderEnabled,
                    onCheckedChange = { viewModel.setDailyReminder(it) }
                )
            }
            item {
                SettingsToggleItem(
                    title = "Weekly Summary Notification",
                    subtitle = "Receive a summary of your finances every week.",
                    icon = Icons.Default.CalendarToday,
                    checked = isWeeklySummaryEnabled,
                    onCheckedChange = { viewModel.setWeeklySummaryEnabled(it) }
                )
            }
            item {
                SettingsToggleItem(
                    title = "Popup for Unknown Transactions",
                    subtitle = "Show notification for SMS from new merchants.",
                    icon = Icons.Default.Notifications,
                    checked = isUnknownTransactionPopupEnabled,
                    onCheckedChange = { viewModel.setUnknownTransactionPopupEnabled(it) }
                )
            }

            item { SettingSectionHeader("Permissions") }
            item {
                SettingsToggleItem(
                    title = "SMS Access",
                    subtitle = "Allow reading and receiving SMS for auto-detection.",
                    icon = Icons.Default.Message,
                    checked = hasSmsPermission,
                    onCheckedChange = {
                        if (!hasSmsPermission) {
                            permissionLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
                        }
                    }
                )
            }
            item {
                SettingsToggleItem(
                    title = "Enable Notifications",
                    subtitle = "Show alerts for new transactions and reminders.",
                    icon = Icons.Default.Notifications,
                    checked = hasNotificationPermission,
                    onCheckedChange = {
                        if(!hasNotificationPermission) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }

            item { SettingSectionHeader("Data Management") }
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    SettingsActionItem(
                        text = "Rescan SMS Inbox",
                        icon = Icons.Default.Refresh,
                        onClick = {
                            if (hasSmsPermission) {
                                Toast.makeText(context, "Scanning all messages...", Toast.LENGTH_SHORT).show()
                                viewModel.rescanAllSmsMessages()
                                navController.navigate("review_sms_screen")
                            } else {
                                Toast.makeText(context, "Please grant SMS permission first.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    SettingsActionItem(
                        text = "Export Data as JSON",
                        icon = Icons.Default.DataObject,
                        onClick = {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val fileName = "FinanceApp_Backup_${sdf.format(Date())}.json"
                            fileSaverLauncher.launch(fileName)
                        }
                    )
                    // --- ADDED: The new "Export as CSV" button ---
                    SettingsActionItem(
                        text = "Export Transactions as CSV",
                        icon = Icons.Default.GridOn,
                        onClick = {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val fileName = "FinanceApp_Transactions_${sdf.format(Date())}.csv"
                            csvFileSaverLauncher.launch(fileName)
                        }
                    )

                    SettingsActionItem(
                        text = "Import from JSON",
                        icon = Icons.Default.Download,
                        onClick = { showImportJsonDialog = true }
                    )
                    // --- ADDED: CSV Import Button ---
                    SettingsActionItem(
                        text = "Import from CSV",
                        icon = Icons.Default.PostAdd,
                        onClick = { showImportCsvDialog = true }
                    )
                    SettingsActionItem(
                        text = "Import Data",
                        icon = Icons.Default.Download,
                        onClick = { showImportConfirmDialog = true }
                    )
                }
            }
        }
    }
    if (isScanning) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Scanning SMS Inbox...", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    if (showImportCsvDialog) {
        AlertDialog(
            onDismissRequest = { showImportCsvDialog = false },
            title = { Text("Import from CSV?") },
            text = { Text("This will add transactions from the CSV file. If transactions already exist, this may create duplicates. Are you sure you want to continue?") },
            confirmButton = {
                Button(onClick = {
                    showImportCsvDialog = false
                    csvImportLauncher.launch(arrayOf("text/csv", "text/comma-separated-values"))
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showImportCsvDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showImportConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showImportConfirmDialog = false },
            title = { Text("Import Data?") },
            text = { Text("This will delete all current data and replace it with the data from your backup file. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showImportConfirmDialog = false
                        filePickerLauncher.launch(arrayOf("application/json"))
                    }
                ) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showImportJsonDialog) {
        AlertDialog(
            onDismissRequest = { showImportJsonDialog = false },
            title = { Text("Import from JSON?") },
            text = { Text("This will DELETE all current data and replace it. This cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    showImportJsonDialog = false
                    jsonImportLauncher.launch(arrayOf("application/json"))
                }) { Text("Wipe and Import") }
            },
            dismissButton = {
                TextButton(onClick = { showImportJsonDialog = false }) { Text("Cancel") }
            }
        )
    }
}



@Composable
fun SettingSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
    )
    Divider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled) },
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun SettingsActionItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(text)
        Spacer(Modifier.weight(1f))
    }
}
