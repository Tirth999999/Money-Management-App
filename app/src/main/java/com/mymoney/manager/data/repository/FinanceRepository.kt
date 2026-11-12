package com.mymoney.manager.data.repository

import androidx.room.withTransaction
import com.mymoney.manager.data.local.database.AppDatabase
import com.mymoney.manager.data.local.entity.AccountEntity
import com.mymoney.manager.data.local.entity.CategoryEntity
import com.mymoney.manager.data.local.entity.LoanEntity
import com.mymoney.manager.data.local.entity.PersonEntity
import com.mymoney.manager.data.local.entity.RepaymentEntity
import com.mymoney.manager.data.local.entity.TransactionDraftEntity
import com.mymoney.manager.data.local.entity.TransactionEntity
import com.mymoney.manager.data.model.AccountType
import com.mymoney.manager.data.model.DetectionSource
import com.mymoney.manager.data.model.DraftStatus
import com.mymoney.manager.data.model.LoanStatus
import com.mymoney.manager.data.model.LoanType
import com.mymoney.manager.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FinanceRepository(private val database: AppDatabase) {

    private val accountDao = database.accountDao()
    private val categoryDao = database.categoryDao()
    private val transactionDao = database.transactionDao()
    private val draftDao = database.draftDao()
    private val personDao = database.personDao()
    private val loanDao = database.loanDao()

    // ----------------------------------------------------
    // ACCOUNTS
    // ----------------------------------------------------
    fun getAllAccounts(): Flow<List<AccountEntity>> = accountDao.getAllActiveAccounts()

    suspend fun getActiveAccountsList(): List<AccountEntity> = accountDao.getActiveAccountsList()

    fun getTotalAvailablePaise(): Flow<Long> = accountDao.getTotalAvailablePaise().map { it ?: 0L }

    suspend fun getAccountById(id: Long): AccountEntity? = accountDao.getAccountById(id)

    suspend fun getOrCreateDefaultAccount(isOnline: Boolean): AccountEntity {
        val targetName = if (isOnline) "Online — HDFC" else "Cash"
        val existing = accountDao.findAccountByName(targetName)
        if (existing != null) return existing

        val newAccount = AccountEntity(
            name = targetName,
            openingBalancePaise = 0L,
            currentBalancePaise = 0L,
            iconName = if (isOnline) "account_balance" else "payments",
            colorHex = if (isOnline) 0xFF006C50 else 0xFF2E7D32
        )
        val id = accountDao.insertAccount(newAccount)
        return newAccount.copy(id = id)
    }

    suspend fun createAccount(account: AccountEntity): Long {
        return database.withTransaction {
            val initialBalance = account.openingBalancePaise
            val accountWithBalance = account.copy(currentBalancePaise = initialBalance)
            accountDao.insertAccount(accountWithBalance)
        }
    }

    suspend fun updateAccount(account: AccountEntity) {
        accountDao.updateAccount(account)
    }

    suspend fun updateAccountWithOpeningBalance(
        accountId: Long,
        newName: String,
        newType: AccountType,
        newOpeningPaise: Long,
        newIcon: String,
        newColor: Long
    ) {
        database.withTransaction {
            val existing = accountDao.getAccountById(accountId) ?: return@withTransaction
            val openingDifference = newOpeningPaise - existing.openingBalancePaise
            val newCurrentBalance = existing.currentBalancePaise + openingDifference

            accountDao.updateAccountDetails(
                id = accountId,
                name = newName,
                type = newType,
                opening = newOpeningPaise,
                current = newCurrentBalance,
                icon = newIcon,
                color = newColor
            )
        }
    }

    suspend fun deleteAccount(accountId: Long) {
        database.withTransaction {
            if (accountDao.getActiveCount() <= 1) {
                throw IllegalStateException("Cannot delete the only remaining account. At least one account must exist.")
            }
            accountDao.deleteAccountById(accountId)
        }
    }

    suspend fun cleanupDuplicateCategories() {
        val allCats = categoryDao.getAllCategoriesList()
        val seen = mutableSetOf<Pair<String, TransactionType>>()
        for (cat in allCats) {
            val key = Pair(cat.name.trim().lowercase(), cat.type)
            if (seen.contains(key)) {
                categoryDao.deleteCategoryById(cat.id)
            } else {
                seen.add(key)
            }
        }
    }

    // ----------------------------------------------------
    // TRANSACTIONS & BALANCE UPDATES
    // ----------------------------------------------------
    fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun getTransactionsBetweenDates(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsBetweenDates(startDate, endDate)

    fun getIncomeSumBetweenDates(startDate: Long, endDate: Long): Flow<Long> =
        transactionDao.getIncomeSumBetweenDates(startDate, endDate).map { it ?: 0L }

    fun getExpenseSumBetweenDates(startDate: Long, endDate: Long): Flow<Long> =
        transactionDao.getExpenseSumBetweenDates(startDate, endDate).map { it ?: 0L }

    suspend fun recordTransaction(transaction: TransactionEntity): Long {
        return database.withTransaction {
            val txId = transactionDao.insertTransaction(transaction)

            // Update Account Balances based on transaction type
            val account = accountDao.getAccountById(transaction.accountId)
                ?: throw IllegalStateException("Account #${transaction.accountId} not found")

            when (transaction.type) {
                TransactionType.INCOME -> {
                    val newBalance = account.currentBalancePaise + transaction.amountPaise
                    accountDao.updateBalance(account.id, newBalance)
                }
                TransactionType.EXPENSE -> {
                    val newBalance = account.currentBalancePaise - transaction.amountPaise
                    accountDao.updateBalance(account.id, newBalance)
                }
                TransactionType.TRANSFER -> {
                    val destId = transaction.destinationAccountId
                        ?: throw IllegalArgumentException("Destination account required for transfer")
                    val destAccount = accountDao.getAccountById(destId)
                        ?: throw IllegalStateException("Destination account #$destId not found")

                    // Debit source account
                    accountDao.updateBalance(account.id, account.currentBalancePaise - transaction.amountPaise)
                    // Credit destination account
                    accountDao.updateBalance(destAccount.id, destAccount.currentBalancePaise + transaction.amountPaise)
                }
                TransactionType.LEND -> {
                    // Money lent to someone: Account decreases
                    val newBalance = account.currentBalancePaise - transaction.amountPaise
                    accountDao.updateBalance(account.id, newBalance)
                }
                TransactionType.BORROW -> {
                    // Money borrowed from someone: Account increases
                    val newBalance = account.currentBalancePaise + transaction.amountPaise
                    accountDao.updateBalance(account.id, newBalance)
                }
                TransactionType.REPAYMENT_RECEIVED -> {
                    // Someone repaid me: Account increases
                    val newBalance = account.currentBalancePaise + transaction.amountPaise
                    accountDao.updateBalance(account.id, newBalance)
                }
                TransactionType.REPAYMENT_PAID -> {
                    // I repaid someone: Account decreases
                    val newBalance = account.currentBalancePaise - transaction.amountPaise
                    accountDao.updateBalance(account.id, newBalance)
                }
            }

            txId
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        database.withTransaction {
            // Reverse account balance impact
            val account = accountDao.getAccountById(transaction.accountId)
            if (account != null) {
                when (transaction.type) {
                    TransactionType.INCOME -> {
                        accountDao.updateBalance(account.id, account.currentBalancePaise - transaction.amountPaise)
                    }
                    TransactionType.EXPENSE -> {
                        accountDao.updateBalance(account.id, account.currentBalancePaise + transaction.amountPaise)
                    }
                    TransactionType.TRANSFER -> {
                        val destId = transaction.destinationAccountId
                        if (destId != null) {
                            val destAccount = accountDao.getAccountById(destId)
                            if (destAccount != null) {
                                accountDao.updateBalance(account.id, account.currentBalancePaise + transaction.amountPaise)
                                accountDao.updateBalance(destAccount.id, destAccount.currentBalancePaise - transaction.amountPaise)
                            }
                        }
                    }
                    TransactionType.LEND -> {
                        accountDao.updateBalance(account.id, account.currentBalancePaise + transaction.amountPaise)
                    }
                    TransactionType.BORROW -> {
                        accountDao.updateBalance(account.id, account.currentBalancePaise - transaction.amountPaise)
                    }
                    TransactionType.REPAYMENT_RECEIVED -> {
                        accountDao.updateBalance(account.id, account.currentBalancePaise - transaction.amountPaise)
                    }
                    TransactionType.REPAYMENT_PAID -> {
                        accountDao.updateBalance(account.id, account.currentBalancePaise + transaction.amountPaise)
                    }
                }
            }
            transactionDao.deleteTransaction(transaction)
        }
    }

    // ----------------------------------------------------
    // DRAFTS (SMS & VOICE AUTOMATION)
    // ----------------------------------------------------
    fun getPendingDrafts(): Flow<List<TransactionDraftEntity>> = draftDao.getPendingDrafts()

    fun getPendingDraftsCount(): Flow<Int> = draftDao.getPendingDraftsCount()

    suspend fun getPendingDraftsCountSync(): Int = draftDao.getPendingDraftsCountSync()

    suspend fun isDraftDuplicate(reference: String?): Boolean {
        if (reference.isNullOrBlank()) return false
        val existingDraft = draftDao.getDraftByReference(reference)
        if (existingDraft != null) return true
        val existingTx = transactionDao.getTransactionByReference(reference)
        return existingTx != null
    }

    suspend fun createDraft(draft: TransactionDraftEntity): Long {
        return draftDao.insertDraft(draft)
    }

    suspend fun approveDraft(draftId: Long, categoryId: Long? = null, accountId: Long? = null): Long {
        return database.withTransaction {
            val draft = draftDao.getDraftById(draftId)
                ?: throw IllegalStateException("Draft #$draftId not found")

            val effectiveAccountId = accountId ?: draft.accountId
            val effectiveCategoryId = categoryId ?: draft.categoryId

            // Create approved transaction
            val transaction = TransactionEntity(
                type = draft.type,
                amountPaise = draft.amountPaise,
                accountId = effectiveAccountId,
                categoryId = effectiveCategoryId,
                description = draft.description,
                transactionDate = draft.transactionDate,
                source = draft.source,
                referenceId = draft.transactionReference,
                notes = if (draft.source == DetectionSource.SMS) "Approved from SMS draft" else "Approved from Voice draft"
            )

            val txId = recordTransaction(transaction)
            draftDao.updateStatus(draftId, DraftStatus.APPROVED)
            txId
        }
    }

    suspend fun rejectDraft(draftId: Long) {
        draftDao.updateStatus(draftId, DraftStatus.REJECTED)
    }

    suspend fun deleteDraft(draftId: Long) {
        draftDao.deleteDraftById(draftId)
    }

    suspend fun updateDraft(draft: TransactionDraftEntity) {
        draftDao.updateDraft(draft)
    }

    suspend fun updateAndApproveDraft(draft: TransactionDraftEntity): Long {
        return database.withTransaction {
            draftDao.updateDraft(draft)
            approveDraft(draft.id, draft.categoryId, draft.accountId)
        }
    }

    // ----------------------------------------------------
    // LENDING & BORROWING
    // ----------------------------------------------------
    fun getActiveLoans(type: LoanType): Flow<List<LoanEntity>> = loanDao.getActiveLoansByType(type)

    fun getAllLoans(): Flow<List<LoanEntity>> = loanDao.getAllLoans()

    fun getTotalReceivablesPaise(): Flow<Long> = loanDao.getTotalReceivablesPaise().map { it ?: 0L }

    fun getTotalLiabilitiesPaise(): Flow<Long> = loanDao.getTotalLiabilitiesPaise().map { it ?: 0L }

    suspend fun createLoan(
        personName: String,
        type: LoanType,
        amountPaise: Long,
        accountId: Long,
        notes: String? = null,
        dueDate: Long? = null
    ): Long {
        return database.withTransaction {
            var person = personDao.findPersonByName(personName.trim())
            if (person == null) {
                val pId = personDao.insertPerson(PersonEntity(name = personName.trim()))
                person = personDao.getPersonById(pId)!!
            }

            val loan = LoanEntity(
                personId = person.id,
                type = type,
                originalAmountPaise = amountPaise,
                outstandingAmountPaise = amountPaise,
                accountId = accountId,
                dueDate = dueDate,
                notes = notes,
                status = LoanStatus.ACTIVE
            )
            val loanId = loanDao.insertLoan(loan)

            // Record transaction in ledger and update account balance
            val txType = if (type == LoanType.LENT) TransactionType.LEND else TransactionType.BORROW
            val desc = if (type == LoanType.LENT) "Lent to ${person.name}" else "Borrowed from ${person.name}"

            val transaction = TransactionEntity(
                type = txType,
                amountPaise = amountPaise,
                accountId = accountId,
                description = desc,
                personId = person.id,
                loanId = loanId,
                notes = notes
            )
            recordTransaction(transaction)

            loanId
        }
    }

    suspend fun recordRepayment(
        loanId: Long,
        amountPaise: Long,
        accountId: Long,
        notes: String? = null
    ): Long {
        return database.withTransaction {
            val loan = loanDao.getLoanById(loanId)
                ?: throw IllegalStateException("Loan #$loanId not found")

            val newOutstanding = (loan.outstandingAmountPaise - amountPaise).coerceAtLeast(0L)
            val isSettled = newOutstanding == 0L

            val updatedLoan = loan.copy(
                outstandingAmountPaise = newOutstanding,
                status = if (isSettled) LoanStatus.SETTLED else LoanStatus.ACTIVE
            )
            loanDao.updateLoan(updatedLoan)

            val repayment = RepaymentEntity(
                loanId = loanId,
                amountPaise = amountPaise,
                accountId = accountId,
                notes = notes
            )
            val repaymentId = loanDao.insertRepayment(repayment)

            val person = personDao.getPersonById(loan.personId)
            val personName = person?.name ?: "Contact"

            val txType = if (loan.type == LoanType.LENT) {
                TransactionType.REPAYMENT_RECEIVED
            } else {
                TransactionType.REPAYMENT_PAID
            }

            val desc = if (loan.type == LoanType.LENT) {
                "Repayment received from $personName"
            } else {
                "Repayment paid to $personName"
            }

            val transaction = TransactionEntity(
                type = txType,
                amountPaise = amountPaise,
                accountId = accountId,
                description = desc,
                personId = loan.personId,
                loanId = loanId,
                notes = notes
            )
            recordTransaction(transaction)

            repaymentId
        }
    }

    // ----------------------------------------------------
    // CATEGORIES & PERSONS
    // ----------------------------------------------------
    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllActiveCategories()

    fun getCategoriesByType(type: TransactionType): Flow<List<CategoryEntity>> =
        categoryDao.getCategoriesByType(type)

    suspend fun createCategory(category: CategoryEntity): Long = categoryDao.insertCategory(category)

    suspend fun updateCategory(category: CategoryEntity) = categoryDao.updateCategory(category)

    suspend fun deleteCategory(category: CategoryEntity) = categoryDao.deleteCategory(category)

    fun getAllPersons(): Flow<List<PersonEntity>> = personDao.getAllPersons()
}
