package com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.gson.Gson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditImportedTransactionScreen(
    navController: NavController,
    lineNumber: Int,
    initialData: List<String>,
) {
    var date by remember { mutableStateOf(initialData.getOrElse(0) { "" }) }
    var description by remember { mutableStateOf(initialData.getOrElse(1) { "" }) }
    var amount by remember { mutableStateOf(initialData.getOrElse(2) { "" }) }
    var type by remember { mutableStateOf(initialData.getOrElse(3) { "" }) }
    var categoryName by remember { mutableStateOf(initialData.getOrElse(4) { "" }) }
    var accountName by remember { mutableStateOf(initialData.getOrElse(5) { "" }) }
    var notes by remember { mutableStateOf(initialData.getOrElse(6) { "" }) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit CSV Row (Line $lineNumber)") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val correctedData = listOf(date, description, amount, type, categoryName, accountName, notes)
                        val gson = Gson()
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("corrected_row", gson.toJson(correctedData))
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("corrected_row_line", lineNumber)
                        navController.popBackStack()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Update Row")
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                OutlinedTextField(value = date, onValueChange = {
                    date = it
                }, label = { Text("Date (yyyy-MM-dd HH:mm:ss)") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(value = description, onValueChange = {
                    description = it
                }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(value = amount, onValueChange = {
                    amount = it
                }, label = {
                    Text(
                        "Amount",
                    )
                }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            item {
                OutlinedTextField(value = type, onValueChange = {
                    type = it
                }, label = { Text("Type (income/expense)") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(value = categoryName, onValueChange = {
                    categoryName = it
                }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(value = accountName, onValueChange = {
                    accountName = it
                }, label = { Text("Account") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
