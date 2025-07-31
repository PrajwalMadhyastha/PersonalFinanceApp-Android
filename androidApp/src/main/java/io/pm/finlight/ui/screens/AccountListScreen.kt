// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/AccountListScreen.kt
// REASON: UX REFINEMENT - The screen now includes a Scaffold with a SnackbarHost
// and a LaunchedEffect to collect UI events from the AccountViewModel. This
// ensures that feedback, such as the "Account already exists" message, is
// displayed to the user instead of failing silently.
// =================================================================================
package io.pm.finlight.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.pm.finlight.AccountViewModel
import io.pm.finlight.AccountWithBalance
import io.pm.finlight.utils.BankLogoHelper
import io.pm.finlight.ui.components.GlassPanel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AccountListScreen(
    navController: NavController,
    viewModel: AccountViewModel,
) {
    val accounts by viewModel.accountsWithBalance.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }

    // --- NEW: Collect UI events from the ViewModel to show snackbars ---
    LaunchedEffect(key1 = viewModel.uiEvent) {
        viewModel.uiEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // --- NEW: Added Scaffold to host the Snackbar ---
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(accounts, key = { it.account.id }) { accountWithBalance ->
                AccountListItem(
                    modifier = Modifier.animateItemPlacement(),
                    accountWithBalance = accountWithBalance,
                    onClick = { navController.navigate("account_detail/${accountWithBalance.account.id}") },
                    onEditClick = { navController.navigate("edit_account/${accountWithBalance.account.id}") }
                )
            }
        }
    }
}

@Composable
private fun AccountListItem(
    modifier: Modifier = Modifier,
    accountWithBalance: AccountWithBalance,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    GlassPanel(
        modifier = modifier
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