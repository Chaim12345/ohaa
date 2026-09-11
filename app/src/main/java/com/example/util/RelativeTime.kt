package com.example.util

import java.util.Calendar

/**
 * Formats an epoch-millis timestamp as a short relative label, the way mobile
 * chat and coding-agent apps surface session recency: "just now", "5m ago",
 * "3h ago", "2d ago", then "Jan 15" beyond a week.
 *
 * Pure function using only `Calendar` so minSdk 24 needs no core-library
 * desugaring. Future timestamps (clock skew) render as "just now".
 */
fun relativeTime(timestampMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    if (timestampMillis <= 0L) return ""
    val diff = nowMillis - timestampMillis
    return when {
        diff < 60_000L -> "just now"
        diff < 3_600_000L -> "${diff / 60_000L}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000L}h ago"
        diff < 7L * 86_400_000L -> "${diff / 86_400_000L}d ago"
        else -> {
            val cal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
            val month = MONTHS[cal.get(Calendar.MONTH)]
            "$month ${cal.get(Calendar.DAY_OF_MONTH)}"
        }
    }
}

private val MONTHS = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)