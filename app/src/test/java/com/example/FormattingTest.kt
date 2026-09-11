package com.example

import com.example.util.formatUsd
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * `formatUsd` must not depend on the device locale: a French or German device
 * used to render "$1,23" for one dollar twenty-three with default-locale
 * `"%.2f".format(...)`.
 */
class FormattingTest {

    @Test
    fun `formats typical amounts with a dot separator`() {
        assertEquals("\$12.34", formatUsd(12.34))
        assertEquals("\$1234.50", formatUsd(1234.5))
        assertEquals("\$0.00", formatUsd(0.0))
    }

    @Test
    fun `keeps four decimals for sub-cent agent costs`() {
        assertEquals("\$0.0042", formatUsd(0.0042))
        assertEquals("\$0.0009", formatUsd(0.0009))
    }

    @Test
    fun `handles non-finite amounts without throwing`() {
        assertEquals("\$0.00", formatUsd(Double.NaN))
        assertEquals("\$0.00", formatUsd(Double.POSITIVE_INFINITY))
    }

    @Test
    fun `can render without the dollar symbol`() {
        assertEquals("2.50", formatUsd(2.5, withSymbol = false))
    }
}
