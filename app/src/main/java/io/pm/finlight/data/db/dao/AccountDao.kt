package io.pm.finlight.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.pm.finlight.Account
import io.pm.finlight.AccountWithBalance
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Transaction
    @Query(
        """
        SELECT
            A.*,
            IFNULL(TxSums.balance, 0.0) as balance
        FROM
            accounts AS A
        LEFT JOIN
            (SELECT
                accountId,
                SUM(CASE WHEN transactionType = 'income' THEN amount ELSE -amount END) as balance
             FROM transactions
             WHERE isExcluded = 0 -- This is correct for a balance calculation
             GROUP BY accountId) AS TxSums
        ON A.id = TxSums.accountId
        ORDER BY
            A.name ASC
    """
    )
    fun getAccountsWithBalance(): Flow<List<AccountWithBalance>>

    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): Account?

    @Query("SELECT * FROM accounts WHERE id = :accountId")
    fun getAccountById(accountId: Int): Flow<Account?>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertAll(accounts: List<Account>)

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insert(account: Account): Long

    @Update
    suspend fun update(account: Account)

    @Delete
    suspend fun delete(account: Account)
}