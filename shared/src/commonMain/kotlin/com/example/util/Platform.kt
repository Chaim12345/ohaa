package com.example.util

/**
 * Platform helpers the shared UI needs that have no common Kotlin equivalent.
 *
 * These are the `expect` side of the KMP split; each target supplies its own
 * `actual` so the shared screens never touch `java.*` or `android.*`.
 */

/**
 * Current wall-clock time in epoch millis.
 *
 * Android uses `System.currentTimeMillis()`; the web target uses the browser's
 * `Date.now()`. Callers should prefer passing `nowMillis` explicitly in tests so
 * formatting stays deterministic.
 */
expect fun currentTimeMillis(): Long

/**
 * Formats a Double with a fixed number of decimal places.
 *
 * `String.format` is JVM-only, so this small dependency-free implementation is
 * shared by every target to keep cost and credit figures identical.
 */
fun Double.formatDecimals(places: Int): String {
    if (places <= 0) return this.toLong().toString()
    val negative = this < 0
    val scaled = kotlin.math.abs(this) * POW10[places]
    val rounded = kotlin.math.round(scaled).toLong()
    val whole = rounded / POW10[places]
    val frac = rounded % POW10[places]
    return (if (negative) "-" else "") + whole + "." + frac.toString().padStart(places, '0')
}

private val POW10 = longArrayOf(1, 10, 100, 1_000, 10_000, 100_000, 1_000_000)
