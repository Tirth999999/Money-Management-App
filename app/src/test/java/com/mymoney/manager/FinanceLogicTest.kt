package com.mymoney.manager

import com.mymoney.manager.data.model.CurrencyUtils
import com.mymoney.manager.data.model.TransactionType
import com.mymoney.manager.parser.HdfcSmsParser
import com.mymoney.manager.parser.VoiceCommandParser
import com.mymoney.manager.parser.VoiceCommandResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FinanceLogicTest {

    // ----------------------------------------------------
    // SMS PARSER TESTS (Sample 1, Sample 2, Sample 3)
    // ----------------------------------------------------

    @Test
    fun testHdfcSample1_Debit() {
        val sms = """
            Sent Rs.110.00
            From HDFC Bank A/C *1870
            To KHODIYARKRUPA DABELI
            On 31/10/26
            Ref 630471303831
            Not You?
            Call 18002586161/SMS BLOCK UPI to 7308080808
        """.trimIndent()

        val parsed = HdfcSmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(TransactionType.EXPENSE, parsed!!.type)
        assertEquals(11000L, parsed.amountPaise) // ₹110.00 = 11000 paise
        assertEquals("KHODIYARKRUPA DABELI", parsed.description)
        assertEquals("630471303831", parsed.referenceId)
        assertEquals("1870", parsed.accountSuffix)
        assertEquals("Food", parsed.suggestedCategoryName)

        val dateStr = SimpleDateFormat("dd/MM/yy", Locale.ENGLISH).format(Date(parsed.dateMillis))
        assertEquals("31/10/26", dateStr)
    }

    @Test
    fun testHdfcSample2_Debit() {
        val sms = """
            Sent Rs.20.00
            From HDFC Bank A/C *1870
            To Jitendra Patel
            On 31/10/26
            Ref 630471352716
            Not You?
            Call 18002586161/SMS BLOCK UPI to 7308080808
        """.trimIndent()

        val parsed = HdfcSmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(TransactionType.EXPENSE, parsed!!.type)
        assertEquals(2000L, parsed.amountPaise) // ₹20.00 = 2000 paise
        assertEquals("Jitendra Patel", parsed.description)
        assertEquals("630471352716", parsed.referenceId)
        assertEquals("1870", parsed.accountSuffix)

        val dateStr = SimpleDateFormat("dd/MM/yy", Locale.ENGLISH).format(Date(parsed.dateMillis))
        assertEquals("31/10/26", dateStr)
    }

    @Test
    fun testHdfcSample3_Credit() {
        val sms = """
            Credit Alert!
            Rs.15000.00 credited to HDFC Bank A/c XX1870 on 30-10-26 from VPA *ra35@oksbi (UPI 630300548508)
        """.trimIndent()

        val parsed = HdfcSmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(TransactionType.INCOME, parsed!!.type)
        assertEquals(1500000L, parsed.amountPaise) // ₹15,000.00 = 1,500,000 paise
        assertEquals("*ra35@oksbi", parsed.description)
        assertEquals("630300548508", parsed.referenceId)
        assertEquals("1870", parsed.accountSuffix)

        val dateStr = SimpleDateFormat("dd/MM/yy", Locale.ENGLISH).format(Date(parsed.dateMillis))
        assertEquals("30/10/26", dateStr)
    }

    @Test
    fun testIgnoreOtpAndPromotions() {
        val otpSms = "Your HDFC Bank OTP is 849201 for transaction of Rs.500. Do not share OTP."
        assertNull(HdfcSmsParser.parse(otpSms))

        val promoSms = "Congratulations! Get flat 20% off on your HDFC credit card at shopping stores."
        assertNull(HdfcSmsParser.parse(promoSms))
    }

    // ----------------------------------------------------
    // VOICE COMMAND PARSER TESTS
    // ----------------------------------------------------

    @Test
    fun testVoiceAdd100_NeedsClarification() {
        // "Jarvis, add 100 rupees" -> Amount and Type present, but Mode (Online vs Offline) missing
        val result = VoiceCommandParser.parse("Jarvis, add 100 rupees")
        assertTrue(result is VoiceCommandResult.NeedsClarification)
        val clarification = result as VoiceCommandResult.NeedsClarification
        assertEquals(VoiceCommandResult.NeedsClarification.MissingField.ACCOUNT_MODE, clarification.missingField)
        assertEquals(10000L, clarification.partialAmountPaise)
        assertEquals(TransactionType.INCOME, clarification.partialType)
    }

    @Test
    fun testVoiceReduce100Offline_Ready() {
        val result = VoiceCommandParser.parse("Jarvis, reduce 100 rupees offline for food")
        assertTrue(result is VoiceCommandResult.DraftReady)
        val draft = result as VoiceCommandResult.DraftReady
        assertEquals(TransactionType.EXPENSE, draft.type)
        assertEquals(10000L, draft.amountPaise)
        assertEquals(false, draft.isOnline) // Offline = Cash
        assertEquals("Food", draft.suggestedCategory)
    }

    @Test
    fun testVoiceAdd500OnlineFromVishal_Ready() {
        val result = VoiceCommandParser.parse("Jarvis, add 500 rupees online received from Vishal for project payment")
        assertTrue(result is VoiceCommandResult.DraftReady)
        val draft = result as VoiceCommandResult.DraftReady
        assertEquals(TransactionType.INCOME, draft.type)
        assertEquals(50000L, draft.amountPaise)
        assertEquals(true, draft.isOnline) // Online = HDFC
    }

    @Test
    fun testVoiceGujaratiPachasRupiya() {
        // "pachas rupiya" (Gujarati 50 rupees)
        val result = VoiceCommandParser.parse("pachas rupiya")
        assertTrue(result is VoiceCommandResult.NeedsClarification)
        val clarification = result as VoiceCommandResult.NeedsClarification
        assertEquals(5000L, clarification.partialAmountPaise) // 50 * 100 paise = 5000 paise
    }

    @Test
    fun testVoiceTransferCommand() {
        val result = VoiceCommandParser.parse("Jarvis, transfer 2000 rupees from HDFC to Savings")
        assertTrue(result is VoiceCommandResult.TransferReady)
        val transfer = result as VoiceCommandResult.TransferReady
        assertEquals(200000L, transfer.amountPaise)
    }

    // ----------------------------------------------------
    // FINANCIAL BALANCE RECONCILIATION MATH
    // ----------------------------------------------------

    @Test
    fun testPaisePrecisionAndFormatting() {
        val paise = 1500000L
        assertEquals("₹15,000.00", CurrencyUtils.formatPaise(paise))

        val parsed = CurrencyUtils.parseRupeesToPaise("110.00")
        assertEquals(11000L, parsed)

        val smallAmount = CurrencyUtils.parseRupeesToPaise("20.00")
        assertEquals(2000L, smallAmount)
    }
}
