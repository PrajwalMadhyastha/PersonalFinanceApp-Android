package com.example.personalfinanceapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Transaction::class, Account::class, Budget::class, Category::class],
    version = 6
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetDao(): BudgetDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        // --- Migration from 3 to 4: Adds the 'budgets' table ---
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `budgets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `month` INTEGER NOT NULL,
                        `year` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        // --- Migration from 4 to 5: Adds the 'categories' table and refactors 'transactions' ---
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create the new categories table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name` ON `categories` (`name`)")

                // Create a temporary new transactions table with the final schema
                db.execSQL("""
                    CREATE TABLE `transactions_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `categoryId` INTEGER,
                        `amount` REAL NOT NULL,
                        `date` INTEGER NOT NULL,
                        `accountId` INTEGER NOT NULL,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX `index_transactions_categoryId` ON `transactions_new` (`categoryId`)")
                db.execSQL("CREATE INDEX `index_transactions_accountId` ON `transactions_new` (`accountId`)")

                // Copy data from the old table to the new one
                db.execSQL("""
                    INSERT INTO transactions_new (id, amount, date, accountId)
                    SELECT id, amount, date, accountId FROM transactions
                """.trimIndent())

                // Drop the old table
                db.execSQL("DROP TABLE transactions")

                // Rename the new table to the original name
                db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
            }
        }

        // --- Migration from 5 to 6: Adds the 'notes' column to transactions ---
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN notes TEXT")
            }
        }


        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "finance_database"
                    )
                        // Add all migrations in order
                        .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                        .build()

                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}
