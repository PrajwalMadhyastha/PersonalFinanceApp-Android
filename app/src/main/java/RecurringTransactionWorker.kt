// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/RecurringTransactionWorker.kt
// REASON: NEW FILE - This worker contains the core logic for automating recurring
// transactions. It runs daily, fetches all active recurring rules, and determines
// which ones are due for creation based on their interval and last execution date.
// It then inserts the new transactions into the database.
// REASON: FIX - The worker now calls the new `getAllRulesList()` suspend function
// from the DAO to fetch a one-time list of rules. This resolves all previous
// compilation errors related to Flow handling and suspend function calls.
// =================================================================================
package io.pm.finlight

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

class RecurringTransactionWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("RecurringTxnWorker", "Worker starting...")
        return withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(context)
                val recurringDao = db.recurringTransactionDao()
                val transactionDao = db.transactionDao()

                // --- FIX: Call the new suspend function to get a simple list ---
                val allRules = recurringDao.getAllRulesList()

                allRules.forEach { rule ->
                    if (isDue(rule, transactionDao)) {
                        val newTransaction = Transaction(
                            description = rule.description,
                            amount = rule.amount,
                            transactionType = rule.transactionType,
                            date = System.currentTimeMillis(), // Use today's date for the new transaction
                            accountId = rule.accountId,
                            categoryId = rule.categoryId,
                            notes = "Recurring",
                            source = "Recurring Rule"
                        )
                        transactionDao.insert(newTransaction)
                        // Update the last run date for the rule
                        recurringDao.updateLastRunDate(rule.id, System.currentTimeMillis())
                        Log.d("RecurringTxnWorker", "Created transaction for rule: ${rule.description}")
                    }
                }

                // Reschedule for the next day
                ReminderManager.scheduleRecurringTransactionWorker(context)
                Log.d("RecurringTxnWorker", "Worker finished and rescheduled for tomorrow.")
                Result.success()
            } catch (e: Exception) {
                Log.e("RecurringTxnWorker", "Worker failed", e)
                Result.retry()
            }
        }
    }

    /**
     * Determines if a recurring transaction rule is due to be executed today.
     */
    private suspend fun isDue(
        rule: RecurringTransaction,
        transactionDao: TransactionDao
    ): Boolean {
        val today = Calendar.getInstance()

        // If the rule has never run, and the start date is today or in the past, it's due.
        if (rule.lastRunDate == null) {
            val startDateCal = Calendar.getInstance().apply { timeInMillis = rule.startDate }
            return !today.before(startDateCal)
        }

        val lastRunCal = Calendar.getInstance().apply { timeInMillis = rule.lastRunDate }
        val nextDueDate = (lastRunCal.clone() as Calendar).apply {
            when (rule.recurrenceInterval) {
                "Daily" -> add(Calendar.DAY_OF_YEAR, 1)
                "Weekly" -> add(Calendar.WEEK_OF_YEAR, 1)
                "Monthly" -> add(Calendar.MONTH, 1)
                "Yearly" -> add(Calendar.YEAR, 1)
            }
            // Set to beginning of the day for consistent comparison
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val todayStartOfDay = (today.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Check if today is on or after the calculated next due date.
        // We use 'on or after' to catch up on any missed runs (e.g., if the device was off).
        return !todayStartOfDay.before(nextDueDate)
    }
}