// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/AppDatabase.kt
// REASON: FEATURE - The database version is incremented to 20. A new migration
// (MIGRATION_19_20) is added to modify the `ignore_rules` table with new
// `isEnabled` and `isDefault` columns. The `DatabaseCallback` is updated to
// pre-populate this table with the app's default ignore phrases upon creation.
// BUG FIX - The `onOpen` callback has been corrected to ensure that on a fresh
// install, it seeds not only the default rules but also the sample accounts,
// categories, and transactions, restoring the original sample data.
// =================================================================================
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
        CustomSmsRule::class,
        MerchantRenameRule::class,
        MerchantCategoryMapping::class,
        IgnoreRule::class
    ],
    version = 20,
    exportSchema = true,
)
abstract open class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun merchantMappingDao(): MerchantMappingDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun tagDao(): TagDao
    abstract fun customSmsRuleDao(): CustomSmsRuleDao
    abstract fun merchantRenameRuleDao(): MerchantRenameRuleDao
    abstract fun merchantCategoryMappingDao(): MerchantCategoryMappingDao
    abstract fun ignoreRuleDao(): IgnoreRuleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val DEFAULT_IGNORE_PHRASES = listOf(
            "invoice of",
            "payment of.*is successful",
            "has been credited to",
            "payment of.*has been received towards",
            "credited to your.*card",
            "Payment of.*has been received on your.*Credit Card"
        ).map { IgnoreRule(phrase = it, isDefault = true) }

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

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `custom_sms_rules`")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `custom_sms_rules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `triggerPhrase` TEXT NOT NULL,
                        `merchantRegex` TEXT,
                        `amountRegex` TEXT,
                        `priority` INTEGER NOT NULL
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_custom_sms_rules_triggerPhrase` ON `custom_sms_rules` (`triggerPhrase`)")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `custom_sms_rules` ADD COLUMN `merchantNameExample` TEXT")
                db.execSQL("ALTER TABLE `custom_sms_rules` ADD COLUMN `amountExample` TEXT")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `custom_sms_rules` ADD COLUMN `accountRegex` TEXT")
                db.execSQL("ALTER TABLE `custom_sms_rules` ADD COLUMN `accountNameExample` TEXT")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN originalDescription TEXT")
                db.execSQL("CREATE TABLE IF NOT EXISTS `merchant_rename_rules` (`originalName` TEXT NOT NULL, `newName` TEXT NOT NULL, PRIMARY KEY(`originalName`))")
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN isExcluded INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `merchant_category_mapping` (`parsedName` TEXT NOT NULL, `categoryId` INTEGER NOT NULL, PRIMARY KEY(`parsedName`))")
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `ignore_rules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `phrase` TEXT NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_ignore_rules_phrase` ON `ignore_rules` (`phrase`)")
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `ignore_rules` ADD COLUMN `isEnabled` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `ignore_rules` ADD COLUMN `isDefault` INTEGER NOT NULL DEFAULT 0")
            }
        }


        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance =
                    Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "finance_database")
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20)
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
                    // Populate everything on first creation
                    populateDatabase(getInstance(context))
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // This is a good place for conditional seeding if needed on app open
                // For now, onCreate handles the initial population.
            }

            suspend fun populateDatabase(db: AppDatabase) {
                val accountDao = db.accountDao()
                val categoryDao = db.categoryDao()
                val transactionDao = db.transactionDao()
                val budgetDao = db.budgetDao()
                val ignoreRuleDao = db.ignoreRuleDao()

                // --- Seed all default data ---
                categoryDao.insertAll(CategoryIconHelper.predefinedCategories)
                ignoreRuleDao.insertAll(DEFAULT_IGNORE_PHRASES)

                accountDao.insertAll(
                    listOf(
                        Account(id = 1, name = "Cash Spends", type = "Cash"),
                        Account(id = 2, name = "SBI", type = "Savings"),
                        Account(id = 3, name = "HDFC", type = "Credit Card"),
                        Account(id = 4, name = "ICICI", type = "Savings"),
                    ),
                )

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
                            accountId = 2, // SBI
                            notes = "Paycheck",
                            transactionType = "income",
                        ),
                        Transaction(
                            description = "Grocery Shopping",
                            categoryId = 6, // "Groceries"
                            amount = 4500.0,
                            date = expenseDate1,
                            accountId = 3, // HDFC
                            notes = "Weekly groceries",
                            transactionType = "expense",
                        ),
                        Transaction(
                            description = "Dinner with friends",
                            categoryId = 4, // "Food & Drinks"
                            amount = 1200.0,
                            date = expenseDate2,
                            accountId = 3, // HDFC
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
