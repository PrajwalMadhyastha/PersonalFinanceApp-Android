package io.pm.finlight

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * A BroadcastReceiver that listens for the device boot completion event.
 * Its purpose is to re-schedule all necessary background workers (like daily,
 * weekly, and monthly reports) to ensure they persist across device reboots.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device boot completed. Re-scheduling workers.")
            // Re-schedule all workers based on their enabled status in SharedPreferences.
            val settings = context.getSharedPreferences("finance_app_settings", Context.MODE_PRIVATE)

            if (settings.getBoolean("daily_report_enabled", false)) {
                ReminderManager.scheduleDailyReport(context)
            }
            if (settings.getBoolean("weekly_summary_enabled", true)) {
                ReminderManager.scheduleWeeklySummary(context)
            }
            // You would also add the check for the monthly summary here once its toggle exists.
            // For now, we assume it's always on if scheduled.
            ReminderManager.scheduleMonthlySummary(context)
        }
    }
}
