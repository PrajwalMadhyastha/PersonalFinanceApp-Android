package io.pm.finlight.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.pm.finlight.Tag
import io.pm.finlight.TagViewModel

@Composable
fun TagManagementScreen(viewModel: TagViewModel = viewModel()) {
    val tags by viewModel.allTags.collectAsState()
    var newTagName by remember { mutableStateOf("") }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newTagName,
                onValueChange = { newTagName = it },
                label = { Text("New Tag Name") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    viewModel.addTag(newTagName)
                    newTagName = "" // Clear input field
                },
                enabled = newTagName.isNotBlank()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Tag")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()

        if (tags.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tags created yet. Add one above!")
            }
        } else {
            LazyColumn {
                items(tags) { tag ->
                    ListItem(headlineContent = { Text(tag.name) })
                    HorizontalDivider()
                }
            }
        }
    }
}