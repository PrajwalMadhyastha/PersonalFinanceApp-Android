// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/SettingsScreen.kt
// REASON: Reorganized the "Notifications & Automation" and "SMS Scanning" sections
// for better logical grouping. Report toggles are now paired with their
// corresponding time settings, which are enabled/disabled based on the toggle state.
// The weekly day picker has been improved to prevent text wrapping.
// The monthly day picker now uses a collapsible grid for better UX.
// =================================================================================
package io.pm.finlight.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import io.pm.finlight.DataExportService
import io.pm.finlight.ScanResult
import io.pm.finlight.SettingsViewModel
import io.pm.finlight.ui.components.TimePickerDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isScanning by viewModel.isScanning.collectAsState()

    var showDatePickerDialog by remember { mutableStateOf(false) }
    val smsScanStartDate by viewModel.smsScanStartDate.collectAsState()
    val dateFormatter = remember { SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()) }

    val isAppLockEnabled by viewModel.appLockEnabled.collectAsState()
    val isWeeklySummaryEnabled by viewModel.weeklySummaryEnabled.collectAsState()
    val isDailyReportEnabled by viewModel.dailyReportEnabled.collectAsState()
    val isMonthlySummaryEnabled by viewModel.monthlySummaryEnabled.collectAsState()
    val isUnknownTransactionPopupEnabled by viewModel.unknownTransactionPopupEnabled.collectAsState()
    val isBackupEnabled by viewModel.backupEnabled.collectAsState()
    var showSmsRationaleDialog by remember { mutableStateOf(false) }
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var hasNotificationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val dailyReportTime by viewModel.dailyReportTime.collectAsState()
    var showDailyTimePicker by remember { mutableStateOf(false) }

    val weeklyReportTime by viewModel.weeklyReportTime.collectAsState()
    var showWeeklyTimePicker by remember { mutableStateOf(false) }
    val monthlyReportTime by viewModel.monthlyReportTime.collectAsState()
    var showMonthlyTimePicker by remember { mutableStateOf(false) }


    LaunchedEffect(key1 = viewModel.scanEvent) {
        viewModel.scanEvent.collect { result ->
            if (result is ScanResult.Success) {
                if (result.count > 0) {
                    navController.navigate("review_sms_screen")
                } else {
                    Toast.makeText(context, "No new transactions found.", Toast.LENGTH_SHORT).show()
                }
            } else if (result is ScanResult.Error) {
                Toast.makeText(context, "An error occurred during scan.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            hasSmsPermission = allGranted
            if (!allGranted) {
                Toast.makeText(context, "Some SMS permissions were denied.", Toast.LENGTH_SHORT).show()
            }
        }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            hasNotificationPermission = isGranted
        }

    val jsonFileSaverLauncher =
        rememberLauncherForActivityResult(
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
            },
        )

    var showImportJsonDialog by remember { mutableStateOf(false) }
    var showImportCsvDialog by remember { mutableStateOf(false) }

    val csvFileSaverLauncher =
        rememberLauncherForActivityResult(
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
            },
        )

    val csvImportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
            onResult = { uri ->
                uri?.let {
                    Log.d("SettingsScreen", "CSV file selected: $it. Starting validation.")
                    viewModel.validateCsvFile(it)
                    navController.navigate("csv_validation_screen")
                }
            },
        )

    val jsonImportLauncher =
        rememberLauncherForActivityResult(
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
            },
        )

    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = smsScanStartDate)
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            viewModel.saveSmsScanStartDate(it)
                        }
                        showDatePickerDialog = false
                    },
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePickerDialog = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showSmsRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showSmsRationaleDialog = false },
            title = { Text("Permission Required") },
            text = {
                Text(
                    "To automatically capture transactions, this app needs permission to read and receive SMS messages. Your data is processed only on your device and is never shared.",
                )
            },
            confirmButton = {
                Button(onClick = {
                    showSmsRationaleDialog = false
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_SMS,
                            Manifest.permission.RECEIVE_SMS,
                        ),
                    )
                }) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSmsRationaleDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showDailyTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = dailyReportTime.first,
            initialMinute = dailyReportTime.second,
            is24Hour = false
        )
        TimePickerDialog(
            onDismissRequest = { showDailyTimePicker = false },
            onConfirm = {
                viewModel.saveDailyReportTime(timePickerState.hour, timePickerState.minute)
                showDailyTimePicker = false
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    if (showWeeklyTimePicker) {
        WeeklyReportTimePicker(
            initialDay = weeklyReportTime.first,
            initialHour = weeklyReportTime.second,
            initialMinute = weeklyReportTime.third,
            onDismiss = { showWeeklyTimePicker = false },
            onConfirm = { day, hour, minute ->
                viewModel.saveWeeklyReportTime(day, hour, minute)
                showWeeklyTimePicker = false
            }
        )
    }

    if (showMonthlyTimePicker) {
        MonthlyReportTimePicker(
            initialDay = monthlyReportTime.first,
            initialHour = monthlyReportTime.second,
            initialMinute = monthlyReportTime.third,
            onDismiss = { showMonthlyTimePicker = false },
            onConfirm = { day, hour, minute ->
                viewModel.saveMonthlyReportTime(day, hour, minute)
                showMonthlyTimePicker = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 9.dp),
    ) {
        item { SettingSectionHeader("App Management") }
        item {
            SettingsActionItem(
                text = "Manage Accounts",
                subtitle = "View, add, or edit your financial accounts.",
                icon = Icons.Default.AccountBalanceWallet,
                onClick = { navController.navigate("account_list") },
            )
        }
        item {
            SettingsActionItem(
                text = "Manage Budgets",
                subtitle = "Set and edit your overall and category-specific monthly budgets.",
                icon = Icons.Default.Savings,
                onClick = { navController.navigate("budget_screen") },
            )
        }
        item {
            SettingsActionItem(
                text = "Manage Categories",
                subtitle = "Add, edit, or remove transaction categories.",
                icon = Icons.Default.Category,
                onClick = { navController.navigate("category_list") },
            )
        }
        item {
            SettingsActionItem(
                text = "Manage Tags",
                subtitle = "Create and organize custom tags for transactions.",
                icon = Icons.Default.NewLabel,
                onClick = { navController.navigate("tag_management") },
            )
        }

        item { SettingSectionHeader("Security") }
        item {
            SettingsToggleItem(
                title = "Enable App Lock",
                subtitle = "Use biometrics or screen lock to secure the app.",
                icon = Icons.Default.Lock,
                checked = isAppLockEnabled,
                onCheckedChange = { viewModel.setAppLockEnabled(it) },
            )
        }

        item { SettingSectionHeader("Notifications & Automation") }
        item {
            SettingsToggleItem(
                title = "Daily Summary",
                subtitle = "Get a report of yesterday's spending each day.",
                icon = Icons.Default.NotificationsActive,
                checked = isDailyReportEnabled,
                onCheckedChange = { viewModel.setDailyReportEnabled(it) },
            )
        }
        item {
            SettingsActionItem(
                text = "Daily Report Time",
                subtitle = "Current: ${String.format("%02d:%02d", dailyReportTime.first, dailyReportTime.second)}",
                icon = Icons.Default.Schedule,
                onClick = { showDailyTimePicker = true },
                enabled = isDailyReportEnabled
            )
        }
        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) }
        item {
            SettingsToggleItem(
                title = "Weekly Summary",
                subtitle = "Receive a summary of your finances every week.",
                icon = Icons.Default.CalendarToday,
                checked = isWeeklySummaryEnabled,
                onCheckedChange = { viewModel.setWeeklySummaryEnabled(it) },
            )
        }
        item {
            val dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(
                Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, weeklyReportTime.first) }.time
            )
            SettingsActionItem(
                text = "Weekly Report Time",
                subtitle = "Current: $dayName at ${String.format("%02d:%02d", weeklyReportTime.second, weeklyReportTime.third)}",
                icon = Icons.Default.Schedule,
                onClick = { showWeeklyTimePicker = true },
                enabled = isWeeklySummaryEnabled
            )
        }
        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) }
        item {
            SettingsToggleItem(
                title = "Monthly Summary",
                subtitle = "Receive a summary of last month's finances.",
                icon = Icons.Default.Event,
                checked = isMonthlySummaryEnabled,
                onCheckedChange = { viewModel.setMonthlySummaryEnabled(it) },
            )
        }
        item {
            SettingsActionItem(
                text = "Monthly Report Time",
                subtitle = "Current: Day ${monthlyReportTime.first} of the month at ${String.format("%02d:%02d", monthlyReportTime.second, monthlyReportTime.third)}",
                icon = Icons.Default.Schedule,
                onClick = { showMonthlyTimePicker = true },
                enabled = isMonthlySummaryEnabled
            )
        }

        item { SettingSectionHeader("Permissions") }
        item {
            SettingsToggleItem(
                title = "SMS Access",
                subtitle = "Allow reading and receiving SMS for auto-detection.",
                icon = Icons.AutoMirrored.Filled.Message,
                checked = hasSmsPermission,
                onCheckedChange = { isChecked ->
                    if (isChecked && !hasSmsPermission) {
                        showSmsRationaleDialog = true
                    }
                },
                enabled = !hasSmsPermission,
            )
        }
        item {
            SettingsToggleItem(
                title = "Enable Notifications",
                subtitle = "Show alerts for new transactions and reminders.",
                icon = Icons.Default.Notifications,
                checked = hasNotificationPermission,
                onCheckedChange = {
                    if (!hasNotificationPermission) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            )
        }

        item { SettingSectionHeader("SMS Scanning") }
        item {
            SettingsToggleItem(
                title = "Popup for Unknown Transactions",
                subtitle = "Show notification for SMS from new merchants.",
                icon = Icons.Default.Notifications,
                checked = isUnknownTransactionPopupEnabled,
                onCheckedChange = { viewModel.setUnknownTransactionPopupEnabled(it) },
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (hasSmsPermission) {
                                showDatePickerDialog = true
                            } else {
                                showSmsRationaleDialog = true
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.EventRepeat, contentDescription = null, modifier = Modifier.padding(end = 16.dp))
                    Column {
                        Text("Scan From Date", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Current: ${dateFormatter.format(Date(smsScanStartDate))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedButton(
                    onClick = {
                        if (hasSmsPermission) {
                            if (!isScanning) {
                                viewModel.rescanSmsForReview(smsScanStartDate)
                            }
                        } else {
                            showSmsRationaleDialog = true
                        }
                    },
                    enabled = !isScanning
                ) {
                    Text("Scan")
                }
            }
        }
        item {
            SettingsActionItem(
                text = "Scan Full Inbox",
                subtitle = "Scan all messages to find transactions for review.",
                icon = Icons.AutoMirrored.Filled.ManageSearch,
                onClick = {
                    if (hasSmsPermission) {
                        if (!isScanning) {
                            viewModel.rescanSmsForReview(null)
                        }
                    } else {
                        showSmsRationaleDialog = true
                    }
                },
            )
        }
        item { SettingSectionHeader("Data Management") }
        item {
            SettingsToggleItem(
                title = "Enable Google Drive Backup",
                subtitle = "Automatically back up app data to your Google account.",
                icon = Icons.Default.CloudUpload,
                checked = isBackupEnabled,
                onCheckedChange = { viewModel.setBackupEnabled(it) },
            )
        }
        item {
            SettingsActionItem(
                text = "Export Data as JSON",
                subtitle = "Create a full backup of all your app data.",
                icon = Icons.Default.DataObject,
                onClick = {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val fileName = "FinanceApp_Backup_${sdf.format(Date())}.json"
                    jsonFileSaverLauncher.launch(fileName)
                },
            )
        }

        item {
            SettingsActionItem(
                text = "Export Transactions as CSV",
                subtitle = "Save all transactions in a spreadsheet-compatible format.",
                icon = Icons.Default.GridOn,
                onClick = {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val fileName = "FinanceApp_Transactions_${sdf.format(Date())}.csv"
                    csvFileSaverLauncher.launch(fileName)
                },
            )
        }

        item {
            SettingsActionItem(
                text = "Import from JSON",
                subtitle = "Restore your app data from a full backup file.",
                icon = Icons.Default.Download,
                onClick = { showImportJsonDialog = true },
            )
        }
        item {
            SettingsActionItem(
                text = "Import from CSV",
                subtitle = "Add new transactions from a CSV file.",
                icon = Icons.Default.PostAdd,
                onClick = { showImportCsvDialog = true },
            )
        }
    }

    if (isScanning) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
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
            text = {
                Text(
                    "This will add transactions from the CSV file. If transactions already exist, this may create duplicates. Are you sure you want to continue?",
                )
            },
            confirmButton = {
                Button(onClick = {
                    showImportCsvDialog = false
                    csvImportLauncher.launch(arrayOf("text/csv", "text/comma-separated-values"))
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showImportCsvDialog = false }) { Text("Cancel") }
            },
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
            },
        )
    }
}

@Composable
fun SettingSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
    )
    HorizontalDivider()
}

@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled) },
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun SettingsActionItem(
    text: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val contentColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val subtitleColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)

    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        enabled = enabled
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = contentColor)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text, style = MaterialTheme.typography.bodyLarge, color = contentColor)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeeklyReportTimePicker(
    initialDay: Int,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int) -> Unit
) {
    var selectedDay by remember { mutableStateOf(initialDay) }
    val timePickerState = rememberTimePickerState(initialHour, initialMinute, false)
    val days = (1..7).map {
        val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, it) }
        Pair(it, cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Weekly Report Time") },
        text = {
            Column {
                Text("Day of the Week", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    days.chunked(4).forEach { rowDays ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowDays.forEach { (dayInt, dayName) ->
                                val isSelected = dayInt == selectedDay
                                OutlinedButton(
                                    modifier = Modifier.weight(1f),
                                    shape = MaterialTheme.shapes.medium,
                                    onClick = { selectedDay = dayInt },
                                    colors = if (isSelected) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) else ButtonDefaults.outlinedButtonColors(),
                                    contentPadding = PaddingValues(vertical = 12.dp)
                                ) {
                                    Text(dayName)
                                }
                            }
                            if (rowDays.size < 4) {
                                Spacer(modifier = Modifier.weight(4f - rowDays.size))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedDay, timePickerState.hour, timePickerState.minute) }) {
                Text("Set Time")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthlyReportTimePicker(
    initialDay: Int,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int) -> Unit
) {
    var selectedDay by remember { mutableStateOf(initialDay) }
    val timePickerState = rememberTimePickerState(initialHour, initialMinute, false)
    var isDayPickerExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Monthly Report Time") },
        text = {
            Column {
                Text("Day of the Month", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { isDayPickerExpanded = !isDayPickerExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Day: $selectedDay")
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = if (isDayPickerExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Toggle day picker"
                    )
                }
                AnimatedVisibility(visible = isDayPickerExpanded) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 52.dp),
                        modifier = Modifier.heightIn(max = 240.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items((1..28).toList()) { day ->
                            val isSelected = day == selectedDay
                            OutlinedButton(
                                onClick = {
                                    selectedDay = day
                                    isDayPickerExpanded = false
                                },
                                shape = CircleShape,
                                colors = if (isSelected) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) else ButtonDefaults.outlinedButtonColors(),
                                modifier = Modifier.size(48.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("$day")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedDay, timePickerState.hour, timePickerState.minute) }) {
                Text("Set Time")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
