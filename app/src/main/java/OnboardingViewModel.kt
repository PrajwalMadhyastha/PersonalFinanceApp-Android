package io.pm.finlight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// --- UPDATED: Now holds a full Category object to access the iconKey ---
data class SelectableCategory(val category: Category, var isSelected: Boolean)

class OnboardingViewModel(
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _userName = MutableStateFlow("")
    val userName = _userName.asStateFlow()

    // --- UPDATED: Initialize from the centralized CategoryIconHelper ---
    private val _categories = MutableStateFlow<List<SelectableCategory>>(
        CategoryIconHelper.predefinedCategories.map {
            // Pre-select a few common categories for the user
            val preselected = setOf("Groceries", "Food & Drinks", "Shopping", "Bills", "Travel")
            SelectableCategory(it, it.name in preselected)
        }
    )
    val categories = _categories.asStateFlow()

    private val _monthlyBudget = MutableStateFlow("")
    val monthlyBudget = _monthlyBudget.asStateFlow()

    fun onNameChanged(newName: String) {
        _userName.value = newName
    }

    // --- UPDATED: Logic now toggles based on the category object ---
    fun toggleCategorySelection(categoryToToggle: Category) {
        _categories.update { currentCategories ->
            currentCategories.map {
                if (it.category.name == categoryToToggle.name) {
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

    fun finishOnboarding() {
        viewModelScope.launch {
            if (_userName.value.isNotBlank()) {
                settingsRepository.saveUserName(_userName.value)
            }

            // --- UPDATED: Save the full category object ---
            _categories.value.filter { it.isSelected }.forEach { selectableCategory ->
                categoryRepository.insert(selectableCategory.category)
            }

            val budgetFloat = _monthlyBudget.value.toFloatOrNull() ?: 0f
            if (budgetFloat > 0) {
                settingsRepository.saveOverallBudgetForCurrentMonth(budgetFloat)
            }
        }
    }
}
