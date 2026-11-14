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
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 30 -> "${days}d ago"
        else -> "${days / 30}mo ago"
    }
}
