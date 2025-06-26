// FILE: app/src/main/java/io/pm/finlight/OnboardingViewModelFactory.kt

package io.pm.finlight

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory for creating an OnboardingViewModel with a constructor that takes an Application.
 * This is necessary to provide the repositories to the ViewModel.
 */
class OnboardingViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
            val db = AppDatabase.getInstance(application)
            val accountRepository = AccountRepository(db.accountDao())
            val categoryRepository = CategoryRepository(db.categoryDao())
            // --- NEW: Create SettingsRepository dependency ---
            val settingsRepository = SettingsRepository(application)

            @Suppress("UNCHECKED_CAST")
            // --- UPDATED: Pass SettingsRepository to ViewModel ---
            return OnboardingViewModel(accountRepository, categoryRepository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
