package com.mymoney.manager.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mymoney.manager.data.model.DetectionSource
import com.mymoney.manager.data.model.DraftStatus
import com.mymoney.manager.data.model.TransactionType

@Entity(
    tableName = "transaction_drafts",
    indices = [
        Index(value = ["transactionReference"]),
        Index(value = ["status"])
    ]
)
data class TransactionDraftEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TransactionType,
    val amountPaise: Long,
    val accountId: Long,
    val categoryId: Long? = null,
    val description: String = "",
    val transactionDate: Long = System.currentTimeMillis(),
    val source: DetectionSource,
    val originalSms: String? = null,
    val transactionReference: String? = null,
    val confidence: Float = 1.0f,
    val status: DraftStatus = DraftStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)
