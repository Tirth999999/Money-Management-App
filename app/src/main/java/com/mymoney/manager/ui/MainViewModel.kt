package com.mymoney.manager.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mymoney.manager.data.local.database.AppDatabase
import com.mymoney.manager.data.local.entity.AccountEntity
import com.mymoney.manager.data.local.entity.CategoryEntity
import com.mymoney.manager.data.local.entity.LoanEntity
import com.mymoney.manager.data.local.entity.PersonEntity
import com.mymoney.manager.data.local.entity.TransactionDraftEntity
import com.mymoney.manager.data.local.entity.TransactionEntity
import com.mymoney.manager.data.model.AccountType
import com.mymoney.manager.data.model.CurrencyUtils
import com.mymoney.manager.data.model.DetectionSource
import com.mymoney.manager.data.model.LoanStatus
import com.mymoney.manager.data.model.LoanType
import com.mymoney.manager.data.model.TransactionType
import com.mymoney.manager.data.repository.FinanceRepository
import com.mymoney.manager.parser.VoiceCommandParser
import com.mymoney.manager.parser.VoiceCommandResult
import com.mymoney.manager.reminder.DailyReminderManager
import com.mymoney.manager.voice.JarvisVoiceManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class TimePeriod(val displayName: String) {
    DAY("Day"),
    MONTH("Month"),
    YEAR("Year"),
    ALL("All")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = FinanceRepository(db)

    // Ensure database prepopulates on initial startup and heals duplicates
    init {
        viewModelScope.launch {
            if (db.accountDao().getCount() == 0 || db.categoryDao().getCount() == 0) {
                AppDatabase.prepopulateDatabase(db)
            }
            repository.cleanupDuplicateCategories()
        }
    }

    // Accounts
    val accounts: StateFlow<List<AccountEntity>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val totalAvailablePaise: StateFlow<Long> = repository.getTotalAvailablePaise()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Categories
    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filter states
    val selectedPeriod = MutableStateFlow(TimePeriod.MONTH)
    val searchQuery = MutableStateFlow("")
    val selectedTypeFilter = MutableStateFlow<TransactionType?>(null)
    val selectedAccountFilter = MutableStateFlow<Long?>(null)

    // Calendar range calculation
    private val calendar = Calendar.getInstance()

    // Transactions with filtering
    private val rawTransactions = repository.getAllTransactions()

    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        rawTransactions,
        selectedPeriod,
        searchQuery,
        selectedTypeFilter,
        selectedAccountFilter
    ) { txList, period, query, typeFilter, accountFilter ->
        val now = System.currentTimeMillis()
        val (start, end) = getPeriodRange(period, now)

        txList.filter { tx ->
            val inPeriod = tx.transactionDate in start..end
            val matchesType = typeFilter == null || tx.type == typeFilter
            val matchesAccount = accountFilter == null || tx.accountId == accountFilter || tx.destinationAccountId == accountFilter
            val matchesSearch = query.isBlank() ||
                    tx.description.contains(query, ignoreCase = true) ||
                    (tx.notes?.contains(query, ignoreCase = true) == true) ||
                    CurrencyUtils.formatPaise(tx.amountPaise).contains(query)

            inPeriod && matchesType && matchesAccount && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Income & Expense totals for current period
    val periodIncomePaise: StateFlow<Long> = filteredTransactions.mapState { txList ->
        txList.filter { it.type == TransactionType.INCOME }.sumOf { it.amountPaise }
    }

    val periodExpensePaise: StateFlow<Long> = filteredTransactions.mapState { txList ->
        txList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountPaise }
    }

    // Pending Drafts
    val pendingDrafts: StateFlow<List<TransactionDraftEntity>> = repository.getPendingDrafts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingDraftsCount: StateFlow<Int> = repository.getPendingDraftsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Lending & Borrowing
    val lentLoans: StateFlow<List<LoanEntity>> = repository.getActiveLoans(LoanType.LENT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val borrowedLoans: StateFlow<List<LoanEntity>> = repository.getActiveLoans(LoanType.BORROWED)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalReceivablesPaise: StateFlow<Long> = repository.getTotalReceivablesPaise()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalLiabilitiesPaise: StateFlow<Long> = repository.getTotalLiabilitiesPaise()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val persons: StateFlow<List<PersonEntity>> = repository.getAllPersons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Voice Dialog State
    private val _voiceDialogVisible = MutableStateFlow(false)
    val voiceDialogVisible: StateFlow<Boolean> = _voiceDialogVisible.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _voiceRecognizedText = MutableStateFlow("")
    val voiceRecognizedText: StateFlow<String> = _voiceRecognizedText.asStateFlow()

    private val _jarvisSpokenPrompt = MutableStateFlow("Listening... Speak in English, Hindi or Gujarati")
    val jarvisSpokenPrompt: StateFlow<String> = _jarvisSpokenPrompt.asStateFlow()

    private val _needsClarification = MutableStateFlow<VoiceCommandResult.NeedsClarification?>(null)
    val needsClarification: StateFlow<VoiceCommandResult.NeedsClarification?> = _needsClarification.asStateFlow()

    private val _voiceSuccessMessage = MutableStateFlow<String?>(null)
    val voiceSuccessMessage: StateFlow<String?> = _voiceSuccessMessage.asStateFlow()

    // In-app snackbar messages for instant feedback
    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    fun emitSnackbar(message: String) {
        viewModelScope.launch {
            _snackbarMessage.emit(message)
        }
    }

    var voiceManager: JarvisVoiceManager? = null

    fun setPeriod(period: TimePeriod) {
        selectedPeriod.value = period
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setTypeFilter(type: TransactionType?) {
        selectedTypeFilter.value = type
    }

    fun setAccountFilter(accountId: Long?) {
        selectedAccountFilter.value = accountId
    }

    // --- Draft Actions ---
    fun approveDraft(draftId: Long, categoryId: Long? = null, accountId: Long? = null) {
        viewModelScope.launch {
            repository.approveDraft(draftId, categoryId, accountId)
            emitSnackbar("Draft approved & balance updated!")
        }
    }

    fun rejectDraft(draftId: Long) {
        viewModelScope.launch {
            repository.rejectDraft(draftId)
            emitSnackbar("Draft rejected.")
        }
    }

    fun deleteDraft(draftId: Long) {
        viewModelScope.launch {
            repository.deleteDraft(draftId)
            emitSnackbar("Draft deleted.")
        }
    }

    fun updateDraft(
        draft: TransactionDraftEntity,
        approveImmediately: Boolean = false
    ) {
        viewModelScope.launch {
            if (approveImmediately) {
                repository.updateAndApproveDraft(draft)
                emitSnackbar("Draft saved & approved into balance!")
            } else {
                repository.updateDraft(draft)
                emitSnackbar("Draft updated successfully.")
            }
        }
    }

    // --- Transaction Actions ---
    fun addManualTransaction(
        type: TransactionType,
        amountPaise: Long,
        accountId: Long,
        destinationAccountId: Long? = null,
        categoryId: Long? = null,
        description: String = "",
        personName: String? = null,
        notes: String? = null
    ) {
        viewModelScope.launch {
            if (type == TransactionType.LEND || type == TransactionType.BORROW) {
                if (!personName.isNullOrBlank()) {
                    val loanType = if (type == TransactionType.LEND) LoanType.LENT else LoanType.BORROWED
                    repository.createLoan(
                        personName = personName,
                        type = loanType,
                        amountPaise = amountPaise,
                        accountId = accountId,
                        notes = notes
                    )
                    emitSnackbar("Recorded: ${if (type == TransactionType.LEND) "Lent to" else "Borrowed from"} $personName")
                    return@launch
                }
            }

            val tx = TransactionEntity(
                type = type,
                amountPaise = amountPaise,
                accountId = accountId,
                destinationAccountId = destinationAccountId,
                categoryId = categoryId,
                description = description.ifBlank { type.displayName },
                notes = notes,
                source = DetectionSource.MANUAL
            )
            repository.recordTransaction(tx)
            emitSnackbar("Transaction recorded successfully!")
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            emitSnackbar("Transaction deleted and balance adjusted.")
        }
    }

    fun transferMoney(fromAccountId: Long, toAccountId: Long, amountPaise: Long, notes: String? = null) {
        viewModelScope.launch {
            val fromAccount = repository.getAccountById(fromAccountId)
            val toAccount = repository.getAccountById(toAccountId)
            val desc = "Transfer to ${toAccount?.name ?: "Account"}"

            val tx = TransactionEntity(
                type = TransactionType.TRANSFER,
                amountPaise = amountPaise,
                accountId = fromAccountId,
                destinationAccountId = toAccountId,
                description = desc,
                notes = notes,
                source = DetectionSource.MANUAL
            )
            repository.recordTransaction(tx)
            emitSnackbar("Transfer completed!")
        }
    }

    // --- Account & Category Actions ---
    fun createAccount(name: String, openingBalancePaise: Long, accountType: AccountType = AccountType.PRIMARY, icon: String = "account_balance", colorHex: Long = 0xFF2563EB) {
        viewModelScope.launch {
            val account = AccountEntity(
                name = name,
                accountType = accountType,
                openingBalancePaise = openingBalancePaise,
                iconName = icon,
                colorHex = colorHex
            )
            repository.createAccount(account)
            emitSnackbar("Account '$name' created!")
        }
    }

    fun updateAccount(
        accountId: Long,
        name: String,
        type: AccountType,
        openingBalancePaise: Long,
        icon: String,
        colorHex: Long
    ) {
        viewModelScope.launch {
            repository.updateAccountWithOpeningBalance(
                accountId = accountId,
                newName = name,
                newType = type,
                newOpeningPaise = openingBalancePaise,
                newIcon = icon,
                newColor = colorHex
            )
            emitSnackbar("Account '$name' updated successfully!")
        }
    }

    fun deleteAccount(accountId: Long, accountName: String) {
        viewModelScope.launch {
            try {
                repository.deleteAccount(accountId)
                emitSnackbar("Account '$accountName' deleted.")
            } catch (e: Exception) {
                emitSnackbar(e.message ?: "Failed to delete account")
            }
        }
    }

    fun cleanupDuplicates() {
        viewModelScope.launch {
            repository.cleanupDuplicateCategories()
            emitSnackbar("Categories verified & duplicates removed.")
        }
    }

    fun createCategory(name: String, type: TransactionType, icon: String = "category", colorHex: Long = 0xFFD97706) {
        viewModelScope.launch {
            val cat = CategoryEntity(
                name = name,
                type = type,
                iconName = icon,
                colorHex = colorHex
            )
            repository.createCategory(cat)
            emitSnackbar("Category '$name' created!")
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            emitSnackbar("Category deleted.")
        }
    }

    // --- Settle Loan / Repayment ---
    fun recordLoanRepayment(loanId: Long, amountPaise: Long, accountId: Long, notes: String? = null) {
        viewModelScope.launch {
            repository.recordRepayment(loanId, amountPaise, accountId, notes)
            emitSnackbar("Loan repayment recorded!")
        }
    }

    fun triggerTestNotification() {
        DailyReminderManager.sendTestNotification(getApplication(), 1)
        emitSnackbar("Test notification sent! Check status bar.")
    }

    // --- Voice Assistant (Jarvis) ---
    fun openVoiceAssistant() {
        _voiceDialogVisible.value = true
        _voiceRecognizedText.value = ""
        _needsClarification.value = null
        _voiceSuccessMessage.value = null
        _jarvisSpokenPrompt.value = "Listening... Speak in English, Hindi or Gujarati"
        voiceManager?.startListening()
    }

    fun closeVoiceAssistant() {
        voiceManager?.stopListening()
        _voiceDialogVisible.value = false
        _needsClarification.value = null
    }

    fun handleVoiceResult(speechText: String) {
        _voiceRecognizedText.value = speechText
        val partial = _needsClarification.value

        val result = VoiceCommandParser.parse(
            input = speechText,
            partialAmountPaise = partial?.partialAmountPaise,
            partialIsOnline = partial?.partialIsOnline,
            partialType = partial?.partialType,
            availableAccounts = accounts.value,
            partialAccountId = partial?.partialAccountId
        )

        when (result) {
            is VoiceCommandResult.DraftReady -> {
                _needsClarification.value = null
                saveVoiceDraft(result)
            }
            is VoiceCommandResult.NeedsClarification -> {
                _needsClarification.value = result
                _jarvisSpokenPrompt.value = result.spokenPrompt
                // Hands-free continuous voice: automatically resume listening after question!
                voiceManager?.speak(result.spokenPrompt) {
                    voiceManager?.startListening()
                }
            }
            is VoiceCommandResult.TransferReady -> {
                executeVoiceTransfer(result)
            }
            is VoiceCommandResult.LoanReady -> {
                executeVoiceLoan(result)
            }
            is VoiceCommandResult.AnalysisQuery -> {
                answerAnalysisQuery(result)
            }
            is VoiceCommandResult.Unrecognized -> {
                val prompt = "Could not understand. Please specify amount, account name or cash, and income or expense."
                _jarvisSpokenPrompt.value = prompt
                voiceManager?.speak(prompt) {
                    voiceManager?.startListening()
                }
            }
        }
    }

    fun clarifyAccountMode(isOnline: Boolean) {
        val current = _needsClarification.value ?: return
        handleVoiceResult(if (isOnline) "online" else "offline")
    }

    fun clarifyType(type: TransactionType) {
        val current = _needsClarification.value ?: return
        handleVoiceResult(if (type == TransactionType.INCOME) "income" else "expense")
    }

    private fun saveVoiceDraft(result: VoiceCommandResult.DraftReady) {
        viewModelScope.launch {
            val currentAccounts = if (accounts.value.isNotEmpty()) {
                accounts.value
            } else {
                repository.getActiveAccountsList()
            }
            val account = if (result.targetAccountId != null) {
                currentAccounts.find { it.id == result.targetAccountId }
                    ?: repository.getOrCreateDefaultAccount(result.isOnline)
            } else {
                repository.getOrCreateDefaultAccount(result.isOnline)
            }
            var catId: Long? = null
            if (result.suggestedCategory != null) {
                catId = db.categoryDao().findCategoryByName(result.suggestedCategory)?.id
            }

            val draft = TransactionDraftEntity(
                type = result.type,
                amountPaise = result.amountPaise,
                accountId = account.id,
                categoryId = catId,
                description = result.description,
                transactionDate = System.currentTimeMillis(),
                source = DetectionSource.VOICE,
                confidence = 1.0f,
                status = com.mymoney.manager.data.model.DraftStatus.PENDING
            )

            repository.createDraft(draft)

            val amountStr = CurrencyUtils.formatPaise(result.amountPaise)
            val typeStr = if (result.type == TransactionType.INCOME) "income" else "expense"
            val accountName = account.name
            val confirm = "Created $amountStr $typeStr draft for $accountName."

            _voiceSuccessMessage.value = confirm
            _jarvisSpokenPrompt.value = confirm
            voiceManager?.speak(confirm)
        }
    }

    private fun executeVoiceTransfer(result: VoiceCommandResult.TransferReady) {
        viewModelScope.launch {
            val currentAccounts = if (accounts.value.isNotEmpty()) {
                accounts.value
            } else {
                repository.getActiveAccountsList()
            }
            val fromAcc = currentAccounts.find { it.name.contains(result.fromAccountName, ignoreCase = true) }
                ?: repository.getOrCreateDefaultAccount(true)
            val toAcc = currentAccounts.find { it.name.contains(result.toAccountName, ignoreCase = true) }
                ?: db.accountDao().findAccountByName(result.toAccountName)
                ?: repository.getOrCreateDefaultAccount(false)

            transferMoney(fromAcc.id, toAcc.id, result.amountPaise, "Voice Transfer")
            val amountStr = CurrencyUtils.formatPaise(result.amountPaise)
            val confirm = "Transferred $amountStr from ${fromAcc.name} to ${toAcc.name}."
            _voiceSuccessMessage.value = confirm
            _jarvisSpokenPrompt.value = confirm
            voiceManager?.speak(confirm)
        }
    }

    private fun executeVoiceLoan(result: VoiceCommandResult.LoanReady) {
        viewModelScope.launch {
            val account = repository.getOrCreateDefaultAccount(result.isOnline)
            repository.createLoan(
                personName = result.personName,
                type = result.loanType,
                amountPaise = result.amountPaise,
                accountId = account.id
            )
            val amountStr = CurrencyUtils.formatPaise(result.amountPaise)
            val confirm = if (result.loanType == LoanType.LENT) {
                "Recorded: Lent $amountStr to ${result.personName}."
            } else {
                "Recorded: Borrowed $amountStr from ${result.personName}."
            }
            _voiceSuccessMessage.value = confirm
            _jarvisSpokenPrompt.value = confirm
            voiceManager?.speak(confirm)
        }
    }

    private fun answerAnalysisQuery(query: VoiceCommandResult.AnalysisQuery) {
        viewModelScope.launch {
            val answer = when (query.queryType) {
                VoiceCommandResult.AnalysisQuery.QueryType.TOTAL_EXPENSE_MONTH -> {
                    val exp = periodExpensePaise.value
                    "You have spent ${CurrencyUtils.formatPaise(exp)} this month."
                }
                VoiceCommandResult.AnalysisQuery.QueryType.TOTAL_INCOME_MONTH -> {
                    val inc = periodIncomePaise.value
                    "Your total income this month is ${CurrencyUtils.formatPaise(inc)}."
                }
                VoiceCommandResult.AnalysisQuery.QueryType.ACCOUNT_BALANCE -> {
                    val acc = accounts.value.find { it.name.contains(query.targetName ?: "", ignoreCase = true) }
                    if (acc != null) {
                        "Your balance in ${acc.name} is ${CurrencyUtils.formatPaise(acc.currentBalancePaise)}."
                    } else {
                        "Account not found."
                    }
                }
                VoiceCommandResult.AnalysisQuery.QueryType.TOTAL_AVAILABLE_MONEY -> {
                    val total = totalAvailablePaise.value
                    "Your total available money across all accounts is ${CurrencyUtils.formatPaise(total)}."
                }
                VoiceCommandResult.AnalysisQuery.QueryType.HIGHEST_EXPENSE_CATEGORY -> {
                    val txList = filteredTransactions.value.filter { it.type == TransactionType.EXPENSE }
                    val topCat = txList.groupBy { it.categoryId }.maxByOrNull { it.value.sumOf { tx -> tx.amountPaise } }
                    val catName = topCat?.key?.let { db.categoryDao().getCategoryById(it)?.name } ?: "None"
                    "Your highest expense category is $catName."
                }
                else -> "I can only read from your actual saved financial data."
            }
            _jarvisSpokenPrompt.value = answer
            voiceManager?.speak(answer)
        }
    }

    private fun getPeriodRange(period: TimePeriod, now: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        return when (period) {
            TimePeriod.DAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                Pair(start, cal.timeInMillis)
            }
            TimePeriod.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                Pair(start, cal.timeInMillis)
            }
            TimePeriod.YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                Pair(start, cal.timeInMillis)
            }
            TimePeriod.ALL -> Pair(0L, Long.MAX_VALUE)
        }
    }

    private fun <T, R> StateFlow<T>.mapState(transform: (T) -> R): StateFlow<R> {
        val initial = transform(this.value)
        val flow = MutableStateFlow(initial)
        viewModelScope.launch {
            this@mapState.collect {
                flow.value = transform(it)
            }
        }
        return flow.asStateFlow()
    }
}
