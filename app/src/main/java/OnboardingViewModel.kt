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
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // --- NEW: StateFlow to hold the user's name ---
    private val _userName = MutableStateFlow("")
    val userName = _userName.asStateFlow()

    private val _categories = MutableStateFlow<List<SelectableCategory>>(
        listOf(
            SelectableCategory("Groceries", true),
            SelectableCategory("Food", true),
            SelectableCategory("Transportation", true),
            SelectableCategory("Utilities", true),
            SelectableCategory("Rent", false),
            SelectableCategory("Shopping", false),
            SelectableCategory("Entertainment", false),
        )
    )
    val categories = _categories.asStateFlow()

    private val _monthlyBudget = MutableStateFlow("")
    val monthlyBudget = _monthlyBudget.asStateFlow()

    // --- NEW: Function to update the name state ---
    fun onNameChanged(newName: String) {
        _userName.value = newName
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
     * Saves the initial categories and budget to the database and preferences.
     */
    fun finishOnboarding() {
        viewModelScope.launch {
            // --- NEW: Save the user's name ---
            if (_userName.value.isNotBlank()) {
                settingsRepository.saveUserName(_userName.value)
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
