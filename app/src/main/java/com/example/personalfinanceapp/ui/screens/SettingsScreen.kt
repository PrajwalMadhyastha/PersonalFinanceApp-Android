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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val settingsRepository = remember { SettingsRepository(context) }
    val isAppLockEnabled by settingsRepository.getAppLockEnabled().collectAsState(initial = false)

    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasSmsPermission = perms.all { it.value }
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    if (navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        // --- CORRECTED: Replaced Column with LazyColumn to enable scrolling ---
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // General Settings Section
            item {
                val currentBudget by viewModel.overallBudget.collectAsState()
                var budgetInput by remember(currentBudget) {
                    mutableStateOf(if (currentBudget > 0) currentBudget.toString() else "")
                }

                Text("General", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it },
                    label = { Text("Overall Monthly Budget") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Text("₹") }
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.saveOverallBudget(budgetInput)
                        Toast.makeText(context, "Budget Saved!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save Budget") }
            }

            // Security Settings Section
            item {
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                Text("Security", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                ListItem(
                    headlineContent = { Text("Enable App Lock") },
                    supportingContent = { Text("Use biometrics or screen lock to secure the app.") },
                    leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = isAppLockEnabled,
                            onCheckedChange = { isEnabled ->
                                scope.launch {
                                    settingsRepository.saveAppLockEnabled(isEnabled)
                                }
                            }
                        )
                    }
                )
            }

            // Permissions Section
            item {
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                Text("Permissions", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                ListItem(
                    headlineContent = { Text("SMS Access") },
                    supportingContent = { Text("Allow reading and receiving SMS for auto-detection.") },
                    leadingContent = { Icon(Icons.Default.Message, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = hasSmsPermission,
                            onCheckedChange = {
                                if (!hasSmsPermission) {
                                    smsPermissionLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
                                }
                            }
                        )
                    }
                )
                ListItem(
                    headlineContent = { Text("Enable Notifications") },
                    supportingContent = { Text("Show an alert when a new transaction is detected.") },
                    leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = hasNotificationPermission,
                            onCheckedChange = {
                                if(!hasNotificationPermission) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        )
                    }
                )
            }

            // SMS Automation Section
            item {
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                Text("SMS Automation", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (hasSmsPermission) {
                                navController.navigate("review_sms_screen")
                            } else {
                                Toast.makeText(context, "Please grant SMS permission first.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Review SMS")
                    }
                    OutlinedButton(
                        onClick = { navController.navigate("sms_debug_screen") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("SMS Debug")
                    }
                }
            }

            // Data Management Section
            item {
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                Text("Data Management", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                Button(
                    onClick = {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val date = sdf.format(Date())
                        val fileName = "FinanceApp_Backup_$date.json"
                        fileSaverLauncher.launch(fileName)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Export All Data")
                }
            }

        }
    }
}
