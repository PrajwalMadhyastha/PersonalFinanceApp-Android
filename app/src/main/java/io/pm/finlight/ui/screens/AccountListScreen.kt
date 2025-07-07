// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/AccountListScreen.kt
// REASON: MAJOR REFACTOR - The screen has been fully redesigned to align with
// the "Project Aurora" vision. The standard ListItem has been replaced with a
// custom GlassPanel component. The layout inside the panel is enhanced to
// better feature the bank logo and balance, and all text colors are now
// theme-aware to ensure high contrast and legibility in dark mode.
// =================================================================================
package io.pm.finlight.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.pm.finlight.AccountViewModel
import io.pm.finlight.AccountWithBalance
import io.pm.finlight.BankLogoHelper
import io.pm.finlight.ui.components.GlassPanel
import java.text.NumberFormat
import java.util.*

@Composable
fun AccountListScreen(
    navController: NavController,
    viewModel: AccountViewModel,
) {
    val accounts by viewModel.accountsWithBalance.collectAsState(initial = emptyList())

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(accounts, key = { it.account.id }) { accountWithBalance ->
            AccountListItem(
                accountWithBalance = accountWithBalance,
                onClick = { navController.navigate("account_detail/${accountWithBalance.account.id}") },
                onEditClick = { navController.navigate("edit_account/${accountWithBalance.account.id}") }
            )
        }
    }
}

@Composable
private fun AccountListItem(
    accountWithBalance: AccountWithBalance,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = BankLogoHelper.getLogoForAccount(accountWithBalance.account.name)),
                contentDescription = "${accountWithBalance.account.name} Logo",
                modifier = Modifier.size(40.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = accountWithBalance.account.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Balance: ${currencyFormat.format(accountWithBalance.balance)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onEditClick) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Account",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
