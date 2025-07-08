// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/components/DashboardComponents.kt
// REASON: REFACTOR - The unused `RecentActivityCard` has been removed from this
// file. The correct component, `AuroraRecentActivityCard`, is located in
// `GlassmorphismComponents.kt` and has been updated there. This cleanup
// prevents future confusion.
// =================================================================================
package io.pm.finlight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import io.pm.finlight.BottomNavItem
import io.pm.finlight.BudgetViewModel
import io.pm.finlight.BudgetWithSpending
import io.pm.finlight.CategoryIconHelper
import io.pm.finlight.TransactionDetails

// Note: The old OverallBudgetCard, StatCard, and AccountSummaryCard have been
// removed and replaced by the new Aurora-themed components in GlassmorphismComponents.kt

@Composable
fun NetWorthCard(netWorth: Double) {
    Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Net Worth", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "₹${"%,.2f".format(netWorth)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// --- REMOVED: The old RecentActivityCard, BudgetWatchCard, and BudgetItem are no longer needed ---
// They have been replaced by the new components in GlassmorphismComponents.kt
