package com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.personalfinanceapp.AccountViewModel

@Composable
fun EditAccountScreen(
    navController: NavController,
    viewModel: AccountViewModel,
    accountId: Int,
) {
    val account by viewModel.getAccountById(accountId).collectAsState(initial = null)
    var accountName by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(account) {
        account?.let {
            accountName = it.name
            accountType = it.type
        }
    }

    account?.let { currentAccount ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = accountName,
                onValueChange = { accountName = it },
                label = { Text("Account Name") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = accountType,
                onValueChange = { accountType = it },
                label = { Text("Account Type") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val updatedAccount =
                            currentAccount.copy(
                                name = accountName,
                                type = accountType,
                            )
                        viewModel.updateAccount(updatedAccount)
                        navController.popBackStack()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Update Account")
                }
            }
            // The delete button can be moved here or kept in a TopAppBar action in MainActivity
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this account? This will also delete all associated transactions.") },
            confirmButton = {
                Button(
                    onClick = {
                        account?.let {
                            viewModel.deleteAccount(it)
                            showDeleteDialog = false
                            navController.popBackStack()
                        }
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}
