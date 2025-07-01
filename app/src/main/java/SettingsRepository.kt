// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/SettingsRepository.kt
// REASON: Added functions to save and retrieve the enabled state for the new
// monthly summary notification toggle.
// =================================================================================
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
        private const val KEY_BACKUP_ENABLED = "google_drive_backup_enabled"
        private const val KEY_DAILY_REPORT_HOUR = "daily_report_hour"
        private const val KEY_DAILY_REPORT_MINUTE = "daily_report_minute"
        private const val KEY_WEEKLY_REPORT_DAY = "weekly_report_day"
        private const val KEY_WEEKLY_REPORT_HOUR = "weekly_report_hour"
        private const val KEY_WEEKLY_REPORT_MINUTE = "weekly_report_minute"
        private const val KEY_MONTHLY_REPORT_DAY = "monthly_report_day"
        private const val KEY_MONTHLY_REPORT_HOUR = "monthly_report_hour"
        private const val KEY_MONTHLY_REPORT_MINUTE = "monthly_report_minute"
        // --- NEW: Key for monthly summary toggle ---
        private const val KEY_MONTHLY_SUMMARY_ENABLED = "monthly_summary_enabled"
    }

    fun saveBackupEnabled(isEnabled: Boolean) {
        prefs.edit().putBoolean(KEY_BACKUP_ENABLED, isEnabled).apply()
    }

    fun getBackupEnabled(): Flow<Boolean> {
        return callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, changedKey ->
                if (changedKey == KEY_BACKUP_ENABLED) {
                    trySend(sharedPreferences.getBoolean(KEY_BACKUP_ENABLED, true))
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
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
                    trySend(prefs.getBoolean(key, false))
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

    fun getOverallBudgetForMonth(year: Int, month: Int): Flow<Float> {
        return callbackFlow {
            val currentMonthKey = getBudgetKey(year, month)

            val previousMonthCalendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1) // Calendar month is 0-indexed
                add(Calendar.MONTH, -1)
            }
            val previousMonthKey = getBudgetKey(
                previousMonthCalendar.get(Calendar.YEAR),
                previousMonthCalendar.get(Calendar.MONTH) + 1
            )

            val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, changedKey ->
                if (changedKey == currentMonthKey) {
                    trySend(sharedPreferences.getFloat(currentMonthKey, 0f))
                } else if (changedKey == previousMonthKey && !sharedPreferences.contains(currentMonthKey)) {
                    trySend(sharedPreferences.getFloat(previousMonthKey, 0f))
                }
            }

            prefs.registerOnSharedPreferenceChangeListener(listener)

            val budget = if (prefs.contains(currentMonthKey)) {
                prefs.getFloat(currentMonthKey, 0f)
            } else {
                prefs.getFloat(previousMonthKey, 0f)
            }
            trySend(budget)

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
                if (key == KEY_WEEKLY_SUMMARY_ENABLED) { trySend(prefs.getBoolean(key, true)) }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            trySend(prefs.getBoolean(KEY_WEEKLY_SUMMARY_ENABLED, true))
            awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }
    }

    // --- NEW: Functions for monthly summary toggle ---
    fun saveMonthlySummaryEnabled(isEnabled: Boolean) {
        prefs.edit().putBoolean(KEY_MONTHLY_SUMMARY_ENABLED, isEnabled).apply()
    }

    fun getMonthlySummaryEnabled(): Flow<Boolean> {
        return callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == KEY_MONTHLY_SUMMARY_ENABLED) { trySend(prefs.getBoolean(key, true)) }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            trySend(prefs.getBoolean(KEY_MONTHLY_SUMMARY_ENABLED, true))
            awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }
    }

    fun saveUnknownTransactionPopupEnabled(isEnabled: Boolean) {
        prefs.edit().putBoolean(KEY_UNKNOWN_TRANSACTION_POPUP_ENABLED, isEnabled).apply()
    }
    fun getUnknownTransactionPopupEnabled(): Flow<Boolean> {
        return callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == KEY_UNKNOWN_TRANSACTION_POPUP_ENABLED) { trySend(prefs.getBoolean(key, true)) }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            trySend(prefs.getBoolean(KEY_UNKNOWN_TRANSACTION_POPUP_ENABLED, true))
            awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }
    }

    fun saveDailyReportTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_DAILY_REPORT_HOUR, hour)
            .putInt(KEY_DAILY_REPORT_MINUTE, minute)
            .apply()
    }

    fun getDailyReportTime(): Flow<Pair<Int, Int>> {
        return callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, changedKey ->
                if (changedKey == KEY_DAILY_REPORT_HOUR || changedKey == KEY_DAILY_REPORT_MINUTE) {
                    trySend(
                        Pair(
                            sharedPreferences.getInt(KEY_DAILY_REPORT_HOUR, 9),
                            sharedPreferences.getInt(KEY_DAILY_REPORT_MINUTE, 0)
                        )
                    )
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            trySend(
                Pair(
                    prefs.getInt(KEY_DAILY_REPORT_HOUR, 9),
                    prefs.getInt(KEY_DAILY_REPORT_MINUTE, 0)
                )
            )
            awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }
    }

    fun saveWeeklyReportTime(dayOfWeek: Int, hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_WEEKLY_REPORT_DAY, dayOfWeek)
            .putInt(KEY_WEEKLY_REPORT_HOUR, hour)
            .putInt(KEY_WEEKLY_REPORT_MINUTE, minute)
            .apply()
    }

    fun getWeeklyReportTime(): Flow<Triple<Int, Int, Int>> {
        return callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
                if (key == KEY_WEEKLY_REPORT_DAY || key == KEY_WEEKLY_REPORT_HOUR || key == KEY_WEEKLY_REPORT_MINUTE) {
                    trySend(Triple(
                        sp.getInt(KEY_WEEKLY_REPORT_DAY, Calendar.MONDAY),
                        sp.getInt(KEY_WEEKLY_REPORT_HOUR, 9),
                        sp.getInt(KEY_WEEKLY_REPORT_MINUTE, 0)
                    ))
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            trySend(Triple(
                prefs.getInt(KEY_WEEKLY_REPORT_DAY, Calendar.MONDAY),
                prefs.getInt(KEY_WEEKLY_REPORT_HOUR, 9),
                prefs.getInt(KEY_WEEKLY_REPORT_MINUTE, 0)
            ))
            awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }
    }

    fun saveMonthlyReportTime(dayOfMonth: Int, hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_MONTHLY_REPORT_DAY, dayOfMonth)
            .putInt(KEY_MONTHLY_REPORT_HOUR, hour)
            .putInt(KEY_MONTHLY_REPORT_MINUTE, minute)
            .apply()
    }

    fun getMonthlyReportTime(): Flow<Triple<Int, Int, Int>> {
        return callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
                if (key == KEY_MONTHLY_REPORT_DAY || key == KEY_MONTHLY_REPORT_HOUR || key == KEY_MONTHLY_REPORT_MINUTE) {
                    trySend(Triple(
                        sp.getInt(KEY_MONTHLY_REPORT_DAY, 1),
                        sp.getInt(KEY_MONTHLY_REPORT_HOUR, 9),
                        sp.getInt(KEY_MONTHLY_REPORT_MINUTE, 0)
                    ))
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            trySend(Triple(
                prefs.getInt(KEY_MONTHLY_REPORT_DAY, 1),
                prefs.getInt(KEY_MONTHLY_REPORT_HOUR, 9),
                prefs.getInt(KEY_MONTHLY_REPORT_MINUTE, 0)
            ))
            awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }
    }
}
