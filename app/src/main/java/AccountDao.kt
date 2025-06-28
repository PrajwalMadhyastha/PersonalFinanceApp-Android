package io.pm.finlight

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    // --- PERFORMANCE OPTIMIZATION ---
    // The original query ran a sub-query for every account, which is very inefficient (N+1 problem).
    // This new query is significantly more performant. It calculates the balances for all accounts
    // in a single pass using GROUP BY, and then LEFT JOINs the results back to the accounts table.
    // This ensures that even with thousands of transactions and many accounts, the query remains fast.
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
             GROUP BY accountId) AS TxSums
        ON A.id = TxSums.accountId
        ORDER BY
            A.name ASC
    """
    )
    fun getAccountsWithBalance(): Flow<List<AccountWithBalance>>

    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): Account?

    @Query("SELECT * FROM accounts WHERE id = :accountId")
    fun getAccountById(accountId: Int): Flow<Account?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<Account>)

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(account: Account)

    @Update
    suspend fun update(account: Account)

    @Delete
    suspend fun delete(account: Account)
}
