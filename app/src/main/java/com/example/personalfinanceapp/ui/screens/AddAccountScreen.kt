package com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.personalfinanceapp.AccountViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(navController: NavController, viewModel: AccountViewModel) {
    var accountName by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Account") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = accountName,
                onValueChange = { accountName = it },
                label = { Text("Account Name (e.g., Savings, Credit Card)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = accountType,
                onValueChange = { accountType = it },
                label = { Text("Account Type (e.g., Bank, Wallet)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (accountName.isNotBlank() && accountType.isNotBlank()) {
                            viewModel.addAccount(accountName, accountType)
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = accountName.isNotBlank() && accountType.isNotBlank()
                ) {
                    Text("Save Account")
                }
            }
        }
    }
}
