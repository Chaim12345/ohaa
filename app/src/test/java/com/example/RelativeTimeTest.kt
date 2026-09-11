package com.example

import com.example.util.relativeTime
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class RelativeTimeTest {

    private val now = 1_000_000_000_000L // 2001-09-09T01:46:40Z

    @Test
    fun `recent timestamps render as just now`() {
        assertEquals("just now", relativeTime(now, now))
        assertEquals("just now", relativeTime(now - 59_000L, now))
        assertEquals("just now", relativeTime(now + 30_000L, now)) // clock skew
    }

    @Test
    fun `minutes hours and days render compactly`() {
        assertEquals("5m ago", relativeTime(now - 5 * 60_000L, now))
        assertEquals("1h ago", relativeTime(now - 90 * 60_000L, now))
        assertEquals("3h ago", relativeTime(now - 3 * 3_600_000L, now))
        assertEquals("2d ago", relativeTime(now - 2 * 86_400_000L, now))
    }

    @Test
    fun `beyond a week falls back to month and day`() {
        // 2001-09-01T00:00:00Z → "Sep 1"
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2001, Calendar.SEPTEMBER, 1, 0, 0, 0)
        }
        assertEquals("Sep 1", relativeTime(cal.timeInMillis, cal.timeInMillis + 8L * 86_400_000L))
    }

    @Test
    fun `zero timestamps render empty`() {
        assertEquals("", relativeTime(0L, now))
        assertEquals("", relativeTime(-1L, now))
    }
}