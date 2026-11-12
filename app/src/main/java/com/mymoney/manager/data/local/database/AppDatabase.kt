package com.mymoney.manager.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mymoney.manager.data.local.dao.AccountDao
import com.mymoney.manager.data.local.dao.CategoryDao
import com.mymoney.manager.data.local.dao.DraftDao
import com.mymoney.manager.data.local.dao.LoanDao
import com.mymoney.manager.data.local.dao.PersonDao
import com.mymoney.manager.data.local.dao.TransactionDao
import com.mymoney.manager.data.local.entity.AccountEntity
import com.mymoney.manager.data.local.entity.CategoryEntity
import com.mymoney.manager.data.local.entity.LoanEntity
import com.mymoney.manager.data.local.entity.PersonEntity
import com.mymoney.manager.data.local.entity.RepaymentEntity
import com.mymoney.manager.data.local.entity.TransactionDraftEntity
import com.mymoney.manager.data.local.entity.TransactionEntity
import com.mymoney.manager.data.model.AccountType
import com.mymoney.manager.data.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        TransactionDraftEntity::class,
        PersonEntity::class,
        LoanEntity::class,
        RepaymentEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun draftDao(): DraftDao
    abstract fun personDao(): PersonDao
    abstract fun loanDao(): LoanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "my_money_manager.db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        prepopulateDatabase(database)
                    }
                }
            }
        }

        suspend fun prepopulateDatabase(database: AppDatabase) {
            // Prepopulate Initial Accounts only if none exist: Online — HDFC and Cash
            if (database.accountDao().getCount() == 0) {
                val initialAccounts = listOf(
                    AccountEntity(
                        id = 1,
                        name = "Online — HDFC",
                        accountType = AccountType.PRIMARY,
                        openingBalancePaise = 0L,
                        currentBalancePaise = 0L,
                        iconName = "account_balance",
                        colorHex = 0xFF2563EB
                    ),
                    AccountEntity(
                        id = 2,
                        name = "Cash",
                        accountType = AccountType.PRIMARY,
                        openingBalancePaise = 0L,
                        currentBalancePaise = 0L,
                        iconName = "payments",
                        colorHex = 0xFF10B981
                    )
                )
                database.accountDao().insertAccounts(initialAccounts)
            }

            // Prepopulate Default Categories only if none exist
            if (database.categoryDao().getCount() == 0) {
                val defaultCategories = listOf(
                    // Income Categories
                    CategoryEntity(name = "Salary", type = TransactionType.INCOME, iconName = "work", colorHex = 0xFF10B981),
                    CategoryEntity(name = "Freelancing", type = TransactionType.INCOME, iconName = "computer", colorHex = 0xFF059669),
                    CategoryEntity(name = "Project Payment", type = TransactionType.INCOME, iconName = "assignment", colorHex = 0xFF0284C7),
                    CategoryEntity(name = "Other Income", type = TransactionType.INCOME, iconName = "savings", colorHex = 0xFF16A34A),

                    // Expense Categories
                    CategoryEntity(name = "Food", type = TransactionType.EXPENSE, iconName = "restaurant", colorHex = 0xFFEA580C),
                    CategoryEntity(name = "Shopping", type = TransactionType.EXPENSE, iconName = "shopping_bag", colorHex = 0xFF9333EA),
                    CategoryEntity(name = "Travel", type = TransactionType.EXPENSE, iconName = "directions_car", colorHex = 0xFF2563EB),
                    CategoryEntity(name = "Bills", type = TransactionType.EXPENSE, iconName = "receipt_long", colorHex = 0xFFDC2626),
                    CategoryEntity(name = "Entertainment", type = TransactionType.EXPENSE, iconName = "movie", colorHex = 0xFFD97706),
                    CategoryEntity(name = "Health", type = TransactionType.EXPENSE, iconName = "medical_services", colorHex = 0xFFDB2777),
                    CategoryEntity(name = "Education", type = TransactionType.EXPENSE, iconName = "school", colorHex = 0xFF4F46E5),
                    CategoryEntity(name = "Personal", type = TransactionType.EXPENSE, iconName = "person", colorHex = 0xFF0891B2),
                    CategoryEntity(name = "Other", type = TransactionType.EXPENSE, iconName = "category", colorHex = 0xFF475569)
                )
                database.categoryDao().insertCategories(defaultCategories)
            }
        }
    }
}
