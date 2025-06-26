package io.pm.finlight

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Calendar

/**
 * A repository for managing simple key-value settings using SharedPreferences.
 * This is ideal for user preferences or data that doesn't need a full database table.
 *
 * @param context The application context, required to access SharedPreferences.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "finance_app_settings"
        private const val KEY_BUDGET_PREFIX = "overall_budget_"
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_WEEKLY_SUMMARY_ENABLED = "weekly_summary_enabled"
        private const val KEY_UNKNOWN_TRANSACTION_POPUP_ENABLED = "unknown_transaction_popup_enabled"
        private const val KEY_DAILY_REPORT_ENABLED = "daily_report_enabled"
        private const val KEY_SMS_SCAN_START_DATE = "sms_scan_start_date"
        private const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"
    }

    fun hasSeenOnboarding(): Boolean {
        return prefs.getBoolean(KEY_HAS_SEEN_ONBOARDING, false)
    }

    fun setHasSeenOnboarding(hasSeen: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_SEEN_ONBOARDING, hasSeen).apply()
    }


    /**
     * Generates a unique key for the overall budget for a specific month and year.
     * Example: "overall_budget_2024_06" for June 2024.
     */
    private fun getBudgetKey(year: Int, month: Int): String {
        // Using String.format to ensure month is zero-padded (e.g., 01, 02, ... 12)
        return String.format("%s%d_%02d", KEY_BUDGET_PREFIX, year, month)
    }

    /**
     * Saves the overall budget amount for the current month.
     *
     * @param amount The budget amount to save.
     */
    fun saveOverallBudgetForCurrentMonth(amount: Float) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // Month is 0-based
        val key = getBudgetKey(year, month)

        // Use the 'edit' KTX extension function for a concise transaction.
        prefs.edit().putFloat(key, amount).apply()
    }

    fun saveSmsScanStartDate(date: Long) {
        prefs.edit().putLong(KEY_SMS_SCAN_START_DATE, date).apply()
    }

    // --- NEW: Flow to read the scan start date preference ---
    fun getSmsScanStartDate(): Flow<Long> {
        return callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, changedKey ->
                if (changedKey == KEY_SMS_SCAN_START_DATE) {
                    trySend(sharedPreferences.getLong(KEY_SMS_SCAN_START_DATE, 0L))
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            // Default to 30 days ago if no setting is saved
            val thirtyDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis
            trySend(prefs.getLong(KEY_SMS_SCAN_START_DATE, thirtyDaysAgo))
            awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }
    }

    /**
     * Retrieves the overall budget for the current month as a Flow.
     * This Flow will automatically emit a new value whenever the budget is updated.
     *
     * @return A Flow that emits the budget amount (Float). Defaults to 0f if not set.
     */

    fun saveAppLockEnabled(isEnabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, isEnabled).apply()
    }
    fun saveDailyReportEnabled(isEnabled: Boolean) {
        prefs.edit().putBoolean(KEY_DAILY_REPORT_ENABLED, isEnabled).apply()
    }
    fun getDailyReportEnabled(): Flow<Boolean> {
        return callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == KEY_DAILY_REPORT_ENABLED) {
                    trySend(prefs.getBoolean(key, false)) // Default to false
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            trySend(prefs.getBoolean(KEY_DAILY_REPORT_ENABLED, false))
            awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }
    }

    // --- NEW: Flow to read the app lock preference ---
    fun getAppLockEnabled(): Flow<Boolean> {
        return callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, changedKey ->
                if (changedKey == KEY_APP_LOCK_ENABLED) {
                    trySend(sharedPreferences.getBoolean(KEY_APP_LOCK_ENABLED, false))
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            trySend(prefs.getBoolean(KEY_APP_LOCK_ENABLED, false))
            awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }
    }
    fun isAppLockEnabledBlocking(): Boolean {
        return prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)
    }

    fun getOverallBudgetForCurrentMonth(): Flow<Float> {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val key = getBudgetKey(year, month)

        // callbackFlow is used to convert a callback-based API (like OnSharedPreferenceChangeListener)
        // into a modern Flow, so the UI can react to changes.
        return callbackFlow {
            // 1. Create a listener to watch for changes in SharedPreferences.
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, changedKey ->
                if (changedKey == key) {
                    // If our specific budget key changed, emit the new value.
                    trySend(sharedPreferences.getFloat(key, 0f))
                }
            }

            // 2. Register the listener.
            prefs.registerOnSharedPreferenceChangeListener(listener)

            // 3. Emit the initial value when the Flow is first collected.
            trySend(prefs.getFloat(key, 0f))

            // 4. Unregister the listener when the Flow is cancelled to prevent memory leaks.
            awaitClose {
                prefs.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }
    }
    fun saveWeeklySummaryEnabled(isEnabled: Boolean) {
        prefs.edit().putBoolean(KEY_WEEKLY_SUMMARY_ENABLED, isEnabled).apply()
    }
    fun getWeeklySummaryEnabled(): Flow<Boolean> {
        return callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == KEY_WEEKLY_SUMMARY_ENABLED) { trySend(prefs.getBoolean(key, true)) } // Default to true
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            trySend(prefs.getBoolean(KEY_WEEKLY_SUMMARY_ENABLED, true))
            awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }
    }

    // --- NEW: Functions for Unknown Transaction Popup ---
    fun saveUnknownTransactionPopupEnabled(isEnabled: Boolean) {
        prefs.edit().putBoolean(KEY_UNKNOWN_TRANSACTION_POPUP_ENABLED, isEnabled).apply()
    }
    fun getUnknownTransactionPopupEnabled(): Flow<Boolean> {
        return callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == KEY_UNKNOWN_TRANSACTION_POPUP_ENABLED) { trySend(prefs.getBoolean(key, true)) } // Default to true
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            trySend(prefs.getBoolean(KEY_UNKNOWN_TRANSACTION_POPUP_ENABLED, true))
            awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }
    }
}
