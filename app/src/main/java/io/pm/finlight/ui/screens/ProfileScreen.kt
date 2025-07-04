// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/ProfileScreen.kt
// REASON: REFACTOR - The settings items have been completely reorganized into
// logical groups (General, Automation & AI, etc.) for improved clarity and
// usability.
// FEATURE - Added new entry points for "Manage Custom Parse Rules" and "Manage
// Parser Ignore List" to make these features accessible to the user.
// REFACTOR - Consolidated individual permission toggles into a single "Manage
// App Permissions" item that navigates the user to the system settings screen
// for the app, which is a more standard and robust approach.
// REFACTOR - Adjusted LazyColumn spacing for better control within list items.
// The primary visual alignment fix is in the `SettingsComponents.kt` file.
// =================================================================================
package io.pm.finlight.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import io.pm.finlight.*
import io.pm.finlight.R
import io.pm.finlight.ui.components.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel = viewModel(),
    settingsViewModel: SettingsViewModel
) {
    // --- State from ProfileViewModel ---
    val userName by profileViewModel.userName.collectAsState()
    val savedProfilePictureUri by profileViewModel.profilePictureUri.collectAsState()

    // --- State and Logic from SettingsViewModel ---
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isScanning by settingsViewModel.isScanning.collectAsState()

    var showDatePickerDialog by remember { mutableStateOf(false) }
    val smsScanStartDate by settingsViewModel.smsScanStartDate.collectAsState()
    val dateFormatter = remember { SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()) }

    val isAppLockEnabled by settingsViewModel.appLockEnabled.collectAsState()
    val isWeeklySummaryEnabled by settingsViewModel.weeklySummaryEnabled.collectAsState()
    val isDailyReportEnabled by settingsViewModel.dailyReportEnabled.collectAsState()
    val isMonthlySummaryEnabled by settingsViewModel.monthlySummaryEnabled.collectAsState()
    val isUnknownTransactionPopupEnabled by settingsViewModel.unknownTransactionPopupEnabled.collectAsState()
    val isBackupEnabled by settingsViewModel.backupEnabled.collectAsState()

    val dailyReportTime by settingsViewModel.dailyReportTime.collectAsState()
    var showDailyTimePicker by remember { mutableStateOf(false) }

    val weeklyReportTime by settingsViewModel.weeklyReportTime.collectAsState()
    var showWeeklyTimePicker by remember { mutableStateOf(false) }
    val monthlyReportTime by settingsViewModel.monthlyReportTime.collectAsState()
    var showMonthlyTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = settingsViewModel.scanEvent) {
        settingsViewModel.scanEvent.collect { result ->
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
                    settingsViewModel.validateCsvFile(it)
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

    // --- UI Layout ---
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp) // Let items control their own padding
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable { navController.navigate("edit_profile") },
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = savedProfilePictureUri,
                            contentDescription = "User Profile Picture",
                            placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
                            error = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        )

                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(userName, style = MaterialTheme.typography.titleLarge)
                        }
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { SettingSectionHeader("General") }
        item {
            SettingsActionItem(
                text = "Manage Accounts",
                subtitle = "View, add, or edit your financial accounts",
                icon = Icons.Default.AccountBalanceWallet,
                onClick = { navController.navigate("account_list") },
            )
        }
        item {
            SettingsActionItem(
                text = "Manage Categories",
                subtitle = "Add, edit, or remove transaction categories",
                icon = Icons.Default.Category,
                onClick = { navController.navigate("category_list") },
            )
        }
        item {
            SettingsActionItem(
                text = "Manage Budgets",
                subtitle = "Set and edit your monthly budgets",
                icon = Icons.Default.Savings,
                onClick = { navController.navigate("budget_screen") },
            )
        }
        item {
            SettingsActionItem(
                text = "Manage Tags",
                subtitle = "Create and organize custom tags",
                icon = Icons.Default.NewLabel,
                onClick = { navController.navigate("tag_management") },
            )
        }

        item { SettingSectionHeader("Automation & AI") }
        item {
            SettingsActionItem(
                text = "Scan Full Inbox",
                subtitle = "Scan all messages to find transactions for review",
                icon = Icons.AutoMirrored.Filled.ManageSearch,
                onClick = {
                    if (hasSmsPermission(context)) {
                        if (!isScanning) {
                            settingsViewModel.rescanSmsForReview(null)
                        }
                    } else {
                        Toast.makeText(context, "SMS permission is required.", Toast.LENGTH_SHORT).show()
                    }
                },
            )
        }
        item {
            SettingsActionItem(
                text = "Manage Custom Parse Rules",
                subtitle = "View or delete your custom SMS parsing rules",
                icon = Icons.Default.Rule,
                onClick = { navController.navigate("manage_parse_rules") },
            )
        }
        item {
            SettingsActionItem(
                text = "Manage Parser Ignore List",
                subtitle = "Add or remove phrases to ignore during parsing",
                icon = Icons.Default.Block,
                onClick = { navController.navigate("manage_ignore_rules") },
            )
        }
        item {
            SettingsToggleItem(
                title = "Popup for Unknown Transactions",
                subtitle = "Show notification for SMS from new merchants",
                icon = Icons.Default.HelpOutline,
                checked = isUnknownTransactionPopupEnabled,
                onCheckedChange = { settingsViewModel.setUnknownTransactionPopupEnabled(it) },
            )
        }


        item { SettingSectionHeader("Notifications") }
        item {
            SettingsToggleItem(
                title = "Daily Summary",
                subtitle = "Get a report of yesterday's spending each day",
                icon = Icons.Default.Notifications,
                checked = isDailyReportEnabled,
                onCheckedChange = { settingsViewModel.setDailyReportEnabled(it) },
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
                subtitle = "Receive a summary of your finances every week",
                icon = Icons.Default.CalendarToday,
                checked = isWeeklySummaryEnabled,
                onCheckedChange = { settingsViewModel.setWeeklySummaryEnabled(it) },
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
                subtitle = "Receive a summary of last month's finances",
                icon = Icons.Default.Event,
                checked = isMonthlySummaryEnabled,
                onCheckedChange = { settingsViewModel.setMonthlySummaryEnabled(it) },
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

        item { SettingSectionHeader("Security & Privacy") }
        item {
            SettingsToggleItem(
                title = "Enable App Lock",
                subtitle = "Use biometrics or screen lock to secure the app",
                icon = Icons.Default.Fingerprint,
                checked = isAppLockEnabled,
                onCheckedChange = { settingsViewModel.setAppLockEnabled(it) },
            )
        }
        item {
            SettingsActionItem(
                text = "Manage App Permissions",
                subtitle = "Control access to SMS, notifications, etc.",
                icon = Icons.Default.Shield,
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", context.packageName, null)
                    intent.data = uri
                    context.startActivity(intent)
                }
            )
        }


        item { SettingSectionHeader("Data Management") }
        item {
            SettingsToggleItem(
                title = "Enable Google Drive Backup",
                subtitle = "Automatically back up app data to your Google account",
                icon = Icons.Default.CloudUpload,
                checked = isBackupEnabled,
                onCheckedChange = { settingsViewModel.setBackupEnabled(it) },
            )
        }
        item {
            SettingsActionItem(
                text = "Export Data as JSON",
                subtitle = "Create a full backup of all your app data",
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
                subtitle = "Save all transactions in a spreadsheet-compatible format",
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
                subtitle = "Restore your app data from a full backup file",
                icon = Icons.Default.Download,
                onClick = { showImportJsonDialog = true },
            )
        }
        item {
            SettingsActionItem(
                text = "Import from CSV",
                subtitle = "Add new transactions from a CSV file",
                icon = Icons.Default.PostAdd,
                onClick = { showImportCsvDialog = true },
            )
        }
    }

    // --- Dialogs ---
    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = smsScanStartDate)
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            settingsViewModel.saveSmsScanStartDate(it)
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

    if (showDailyTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = dailyReportTime.first,
            initialMinute = dailyReportTime.second,
            is24Hour = false
        )
        TimePickerDialog(
            onDismissRequest = { showDailyTimePicker = false },
            onConfirm = {
                settingsViewModel.saveDailyReportTime(timePickerState.hour, timePickerState.minute)
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
                settingsViewModel.saveWeeklyReportTime(day, hour, minute)
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
                settingsViewModel.saveMonthlyReportTime(day, hour, minute)
                showMonthlyTimePicker = false
            }
        )
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

private fun hasSmsPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
}
