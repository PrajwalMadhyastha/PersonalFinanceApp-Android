package io.pm.finlight.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NewLabel
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

/**
 * A screen where users can define custom parsing rules for SMS messages.
 * This screen displays the full SMS text and provides tools for the user
 * to mark different parts of the text (e.g., merchant name, amount).
 *
 * @param navController The NavController for handling navigation.
 * @param smsText The full body of the SMS message to create a rule for.
 */
@Composable
fun RuleCreationScreen(navController: NavController, smsText: String) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section to display the full SMS text
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Full SMS Message",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(smsText, style = MaterialTheme.typography.bodyLarge)
                }
            }

            // Section for action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { /* TODO: Implement text selection logic */ }, enabled = false, modifier = Modifier.weight(1f)) {
                    Text("Mark as Merchant")
                }
                Button(onClick = { /* TODO: Implement text selection logic */ }, enabled = false, modifier = Modifier.weight(1f)) {
                    Text("Mark as Amount")
                }
            }

            // Placeholder for rule summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Defined Rules",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider()
                    RuleSummaryItem(
                        icon = Icons.Default.Title,
                        label = "Merchant",
                        value = "Not set"
                    )
                    RuleSummaryItem(
                        icon = Icons.Default.Pin,
                        label = "Amount",
                        value = "Not set"
                    )
                    RuleSummaryItem(
                        icon = Icons.Default.NewLabel,
                        label = "Transaction Type",
                        value = "Not set"
                    )
                }
            }

            // Save and Cancel buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(onClick = { /* TODO: Implement save logic */ }, enabled = false, modifier = Modifier.weight(1f)) {
                    Text("Save Rule")
                }
            }
        }
    }
}

/**
 * A helper composable to display a single line in the rule summary card.
 *
 * @param icon The icon to display for the rule type.
 * @param label The label for the rule type (e.g., "Merchant").
 * @param value The value extracted or defined for this rule.
 */
@Composable
private fun RuleSummaryItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        Text("$label:", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
