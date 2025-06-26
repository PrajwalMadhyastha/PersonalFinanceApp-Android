package com.example.personalfinanceapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val transactionRepository: TransactionRepository

    // Data for Pie Chart
    val spendingByCategoryPieData: Flow<PieData>
    val monthYear: String

    // Data for Bar Chart, returns the data and the labels for the X-axis
    val monthlyTrendData: Flow<Pair<BarData, List<String>>>

    init {
        val db = AppDatabase.getInstance(application)
        transactionRepository = TransactionRepository(db.transactionDao())

        val calendar = Calendar.getInstance()
        monthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)

        // Date logic for current month's pie chart
        val monthStart =
            Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis
        val monthEnd =
            Calendar.getInstance().apply {
                add(Calendar.MONTH, 1)
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.DAY_OF_MONTH, -1)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
            }.timeInMillis

        spendingByCategoryPieData =
            transactionRepository.getSpendingByCategoryForMonth(monthStart, monthEnd)
                .map { spendingList ->
                    // UPDATED: No longer need Math.abs() as amounts are positive.
                    val entries = spendingList.map { PieEntry(it.totalAmount.toFloat(), it.categoryName) }
                    val dataSet =
                        PieDataSet(entries, "Spending by Category").apply {
                            colors = ColorTemplate.MATERIAL_COLORS.toList()
                            valueTextSize = 12f
                        }
                    PieData(dataSet)
                }

        // Date logic for last 6 months' bar chart
        val sixMonthsAgo = Calendar.getInstance().apply { add(Calendar.MONTH, -6) }.timeInMillis
        monthlyTrendData =
            transactionRepository.getMonthlyTrends(sixMonthsAgo)
                .map { trends ->
                    val incomeEntries = ArrayList<BarEntry>()
                    val expenseEntries = ArrayList<BarEntry>()
                    val labels = ArrayList<String>()

                    trends.forEachIndexed { index, trend ->
                        incomeEntries.add(BarEntry(index.toFloat(), trend.totalIncome.toFloat()))
                        // UPDATED: No longer need Math.abs() for expenses.
                        expenseEntries.add(BarEntry(index.toFloat(), trend.totalExpenses.toFloat()))
                        val date = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(trend.monthYear)
                        labels.add(SimpleDateFormat("MMM", Locale.getDefault()).format(date ?: Date()))
                    }

                    val incomeDataSet = BarDataSet(incomeEntries, "Income").apply { color = ColorTemplate.rgb("#66BB6A") }
                    val expenseDataSet = BarDataSet(expenseEntries, "Expense").apply { color = ColorTemplate.rgb("#EF5350") }

                    val barData = BarData(incomeDataSet, expenseDataSet)
                    Pair(barData, labels)
                }
    }
}
