package com.mymoney.manager.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mymoney.manager.data.model.TransactionType

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["name", "type"], unique = true)
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: TransactionType = TransactionType.EXPENSE,
    val iconName: String = "category",
    val colorHex: Long = 0xFF2563EB,
    val isActive: Boolean = true
)
