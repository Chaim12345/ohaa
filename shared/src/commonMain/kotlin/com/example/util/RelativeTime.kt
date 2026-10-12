package com.example.util


/**
 * Formats an epoch-millis timestamp as a short relative label, the way mobile
 * chat and coding-agent apps surface session recency: "just now", "5m ago",
 * "3h ago", "2d ago", then "Jan 15" beyond a week.
 *
 * Pure Kotlin with no `java.util.Calendar`, so it compiles for wasm as well
 * as Android. Future timestamps (clock skew) render as "just now".
 */
fun relativeTime(timestampMillis: Long, nowMillis: Long = currentTimeMillis()): String {
    if (timestampMillis <= 0L) return ""
    val diff = nowMillis - timestampMillis
    return when {
        diff < 60_000L -> "just now"
        diff < 3_600_000L -> "${diff / 60_000L}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000L}h ago"
        diff < 7L * 86_400_000L -> "${diff / 86_400_000L}d ago"
        else -> {
            val (_, month, day) = civilFromDays(floorDiv(timestampMillis, 86_400_000L))
            "${MONTHS[month - 1]} $day"
        }
    }
}

/** Floor division that works on every target (`Math.floorDiv` is JVM-only). */
private fun floorDiv(a: Long, b: Long): Long {
    val q = a / b
    return if (a % b != 0L && (a xor b) < 0) q - 1 else q
}

/** Inverse of days_from_civil (Howard Hinnant), replacing `java.util.Calendar`. */
private fun civilFromDays(days: Long): Triple<Int, Int, Int> {
    val z = days + 719_468
    val era = (if (z >= 0) z else z - 146_096) / 146_097
    val doe = z - era * 146_097
    val yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
    val y = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = if (mp < 10) mp + 3 else mp - 9
    return Triple((if (m <= 2) y + 1 else y).toInt(), m.toInt(), d.toInt())
}

private val MONTHS = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)