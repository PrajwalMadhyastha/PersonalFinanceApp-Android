package com.example.personalfinanceapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// 1. Add the Budget entity to the list
// 2. IMPORTANT: Increment the database version from 3 to 4
@Database(
    entities = [Transaction::class, Account::class, Budget::class],
    version = 4
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    // 3. Add the new abstract function for the BudgetDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        // 4. --- THIS IS THE MIGRATION LOGIC ---
        // Create a Migration object that tells Room how to get from version 3 to 4.
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Here, you write the raw SQL to update your database.
                // We are creating the new 'budgets' table with the schema defined
                // in our Budget entity.
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
                        // We are no longer using fallbackToDestructiveMigration.
                        // Instead, we provide our specific migration plan.
                        .addMigrations(MIGRATION_3_4)
                        .build()

                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}