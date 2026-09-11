package com.example

import com.example.data.repository.OpenHandsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class TimestampParserTest {

    private fun utcMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.clear()
        cal.set(year, month - 1, day, hour, minute, second)
        return cal.timeInMillis
    }

    @Test
    fun `parses cloud style Zulu timestamps`() {
        // 2025-01-15T10:30:00Z
        assertEquals(utcMillis(2025, 1, 15, 10, 30, 0), OpenHandsRepository.parseTimestamp("2025-01-15T10:30:00Z"))
    }

    @Test
    fun `parses fractional seconds`() {
        assertEquals(
            utcMillis(2025, 1, 15, 10, 30, 0) + 123,
            OpenHandsRepository.parseTimestamp("2025-01-15T10:30:00.123Z")
        )
    }

    @Test
    fun `parses numeric offsets`() {
        // 10:30+02:00 == 08:30Z
        assertEquals(utcMillis(2025, 1, 15, 8, 30, 0), OpenHandsRepository.parseTimestamp("2025-01-15T10:30:00+02:00"))
        // 08:30-01:00 == 09:30Z
        assertEquals(utcMillis(2025, 1, 15, 9, 30, 0), OpenHandsRepository.parseTimestamp("2025-01-15T08:30:00-01:00"))
    }

    @Test
    fun `date-only value maps to midnight utc`() {
        assertEquals(utcMillis(2025, 1, 15, 0, 0, 0), OpenHandsRepository.parseTimestamp("2025-01-15"))
    }

    @Test
    fun `garbage and blank inputs return null`() {
        assertNull(OpenHandsRepository.parseTimestamp(null))
        assertNull(OpenHandsRepository.parseTimestamp(""))
        assertNull(OpenHandsRepository.parseTimestamp("not-a-date"))
        assertNull(OpenHandsRepository.parseTimestamp("2025-13-99T99:99:99Z"))
    }
}
