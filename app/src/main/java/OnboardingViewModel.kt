package io.pm.finlight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _userName = MutableStateFlow("")
    val userName = _userName.asStateFlow()

    private val _monthlyBudget = MutableStateFlow("")
    val monthlyBudget = _monthlyBudget.asStateFlow()

    fun onNameChanged(newName: String) {
        _userName.value = newName
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

            // --- BUG FIX: Ensure the full list of predefined categories is inserted ---
            // This replaces any faulty logic that was creating a few default categories.
            // It guarantees all users start with the complete, visually-rich category set.
            categoryRepository.insertAll(CategoryIconHelper.predefinedCategories)

            val budgetFloat = _monthlyBudget.value.toFloatOrNull() ?: 0f
            if (budgetFloat > 0) {
                settingsRepository.saveOverallBudgetForCurrentMonth(budgetFloat)
            }
        }
    }
}
