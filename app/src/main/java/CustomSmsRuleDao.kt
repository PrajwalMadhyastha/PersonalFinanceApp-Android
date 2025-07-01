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
     * Retrieves all custom SMS rules for a specific sender, ordered by priority in descending order.
     * This ensures that higher-priority rules are evaluated first.
     *
     * @param sender The SMS sender address to fetch rules for.
     * @return A Flow emitting a list of CustomSmsRule objects.
     */
    @Query("SELECT * FROM custom_sms_rules WHERE smsSender = :sender ORDER BY priority DESC")
    fun getRulesForSender(sender: String): Flow<List<CustomSmsRule>>
}
