package com.example.ui

import java.util.concurrent.TimeUnit

/** Renders a timestamp as a short, human-friendly age such as "3h ago". */
fun formatRelativeTime(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
    if (epochMillis <= 0L) return "unknown"
    val delta = now - epochMillis
    if (delta < 0) return "just now"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
    val hours = TimeUnit.MILLISECONDS.toHours(delta)
    val days = TimeUnit.MILLISECONDS.toDays(delta)
    val months = days / 30
    val years = days / 365
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 30 -> "${days}d ago"
        // Months past 30d, but never round a value under a year up to "1y";
        // "24mo ago" for anything > 12 months is the bug this fixes.
        months < 12 -> "${months}mo ago"
        else -> "${years}y ago"
    }
}
