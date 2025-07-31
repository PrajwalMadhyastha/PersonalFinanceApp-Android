package io.pm.finlight.data.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.pm.finlight.shared.db.AppDatabase

/**
 * The Android-specific implementation of the DatabaseDriverFactory.
 * This class uses the AndroidSqliteDriver to create a database instance
 * using the application context.
 */
actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(AppDatabase.Schema, context, "finlight.db")
    }
}
