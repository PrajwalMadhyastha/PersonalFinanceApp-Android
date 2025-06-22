package com.example.personalfinanceapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

// --- UPDATED: Add MerchantMapping to entities and increment version to 3 ---
@Database(
    entities = [Transaction::class, Account::class, Category::class, Budget::class, MerchantMapping::class],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    // --- NEW: Add the abstract function for the new DAO ---
    abstract fun merchantMappingDao(): MerchantMappingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN transactionType TEXT NOT NULL DEFAULT 'expense'")
                db.execSQL("UPDATE transactions SET transactionType = 'income' WHERE amount > 0")
                db.execSQL("UPDATE transactions SET amount = ABS(amount)")
            }
        }

        // --- NEW: The migration logic from version 2 to 3 ---
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create the new merchant_mappings table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `merchant_mappings` (
                        `smsSender` TEXT NOT NULL, 
                        `merchantName` TEXT NOT NULL, 
                        PRIMARY KEY(`smsSender`)
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finance_database"
                )
                    // --- UPDATED: Add both migrations to the builder ---
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(DatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val database = getInstance(context)
                    populateDatabase(database)
                }
            }

            suspend fun populateDatabase(db: AppDatabase) {
                val accountDao = db.accountDao()
                val categoryDao = db.categoryDao()
                val transactionDao = db.transactionDao()
                // --- NEW: Get Budget DAO ---
                val budgetDao = db.budgetDao()

                // Clear out any existing data
                transactionDao.deleteAll()
                budgetDao.deleteAll() // Assuming you add this
                categoryDao.deleteAll()
                accountDao.deleteAll()

                // --- 1. Populate Accounts ---
                accountDao.insertAll(listOf(
                    Account(id = 1, name = "SBI", type = "Savings"),
                    Account(id = 2, name = "HDFC", type = "Credit Card"),
                    Account(id = 3, name = "ICICI", type = "Savings")
                ))

                // --- 2. Populate Categories ---
                categoryDao.insertAll(listOf(
                    Category(id = 1, name = "Salary"),
                    Category(id = 2, name = "Groceries"),
                    Category(id = 3, name = "Rent"),
                    Category(id = 4, name = "Food"),
                    Category(id = 5, name = "Transportation"),
                    Category(id = 6, name = "Utilities")
                ))

                // --- 3. Populate Transactions (with corrected dates) ---
                val calendar = Calendar.getInstance()

                // Set to the start of the current month for an income transaction
                calendar.set(Calendar.DAY_OF_MONTH, 5)
                val incomeDate = calendar.timeInMillis

                // Set to a few days ago within the current month for expenses
                calendar.set(Calendar.DAY_OF_MONTH, 10)
                val expenseDate1 = calendar.timeInMillis

                calendar.set(Calendar.DAY_OF_MONTH, 15)
                val expenseDate2 = calendar.timeInMillis

                transactionDao.insertAll(listOf(
                    Transaction(description = "Monthly Salary", categoryId = 1, amount = 75000.0, date = incomeDate, accountId = 1, notes = "Paycheck", transactionType = "income"),
                    Transaction(description = "Grocery Shopping", categoryId = 2, amount = 4500.0, date = expenseDate1, accountId = 2, notes = "Weekly groceries", transactionType = "expense"),
                    Transaction(description = "Dinner with friends", categoryId = 4, amount = 1200.0, date = expenseDate2, accountId = 2, notes = null, transactionType = "expense"),
                    // A transaction from last month to test reports
                    Transaction(description = "Apartment Rent", categoryId = 3, amount = 25000.0, date = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }.timeInMillis, accountId = 1, notes = "Monthly rent payment", transactionType = "expense"),
                    Transaction(description = "Bus", categoryId = 5, amount = 150.0, date = Calendar.getInstance().apply{ add(Calendar.DAY_OF_MONTH, -2)}.timeInMillis, accountId = 1, notes = "Travel", transactionType = "expense"),
                    Transaction(description = "Electricity Bill", categoryId = 6, amount = 850.0, date = Calendar.getInstance().apply{ add(Calendar.DAY_OF_MONTH, -1)}.timeInMillis, accountId = 3, notes = "Power bill", transactionType = "expense")
                ))

                // --- 4. Populate Budgets for the current month ---
                val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based
                val year = calendar.get(Calendar.YEAR)

                budgetDao.insertAll(listOf(
                    Budget(categoryName = "Groceries", amount = 10000.0, month = month, year = year),
                    Budget(categoryName = "Food", amount = 5000.0, month = month, year = year),
                    Budget(categoryName = "Utilities", amount = 2000.0, month = month, year = year)
                ))
            }
        }
    }
}
