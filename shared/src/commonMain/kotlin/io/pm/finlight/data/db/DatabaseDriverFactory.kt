package io.pm.finlight.data.db

import app.cash.sqldelight.db.SqlDriver

/**
 * An expected class that defines the contract for creating a platform-specific
 * SQLDelight database driver. The actual implementations will be provided
 * in the androidMain and iosMain source sets.
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
