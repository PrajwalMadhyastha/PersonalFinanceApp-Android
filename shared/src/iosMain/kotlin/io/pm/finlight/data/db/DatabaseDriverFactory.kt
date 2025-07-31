package io.pm.finlight.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import io.pm.finlight.shared.db.AppDatabase

/**
 * The iOS-specific implementation of the DatabaseDriverFactory.
 * This class uses the NativeSqliteDriver, which is the standard
 * SQLDelight driver for native platforms like iOS.
 */
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(AppDatabase.Schema, "finlight.db")
    }
}
