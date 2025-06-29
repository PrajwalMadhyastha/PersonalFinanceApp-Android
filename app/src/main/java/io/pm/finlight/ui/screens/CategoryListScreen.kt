package io.pm.finlight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import io.pm.finlight.Category
import io.pm.finlight.CategoryIconHelper
import io.pm.finlight.CategoryViewModel
import io.pm.finlight.ui.components.DeleteCategoryDialog

@Composable
fun CategoryListScreen(
    navController: NavController,
    viewModel: CategoryViewModel,
) {
    val categories by viewModel.allCategories.collectAsState(initial = emptyList())
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
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Button(
            onClick = {
                selectedCategory = null
                showEditDialog = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add New Category")
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()

        LazyColumn {
            items(categories) { category ->
                ListItem(
                    headlineContent = { Text(category.name) },
                    leadingContent = {
                        // --- UPDATED: To handle letter-based icons ---
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CategoryIconHelper.getIconBackgroundColor(category.colorKey)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (category.iconKey == "letter_default") {
                                Text(
                                    text = category.name.firstOrNull()?.uppercase() ?: "?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.Black
                                )
                            } else {
                                Icon(
                                    imageVector = CategoryIconHelper.getIcon(category.iconKey),
                                    contentDescription = category.name,
                                    tint = Color.Black,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    },
                    trailingContent = {
                        Row {
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
                    }
                )
                HorizontalDivider()
            }
        }
    }

    if (showEditDialog) {
        EditCategoryDialog(
            category = selectedCategory,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, iconKey, colorKey ->
                if (selectedCategory == null) {
                    viewModel.addCategory(name, iconKey, colorKey)
                } else {
                    viewModel.updateCategory(selectedCategory!!.copy(name = name, iconKey = iconKey, colorKey = colorKey))
                }
                showEditDialog = false
            },
        )
    }

    if (showDeleteDialog && selectedCategory != null) {
        DeleteCategoryDialog(
            category = selectedCategory!!,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                viewModel.deleteCategory(selectedCategory!!)
                showDeleteDialog = false
            },
        )
    }
}


@Composable
fun EditCategoryDialog(
    category: Category?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var selectedIconKey by remember { mutableStateOf(category?.iconKey ?: "category") }
    var selectedColorKey by remember { mutableStateOf(category?.colorKey ?: "gray_light") }
    val allIcons = remember { CategoryIconHelper.getAllIcons().entries.toList() }
    val allColors = remember { CategoryIconHelper.getAllIconColors().entries.toList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Add Category" else "Edit Category") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text("Select Icon", style = MaterialTheme.typography.titleMedium)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 48.dp),
                    modifier = Modifier.heightIn(max = 150.dp)
                ) {
                    items(allIcons) { (key, icon) ->
                        IconButton(
                            onClick = { selectedIconKey = key },
                            modifier = Modifier
                                .padding(4.dp)
                                .border(
                                    width = 1.dp,
                                    color = if (selectedIconKey == key) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = MaterialTheme.shapes.medium
                                )
                        ) {
                            Icon(imageVector = icon, contentDescription = key)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Select Color", style = MaterialTheme.typography.titleMedium)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 48.dp),
                    modifier = Modifier.heightIn(max = 100.dp)
                ) {
                    items(allColors) { (key, color) ->
                        Box(
                            modifier = Modifier
                                .padding(6.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColorKey = key }
                                .border(
                                    width = 2.dp,
                                    color = if (selectedColorKey == key) MaterialTheme.colorScheme.outline else Color.Transparent,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, selectedIconKey, selectedColorKey)
                    }
                },
                enabled = name.isNotBlank(),
            ) {
                Text(if (category == null) "Add" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
