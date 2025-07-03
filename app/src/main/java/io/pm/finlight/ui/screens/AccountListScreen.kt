package io.pm.finlight.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.pm.finlight.AccountViewModel
import io.pm.finlight.BankLogoHelper

@Composable
fun AccountListScreen(
    navController: NavController,
    viewModel: AccountViewModel,
) {
    val accounts by viewModel.accountsWithBalance.collectAsState(initial = emptyList())

    LazyColumn {
        items(accounts) { accountWithBalance ->
            ListItem(
                headlineContent = { Text(accountWithBalance.account.name) },
                supportingContent = { Text("Balance: ₹${"%,.2f".format(accountWithBalance.balance)}") },
                leadingContent = {
                    Image(
                        painter = painterResource(id = BankLogoHelper.getLogoForAccount(accountWithBalance.account.name)),
                        contentDescription = "${accountWithBalance.account.name} Logo",
                        modifier = Modifier.size(40.dp)
                    )
                },
                trailingContent = {
                    IconButton(onClick = { navController.navigate("edit_account/${accountWithBalance.account.id}") }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Account")
                    }
                },
                modifier = Modifier.clickable { navController.navigate("account_detail/${accountWithBalance.account.id}") },
            )
        }
    }
}
