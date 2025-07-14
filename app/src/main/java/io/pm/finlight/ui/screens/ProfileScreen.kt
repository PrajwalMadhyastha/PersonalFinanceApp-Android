// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/ProfileScreen.kt
// REASON: FEATURE - Added a new "Savings Goals" item to the "General" settings
// section, providing a navigation entry point to the new GoalScreen.
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
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil.compose.AsyncImage
import io.pm.finlight.*
import io.pm.finlight.R
import io.pm.finlight.ui.components.*
import androidx.compose.ui.unit.dp
import io.pm.finlight.ui.theme.AppTheme
import io.pm.finlight.ui.theme.PopupSurfaceDark
import io.pm.finlight.ui.theme.PopupSurfaceLight
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Helper function to determine if a color is 'dark' based on luminance.
private fun Color.isDark() = (red * 0.299 + green * 0.587 + blue * 0.114) < 0.5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel = viewModel(),
    settingsViewModel: SettingsViewModel
) {
    // region State Variables
    val userName by profileViewModel.userName.collectAsState()
    val savedProfilePictureUri by profileViewModel.profilePictureUri.collectAsState()
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
    var showImportJsonDialog by remember { mutableStateOf(false) }
    var showImportCsvDialog by remember { mutableStateOf(false) }
    val selectedTheme by settingsViewModel.selectedTheme.collectAsState()
    // endregion

    // region Event Handlers & Launchers
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

    val jsonFileSaverLauncher = rememberLauncherForActivityResult(
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

    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                Log.d("SettingsScreen", "CSV file selected: $it. Starting validation.")
                settingsViewModel.validateCsvFile(it)
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
    // endregion

    // --- UI Layout ---
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("edit_profile") }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
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
                        Text(userName, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text("Edit Profile", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            SettingsSection(title = "Appearance") {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Select the app's color palette.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        AppTheme.entries.forEach { theme ->
                            ThemePickerItem(
                                theme = theme,
                                isSelected = selectedTheme == theme,
                                onClick = { settingsViewModel.saveSelectedTheme(theme) }
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingsSection(title = "General") {
                SettingsActionItem(
                    text = "Manage Accounts",
                    subtitle = "View, add, or edit your financial accounts",
                    icon = Icons.Default.AccountBalanceWallet,
                    onClick = { navController.navigate("account_list") },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                SettingsActionItem(
                    text = "Manage Categories",
                    subtitle = "Add, edit, or remove transaction categories",
                    icon = Icons.Default.Category,
                    onClick = { navController.navigate("category_list") },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                SettingsActionItem(
                    text = "Manage Budgets",
                    subtitle = "Set and edit your monthly budgets",
                    icon = Icons.Default.Savings,
                    onClick = { navController.navigate("budget_screen") },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                // --- NEW: Savings Goals navigation item ---
                SettingsActionItem(
                    text = "Savings Goals",
                    subtitle = "Set and track your savings goals",
                    icon = Icons.Default.TrackChanges,
                    onClick = { navController.navigate("goals_screen") },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                SettingsActionItem(
                    text = "Manage Recurring Rules",
                    subtitle = "Automate your regular bills and income",
                    icon = Icons.Default.Autorenew,
                    onClick = { navController.navigate("recurring_transactions") },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                SettingsActionItem(
                    text = "Manage Tags",
                    subtitle = "Create and organize custom tags",
                    icon = Icons.Default.NewLabel,
                    onClick = { navController.navigate("tag_management") },
                )
            }
        }

        item {
            SettingsSection(title = "Automation & AI") {
                SettingsActionItem(
                    text = "Scan Full Inbox",
                    subtitle = "Scan all messages to find transactions",
                    icon = Icons.AutoMirrored.Filled.ManageSearch,
                    onClick = {
                        if (hasSmsPermission(context)) {
                            if (!isScanning) settingsViewModel.rescanSmsForReview(null)
                        } else {
                            Toast.makeText(context, "SMS permission is required.", Toast.LENGTH_SHORT).show()
                        }
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text("Scan from specific date", color = MaterialTheme.colorScheme.onSurface) },
                    supportingContent = {
                        Text(
                            text = "Start date: ${dateFormatter.format(Date(smsScanStartDate))}",
                            modifier = Modifier.clickable { showDatePickerDialog = true },
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingContent = { Icon(Icons.Default.Event, "Scan from date", tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = {
                        Button(
                            onClick = {
                                if (hasSmsPermission(context)) {
                                    if (!isScanning) settingsViewModel.rescanSmsForReview(smsScanStartDate)
                                } else {
                                    Toast.makeText(context, "SMS permission is required.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isScanning
                        ) { Text("Scan") }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                SettingsActionItem(
                    text = "Manage Custom Parse Rules",
                    subtitle = "View or delete your SMS parsing rules",
                    icon = Icons.Default.Rule,
                    onClick = { navController.navigate("manage_parse_rules") },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                SettingsActionItem(
                    text = "Manage Parser Ignore List",
                    subtitle = "Add or remove phrases to ignore",
                    icon = Icons.Default.Block,
                    onClick = { navController.navigate("manage_ignore_rules") },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                SettingsToggleItem(
                    title = "Popup for Unknown Transactions",
                    subtitle = "Show notification for new merchants",
                    icon = Icons.Default.HelpOutline,
                    checked = isUnknownTransactionPopupEnabled,
                    onCheckedChange = { settingsViewModel.setUnknownTransactionPopupEnabled(it) },
                )
            }
        }

        item {
            SettingsSection("Notifications") {
                SettingsToggleItem(
                    title = "Daily Summary",
                    subtitle = "Report of yesterday's spending",
                    icon = Icons.Default.Notifications,
                    checked = isDailyReportEnabled,
                    onCheckedChange = { settingsViewModel.setDailyReportEnabled(it) },
                )
                SettingsActionItem(
                    text = "Daily Report Time",
                    subtitle = "Current: ${String.format("%02d:%02d", dailyReportTime.first, dailyReportTime.second)}",
                    icon = Icons.Default.Schedule,
                    onClick = { showDailyTimePicker = true },
                    enabled = isDailyReportEnabled
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                SettingsToggleItem(
                    title = "Weekly Summary",
                    subtitle = "Summary of your finances every week",
                    icon = Icons.Default.CalendarViewWeek,
                    checked = isWeeklySummaryEnabled,
                    onCheckedChange = { settingsViewModel.setWeeklySummaryEnabled(it) },
                )
                SettingsActionItem(
                    text = "Weekly Report Time",
                    subtitle = "Current: ${SimpleDateFormat("EEEE", Locale.getDefault()).format(
                        Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, weeklyReportTime.first) }.time
                    )} at ${String.format("%02d:%02d", weeklyReportTime.second, weeklyReportTime.third)}",
                    icon = Icons.Default.Schedule,
                    onClick = { showWeeklyTimePicker = true },
                    enabled = isWeeklySummaryEnabled
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                SettingsToggleItem(
                    title = "Monthly Summary",
                    subtitle = "Summary of last month's finances",
                    icon = Icons.Default.CalendarViewMonth,
                    checked = isMonthlySummaryEnabled,
                    onCheckedChange = { settingsViewModel.setMonthlySummaryEnabled(it) },
                )
                SettingsActionItem(
                    text = "Monthly Report Time",
                    subtitle = "Current: Day ${monthlyReportTime.first} at ${String.format("%02d:%02d", monthlyReportTime.second, monthlyReportTime.third)}",
                    icon = Icons.Default.Schedule,
                    onClick = { showMonthlyTimePicker = true },
                    enabled = isMonthlySummaryEnabled
                )
            }
        }

        item {
            SettingsSection("Security & Data") {
                SettingsToggleItem(
                    title = "Enable App Lock",
                    subtitle = "Use biometrics to secure the app",
                    icon = Icons.Default.Fingerprint,
                    checked = isAppLockEnabled,
                    onCheckedChange = { settingsViewModel.setAppLockEnabled(it) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                SettingsActionItem(
                    text = "Export Data (JSON)",
                    subtitle = "Create a full backup of all your data",
                    icon = Icons.Default.DataObject,
                    onClick = {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val fileName = "Finlight_Backup_${sdf.format(Date())}.json"
                        jsonFileSaverLauncher.launch(fileName)
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                SettingsActionItem(
                    text = "Export Transactions (CSV)",
                    subtitle = "Save transactions in a spreadsheet format",
                    icon = Icons.Default.GridOn,
                    onClick = {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val fileName = "Finlight_Transactions_${sdf.format(Date())}.csv"
                        csvFileSaverLauncher.launch(fileName)
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                SettingsActionItem(
                    text = "Import from JSON",
                    subtitle = "Restore data from a backup file",
                    icon = Icons.Default.Download,
                    onClick = { showImportJsonDialog = true },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                SettingsActionItem(
                    text = "Import from CSV",
                    subtitle = "Add new transactions from a CSV file",
                    icon = Icons.Default.PostAdd,
                    onClick = { showImportCsvDialog = true },
                )
            }
        }
    }

    // region Dialogs
    val isThemeDark = MaterialTheme.colorScheme.surface.isDark()
    val popupContainerColor = if (isThemeDark) PopupSurfaceDark else PopupSurfaceLight

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
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) { Text("Cancel") }
            },
            colors = DatePickerDefaults.colors(containerColor = popupContainerColor)
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
        AlertDialog(
            onDismissRequest = { showDailyTimePicker = false },
            title = { Text("Select Daily Report Time") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsViewModel.saveDailyReportTime(timePickerState.hour, timePickerState.minute)
                        showDailyTimePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDailyTimePicker = false }) { Text("Cancel") }
            },
            containerColor = popupContainerColor
        )
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
                    Text("Scanning SMS Inbox...", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
            }
        }
    }

    if (showImportCsvDialog) {
        AlertDialog(
            onDismissRequest = { showImportCsvDialog = false },
            title = { Text("Import from CSV?") },
            text = { Text("This will add transactions from the CSV file. This may create duplicates. Continue?") },
            confirmButton = {
                Button(onClick = {
                    showImportCsvDialog = false
                    csvImportLauncher.launch(arrayOf("text/csv", "text/comma-separated-values"))
                }) { Text("Continue") }
            },
            dismissButton = { TextButton(onClick = { showImportCsvDialog = false }) { Text("Cancel") } },
            containerColor = popupContainerColor
        )
    }

    if (showImportJsonDialog) {
        AlertDialog(
            onDismissRequest = { showImportJsonDialog = false },
            title = { Text("Import from JSON?") },
            text = { Text("WARNING: This will DELETE all current data and replace it. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showImportJsonDialog = false
                        jsonImportLauncher.launch(arrayOf("application/json"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Wipe and Import") }
            },
            dismissButton = { TextButton(onClick = { showImportJsonDialog = false }) { Text("Cancel") } },
            containerColor = popupContainerColor
        )
    }
    // endregion
}

private fun hasSmsPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
        )
        GlassPanel {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun ThemePickerItem(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .border(2.dp, borderColor, CircleShape)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                if (isDark) theme.darkColor else theme.lightColor,
                                if (isDark) theme.darkColor.copy(alpha = 0.7f) else theme.lightColor.copy(alpha = 0.7f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = theme.icon,
                    contentDescription = theme.displayName,
                    tint = if (isDark) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Text(
            text = theme.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
