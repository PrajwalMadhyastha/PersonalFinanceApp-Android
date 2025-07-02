package io.pm.finlight

import android.app.Application
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import com.github.mikephil.charting.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val transactionRepository: TransactionRepository
    val spendingByCategoryPieData: Flow<PieData>
    val monthYear: String
    val monthlyTrendData: Flow<Pair<BarData, List<String>>>

    init {
        val db = AppDatabase.getInstance(application)
        transactionRepository = TransactionRepository(db.transactionDao())

        val calendar = Calendar.getInstance()
        // --- FIX: Corrected the invalid date format pattern ---
        monthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)

        val monthStart = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0) }.timeInMillis
        val monthEnd = Calendar.getInstance().apply { add(Calendar.MONTH, 1); set(Calendar.DAY_OF_MONTH, 1); add(Calendar.DAY_OF_MONTH, -1); set(Calendar.HOUR_OF_DAY, 23) }.timeInMillis

        spendingByCategoryPieData =
            transactionRepository.getSpendingByCategoryForMonth(
                startDate = monthStart,
                endDate = monthEnd,
                keyword = null,
                accountId = null,
                categoryId = null
            ).map { spendingList ->
                val entries = spendingList.map { PieEntry(it.totalAmount.toFloat(), it.categoryName) }
                val colors = spendingList.map {
                    (CategoryIconHelper.getIconBackgroundColor(it.colorKey ?: "gray_light")).toArgb()
                }
                val dataSet =
                    PieDataSet(entries, "Spending by Category").apply {
                        this.colors = colors
                        valueTextSize = 12f
                    }
                PieData(dataSet)
            }

        val sixMonthsAgo = Calendar.getInstance().apply { add(Calendar.MONTH, -6) }.timeInMillis
        monthlyTrendData =
            transactionRepository.getMonthlyTrends(sixMonthsAgo)
                .map { trends ->
                    val incomeEntries = ArrayList<BarEntry>()
                    val expenseEntries = ArrayList<BarEntry>()
                    val labels = ArrayList<String>()

                    trends.forEachIndexed { index, trend ->
                        incomeEntries.add(BarEntry(index.toFloat(), trend.totalIncome.toFloat()))
                        expenseEntries.add(BarEntry(index.toFloat(), trend.totalExpenses.toFloat()))
                        val date = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(trend.monthYear)
                        labels.add(SimpleDateFormat("MMM", Locale.getDefault()).format(date ?: Date()))
                    }

                    val incomeDataSet = BarDataSet(incomeEntries, "Income").apply { color = android.graphics.Color.rgb(102, 187, 106) }
                    val expenseDataSet = BarDataSet(expenseEntries, "Expense").apply { color = android.graphics.Color.rgb(239, 83, 80) }

                    val barData = BarData(incomeDataSet, expenseDataSet)
                    Pair(barData, labels)
                }
    }
}
