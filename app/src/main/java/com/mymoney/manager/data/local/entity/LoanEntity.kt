package com.mymoney.manager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mymoney.manager.data.model.LoanStatus
import com.mymoney.manager.data.model.LoanType

@Entity(
    tableName = "loans",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["personId"]),
        Index(value = ["accountId"]),
        Index(value = ["status"])
    ]
)
data class LoanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val personId: Long,
    val type: LoanType, // LENT (Receivable - Money Lent to someone) or BORROWED (Liability - Money Borrowed from someone)
    val originalAmountPaise: Long,
    val outstandingAmountPaise: Long,
    val accountId: Long,
    val startDate: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val notes: String? = null,
    val status: LoanStatus = LoanStatus.ACTIVE
)
