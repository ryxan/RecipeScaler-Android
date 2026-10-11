package com.ryxan.recipescaler.core.domain.parsing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [QuantityParser] — decimals, fractions, mixed numbers, Unicode
 * glyphs, thousands separators, decimal commas, and ranges.
 */
class QuantityParserTest {

    // ── Basic forms ───────────────────────────────────────────────────────────

    @Test fun `decimal`() = assertEquals(1.5, QuantityParser.parse("1.5")!!, 0.0001)
    @Test fun `simple fraction`() = assertEquals(0.5, QuantityParser.parse("1/2")!!, 0.0001)
    @Test fun `mixed with space`() = assertEquals(1.5, QuantityParser.parse("1 1/2")!!, 0.0001)

    @Test fun `mixed with hyphen is not a range`() {
        assertEquals(1.5, QuantityParser.parse("1-1/2")!!, 0.0001)
        assertFalse(QuantityParser.isRange("1-1/2"))
    }

    // ── Unicode fractions ─────────────────────────────────────────────────────

    @Test fun `unicode bare half`() = assertEquals(0.5, QuantityParser.parse("½")!!, 0.0001)
    @Test fun `unicode attached half`() = assertEquals(1.5, QuantityParser.parse("1½")!!, 0.0001)
    @Test fun `unicode spaced half`() = assertEquals(1.5, QuantityParser.parse("1 ½")!!, 0.0001)

    // ── Commas ────────────────────────────────────────────────────────────────

    @Test fun `thousands separator`() = assertEquals(1000.0, QuantityParser.parse("1,000")!!, 0.0001)
    @Test fun `thousands with decimals`() = assertEquals(12345.5, QuantityParser.parse("12,345.5")!!, 0.0001)
    @Test fun `decimal comma`() = assertEquals(1.5, QuantityParser.parse("1,5")!!, 0.0001)

    // ── Ranges ────────────────────────────────────────────────────────────────

    @Test fun `dash range`() {
        assertEquals(2.0, QuantityParser.parse("2-3")!!, 0.0001)
        assertTrue(QuantityParser.isRange("2-3"))
    }

    @Test fun `spaced dash range`() {
        assertEquals(2.0, QuantityParser.parse("2 - 3")!!, 0.0001)
        assertTrue(QuantityParser.isRange("2 - 3"))
    }

    @Test fun `en dash range`() {
        assertEquals(2.0, QuantityParser.parse("2–3")!!, 0.0001)
        assertTrue(QuantityParser.isRange("2–3"))
    }

    @Test fun `to range`() {
        assertEquals(2.0, QuantityParser.parse("2 to 3")!!, 0.0001)
        assertTrue(QuantityParser.isRange("2 to 3"))
    }

    // ── Failures and edge cases ───────────────────────────────────────────────

    @Test fun `unparseable words are null`() = assertNull(QuantityParser.parse("a few"))
    @Test fun `blank is null`() = assertNull(QuantityParser.parse(""))
    @Test fun `zero parses as zero`() = assertEquals(0.0, QuantityParser.parse("0")!!, 0.0001)
    @Test fun `zero denominator is null`() = assertNull(QuantityParser.parse("1/0"))
    @Test fun `non-range is not flagged`() = assertFalse(QuantityParser.isRange("1.5"))

    @Test fun `NaN is null`() = assertNull(QuantityParser.parse("NaN"))
    @Test fun `Infinity is null`() = assertNull(QuantityParser.parse("Infinity"))
    @Test fun `exponent is null`() = assertNull(QuantityParser.parse("1e3"))
    @Test fun `negative is null`() = assertNull(QuantityParser.parse("-2"))
    @Test fun `leading dot decimal`() = assertEquals(0.5, QuantityParser.parse(".5")!!, 0.0001)
    @Test fun `trailing dot decimal`() = assertEquals(5.0, QuantityParser.parse("5.")!!, 0.0001)
}
