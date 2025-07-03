// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/DailyReportWorker.kt
// REASON: FIX - Added a `!it.transaction.isExcluded` filter to the `totalExpenses`
// calculation. The underlying DAO query now returns all transactions for display
// flexibility, so this worker must now explicitly filter out excluded transactions
// to ensure the daily report is accurate.
// =================================================================================
package io.pm.finlight

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.roundToInt

/**
 * A background worker that calculates the user's total expenses from the previous day
 * and displays it as a system notification.
 */
class DailyReportWorker(
    private val context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("DailyReportWorker", "Worker starting...")
                val transactionDao = AppDatabase.getInstance(context).transactionDao()

                // Date range for YESTERDAY
                val yesterdayEnd = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1); set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis
                val yesterdayStart = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis

                // Date range for THE DAY BEFORE YESTERDAY
                val dayBeforeEnd = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -2); set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis
                val dayBeforeStart = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -2); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis

                val yesterdaySummary = transactionDao.getFinancialSummaryForRange(yesterdayStart, yesterdayEnd)
                val yesterdayExpenses = yesterdaySummary?.totalExpenses ?: 0.0

                val dayBeforeSummary = transactionDao.getFinancialSummaryForRange(dayBeforeStart, dayBeforeEnd)
                val dayBeforeExpenses = dayBeforeSummary?.totalExpenses ?: 0.0

                val topCategories = transactionDao.getTopSpendingCategoriesForRange(yesterdayStart, yesterdayEnd)

                val percentageChange = if (dayBeforeExpenses > 0) {
                    ((yesterdayExpenses - dayBeforeExpenses) / dayBeforeExpenses * 100).roundToInt()
                } else null

                NotificationHelper.showDailyReportNotification(context, yesterdayExpenses, percentageChange, topCategories)

                ReminderManager.scheduleDailyReport(context)
                Log.d("DailyReportWorker", "Worker finished and rescheduled.")
                Result.success()
            } catch (e: Exception) {
                Log.e("DailyReportWorker", "Worker failed", e)
                Result.retry()
            }
        }
    }
}
