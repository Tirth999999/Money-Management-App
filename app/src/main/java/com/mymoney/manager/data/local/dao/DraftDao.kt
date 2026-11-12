package com.mymoney.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mymoney.manager.data.local.entity.TransactionDraftEntity
import com.mymoney.manager.data.model.DraftStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftDao {

    @Query("SELECT * FROM transaction_drafts WHERE status = 'PENDING' ORDER BY transactionDate DESC, id DESC")
    fun getPendingDrafts(): Flow<List<TransactionDraftEntity>>

    @Query("SELECT * FROM transaction_drafts ORDER BY transactionDate DESC, id DESC")
    fun getAllDrafts(): Flow<List<TransactionDraftEntity>>

    @Query("SELECT * FROM transaction_drafts WHERE id = :id LIMIT 1")
    suspend fun getDraftById(id: Long): TransactionDraftEntity?

    @Query("SELECT * FROM transaction_drafts WHERE transactionReference = :reference LIMIT 1")
    suspend fun getDraftByReference(reference: String): TransactionDraftEntity?

    @Query("SELECT COUNT(*) FROM transaction_drafts WHERE status = 'PENDING'")
    fun getPendingDraftsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM transaction_drafts WHERE status = 'PENDING'")
    suspend fun getPendingDraftsCountSync(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: TransactionDraftEntity): Long

    @Update
    suspend fun updateDraft(draft: TransactionDraftEntity)

    @Query("UPDATE transaction_drafts SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: DraftStatus)

    @Delete
    suspend fun deleteDraft(draft: TransactionDraftEntity)

    @Query("DELETE FROM transaction_drafts WHERE id = :id")
    suspend fun deleteDraftById(id: Long)
}
