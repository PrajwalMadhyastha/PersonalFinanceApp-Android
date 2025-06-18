package com.example.personalfinanceapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// 1. Add the Account entity to the list
// 2. IMPORTANT: Increment the database version from 1 to 2
@Database(
    entities = [Transaction::class, Account::class],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    // 3. Add the new abstract function for the AccountDao
    abstract fun accountDao(): AccountDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "finance_database" // This is the filename of the database on the device
                    )
                        // This will wipe and recreate the database when the version number changes.
                        // This is okay for development, but for a real app, you would
                        // create a proper Migration plan.
                        .fallbackToDestructiveMigration()
                        .build()

                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}