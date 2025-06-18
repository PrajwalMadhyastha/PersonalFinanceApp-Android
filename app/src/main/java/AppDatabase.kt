package com.example.personalfinanceapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// 1. The @Database annotation tells Room this is the main database class.
@Database(
    entities = [Transaction::class], // 2. List all your @Entity classes here.
    version = 1 // 3. The current version of the database schema.
)
abstract class AppDatabase : RoomDatabase() {

    // 4. An abstract function for each DAO. Room will generate the implementation.
    abstract fun transactionDao(): TransactionDao

    // 5. The companion object makes our database a singleton to prevent having
    //    multiple instances open at the same time, which is a common source of bugs.
    companion object {
        // @Volatile means the value of this instance is always up-to-date and the
        // same to all execution threads. It ensures that the value of INSTANCE
        // is always read from main memory.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            // The synchronized block ensures that only one thread can execute this
            // code at a time, preventing a race condition where two instances
            // could be created at once.
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "finance_database" // This is the actual filename of the database on the device.
                    )
                        // In a real app, you would add a migration strategy here.
                        // For now, .fallbackToDestructiveMigration() is fine for development.
                        .fallbackToDestructiveMigration()
                        .build()

                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}