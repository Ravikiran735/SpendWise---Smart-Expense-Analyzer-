package com.spendwise.app.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    fun getSymbol(currency: String = "INR"): String {
        return when (currency.uppercase()) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            else -> "₹"
        }
    }

    fun format(amount: Double, currency: String = "INR", includeDecimals: Boolean = true): String {
        val symbol = getSymbol(currency)
        val locale = when (currency.uppercase()) {
            "INR" -> Locale("en", "IN")
            "USD" -> Locale.US
            "EUR" -> Locale.GERMANY
            "GBP" -> Locale.UK
            else -> Locale("en", "IN")
        }

        val formatter = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = if (includeDecimals) 2 else 0
            maximumFractionDigits = if (includeDecimals) 2 else 0
        }

        return "$symbol${formatter.format(amount)}"
    }
}
