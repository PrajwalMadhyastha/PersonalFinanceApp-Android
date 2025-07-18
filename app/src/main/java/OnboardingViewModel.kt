// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/OnboardingViewModel.kt
// REASON: FEATURE - The ViewModel is updated to detect the user's home
// currency from their device locale. It exposes this currency and provides a
// function to save the final selection, integrating currency setup into the
// onboarding flow.
// =================================================================================
package io.pm.finlight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Currency
import java.util.Locale

class OnboardingViewModel(
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _userName = MutableStateFlow("")
    val userName = _userName.asStateFlow()

    private val _monthlyBudget = MutableStateFlow("")
    val monthlyBudget = _monthlyBudget.asStateFlow()

    // --- NEW: State for home currency ---
    private val _homeCurrency = MutableStateFlow<CurrencyInfo?>(null)
    val homeCurrency = _homeCurrency.asStateFlow()

    init {
        // --- NEW: Detect home currency on init ---
        detectHomeCurrency()
    }

    fun onNameChanged(newName: String) {
        _userName.value = newName
    }

    fun onBudgetChanged(newBudget: String) {
        if (newBudget.all { it.isDigit() }) {
            _monthlyBudget.value = newBudget
        }
    }

    // --- NEW: Function to detect currency from locale ---
    private fun detectHomeCurrency() {
        viewModelScope.launch {
            try {
                val defaultLocale = Locale.getDefault()
                val currency = Currency.getInstance(defaultLocale)
                _homeCurrency.value = CurrencyInfo(
                    countryName = defaultLocale.displayCountry,
                    currencyCode = currency.currencyCode,
                    currencySymbol = currency.getSymbol(defaultLocale)
                )
            } catch (e: Exception) {
                // Fallback to INR if detection fails
                _homeCurrency.value = CurrencyHelper.getCurrencyInfo("INR")
            }
        }
    }

    // --- NEW: Function to update the selected home currency ---
    fun onHomeCurrencyChanged(currencyInfo: CurrencyInfo) {
        _homeCurrency.value = currencyInfo
    }

    fun finishOnboarding() {
        viewModelScope.launch {
            if (_userName.value.isNotBlank()) {
                settingsRepository.saveUserName(_userName.value)
            }

            // --- NEW: Save the selected home currency ---
            _homeCurrency.value?.let {
                settingsRepository.saveHomeCurrency(it.currencyCode)
            }

            categoryRepository.insertAll(CategoryIconHelper.predefinedCategories)

            val budgetFloat = _monthlyBudget.value.toFloatOrNull() ?: 0f
            if (budgetFloat > 0) {
                settingsRepository.saveOverallBudgetForCurrentMonth(budgetFloat)
            }
        }
    }
}
