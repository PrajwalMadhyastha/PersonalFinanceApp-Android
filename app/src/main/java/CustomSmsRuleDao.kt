// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/CustomSmsRuleDao.kt
// REASON: ARCHITECTURAL REFACTOR - The DAO has been updated to align with the new
// trigger-based system. The getRulesForSender function has been replaced with
// getAllRules, as rules are no longer tied to a specific sender and the parser
// needs to check all of them.
// =================================================================================
package io.pm.finlight

import androidx.room.Dao
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
     * @return A list of all CustomSmsRule objects.
     */
    @Query("SELECT * FROM custom_sms_rules ORDER BY priority DESC")
    suspend fun getAllRules(): List<CustomSmsRule>
}
