package io.pm.finlight

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(
    entities = [
        Transaction::class,
        Account::class,
        Category::class,
        Budget::class,
        MerchantMapping::class,
        RecurringTransaction::class,
        Tag::class,
        TransactionTagCrossRef::class,
        TransactionImage::class,
        CustomSmsRule::class // --- NEW: Added CustomSmsRule entity ---
    ],
    version = 12, // --- UPDATED: Incremented version to 12 ---
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun merchantMappingDao(): MerchantMappingDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun tagDao(): TagDao
    abstract fun customSmsRuleDao(): CustomSmsRuleDao // --- NEW: Added DAO for custom rules ---

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
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `merchant_mappings` (`smsSender` TEXT NOT NULL, `merchantName` TEXT NOT NULL, PRIMARY KEY(`smsSender`))")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN sourceSmsId INTEGER")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `recurring_transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `description` TEXT NOT NULL, `amount` REAL NOT NULL, `transactionType` TEXT NOT NULL, `recurrenceInterval` TEXT NOT NULL, `startDate` INTEGER NOT NULL, `accountId` INTEGER NOT NULL, `categoryId` INTEGER, FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_accountId` ON `recurring_transactions` (`accountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_categoryId` ON `recurring_transactions` (`categoryId`)")
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `tags` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `transaction_tag_cross_ref` (`transactionId` INTEGER NOT NULL, `tagId` INTEGER NOT NULL, PRIMARY KEY(`transactionId`, `tagId`), FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON DELETE CASCADE, FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON DELETE CASCADE)")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN sourceSmsHash TEXT")
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN source TEXT NOT NULL DEFAULT 'Manual Entry'")
                db.execSQL("UPDATE transactions SET source = 'Reviewed Import' WHERE sourceSmsId IS NOT NULL")
            }
        }
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN iconKey TEXT NOT NULL DEFAULT 'category'")
            }
        }
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN colorKey TEXT NOT NULL DEFAULT 'gray_light'")
            }
        }
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `transaction_images` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `transactionId` INTEGER NOT NULL,
                        `imageUri` TEXT NOT NULL,
                        FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_images_transactionId` ON `transaction_images` (`transactionId`)")
            }
        }

        // --- NEW: Migration to create the custom_sms_rules table ---
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `custom_sms_rules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `smsSender` TEXT NOT NULL,
                        `ruleType` TEXT NOT NULL,
                        `regexPattern` TEXT NOT NULL,
                        `priority` INTEGER NOT NULL
                    )
                """)
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance =
                    Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "finance_database")
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12) // --- UPDATED: Added new migration ---
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
                    populateDatabase(getInstance(context))
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // This remains as a good failsafe check
                CoroutineScope(Dispatchers.IO).launch {
                    val database = getInstance(context)
                    val categoryDao = database.categoryDao()
                    val categories = categoryDao.getAllCategories().first()
                    if (categories.isEmpty()) {
                        categoryDao.insertAll(CategoryIconHelper.predefinedCategories)
                    }
                }
            }

            suspend fun populateDatabase(db: AppDatabase) {
                val accountDao = db.accountDao()
                val categoryDao = db.categoryDao()
                val transactionDao = db.transactionDao()
                val budgetDao = db.budgetDao()

                // 1. Clear all existing data
                transactionDao.deleteAll()
                budgetDao.deleteAll()
                categoryDao.deleteAll()
                accountDao.deleteAll()

                // 2. Populate with defaults in the correct order
                // FIRST, insert categories so they can be referenced by transactions/budgets.
                categoryDao.insertAll(CategoryIconHelper.predefinedCategories)

                // SECOND, insert accounts.
                accountDao.insertAll(
                    listOf(
                        Account(id = 1, name = "SBI", type = "Savings"),
                        Account(id = 2, name = "HDFC", type = "Credit Card"),
                        Account(id = 3, name = "ICICI", type = "Savings"),
                    ),
                )

                // THIRD, insert sample data that references the defaults.
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_MONTH, 5)
                val incomeDate = calendar.timeInMillis
                calendar.set(Calendar.DAY_OF_MONTH, 10)
                val expenseDate1 = calendar.timeInMillis
                calendar.set(Calendar.DAY_OF_MONTH, 15)
                val expenseDate2 = calendar.timeInMillis

                transactionDao.insertAll(
                    listOf(
                        Transaction(
                            description = "Monthly Salary",
                            categoryId = 12, // "Salary"
                            amount = 75000.0,
                            date = incomeDate,
                            accountId = 1,
                            notes = "Paycheck",
                            transactionType = "income",
                        ),
                        Transaction(
                            description = "Grocery Shopping",
                            categoryId = 6, // "Groceries"
                            amount = 4500.0,
                            date = expenseDate1,
                            accountId = 2,
                            notes = "Weekly groceries",
                            transactionType = "expense",
                        ),
                        Transaction(
                            description = "Dinner with friends",
                            categoryId = 4, // "Food & Drinks"
                            amount = 1200.0,
                            date = expenseDate2,
                            accountId = 2,
                            notes = null,
                            transactionType = "expense",
                        )
                    )
                )

                val month = calendar.get(Calendar.MONTH) + 1
                val year = calendar.get(Calendar.YEAR)

                budgetDao.insertAll(
                    listOf(
                        Budget(categoryName = "Groceries", amount = 10000.0, month = month, year = year),
                        Budget(categoryName = "Food & Drinks", amount = 5000.0, month = month, year = year),
                        Budget(categoryName = "Bills", amount = 2000.0, month = month, year = year),
                    ),
                )
            }
        }
    }
}
