package com.mymoney.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mymoney.manager.data.local.entity.LoanEntity
import com.mymoney.manager.data.local.entity.RepaymentEntity
import com.mymoney.manager.data.model.LoanStatus
import com.mymoney.manager.data.model.LoanType
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {

    @Query("SELECT * FROM loans WHERE status = :status ORDER BY startDate DESC")
    fun getLoansByStatus(status: LoanStatus): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans ORDER BY startDate DESC")
    fun getAllLoans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE type = :type AND status = 'ACTIVE' ORDER BY startDate DESC")
    fun getActiveLoansByType(type: LoanType): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE id = :id LIMIT 1")
    suspend fun getLoanById(id: Long): LoanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity): Long

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepayment(repayment: RepaymentEntity): Long

    @Query("SELECT * FROM repayments WHERE loanId = :loanId ORDER BY repaymentDate DESC")
    fun getRepaymentsForLoan(loanId: Long): Flow<List<RepaymentEntity>>

    // Total receivables (money lent to others that is still outstanding)
    @Query("SELECT SUM(outstandingAmountPaise) FROM loans WHERE type = 'LENT' AND status = 'ACTIVE'")
    fun getTotalReceivablesPaise(): Flow<Long?>

    // Total liabilities (money borrowed from others that is still outstanding)
    @Query("SELECT SUM(outstandingAmountPaise) FROM loans WHERE type = 'BORROWED' AND status = 'ACTIVE'")
    fun getTotalLiabilitiesPaise(): Flow<Long?>
}
