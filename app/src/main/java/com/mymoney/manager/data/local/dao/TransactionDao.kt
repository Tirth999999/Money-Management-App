package com.mymoney.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mymoney.manager.data.local.entity.TransactionEntity
import com.mymoney.manager.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY transactionDate DESC, id DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId OR destinationAccountId = :accountId ORDER BY transactionDate DESC")
    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE transactionDate BETWEEN :startDate AND :endDate ORDER BY transactionDate DESC, id DESC")
    fun getTransactionsBetweenDates(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE referenceId = :referenceId LIMIT 1")
    suspend fun getTransactionByReference(referenceId: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    // Financial Analysis Aggregations
    @Query("SELECT SUM(amountPaise) FROM transactions WHERE type = 'INCOME' AND transactionDate BETWEEN :startDate AND :endDate")
    fun getIncomeSumBetweenDates(startDate: Long, endDate: Long): Flow<Long?>

    @Query("SELECT SUM(amountPaise) FROM transactions WHERE type = 'EXPENSE' AND transactionDate BETWEEN :startDate AND :endDate")
    fun getExpenseSumBetweenDates(startDate: Long, endDate: Long): Flow<Long?>

    @Query("SELECT * FROM transactions WHERE type = :type AND transactionDate BETWEEN :startDate AND :endDate ORDER BY transactionDate DESC")
    fun getTransactionsByTypeAndDate(type: TransactionType, startDate: Long, endDate: Long): Flow<List<TransactionEntity>>
}
