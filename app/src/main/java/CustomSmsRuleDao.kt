// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/CustomSmsRuleDao.kt
// REASON: Added new methods to support the rule management screen. `getAllRules`
// now returns a Flow to enable real-time UI updates, and a `delete` method has
// been added to allow users to remove specific rules.
// =================================================================================
package io.pm.finlight

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the CustomSmsRule entity.
 * Provides methods to interact with the custom_sms_rules table in the database.
 */
@Dao
interface CustomSmsRuleDao {

    /**
     * Inserts a new custom SMS rule into the database. If a rule with the same primary key
     * already exists, it will be replaced.
     *
     * @param rule The CustomSmsRule object to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: CustomSmsRule)

    /**
     * Retrieves all custom SMS rules from the database, ordered by priority in descending order.
     * This ensures that higher-priority rules are evaluated first.
     *
     * @return A Flow emitting a list of all CustomSmsRule objects.
     */
    @Query("SELECT * FROM custom_sms_rules ORDER BY priority DESC")
    fun getAllRules(): Flow<List<CustomSmsRule>>

    /**
     * Deletes a specific custom rule from the database.
     *
     * @param rule The CustomSmsRule object to delete.
     */
    @Delete
    suspend fun delete(rule: CustomSmsRule)
}
