package com.mymoney.manager.parser

import com.mymoney.manager.data.model.CurrencyUtils
import com.mymoney.manager.data.model.DetectionSource
import com.mymoney.manager.data.model.DraftStatus
import com.mymoney.manager.data.model.TransactionType
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

data class ParsedSmsDraft(
    val type: TransactionType,
    val amountPaise: Long,
    val description: String,
    val dateMillis: Long,
    val referenceId: String?,
    val suggestedCategoryName: String?,
    val accountSuffix: String?,
    val confidence: Float
)

object HdfcSmsParser {

    // Filter out OTPs, promotions, logins
    private val IGNORE_PATTERNS = listOf(
        Pattern.compile("\\b(otp|one time password|verification code|secret code)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(congratulations|flat \\d+% off|cashback offer|pre-approved|apply now)\\b", Pattern.CASE_INSENSITIVE)
    )

    // Patterns for Amount: "Sent Rs.110.00", "Rs.15000.00 credited", "debited by Rs 500", "INR 250.00"
    private val AMOUNT_PATTERN = Pattern.compile(
        "(?:(?:Sent|debited|spent|paid|transferred)\\s+(?:by\\s+)?)?(?:Rs\\.?|INR)\\s*([0-9]+(?:\\.[0-9]{1,2})?)",
        Pattern.CASE_INSENSITIVE
    )

    // Fallback amount pattern: "Rs.15000.00 credited"
    private val AMOUNT_CREDITED_PATTERN = Pattern.compile(
        "(?:Rs\\.?|INR)\\s*([0-9]+(?:\\.[0-9]{1,2})?)\\s*(?:credited|received|deposited)",
        Pattern.CASE_INSENSITIVE
    )

    // Patterns for HDFC Account Number/Suffix: "HDFC Bank A/C *1870", "A/c XX1870", "A/C ending 1870"
    private val ACCOUNT_PATTERN = Pattern.compile(
        "(?:HDFC Bank|HDFC)?\\s*A/[Cc]\\s*(?:(?:XX|\\*|ending\\s*)?([0-9]{4}))",
        Pattern.CASE_INSENSITIVE
    )

    // Pattern for Merchant / Recipient: "To KHODIYARKRUPA DABELI", "from VPA *ra35@oksbi"
    private val RECIPIENT_TO_PATTERN = Pattern.compile(
        "\\bTo\\s+([^\\n\\r]+?)(?=\\s+On|\\s+Ref|\\s+Not|\\s*$)",
        Pattern.CASE_INSENSITIVE
    )
    private val SENDER_FROM_PATTERN = Pattern.compile(
        "\\bfrom\\s+(?:VPA\\s+)?([^\\n\\r(]+?)(?=\\s*\\(UPI|\\s+on|\\s+Ref|\\s*$)",
        Pattern.CASE_INSENSITIVE
    )

    // Pattern for Reference / UPI Ref: "Ref 630471303831", "(UPI 630300548508)"
    private val REF_PATTERN = Pattern.compile(
        "(?:Ref(?:erence)?(?:\\s+No\\.?|\\s+ID)?|UPI)\\s*:?\\s*([0-9]{8,18})",
        Pattern.CASE_INSENSITIVE
    )

    // Patterns for Date: "On 31/10/26", "on 30-10-26", "on 31/10/2026"
    private val DATE_PATTERN = Pattern.compile(
        "\\bOn\\s+([0-9]{1,2}[-/][0-9]{1,2}[-/][0-9]{2,4})\\b",
        Pattern.CASE_INSENSITIVE
    )

    fun parse(smsBody: String, smsTimestamp: Long = System.currentTimeMillis()): ParsedSmsDraft? {
        val trimmed = smsBody.trim()
        if (trimmed.isEmpty()) return null

        // Check if message is non-financial or OTP/promo
        for (pattern in IGNORE_PATTERNS) {
            if (pattern.matcher(trimmed).find()) {
                return null
            }
        }

        // Must be related to banking/HDFC
        val lower = trimmed.lowercase(Locale.ROOT)
        val isHdfc = lower.contains("hdfc")
        val isFinancial = lower.contains("sent") || lower.contains("credited") ||
                lower.contains("debited") || lower.contains("rs.") || lower.contains("inr")
        if (!isFinancial) return null

        // Determine transaction type (Income vs Expense)
        val isDebit = lower.contains("sent") || lower.contains("debited") || lower.contains("spent") || lower.contains("paid")
        val isCredit = lower.contains("credit") || lower.contains("credited") || lower.contains("received") || lower.contains("deposited")

        if (!isDebit && !isCredit) return null
        val type = if (isCredit && !isDebit) TransactionType.INCOME else TransactionType.EXPENSE

        // Extract Amount
        var amountPaise = 0L
        val amountMatcher = AMOUNT_PATTERN.matcher(trimmed)
        if (amountMatcher.find()) {
            val amountStr = amountMatcher.group(1)
            if (amountStr != null) {
                amountPaise = CurrencyUtils.parseRupeesToPaise(amountStr)
            }
        }
        if (amountPaise == 0L) {
            val creditedMatcher = AMOUNT_CREDITED_PATTERN.matcher(trimmed)
            if (creditedMatcher.find()) {
                val amountStr = creditedMatcher.group(1)
                if (amountStr != null) {
                    amountPaise = CurrencyUtils.parseRupeesToPaise(amountStr)
                }
            }
        }
        if (amountPaise <= 0L) return null

        // Extract Account Suffix
        var accountSuffix: String? = null
        val accMatcher = ACCOUNT_PATTERN.matcher(trimmed)
        if (accMatcher.find()) {
            accountSuffix = accMatcher.group(1)
        }

        // Extract Description / Beneficiary
        var description = ""
        if (type == TransactionType.EXPENSE) {
            val toMatcher = RECIPIENT_TO_PATTERN.matcher(trimmed)
            if (toMatcher.find()) {
                description = toMatcher.group(1)?.trim() ?: ""
            }
        } else {
            val fromMatcher = SENDER_FROM_PATTERN.matcher(trimmed)
            if (fromMatcher.find()) {
                description = fromMatcher.group(1)?.trim() ?: ""
            }
        }
        if (description.isEmpty()) {
            description = if (type == TransactionType.EXPENSE) "HDFC Bank Payment" else "HDFC Bank Credit"
        }

        // Extract Date
        var txDateMillis = smsTimestamp
        val dateMatcher = DATE_PATTERN.matcher(trimmed)
        if (dateMatcher.find()) {
            val dateStr = dateMatcher.group(1)
            if (dateStr != null) {
                parseDateString(dateStr)?.let {
                    txDateMillis = it
                }
            }
        }

        // Extract Reference ID
        var referenceId: String? = null
        val refMatcher = REF_PATTERN.matcher(trimmed)
        if (refMatcher.find()) {
            referenceId = refMatcher.group(1)
        }
        if (referenceId.isNullOrBlank()) {
            // Generate deterministic fingerprint hash
            referenceId = generateFingerprint(amountPaise, txDateMillis, description)
        }

        // Suggest Category based on description
        val suggestedCat = suggestCategory(description)

        return ParsedSmsDraft(
            type = type,
            amountPaise = amountPaise,
            description = description,
            dateMillis = txDateMillis,
            referenceId = referenceId,
            suggestedCategoryName = suggestedCat,
            accountSuffix = accountSuffix,
            confidence = if (isHdfc && referenceId != null) 0.98f else 0.85f
        )
    }

    private fun parseDateString(dateStr: String): Long? {
        val sanitized = dateStr.replace('/', '-')
        val formats = listOf(
            "dd-MM-yy",
            "dd-MM-yyyy",
            "d-M-yy",
            "d-M-yyyy"
        )
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.ENGLISH)
                sdf.isLenient = false
                val date = sdf.parse(sanitized)
                if (date != null) {
                    val cal = Calendar.getInstance()
                    val currentYear = cal.get(Calendar.YEAR)
                    cal.time = date
                    // If year parsed as e.g. 2026, keep it
                    if (cal.get(Calendar.YEAR) < 2000) {
                        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 2000)
                    }
                    return cal.timeInMillis
                }
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun generateFingerprint(amount: Long, date: Long, desc: String): String {
        val raw = "HDFC-$amount-$date-${desc.lowercase().trim()}"
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(raw.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            "FP-${System.currentTimeMillis()}-${(1000..9999).random()}"
        }
    }

    private fun suggestCategory(desc: String): String {
        val lower = desc.lowercase()
        return when {
            lower.contains("dabeli") || lower.contains("food") || lower.contains("restaurant") ||
                    lower.contains("hotel") || lower.contains("swiggy") || lower.contains("zomato") ||
                    lower.contains("cafe") || lower.contains("tea") || lower.contains("coffee") ||
                    lower.contains("bakes") || lower.contains("sweet") || lower.contains("dhaba") -> "Food"

            lower.contains("mart") || lower.contains("store") || lower.contains("amazon") ||
                    lower.contains("flipkart") || lower.contains("myntra") || lower.contains("retail") ||
                    lower.contains("bazaar") || lower.contains("mall") || lower.contains("clothing") -> "Shopping"

            lower.contains("petrol") || lower.contains("fuel") || lower.contains("uber") ||
                    lower.contains("ola") || lower.contains("irctc") || lower.contains("rail") ||
                    lower.contains("metro") || lower.contains("bus") -> "Travel"

            lower.contains("airtel") || lower.contains("jio") || lower.contains("vodafone") ||
                    lower.contains("electricity") || lower.contains("bill") || lower.contains("recharge") ||
                    lower.contains("bescom") || lower.contains("torrent") || lower.contains("gas") -> "Bills"

            lower.contains("cinema") || lower.contains("pvr") || lower.contains("inox") ||
                    lower.contains("bookmyshow") || lower.contains("netflix") || lower.contains("prime") -> "Entertainment"

            lower.contains("pharmacy") || lower.contains("medical") || lower.contains("hospital") ||
                    lower.contains("dr") || lower.contains("clinic") || lower.contains("apollo") -> "Health"

            lower.contains("salary") || lower.contains("payroll") -> "Salary"
            lower.contains("project") || lower.contains("freelance") || lower.contains("client") -> "Project Payment"

            else -> "Other"
        }
    }
}
