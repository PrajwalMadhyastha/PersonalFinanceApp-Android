// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/TimePeriodReportViewModelFactory.kt
// REASON: REFACTOR - The factory has been updated to accept and provide the
// SettingsRepository. This is a necessary dependency for the ViewModel to
// calculate the daily budget required for the new Spending Consistency card.
// =================================================================================
package io.pm.finlight

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.pm.finlight.data.db.AppDatabase
import io.pm.finlight.data.model.TimePeriod

class TimePeriodReportViewModelFactory(
    private val application: Application,
    private val timePeriod: TimePeriod,
    private val initialDateMillis: Long?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimePeriodReportViewModel::class.java)) {
            val db = AppDatabase.getInstance(application)
            // --- NEW: Add SettingsRepository dependency ---
            val settingsRepository = SettingsRepository(application)
            @Suppress("UNCHECKED_CAST")
            return TimePeriodReportViewModel(
                transactionDao = db.transactionDao(),
                settingsRepository = settingsRepository, // Pass repository
                timePeriod = timePeriod,
                initialDateMillis = initialDateMillis
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
