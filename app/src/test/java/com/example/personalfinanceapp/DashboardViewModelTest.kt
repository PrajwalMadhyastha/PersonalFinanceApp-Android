package com.example.personalfinanceapp

import android.app.Application
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Calendar

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE]) // UPSIDE_DOWN_CAKE is API 34
class DashboardViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var database: AppDatabase
    private lateinit var viewModel: DashboardViewModel
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var budgetDao: BudgetDao

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        val context = ApplicationProvider.getApplicationContext<Application>()

        database =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()

        transactionRepository = TransactionRepository(database.transactionDao())
        accountRepository = AccountRepository(database.accountDao())
        settingsRepository = SettingsRepository(context)
        budgetDao = database.budgetDao()

        viewModel =
            DashboardViewModel(
                transactionRepository = transactionRepository,
                accountRepository = accountRepository,
                budgetDao = budgetDao,
                settingsRepository = settingsRepository,
            )
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun test_safeToSpend_calculationIsCorrect() =
        runTest {
            // --- ARRANGE ---
            // 1. Set a budget.
            val testBudget = 30000f
            settingsRepository.saveOverallBudgetForCurrentMonth(testBudget)

            // 2. Insert test data directly inside the runTest scope.
            val accountDao = database.accountDao()
            val categoryDao = database.categoryDao()
            val transactionDao = database.transactionDao()

            accountDao.insert(Account(id = 1, name = "Test Bank", type = "Savings"))
            categoryDao.insert(Category(id = 1, name = "Food"))

            val calendar = Calendar.getInstance()
            transactionDao.insert(
                Transaction(
                    description = "Groceries",
                    amount = 2500.0,
                    date = calendar.timeInMillis,
                    accountId = 1,
                    categoryId = 1,
                    transactionType = "expense",
                    notes = "",
                ),
            )

            // --- ACT ---
            advanceUntilIdle() // Ensure all initial jobs in ViewModel are complete.
            val safeToSpend = viewModel.safeToSpendPerDay.first()

            // --- ASSERT ---
            val monthlyExpenses = viewModel.monthlyExpenses.first()
            val remainingBudget = testBudget - monthlyExpenses.toFloat()

            val lastDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
            val remainingDays = (lastDayOfMonth - currentDay + 1).coerceAtLeast(1)

            val expectedSafeToSpend = if (remainingBudget > 0) remainingBudget / remainingDays else 0f

            assertEquals("Safe to spend calculation is incorrect", expectedSafeToSpend, safeToSpend, 0.01f)
        }
}
