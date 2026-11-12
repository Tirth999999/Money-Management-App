package com.mymoney.manager.data.local.database

import androidx.room.TypeConverter
import com.mymoney.manager.data.model.AccountType
import com.mymoney.manager.data.model.DetectionSource
import com.mymoney.manager.data.model.DraftStatus
import com.mymoney.manager.data.model.LoanStatus
import com.mymoney.manager.data.model.LoanType
import com.mymoney.manager.data.model.TransactionType

class Converters {

    @TypeConverter
    fun fromTransactionType(value: TransactionType?): String? = value?.name

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? =
        value?.let { enumValueOf<TransactionType>(it) }

    @TypeConverter
    fun fromDraftStatus(value: DraftStatus?): String? = value?.name

    @TypeConverter
    fun toDraftStatus(value: String?): DraftStatus? =
        value?.let { enumValueOf<DraftStatus>(it) }

    @TypeConverter
    fun fromDetectionSource(value: DetectionSource?): String? = value?.name

    @TypeConverter
    fun toDetectionSource(value: String?): DetectionSource? =
        value?.let { enumValueOf<DetectionSource>(it) }

    @TypeConverter
    fun fromAccountType(value: AccountType?): String? = value?.name

    @TypeConverter
    fun toAccountType(value: String?): AccountType? =
        value?.let { enumValueOf<AccountType>(it) }

    @TypeConverter
    fun fromLoanType(value: LoanType?): String? = value?.name

    @TypeConverter
    fun toLoanType(value: String?): LoanType? =
        value?.let { enumValueOf<LoanType>(it) }

    @TypeConverter
    fun fromLoanStatus(value: LoanStatus?): String? = value?.name

    @TypeConverter
    fun toLoanStatus(value: String?): LoanStatus? =
        value?.let { enumValueOf<LoanStatus>(it) }
}
