package io.pm.finlight.data.db

import android.content.Context
import io.pm.finlight.shared.db.AppDatabase

/**
 * A singleton object to provide the shared AppDatabase instance
 * to the androidApp module. This replaces the old Room.databaseBuilder logic.
 */
object DatabaseProvider {
    private var instance: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: AppDatabase(DatabaseDriverFactory(context).createDriver()).also {
                instance = it
            }
        }
    }
}
