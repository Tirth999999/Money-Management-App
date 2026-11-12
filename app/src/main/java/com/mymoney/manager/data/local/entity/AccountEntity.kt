package com.mymoney.manager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mymoney.manager.data.model.AccountType

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val accountType: AccountType = AccountType.PRIMARY,
    val openingBalancePaise: Long = 0L,
    val currentBalancePaise: Long = 0L,
    val iconName: String = "account_balance",
    val colorHex: Long = 0xFF006C50,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
