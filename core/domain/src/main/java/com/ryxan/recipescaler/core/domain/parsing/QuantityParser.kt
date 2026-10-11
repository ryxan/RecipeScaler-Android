package com.ryxan.recipescaler.core.domain.parsing

/**
 * Parses a quantity string into a [Double], or returns null when the input is
 * blank or unparseable.
 *
 * Supported forms:
 * - Decimal            : "1.5", "0.25", ".5"
 * - Fraction           : "1/2", "3/4"
 * - Mixed              : "1 1/2", "2-1/4"
 * - Unicode fractions  : ½ ⅓ ⅔ ¼ ¾ ⅕ ⅖ ⅗ ⅘ ⅙ ⅚ ⅛ ⅜ ⅝ ⅞, bare ("½") or
 *   attached ("1½") or spaced ("1 ½")
 * - Thousands separator: "1,000", "12,345.5"
 * - Decimal comma      : "1,5"
 * - Ranges             : "2-3", "2 - 3", "2–3", "2 to 3" — parse returns the
 *   LOW end and [isRange] reports true. "1-1/2" is a mixed number, not a range.
 */
object QuantityParser {

    // Thousands separator: "1,000", "12,345.5" (comma groups of exactly 3 digits).
    private val THOUSANDS_REGEX = Regex("""^\d{1,3}(,\d{3})+(\.\d+)?$""")
    // Decimal comma: "1,5", "12,34" (single comma followed by 1-2 digits).
    private val DECIMAL_COMMA_REGEX = Regex("""^\d+,\d{1,2}$""")
    // Mixed number: "1 1/2" or "2-1/4".
    private val MIXED_REGEX = Regex("""^(\d+)[\s-]+(\d+)/(\d+)$""")
    // Simple fraction: "3/4".
    private val FRACTION_REGEX = Regex("""^(\d+)/(\d+)$""")
    // Range with a dash (hyphen or en dash): "2-3", "2 - 3", "2–3".
    // The right side must be a plain number — "1-1/2" cannot match.
    private val DASH_RANGE_REGEX = Regex("""^(\d+(?:\.\d+)?)\s*[-–]\s*(\d+(?:\.\d+)?)$""")
    // Range with "to": "2 to 3".
    private val TO_RANGE_REGEX = Regex("""^(\d+(?:\.\d+)?)\s+to\s+(\d+(?:\.\d+)?)$""")

    private val UNICODE_FRACTIONS = mapOf(
        '½' to " 1/2", '⅓' to " 1/3", '⅔' to " 2/3", '¼' to " 1/4", '¾' to " 3/4",
        '⅕' to " 1/5", '⅖' to " 2/5", '⅗' to " 3/5", '⅘' to " 4/5", '⅙' to " 1/6",
        '⅚' to " 5/6", '⅛' to " 1/8", '⅜' to " 3/8", '⅝' to " 5/8", '⅞' to " 7/8"
    )

    /**
     * Parse [input] into a quantity, or null when blank/unparseable/zero-denominator.
     * Ranges return the low end ("2-3" → 2.0). "0" returns 0.0.
     */
    fun parse(input: String): Double? {
        if (input.isBlank()) return null
        val str = normalise(input)

        // Thousands separator — only the strict grouped form, so "1,234,567"
        // works but a stray comma never silently becomes a decimal point.
        if (THOUSANDS_REGEX.matches(str)) {
            return str.replace(",", "").toDoubleOrNull()
        }
        // Decimal comma.
        if (DECIMAL_COMMA_REGEX.matches(str)) {
            return str.replace(',', '.').toDoubleOrNull()
        }

        // Mixed number: "1 1/2" or "2-1/4" — checked before ranges so the
        // hyphen form is never mistaken for a range.
        MIXED_REGEX.find(str)?.let { m ->
            val whole = m.groupValues[1].toDouble()
            val num = m.groupValues[2].toDouble()
            val den = m.groupValues[3].toDouble()
            if (den != 0.0) return whole + num / den
        }

        // Simple fraction: "3/4".
        FRACTION_REGEX.find(str)?.let { m ->
            val num = m.groupValues[1].toDouble()
            val den = m.groupValues[2].toDouble()
            if (den != 0.0) return num / den
        }

        // Ranges: "2-3", "2–3", "2 to 3" → low end.
        DASH_RANGE_REGEX.find(str)?.let { m -> return m.groupValues[1].toDouble() }
        TO_RANGE_REGEX.find(str)?.let { m -> return m.groupValues[1].toDouble() }

        // Plain decimal — restricted so "NaN", "Infinity", "1e3", "-2", "+2"
        // can't slip through toDoubleOrNull.
        if (!str.matches(Regex("""^(\d+\.?\d*|\.\d+)$"""))) return null
        return str.toDoubleOrNull()
    }

    /** True when [input] is a range ("2-3", "2 – 3", "2 to 3"), false otherwise. */
    fun isRange(input: String): Boolean {
        if (input.isBlank()) return false
        val str = normalise(input)
        return DASH_RANGE_REGEX.matches(str) || TO_RANGE_REGEX.matches(str)
    }

    private fun normalise(input: String): String {
        var str = input.trim()
        UNICODE_FRACTIONS.forEach { (char, replacement) ->
            str = str.replace(char.toString(), replacement)
        }
        return str.trim()
    }
}
