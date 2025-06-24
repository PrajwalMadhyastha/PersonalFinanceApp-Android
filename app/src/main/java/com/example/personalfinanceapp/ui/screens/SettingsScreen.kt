package com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
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
import com.example.personalfinanceapp.SettingsRepository
import com.example.personalfinanceapp.SettingsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepository = remember { SettingsRepository(context) }
    val isScanning by viewModel.isScanning.collectAsState()

    // State for all settings
    val isAppLockEnabled by settingsRepository.getAppLockEnabled().collectAsState(initial = false)
    val isWeeklySummaryEnabled by settingsRepository.getWeeklySummaryEnabled().collectAsState(initial = true)
    val isUnknownTransactionPopupEnabled by settingsRepository.getUnknownTransactionPopupEnabled().collectAsState(initial = true)
    // --- ADDED: State for the new reminder toggle ---
    val isDailyReminderEnabled by viewModel.dailyReminderEnabled.collectAsState()


    // Permission Handlers
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
            item { SettingSectionHeader("General") }
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    val currentBudget by viewModel.overallBudget.collectAsState()
                    var budgetInput by remember(currentBudget) { mutableStateOf(if (currentBudget > 0) currentBudget.toString() else "") }

                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it },
                        label = { Text("Overall Monthly Budget") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Text("₹") }
                    )
                    Button(
                        onClick = { viewModel.saveOverallBudget(budgetInput); Toast.makeText(context, "Budget Saved!", Toast.LENGTH_SHORT).show() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save Budget") }
                }
            }

            item { SettingSectionHeader("Security") }
            item {
                SettingsToggleItem(
                    title = "Enable App Lock",
                    subtitle = "Use biometrics or screen lock to secure the app.",
                    icon = Icons.Default.Lock,
                    checked = isAppLockEnabled,
                    onCheckedChange = { settingsRepository.saveAppLockEnabled(it) }
                )
            }

            item { SettingSectionHeader("Notifications") }
            // --- ADDED: Daily Reminder Toggle Switch ---
            item {
                SettingsToggleItem(
                    title = "Daily Review Reminder",
                    subtitle = "Get a notification if you have transactions waiting for your approval.",
                    icon = Icons.Default.NotificationsActive,
                    checked = isDailyReminderEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.setDailyReminder(enabled)
                    }
                )
            }
            item {
                SettingsToggleItem(
                    title = "Weekly Summary Notification",
                    subtitle = "Receive a summary of your finances every week.",
                    icon = Icons.Default.CalendarToday,
                    checked = isWeeklySummaryEnabled,
                    onCheckedChange = { settingsRepository.saveWeeklySummaryEnabled(it) }
                )
            }
            item {
                SettingsToggleItem(
                    title = "Popup for Unknown Transactions",
                    subtitle = "Show notification for SMS from new merchants.",
                    icon = Icons.Default.Notifications,
                    checked = isUnknownTransactionPopupEnabled,
                    onCheckedChange = { settingsRepository.saveUnknownTransactionPopupEnabled(it) }
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
                        text = "Export Data",
                        icon = Icons.Default.UploadFile,
                        onClick = {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val fileName = "FinanceApp_Backup_${sdf.format(Date())}.json"
                            fileSaverLauncher.launch(fileName)
                        }
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
}

@Composable
private fun SettingSectionHeader(title: String) {
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
