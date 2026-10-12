package com.example.util

/**
 * Parses the ISO-8601 timestamps the OpenHands Cloud API returns
 * (e.g. `2026-09-10T05:52:05.094060Z`) into epoch milliseconds.
 *
 * Hand-rolled rather than using kotlinx-datetime so the shared module carries no
 * extra runtime dependency on the web target. Only the forms the API actually
 * emits are accepted; anything unparseable returns 0 so the UI degrades to
 * "unknown time" instead of throwing inside a list render.
 */
fun String?.toEpochMillis(): Long {
    val text = this?.trim().orEmpty()
    if (text.length < 19) return 0L

    fun num(start: Int, end: Int): Long =
        text.substring(start, end).toLongOrNull() ?: return 0L

    val year = num(0, 4)
    val month = num(5, 7)
    val day = num(8, 10)
    val hour = num(11, 13)
    val minute = num(14, 16)
    val second = num(17, 19)
    if (month !in 1..12 || day !in 1..31) return 0L

    // Fractional seconds are optional; take up to 3 digits for millis.
    var millis = 0L
    if (text.length > 19 && text[19] == '.') {
        val frac = text.substring(20).takeWhile { it.isDigit() }.take(3)
        millis = frac.padEnd(3, '0').toLongOrNull() ?: 0L
    }

    // Offset handling: 'Z' is UTC; ±HH:MM shifts the local reading to UTC.
    var offsetMinutes = 0L
    val zIndex = text.indexOf('Z')
    val plusIndex = text.indexOf('+', startIndex = 19)
    val minusIndex = text.indexOf('-', startIndex = 19)
    when {
        zIndex >= 0 -> Unit
        plusIndex >= 0 -> {
            offsetMinutes = num(plusIndex + 1, plusIndex + 3) * 60 + num(plusIndex + 4, plusIndex + 6)
        }
        minusIndex >= 0 -> {
            offsetMinutes = -(num(minusIndex + 1, minusIndex + 3) * 60 + num(minusIndex + 4, minusIndex + 6))
        }
    }

    val days = daysFromCivil(year, month, day)
    val seconds = days * 86_400L + hour * 3_600L + minute * 60L + second - offsetMinutes * 60L
    return seconds * 1_000L + millis
}

/** Howard Hinnant's days_from_civil: days since 1970-01-01 for a proleptic date. */
private fun daysFromCivil(year: Long, month: Long, day: Long): Long {
    val y = if (month <= 2) year - 1 else year
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = y - era * 400
    val doy = (153 * (if (month > 2) month - 3 else month + 9) + 2) / 5 + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146_097 + doe - 719_468
}
