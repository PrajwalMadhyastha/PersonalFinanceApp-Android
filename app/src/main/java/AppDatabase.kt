package com.example.personalfinanceapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Transaction::class, Account::class, Budget::class, Category::class],
    version = 5
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetDao(): BudgetDao
    abstract fun categoryDao(): CategoryDao

    companion object {
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

        // --- UPDATED: A new, non-destructive migration from version 4 to 5 ---
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Step 1: Create the new 'categories' table.
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name` ON `categories` (`name`)")

                // Step 2: Populate the new 'categories' table with the unique descriptions from the old transactions.
                db.execSQL("""
                    INSERT INTO categories (name)
                    SELECT DISTINCT description FROM transactions
                    WHERE description IS NOT NULL AND description != ''
                """.trimIndent())

                // Step 3: Create a temporary 'transactions_new' table with the final desired schema,
                // including the new categoryId column and foreign keys.
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
                // Add indices for performance
                db.execSQL("CREATE INDEX `index_transactions_categoryId` ON `transactions_new` (`categoryId`)")
                db.execSQL("CREATE INDEX `index_transactions_accountId` ON `transactions_new` (`accountId`)")

                // Step 4: Copy the data from the old transactions table into the new one.
                // This query looks up the new categoryId for each transaction by joining
                // the old transactions table with the new categories table on the name/description.
                db.execSQL("""
                    INSERT INTO transactions_new (id, amount, date, accountId, categoryId)
                    SELECT T.id, T.amount, T.date, T.accountId, C.id
                    FROM transactions AS T
                    LEFT JOIN categories AS C ON T.description = C.name
                """.trimIndent())

                // Step 5: Drop the old transactions table.
                db.execSQL("DROP TABLE transactions")

                // Step 6: Rename the new table to 'transactions'.
                db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
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
                        .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                        .build()

                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}
