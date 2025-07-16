package io.pm.finlight

import android.app.Application
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Enum to represent the selectable time periods on the Reports screen.
 */
enum class ReportPeriod(val displayName: String) {
    WEEK("This Week"),
    MONTH("This Month"),
    QUARTER("3 Months"),
    ALL_TIME("All Time")
}

/**
 * Data class to hold key insights calculated for the selected period.
 */
data class ReportInsights(
    val percentageChange: Int?,
    val topCategory: CategorySpending?
)

/**
 * Data class to hold all the computed data for the reports screen.
 */
data class ReportScreenData(
    val pieData: PieData?,
    val trendData: Pair<BarData, List<String>>?,
    val periodTitle: String,
    val insights: ReportInsights?
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val transactionRepository: TransactionRepository
    private val categoryDao: CategoryDao

    val allCategories: StateFlow<List<Category>>

    private val _selectedPeriod = MutableStateFlow(ReportPeriod.MONTH)
    val selectedPeriod: StateFlow<ReportPeriod> = _selectedPeriod.asStateFlow()

    val reportData: StateFlow<ReportScreenData>

    init {
        val db = AppDatabase.getInstance(application)
        transactionRepository = TransactionRepository(db.transactionDao())
        categoryDao = db.categoryDao()

        allCategories = categoryDao.getAllCategories()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        reportData = _selectedPeriod.flatMapLatest { period ->
            val (currentStartDate, currentEndDate) = calculateDateRange(period)
            val (previousStartDate, previousEndDate) = calculatePreviousDateRange(period)
            val trendStartDate = calculateTrendStartDate(period)

            val categorySpendingFlow = transactionRepository.getSpendingByCategoryForMonth(
                startDate = currentStartDate,
                endDate = currentEndDate,
                keyword = null, accountId = null, categoryId = null
            )

            val monthlyTrendFlow = transactionRepository.getMonthlyTrends(trendStartDate)

            val currentSummaryFlow = transactionRepository.getFinancialSummaryForRangeFlow(currentStartDate, currentEndDate)
            val previousSummaryFlow = transactionRepository.getFinancialSummaryForRangeFlow(previousStartDate, previousEndDate)
            val topCategoryFlow = transactionRepository.getTopSpendingCategoriesForRangeFlow(currentStartDate, currentEndDate)

            combine(
                categorySpendingFlow,
                monthlyTrendFlow,
                currentSummaryFlow,
                previousSummaryFlow,
                topCategoryFlow
            ) { spendingList, trends, currentSummary, previousSummary, topCategory ->
                // Create PieData
                val pieEntries = spendingList.map {
                    // --- FIX: Add the category name to the 'data' field for the click listener ---
                    PieEntry(it.totalAmount.toFloat(), it.categoryName, it.categoryName)
                }
                val pieColors = spendingList.map {
                    (CategoryIconHelper.getIconBackgroundColor(it.colorKey ?: "gray_light")).toArgb()
                }
                val pieDataSet = PieDataSet(pieEntries, "Spending by Category").apply {
                    this.colors = pieColors
                    valueTextSize = 12f
                }
                val finalPieData = if (pieEntries.isEmpty()) null else PieData(pieDataSet)

                // Create BarData for trends
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
                val finalTrendData = if (trends.isEmpty()) null else Pair(BarData(incomeDataSet, expenseDataSet), labels)

                // Calculate insights
                val percentageChange = if (previousSummary?.totalExpenses != null && previousSummary.totalExpenses > 0) {
                    val currentExpenses = currentSummary?.totalExpenses ?: 0.0
                    ((currentExpenses - previousSummary.totalExpenses) / previousSummary.totalExpenses * 100).roundToInt()
                } else {
                    null
                }
                val insights = ReportInsights(percentageChange, topCategory)

                val periodTitle = when (period) {
                    ReportPeriod.WEEK -> "This Week"
                    ReportPeriod.MONTH -> "This Month"
                    ReportPeriod.QUARTER -> "Last 3 Months"
                    ReportPeriod.ALL_TIME -> "All Time"
                }

                ReportScreenData(finalPieData, finalTrendData, periodTitle, insights)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReportScreenData(null, null, "This Month", null)
        )
    }

    fun selectPeriod(period: ReportPeriod) {
        _selectedPeriod.value = period
    }

    private fun calculateDateRange(period: ReportPeriod): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis
        val startDate = when (period) {
            ReportPeriod.WEEK -> (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
            ReportPeriod.MONTH -> (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0) }.timeInMillis
            ReportPeriod.QUARTER -> (calendar.clone() as Calendar).apply { add(Calendar.MONTH, -3) }.timeInMillis
            ReportPeriod.ALL_TIME -> 0L
        }
        return Pair(startDate, endDate)
    }

    private fun calculatePreviousDateRange(period: ReportPeriod): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        return when (period) {
            ReportPeriod.WEEK -> {
                val endDate = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
                val startDate = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -14) }.timeInMillis
                Pair(startDate, endDate)
            }
            ReportPeriod.MONTH -> {
                val endDate = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1); add(Calendar.DAY_OF_MONTH, -1) }.timeInMillis
                val startDate = (calendar.clone() as Calendar).apply { add(Calendar.MONTH, -1); set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
                Pair(startDate, endDate)
            }
            ReportPeriod.QUARTER -> {
                val endDate = (calendar.clone() as Calendar).apply { add(Calendar.MONTH, -3) }.timeInMillis
                val startDate = (calendar.clone() as Calendar).apply { add(Calendar.MONTH, -6) }.timeInMillis
                Pair(startDate, endDate)
            }
            ReportPeriod.ALL_TIME -> Pair(0L, 0L) // No previous period for "All Time"
        }
    }

    private fun calculateTrendStartDate(period: ReportPeriod): Long {
        val calendar = Calendar.getInstance()
        return when (period) {
            ReportPeriod.WEEK, ReportPeriod.MONTH -> (calendar.clone() as Calendar).apply { add(Calendar.MONTH, -6) }.timeInMillis
            ReportPeriod.QUARTER -> (calendar.clone() as Calendar).apply { add(Calendar.MONTH, -6) }.timeInMillis
            ReportPeriod.ALL_TIME -> 0L
        }
    }
}
