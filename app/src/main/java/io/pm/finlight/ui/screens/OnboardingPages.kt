// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/OnboardingPages.kt
// REASON: BUG FIX - The automatic focus request logic has been completely removed
// from `UserNamePage` and `BudgetSetupPage`. This was causing unpredictable
// layout behavior when the keyboard appeared. Removing it prioritizes a stable,
// centered layout and definitively resolves the issue where the content would
// shift to the side.
// =================================================================================
package io.pm.finlight.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.pm.finlight.OnboardingViewModel

@Composable
fun WelcomePage() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PrivacyTip,
            contentDescription = "Privacy Icon",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Welcome to Finlight",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            buildAnnotatedString {
                append("Your ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF00C853))) {
                    append("PRIVACY")
                }
                append(" is our ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF00C853))) {
                    append("PRIORITY")
                }
                append(". All your data is ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("STORED SECURELY")
                }
                append(" and ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("ONLY ON YOUR DEVICE")
                }
                append(". Let's get you set up.")
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserNamePage(viewModel: OnboardingViewModel, pagerState: PagerState) {
    val name by viewModel.userName.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "User Icon",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text("What should we call you?", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            "This will be used to personalize your experience.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { viewModel.onNameChanged(it) },
            label = { Text("Your Name") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BudgetSetupPage(viewModel: OnboardingViewModel, pagerState: PagerState) {
    val budget by viewModel.monthlyBudget.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Savings,
            contentDescription = "Budget Icon",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text("Set a Monthly Budget", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            "Give yourself a spending target for the month.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            "This can be changed any time in Settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = budget,
            onValueChange = { viewModel.onBudgetChanged(it) },
            label = { Text("Total Monthly Budget") },
            leadingIcon = { Text("₹") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SmsPermissionPage(onPermissionResult: () -> Unit) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { !it }) {
            Toast.makeText(context, "Permissions denied. You can enable them later in settings.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "SMS Scanning Enabled!", Toast.LENGTH_SHORT).show()
        }
        onPermissionResult()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "SMS Icon", modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Text("Automate Your Tracking", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text(
            "Consider allowing Finlight to read your SMS inbox to automatically detect and import new transactions. This is a huge time-saver!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
        }) {
            Text("Enable SMS Scanning")
        }
    }
}

@Composable
fun SmsScanningInfoPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "Magic Wand Icon",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text("Supercharge Your Setup", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text(
            buildAnnotatedString {
                append("After setup, you can visit the ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("Settings")
                }
                append(" screen at any time to import existing transactions from your SMS inbox.\n\nYou'll have two options:\n\n")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("• Quick Scan:")
                }
                append(" A fast scan of recent messages. This defaults to the last 30 days, but you can pick any start date you like!\n\n")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("• Full Scan:")
                }
                append(" A complete scan of your entire inbox.")
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}


@Composable
fun NotificationPermissionPage(onPermissionResult: () -> Unit) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Notifications enabled!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "You can enable notifications later in settings.", Toast.LENGTH_LONG).show()
        }
        onPermissionResult()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.NotificationsActive, contentDescription = "Notification Icon", modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Text("Stay Updated", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            "Get notified about new transactions and receive daily, weekly and monthly summaries by enabling notifications.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Button(onClick = {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }) {
                Text("Enable Notifications")
            }
            Spacer(Modifier.height(16.dp))
        } else {
            LaunchedEffect(Unit) {
                onPermissionResult()
            }
        }
    }
}

@Composable
fun CompletionPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Success Icon",
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "You're All Set!",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your accounts and preferences have been saved. You can now start tracking your finances.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}
