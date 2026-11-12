package com.mymoney.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mymoney.manager.data.local.entity.AccountEntity
import com.mymoney.manager.data.model.AccountType
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY id ASC")
    fun getAllActiveAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY id ASC")
    suspend fun getActiveAccountsList(): List<AccountEntity>

    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE LOWER(name) LIKE '%' || LOWER(:query) || '%' LIMIT 1")
    suspend fun findAccountByName(query: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("UPDATE accounts SET name = :name, accountType = :type, openingBalancePaise = :opening, currentBalancePaise = :current, iconName = :icon, colorHex = :color WHERE id = :id")
    suspend fun updateAccountDetails(
        id: Long,
        name: String,
        type: AccountType,
        opening: Long,
        current: Long,
        icon: String,
        color: Long
    )

    @Query("UPDATE accounts SET currentBalancePaise = :balance WHERE id = :id")
    suspend fun updateBalance(id: Long, balance: Long)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccountById(id: Long)

    @Query("SELECT SUM(currentBalancePaise) FROM accounts WHERE isActive = 1")
    fun getTotalAvailablePaise(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM accounts WHERE isActive = 1")
    suspend fun getActiveCount(): Int
}
