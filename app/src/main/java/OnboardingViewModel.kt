// FILE: app/src/main/java/io/pm/finlight/OnboardingViewModel.kt

package io.pm.finlight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Data class to manage the state of categories during onboarding
data class SelectableCategory(val name: String, var isSelected: Boolean)

class OnboardingViewModel(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // --- State for Account Setup ---
    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts = _accounts.asStateFlow()

    // --- UPDATED: State for Category Setup now only includes expense categories ---
    private val _categories = MutableStateFlow<List<SelectableCategory>>(
        listOf(
            SelectableCategory("Groceries", true),
            SelectableCategory("Food", true),
            SelectableCategory("Transportation", true),
            SelectableCategory("Utilities", true),
            SelectableCategory("Rent", false),
            SelectableCategory("Shopping", false),
            SelectableCategory("Entertainment", false),
            // "Salary" has been removed to align with the new logic
        )
    )
    val categories = _categories.asStateFlow()

    private val _monthlyBudget = MutableStateFlow("")
    val monthlyBudget = _monthlyBudget.asStateFlow()

    fun addAccount(name: String, type: String) {
        if (name.isNotBlank() && type.isNotBlank()) {
            val newAccount = Account(name = name, type = type)
            _accounts.update { it + newAccount }
        }
    }

    fun removeAccount(account: Account) {
        _accounts.update { it - account }
    }

    fun toggleCategorySelection(categoryName: String) {
        _categories.update { currentCategories ->
            currentCategories.map {
                if (it.name == categoryName) {
                    it.copy(isSelected = !it.isSelected)
                } else {
                    it
                }
            }
        }
    }

    fun onBudgetChanged(newBudget: String) {
        if (newBudget.all { it.isDigit() }) {
            _monthlyBudget.value = newBudget
        }
    }

    /**
     * Saves the initial accounts, categories, and budget to the database and preferences.
     */
    fun finishOnboarding() {
        viewModelScope.launch {
            // Save Accounts
            _accounts.value.forEach { account ->
                accountRepository.insert(account)
            }

            // Save selected Categories
            _categories.value.filter { it.isSelected }.forEach { selectableCategory ->
                categoryRepository.insert(Category(name = selectableCategory.name))
            }

            val budgetFloat = _monthlyBudget.value.toFloatOrNull() ?: 0f
            if (budgetFloat > 0) {
                settingsRepository.saveOverallBudgetForCurrentMonth(budgetFloat)
            }
        }
    }
}
