package io.pm.finlight

import java.util.Calendar

/**
 * A data class to hold a Calendar instance for a specific month
 * and the total amount spent during that month.
 *
 * @param calendar The Calendar object representing the month.
 * @param totalSpent The total expenses for that month.
 */
data class MonthlySummaryItem(val calendar: Calendar, val totalSpent: Double)
