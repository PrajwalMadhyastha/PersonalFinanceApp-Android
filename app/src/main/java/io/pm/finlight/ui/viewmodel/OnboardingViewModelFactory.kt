// FILE: app/src/main/java/io/pm/finlight/OnboardingViewModelFactory.kt

package io.pm.finlight

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.pm.finlight.data.db.AppDatabase

/**
 * Factory for creating an OnboardingViewModel.
 */
class OnboardingViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
            val db = AppDatabase.getInstance(application)
            // --- UPDATED: AccountRepository is no longer needed for onboarding ---
            val categoryRepository = CategoryRepository(db.categoryDao())
            val settingsRepository = SettingsRepository(application)

            @Suppress("UNCHECKED_CAST")
            // --- UPDATED: Pass only the required dependencies ---
            return OnboardingViewModel(categoryRepository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
