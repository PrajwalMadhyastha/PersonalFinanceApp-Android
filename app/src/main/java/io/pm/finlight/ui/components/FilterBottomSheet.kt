// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/components/FilterBottomSheet.kt
// REASON: NEW FILE - A reusable composable for the filter bottom sheet to be
// used across the Transaction and Income screens, avoiding code duplication.
// =================================================================================
package io.pm.finlight.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.pm.finlight.Account
import io.pm.finlight.Category
import io.pm.finlight.TransactionFilterState
import io.pm.finlight.ui.screens.SearchableDropdown

@Composable
fun FilterBottomSheet(
    filterState: TransactionFilterState,
    accounts: List<Account>,
    categories: List<Category>,
    onKeywordChange: (String) -> Unit,
    onAccountChange: (Account?) -> Unit,
    onCategoryChange: (Category?) -> Unit,
    onClearFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Filter Transactions", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = filterState.keyword,
            onValueChange = onKeywordChange,
            label = { Text("Search by keyword") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        )

        SearchableDropdown(
            label = "Account",
            options = accounts,
            selectedOption = filterState.account,
            onOptionSelected = onAccountChange,
            getDisplayName = { it.name }
        )

        SearchableDropdown(
            label = "Category",
            options = categories,
            selectedOption = filterState.category,
            onOptionSelected = onCategoryChange,
            getDisplayName = { it.name }
        )

        Button(
            onClick = onClearFilters,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear All Filters")
        }
    }
}
