package com.mymoney.manager.data.model

enum class TransactionType(val displayName: String) {
    INCOME("Income"),
    EXPENSE("Expense"),
    TRANSFER("Transfer"),
    LEND("Money Lent"),
    BORROW("Money Borrowed"),
    REPAYMENT_RECEIVED("Repayment Received"),
    REPAYMENT_PAID("Repayment Paid")
}

enum class DraftStatus {
    PENDING,
    APPROVED,
    REJECTED,
    DELETED
}

enum class DetectionSource(val displayName: String) {
    MANUAL("Manual"),
    SMS("SMS"),
    VOICE("Voice")
}

enum class AccountType(val displayName: String) {
    PRIMARY("Primary Account"),
    CUSTOM_FUND("Custom Fund")
}

enum class LoanType(val displayName: String) {
    LENT("Money Lent (Receivable)"),
    BORROWED("Money Borrowed (Liability)")
}

enum class LoanStatus {
    ACTIVE,
    SETTLED
}
