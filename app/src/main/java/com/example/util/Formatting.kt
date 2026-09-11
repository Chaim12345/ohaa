package com.example.util

import java.util.Locale

/**
 * Formats a USD amount with an explicit locale so the decimal separator never
 * flips to a comma on non-English devices (the default-locale `"%.2f".format`
 * used to render "$1,23" in much of Europe).
 *
 * Amounts below one cent keep extra precision, which is where agent run costs
 * usually sit before they add up.
 */
fun formatUsd(amount: Double, withSymbol: Boolean = true): String {
    val safe = if (amount.isFinite()) amount else 0.0
    val magnitude = if (safe < 0) -safe else safe
    val text = when {
        magnitude == 0.0 -> "0.00"
        magnitude < 0.01 -> String.format(Locale.US, "%.4f", safe)
        else -> String.format(Locale.US, "%.2f", safe)
    }
    return if (withSymbol) "\$" + text else text
}
