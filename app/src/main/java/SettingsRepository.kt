package io.pm.finlight

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Calendar

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "finance_app_settings"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_PROFILE_PICTURE_URI = "profile_picture_uri"
        private const val KEY_BUDGET_PREFIX = "overall_budget_"
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_WEEKLY_SUMMARY_ENABLED = "weekly_summary_enabled"
        private const val KEY_UNKNOWN_TRANSACTION_POPUP_ENABLED = "unknown_transaction_popup_enabled"
        private const val KEY_DAILY_REPORT_ENABLED = "daily_report_enabled"
        private const val KEY_SMS_SCAN_START_DATE = "sms_scan_start_date"
        private const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"
        // --- NEW: Key for storing the backup preference ---
        private const val KEY_BACKUP_ENABLED = "google_drive_backup_enabled"
    }

    // --- NEW: Function to save the backup preference ---
    fun saveBackupEnabled(isEnabled: Boolean) {
        prefs.edit().putBoolean(KEY_BACKUP_ENABLED, isEnabled).apply()
    }

    // --- NEW: Flow to read the backup preference ---
    fun getBackupEnabled(): Flow<Boolean> {
        return callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, changedKey ->
                if (changedKey == KEY_BACKUP_ENABLED) {
                    trySend(sharedPreferences.getBoolean(KEY_BACKUP_ENABLED, true))
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            // Default to true, as backup is enabled by default in the manifest
            trySend(prefs.getBoolean(KEY_BACKUP_ENABLED, true))
            awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }
    }


    fun saveUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name).apply()
    }

    fun getUserName(): Flow<String> {
        return callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, changedKey ->
                if (changedKey == KEY_USER_NAME) {
                    trySend(sharedPreferences.getString(KEY_USER_NAME, "User") ?: "User")
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            trySend(prefs.getString(KEY_USER_NAME, "User") ?: "User")
            awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }
    }

    fun saveProfilePictureUri(uriString: String?) {
        prefs.edit().putString(KEY_PROFILE_PICTURE_URI, uriString).apply()
    }

    fun getProfilePictureUri(): Flow<String?> {
        return callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, changedKey ->
                if (changedKey == KEY_PROFILE_PICTURE_URI) {
                    trySend(sharedPreferences.getString(KEY_PROFILE_PICTURE_URI, null))
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            trySend(prefs.getString(KEY_PROFILE_PICTURE_URI, null))
            awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }
    }

    fun hasSeenOnboarding(): Boolean {
        return prefs.getBoolean(KEY_HAS_SEEN_ONBOARDING, false)
    }

    fun setHasSeenOnboarding(hasSeen: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_SEEN_ONBOARDING, hasSeen).apply()
    }

    private fun getBudgetKey(year: Int, month: Int): String {
        return String.format("%s%d_%02d", KEY_BUDGET_PREFIX, year, month)
    }

    fun saveOverallBudgetForCurrentMonth(amount: Float) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val key = getBudgetKey(year, month)
        prefs.edit().putFloat(key, amount).apply()
    }

    fun saveSmsScanStartDate(date: Long) {
        prefs.edit().putLong(KEY_SMS_SCAN_START_DATE, date).apply()
    }

    fun getSmsScanStartDate(): Flow<Long> {
        return callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, changedKey ->
                if (changedKey == KEY_SMS_SCAN_START_DATE) {
                    trySend(sharedPreferences.getLong(KEY_SMS_SCAN_START_DATE, 0L))
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            val thirtyDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis
            trySend(prefs.getLong(KEY_SMS_SCAN_START_DATE, thirtyDaysAgo))
            awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }
    }

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

        return callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, changedKey ->
                if (changedKey == key) {
                    trySend(sharedPreferences.getFloat(key, 0f))
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            trySend(prefs.getFloat(key, 0f))
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
