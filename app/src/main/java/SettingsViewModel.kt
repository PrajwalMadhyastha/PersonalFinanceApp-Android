package com.example.personalfinanceapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository: SettingsRepository

    /**
     * A StateFlow that holds the current overall budget amount.
     * The UI will observe this to display the current budget.
     */
    val overallBudget: StateFlow<Float>

    init {
        // Initialize the repository
        settingsRepository = SettingsRepository(application)

        // Convert the Flow from the repository into a StateFlow that the UI can collect.
        // This makes it easy to manage state in the Composable.
        overallBudget = settingsRepository.getOverallBudgetForCurrentMonth()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0f
            )
    }

    /**
     * Saves the overall budget amount entered by the user.
     * @param amountStr The budget amount as a String from the TextField.
     */
    fun saveOverallBudget(amountStr: String) {
        // Safely parse the string to a float, defaulting to 0 if invalid.
        val amount = amountStr.toFloatOrNull() ?: 0f
        settingsRepository.saveOverallBudgetForCurrentMonth(amount)
    }
}
