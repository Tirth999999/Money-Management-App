package com.mymoney.manager.parser

import com.mymoney.manager.data.local.entity.AccountEntity
import com.mymoney.manager.data.model.CurrencyUtils
import com.mymoney.manager.data.model.LoanType
import com.mymoney.manager.data.model.TransactionType
import java.util.Locale
import java.util.regex.Pattern

sealed class VoiceCommandResult {
    data class DraftReady(
        val type: TransactionType,
        val amountPaise: Long,
        val isOnline: Boolean, // fallback flag
        val description: String,
        val suggestedCategory: String?,
        val targetAccountId: Long? = null,
        val targetAccountName: String? = null
    ) : VoiceCommandResult()

    data class NeedsClarification(
        val spokenPrompt: String,
        val partialAmountPaise: Long?,
        val partialIsOnline: Boolean?,
        val partialType: TransactionType?,
        val missingField: MissingField,
        val partialAccountId: Long? = null
    ) : VoiceCommandResult() {
        enum class MissingField {
            AMOUNT,
            ACCOUNT_MODE,
            TRANSACTION_TYPE
        }
    }

    data class TransferReady(
        val amountPaise: Long,
        val fromAccountName: String,
        val toAccountName: String
    ) : VoiceCommandResult()

    data class LoanReady(
        val loanType: LoanType,
        val amountPaise: Long,
        val personName: String,
        val isOnline: Boolean
    ) : VoiceCommandResult()

    data class AnalysisQuery(
        val queryType: QueryType,
        val targetName: String? = null
    ) : VoiceCommandResult() {
        enum class QueryType {
            TOTAL_EXPENSE_MONTH,
            TOTAL_INCOME_MONTH,
            ACCOUNT_BALANCE,
            TOTAL_AVAILABLE_MONEY,
            HIGHEST_EXPENSE_CATEGORY,
            UNKNOWN
        }
    }

    data class Unrecognized(val rawText: String) : VoiceCommandResult()
}

object VoiceCommandParser {

    private val GUJARATI_HINDI_NUMBERS = mapOf(
        "ek hajar" to 1000L,
        "ek hazar" to 1000L,
        "be hajar" to 2000L,
        "do hazar" to 2000L,
        "hajar" to 1000L,
        "hazar" to 1000L,
        "paanch so" to 500L,
        "panch sau" to 500L,
        "char so" to 400L,
        "char sau" to 400L,
        "tran so" to 300L,
        "teen sau" to 300L,
        "be so" to 200L,
        "do sau" to 200L,
        "ek so" to 100L,
        "sau" to 100L,
        "so" to 100L,
        "pachas" to 50L,
        "pachaas" to 50L,
        "chalis" to 40L,
        "chaalis" to 40L,
        "tris" to 30L,
        "tees" to 30L,
        "vis" to 20L,
        "bees" to 20L,
        "das" to 10L
    )

    private val ONLINE_WORDS = listOf(
        "\\bonline\\b", "\\bhdfc\\b", "\\bbank\\b", "\\bupi\\b", "\\bgpay\\b",
        "\\bgoogle pay\\b", "\\bphonepe\\b", "\\bpaytm\\b", "\\bnetbanking\\b",
        "\\bcard\\b", "\\bkhata ma\\b", "\\bbank ma\\b"
    ).map { Pattern.compile(it, Pattern.CASE_INSENSITIVE) }

    private val OFFLINE_WORDS = listOf(
        "\\boffline\\b", "\\bcash\\b", "\\brokad\\b", "\\brokhad\\b", "\\bnagad\\b",
        "\\bhaath ma\\b", "\\bhath ma\\b", "\\bwallet\\b", "\\bpocket\\b", "\\bkissa ma\\b"
    ).map { Pattern.compile(it, Pattern.CASE_INSENSITIVE) }

    private val INCOME_WORDS = listOf(
        "\\bincome\\b", "\\bincomes\\b", "\\bcredit\\b", "\\bcredited\\b", "\\bjama\\b",
        "\\baavya\\b", "\\baaviya\\b", "\\baapo\\b", "\\bmila\\b", "\\bgot\\b",
        "\\breceived\\b", "\\bearned\\b", "\\bsalary\\b", "\\bprofit\\b",
        "\\bdeposit\\b", "\\bdeposited\\b", "\\bumeero\\b", "\\bumero\\b"
    ).map { Pattern.compile(it, Pattern.CASE_INSENSITIVE) }

    private val EXPENSE_WORDS = listOf(
        "\\bexpense\\b", "\\bexpenses\\b", "\\bexpence\\b", "\\bkharch\\b", "\\bkharcha\\b", "\\bkharcho\\b",
        "\\bghatao\\b", "\\bkapo\\b", "\\breduce\\b", "\\bvaprana\\b", "\\bchookvya\\b",
        "\\bdidha\\b", "\\bdebit\\b", "\\bdebited\\b", "\\bspend\\b", "\\bspent\\b", "\\bspending\\b",
        "\\bpaid\\b", "\\bpay\\b", "\\bbought\\b", "\\bpurchased\\b"
    ).map { Pattern.compile(it, Pattern.CASE_INSENSITIVE) }

    fun parse(
        input: String,
        partialAmountPaise: Long? = null,
        partialIsOnline: Boolean? = null,
        partialType: TransactionType? = null,
        availableAccounts: List<AccountEntity> = emptyList(),
        partialAccountId: Long? = null
    ): VoiceCommandResult {
        val raw = input.trim()
        if (raw.isEmpty()) return VoiceCommandResult.Unrecognized("")

        // Normalize text
        var text = raw.lowercase(Locale.ROOT)
            .replace("jarvis,", "")
            .replace("jarvis", "")
            .replace("please", "")
            .trim()

        // 1. Check for Read-Only Analysis Queries
        checkAnalysisQuery(text)?.let { return it }

        // 2. Check for Account Transfers
        checkTransferQuery(text)?.let { return it }

        // 3. Check for Lending and Borrowing
        checkLendingBorrowing(text)?.let { return it }

        // 4. Draft Entry (Income / Expense)
        var amountPaise = partialAmountPaise ?: extractAmount(text)
        var type = partialType ?: extractTransactionType(text)

        // Resolve Target Account dynamically from user's actual accounts
        var targetAccount: AccountEntity? = if (partialAccountId != null) {
            availableAccounts.find { it.id == partialAccountId }
        } else null

        if (targetAccount == null && availableAccounts.isNotEmpty()) {
            targetAccount = findMatchingAccount(text, availableAccounts)
        }

        var isOnline = partialIsOnline ?: if (targetAccount != null) {
            !targetAccount.name.contains("cash", ignoreCase = true)
        } else {
            extractAccountMode(text)
        }

        // If targetAccount still null, try matching default accounts from mode
        if (targetAccount == null && isOnline != null && availableAccounts.isNotEmpty()) {
            targetAccount = if (isOnline) {
                availableAccounts.find { it.name.contains("hdfc", ignoreCase = true) || it.name.contains("online", ignoreCase = true) }
                    ?: availableAccounts.firstOrNull()
            } else {
                availableAccounts.find { it.name.contains("cash", ignoreCase = true) }
                    ?: availableAccounts.getOrNull(1) ?: availableAccounts.firstOrNull()
            }
        }

        if (amountPaise == null || amountPaise == 0L) {
            amountPaise = extractAmount(text)
        }
        if (type == null) {
            type = extractTransactionType(text)
        }

        // Validate the 3 mandatory fields:
        // Field 1: Amount
        if (amountPaise == null || amountPaise == 0L) {
            return VoiceCommandResult.NeedsClarification(
                spokenPrompt = "How much amount should I record?",
                partialAmountPaise = null,
                partialIsOnline = isOnline,
                partialType = type,
                missingField = VoiceCommandResult.NeedsClarification.MissingField.AMOUNT,
                partialAccountId = targetAccount?.id
            )
        }

        val amountDisplay = CurrencyUtils.formatPaise(amountPaise, includeDecimals = false)

        // Field 2: Target Account
        if (targetAccount == null && isOnline == null) {
            val spoken = if (availableAccounts.size > 2) {
                "Which account should I use for $amountDisplay?"
            } else {
                val acc1 = availableAccounts.getOrNull(0)?.name ?: "Online in HDFC"
                val acc2 = availableAccounts.getOrNull(1)?.name ?: "Cash"
                "Should I use $acc1 or $acc2 for $amountDisplay?"
            }
            return VoiceCommandResult.NeedsClarification(
                spokenPrompt = spoken,
                partialAmountPaise = amountPaise,
                partialIsOnline = null,
                partialType = type,
                missingField = VoiceCommandResult.NeedsClarification.MissingField.ACCOUNT_MODE,
                partialAccountId = null
            )
        }

        // Field 3: Type (Income vs Expense)
        if (type == null) {
            return VoiceCommandResult.NeedsClarification(
                spokenPrompt = "Is $amountDisplay an Income or an Expense?",
                partialAmountPaise = amountPaise,
                partialIsOnline = isOnline,
                partialType = null,
                missingField = VoiceCommandResult.NeedsClarification.MissingField.TRANSACTION_TYPE,
                partialAccountId = targetAccount?.id
            )
        }

        // All 3 mandatory fields present! Extract description & category if mentioned
        val description = extractDescription(text, type, isOnline ?: true, targetAccount?.name)
        val suggestedCategory = extractCategory(text)

        return VoiceCommandResult.DraftReady(
            type = type,
            amountPaise = amountPaise,
            isOnline = isOnline ?: true,
            description = description.ifEmpty { if (type == TransactionType.INCOME) "Income Entry" else "Expense Entry" },
            suggestedCategory = suggestedCategory,
            targetAccountId = targetAccount?.id,
            targetAccountName = targetAccount?.name
        )
    }

    private fun findMatchingAccount(text: String, accounts: List<AccountEntity>): AccountEntity? {
        val lower = text.lowercase()
        val normalizedText = lower.replace("—", " ").replace("-", " ").replace("_", " ")

        // 1. Full account name match (exact or normalized)
        for (acc in accounts) {
            val accNameLower = acc.name.lowercase().trim()
            val normalizedAccName = accNameLower.replace("—", " ").replace("-", " ").replace("_", " ").trim()
            if (accNameLower.isNotEmpty() && (lower.contains(accNameLower) || normalizedText.contains(normalizedAccName))) {
                return acc
            }
        }

        // 2. Distinctive non-generic words (e.g. "savings", "hdfc", "emergency", "icici", "kotak", "sbi", "cash")
        val genericWords = setOf("online", "offline", "bank", "account", "fund", "primary", "secondary")
        for (acc in accounts) {
            val words = acc.name.lowercase().split(" ", "—", "-", "_").map { it.trim() }.filter { it.length >= 2 }
            for (w in words) {
                if (w !in genericWords && (lower.contains(w) || normalizedText.contains(w))) {
                    return acc
                }
            }
        }

        // 3. Fallback to generic words (e.g. "online", "cash")
        for (acc in accounts) {
            val words = acc.name.lowercase().split(" ", "—", "-", "_").map { it.trim() }.filter { it.length >= 2 }
            for (w in words) {
                if (w != "bank" && w != "account" && w != "fund" && (lower.contains(w) || normalizedText.contains(w))) {
                    return acc
                }
            }
        }
        return null
    }

    private fun extractAmount(text: String): Long? {
        // First check phonetic Gujarati / Hindi number phrases
        for ((phrase, value) in GUJARATI_HINDI_NUMBERS) {
            val pattern = Pattern.compile("\\b$phrase\\b", Pattern.CASE_INSENSITIVE)
            if (pattern.matcher(text).find()) {
                return value * 100L
            }
        }

        // Check digit numbers: "500 rupees", "₹100", "150.50", "100"
        val regex = Pattern.compile("(?:₹|rs\\.?|rupees?\\s*)?\\b([0-9]+(?:\\.[0-9]{1,2})?)\\b(?:\\s*(?:rupees?|rs\\.?|rupiya))?", Pattern.CASE_INSENSITIVE)
        val matcher = regex.matcher(text)
        while (matcher.find()) {
            val str = matcher.group(1)
            val parsed = str?.toDoubleOrNull()
            if (parsed != null && parsed > 0) {
                return CurrencyUtils.rupeesToPaise(parsed)
            }
        }

        return null
    }

    private fun extractAccountMode(text: String): Boolean? {
        // Strict word boundary check to avoid false positives (e.g. "rupiya" containing "upi")
        for (pattern in ONLINE_WORDS) {
            if (pattern.matcher(text).find()) {
                return true
            }
        }
        for (pattern in OFFLINE_WORDS) {
            if (pattern.matcher(text).find()) {
                return false
            }
        }
        return null
    }

    private fun extractTransactionType(text: String): TransactionType? {
        for (pattern in INCOME_WORDS) {
            if (pattern.matcher(text).find()) {
                return TransactionType.INCOME
            }
        }
        for (pattern in EXPENSE_WORDS) {
            if (pattern.matcher(text).find()) {
                return TransactionType.EXPENSE
            }
        }
        return null
    }

    private fun extractDescription(text: String, type: TransactionType, isOnline: Boolean, accountName: String? = null): String {
        var clean = text
        // Remove amount phrases
        clean = clean.replace(Pattern.compile("(?:₹|rs\\.?|rupees?|rupiya)?\\s*[0-9]+(?:\\.[0-9]+)?\\s*(?:₹|rs\\.?|rupees?|rupiya)?", Pattern.CASE_INSENSITIVE).toRegex(), "")
        for ((phrase, _) in GUJARATI_HINDI_NUMBERS) {
            clean = clean.replace(Pattern.compile("\\b$phrase\\b", Pattern.CASE_INSENSITIVE).toRegex(), "")
        }
        // Remove account name if present
        if (!accountName.isNullOrBlank()) {
            clean = clean.replace(Pattern.compile("\\b${Pattern.quote(accountName)}\\b", Pattern.CASE_INSENSITIVE).toRegex(), " ")
            val words = accountName.split(" ", "—", "-").map { it.trim() }.filter { it.length > 2 }
            for (w in words) {
                clean = clean.replace(Pattern.compile("\\b${Pattern.quote(w)}\\b", Pattern.CASE_INSENSITIVE).toRegex(), " ")
            }
        }
        // Remove keywords
        clean = clean.replace(Pattern.compile("\\b(add|reduce|online|offline|cash|hdfc|bank|rupees|rupiya|for|from|received|paid|to|kharch|kharcha|jama|in)\\b", Pattern.CASE_INSENSITIVE).toRegex(), " ")
        clean = clean.replace("\\s+".toRegex(), " ").trim()
        return clean.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }

    private fun extractCategory(text: String): String? {
        val lower = text.lowercase()
        return when {
            lower.contains("food") || lower.contains("dabeli") || lower.contains("dinner") ||
                    lower.contains("lunch") || lower.contains("breakfast") || lower.contains("snack") ||
                    lower.contains("chai") || lower.contains("coffee") -> "Food"

            lower.contains("travel") || lower.contains("fuel") || lower.contains("petrol") ||
                    lower.contains("cab") || lower.contains("auto") || lower.contains("uber") -> "Travel"

            lower.contains("shopping") || lower.contains("cloth") || lower.contains("shirt") ||
                    lower.contains("shoes") -> "Shopping"

            lower.contains("bill") || lower.contains("electricity") || lower.contains("recharge") -> "Bills"
            lower.contains("salary") -> "Salary"
            lower.contains("project") || lower.contains("freelance") || lower.contains("client") -> "Project Payment"
            else -> null
        }
    }

    private fun checkTransferQuery(text: String): VoiceCommandResult.TransferReady? {
        val lower = text.lowercase()
        if (!lower.contains("transfer")) return null

        val amount = extractAmount(lower) ?: return null
        // "transfer 2000 from HDFC to Savings"
        val regex = Pattern.compile("transfer\\s+(?:.*?)\\s*from\\s+([a-zA-Z0-9\\s—]+?)\\s+to\\s+([a-zA-Z0-9\\s—]+)", Pattern.CASE_INSENSITIVE)
        val matcher = regex.matcher(lower)
        if (matcher.find()) {
            val from = matcher.group(1)?.trim() ?: "Online — HDFC"
            val to = matcher.group(2)?.trim() ?: "Savings"
            return VoiceCommandResult.TransferReady(
                amountPaise = amount,
                fromAccountName = from,
                toAccountName = to
            )
        }
        return null
    }

    private fun checkLendingBorrowing(text: String): VoiceCommandResult.LoanReady? {
        val lower = text.lowercase()
        val isLent = lower.contains("lent") || lower.contains("lend") || lower.contains("gave") || lower.contains("didha")
        val isBorrowed = lower.contains("borrowed") || lower.contains("borrow") || lower.contains("lidha")

        if (!isLent && !isBorrowed) return null
        val amount = extractAmount(lower) ?: return null
        val isOnline = extractAccountMode(lower) ?: true // Default Online

        val personPattern = if (isLent) {
            Pattern.compile("(?:to|didha)\\s+([a-zA-Z]+)", Pattern.CASE_INSENSITIVE)
        } else {
            Pattern.compile("(?:from|lidha)\\s+([a-zA-Z]+)", Pattern.CASE_INSENSITIVE)
        }
        val matcher = personPattern.matcher(lower)
        val personName = if (matcher.find()) matcher.group(1)?.capitalize(Locale.ROOT) ?: "Contact" else "Contact"

        return VoiceCommandResult.LoanReady(
            loanType = if (isLent) LoanType.LENT else LoanType.BORROWED,
            amountPaise = amount,
            personName = personName,
            isOnline = isOnline
        )
    }

    private fun checkAnalysisQuery(text: String): VoiceCommandResult.AnalysisQuery? {
        val lower = text.lowercase()
        return when {
            lower.contains("spend") || lower.contains("expense") && lower.contains("month") ->
                VoiceCommandResult.AnalysisQuery(VoiceCommandResult.AnalysisQuery.QueryType.TOTAL_EXPENSE_MONTH)

            lower.contains("earn") || lower.contains("income") && lower.contains("month") ->
                VoiceCommandResult.AnalysisQuery(VoiceCommandResult.AnalysisQuery.QueryType.TOTAL_INCOME_MONTH)

            lower.contains("hdfc") && (lower.contains("how much") || lower.contains("balance")) ->
                VoiceCommandResult.AnalysisQuery(VoiceCommandResult.AnalysisQuery.QueryType.ACCOUNT_BALANCE, "Online — HDFC")

            lower.contains("cash") && (lower.contains("how much") || lower.contains("balance")) ->
                VoiceCommandResult.AnalysisQuery(VoiceCommandResult.AnalysisQuery.QueryType.ACCOUNT_BALANCE, "Cash")

            lower.contains("total money") || lower.contains("total balance") || lower.contains("how much money do i have") ->
                VoiceCommandResult.AnalysisQuery(VoiceCommandResult.AnalysisQuery.QueryType.TOTAL_AVAILABLE_MONEY)

            lower.contains("highest") && (lower.contains("expense") || lower.contains("category")) ->
                VoiceCommandResult.AnalysisQuery(VoiceCommandResult.AnalysisQuery.QueryType.HIGHEST_EXPENSE_CATEGORY)

            else -> null
        }
    }
}
