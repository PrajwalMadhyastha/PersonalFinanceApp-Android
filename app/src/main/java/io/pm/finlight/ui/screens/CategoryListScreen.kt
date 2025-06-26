package io.pm.finlight.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.pm.finlight.Category
import io.pm.finlight.CategoryViewModel
import io.pm.finlight.ui.components.DeleteCategoryDialog
import io.pm.finlight.ui.components.EditCategoryDialog

@Composable
fun CategoryListScreen(
    navController: NavController,
    viewModel: CategoryViewModel,
) {
    val categories by viewModel.allCategories.collectAsState(initial = emptyList())
    var newCategoryName by remember { mutableStateOf("") }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = viewModel.uiEvent) {
        viewModel.uiEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = newCategoryName,
                onValueChange = { newCategoryName = it },
                label = { Text("New Category Name") },
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    if (newCategoryName.isNotBlank()) {
                        viewModel.addCategory(newCategoryName)
                        newCategoryName = ""
                    }
                },
                enabled = newCategoryName.isNotBlank(),
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()

        LazyColumn {
            items(categories) { category ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = category.name, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        selectedCategory = category
                        showEditDialog = true
                    }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Category")
                    }
                    IconButton(onClick = {
                        selectedCategory = category
                        showDeleteDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Category",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }

    if (showEditDialog) {
        selectedCategory?.let {
            EditCategoryDialog(
                category = it,
                onDismiss = { showEditDialog = false },
                onConfirm = { updatedCategory ->
                    viewModel.updateCategory(updatedCategory)
                    showEditDialog = false
                },
            )
        }
    }

    if (showDeleteDialog) {
        selectedCategory?.let {
            DeleteCategoryDialog(
                category = it,
                onDismiss = { showDeleteDialog = false },
                onConfirm = {
                    viewModel.deleteCategory(it)
                    showDeleteDialog = false
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCategoryScreen(
    navController: NavController,
    viewModel: CategoryViewModel,
    categoryId: Int,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var categoryName by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(key1 = categoryId) {
        val category = viewModel.getCategoryById(categoryId)
        if (category != null) {
            categoryName = category.name
        }
    }

    LaunchedEffect(key1 = viewModel.uiEvent) {
        viewModel.uiEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edit Category") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        categoryToDelete = Category(id = categoryId, name = categoryName)
                        showDeleteDialog = true
                    }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Category")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                label = { Text("Category Name") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    if (categoryName.isNotBlank()) {
                        viewModel.updateCategory(Category(id = categoryId, name = categoryName))
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.align(Alignment.End),
                enabled = categoryName.isNotBlank(),
            ) {
                Text("Update Category")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this category? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        categoryToDelete?.let {
                            viewModel.deleteCategory(it)
                        }
                        showDeleteDialog = false
                        navController.popBackStack()
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}
