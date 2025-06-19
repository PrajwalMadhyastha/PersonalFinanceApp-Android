package com.example.personalfinanceapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReportsViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionRepository: TransactionRepository

    // --- UPDATED: Flow to produce data for MPAndroidChart ---
    val spendingByCategoryPieData: Flow<PieData>
    val monthYear: String

    init {
        val db = AppDatabase.getInstance(application)
        transactionRepository = TransactionRepository(db.transactionDao())

        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        monthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)

        calendar.set(currentYear, currentMonth, 1, 0, 0, 0)
        val startDate = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endDate = calendar.timeInMillis

        val spendingData: Flow<List<CategorySpending>> = transactionRepository.getSpendingByCategoryForMonth(startDate, endDate)

        // --- UPDATED: Transform the spending data into PieData ---
        spendingByCategoryPieData = spendingData.map { spendingList ->
            val entries = spendingList.map {
                // MPAndroidChart uses PieEntry objects
                PieEntry(Math.abs(it.totalAmount).toFloat(), it.categoryName)
            }

            val dataSet = PieDataSet(entries, "Spending by Category")
            // Use one of the library's built-in color templates
            dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
            dataSet.valueTextSize = 12f

            PieData(dataSet)
        }
    }
}
