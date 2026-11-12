package com.mymoney.manager.data.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyUtils {

    private val indianFormat: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("en", "IN"))
        symbols.currencySymbol = "₹"
        DecimalFormat("₹##,##,##0.00", symbols)
    }

    private val indianFormatNoDecimals: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("en", "IN"))
        symbols.currencySymbol = "₹"
        DecimalFormat("₹##,##,##0", symbols)
    }

    /**
     * Converts paise (Long) to formatted INR string, e.g. 1500000L -> "₹15,000.00"
     */
    fun formatPaise(paise: Long, includeDecimals: Boolean = true): String {
        val rupees = paise / 100.0
        return if (includeDecimals) {
            indianFormat.format(rupees)
        } else {
            indianFormatNoDecimals.format(rupees)
        }
    }

    fun paiseToRupees(paise: Long): Double {
        return paise / 100.0
    }

    /**
     * Converts a rupee string or double representation (e.g. "110.00" or 110.0) to paise Long (11000L)
     */
    fun rupeesToPaise(rupees: Double): Long {
        return Math.round(rupees * 100.0)
    }

    fun parseRupeesToPaise(rupeeString: String): Long {
        val cleaned = rupeeString.replace("₹", "")
            .replace(",", "")
            .replace("Rs.", "", ignoreCase = true)
            .replace("Rs", "", ignoreCase = true)
            .trim()
        val parsed = cleaned.toDoubleOrNull() ?: 0.0
        return rupeesToPaise(parsed)
    }
}

fun Long.toRupeesDisplay(includeDecimals: Boolean = true): String {
    return CurrencyUtils.formatPaise(this, includeDecimals)
}
