package com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.navigation.NavController
import com.example.personalfinanceapp.AccountViewModel

@Composable
fun AccountListScreen(
    navController: NavController,
    viewModel: AccountViewModel,
) {
    val accounts by viewModel.accountsWithBalance.collectAsState(initial = emptyList())

    LazyColumn {
        items(accounts) { account ->
            ListItem(
                headlineContent = { Text(account.account.name) },
                supportingContent = { Text("Balance: ₹${"%.2f".format(account.balance)}") },
                trailingContent = {
                    IconButton(onClick = { navController.navigate("edit_account/${account.account.id}") }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Account")
                    }
                },
                modifier = Modifier.clickable { navController.navigate("account_detail/${account.account.id}") },
            )
        }
    }
}
