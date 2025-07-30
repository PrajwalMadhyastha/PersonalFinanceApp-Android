// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/components/ShareSnapshotSheet.kt
// REASON: FEATURE - The bottom sheet for customizing a transaction snapshot has
// been updated to be full-screen. The root Column now fills the maximum height,
// and the LazyColumn of fields expands to fill all available space, providing a
// more immersive and user-friendly experience for selecting fields.
// =================================================================================
package io.pm.finlight.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * An enum representing the fields that can be included in a shared transaction snapshot.
 */
enum class ShareableField(val displayName: String) {
    Date("Date"),
    Description("Description"),
    Amount("Amount"),
    Category("Category"),
    Account("Account"),
    Notes("Notes"),
    Tags("Tags")
}

@Composable
fun ShareSnapshotSheet(
    selectedFields: Set<ShareableField>,
    onFieldToggle: (ShareableField) -> Unit,
    onGenerateClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val allFields = ShareableField.entries

    // The root Column now fills the entire height of the bottom sheet.
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(16.dp)
            .navigationBarsPadding()
    ) {
        Text(
            "Customize Your Snapshot",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "Select the fields you want to include in the shared image.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // The LazyColumn now takes up all available vertical space.
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(allFields) { field ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFieldToggle(field) }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = field in selectedFields,
                        onCheckedChange = { onFieldToggle(field) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            checkmarkColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    Text(
                        text = field.displayName,
                        modifier = Modifier.padding(start = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onCancelClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onGenerateClick,
                modifier = Modifier.weight(1f),
                enabled = selectedFields.isNotEmpty()
            ) {
                Text("Generate Image")
            }
        }
    }
}
