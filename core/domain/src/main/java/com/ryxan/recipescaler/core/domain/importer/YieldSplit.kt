package com.ryxan.recipescaler.core.domain.importer

import com.ryxan.recipescaler.core.domain.parsing.QuantityParser

/** A yield string split into its quantity text and optional trailing label. */
data class YieldSplit(val amountText: String, val label: String?)

private val YIELD_PREFIX = Regex("^(?:serves|makes|yields?|servings:)\\s+", RegexOption.IGNORE_CASE)

/**
 * Splits a servings/yield string as written on a recipe ("Serves 4",
 * "Makes 2 dozen", "12 cookies") into a quantity token and a label.
 *
 * The quantity token is the longest leading run (up to three words) that
 * [QuantityParser] accepts — so "½", "1 1/2" and "4-6" all work. Whatever
 * remains, trimmed, becomes the label ("servings", "dozen", "cookies"), or
 * null when nothing remains. Returns null when no quantity leads the string.
 */
fun splitYield(servings: String): YieldSplit? {
    val stripped = servings.trim().replaceFirst(YIELD_PREFIX, "")
    if (stripped.isEmpty()) return null
    val tokens = stripped.split(Regex("\\s+"))
    for (take in minOf(3, tokens.size) downTo 1) {
        val candidate = tokens.take(take).joinToString(" ")
        if (QuantityParser.parse(candidate) != null) {
            val label = tokens.drop(take).joinToString(" ").trim().ifBlank { null }
            return YieldSplit(amountText = candidate, label = label)
        }
    }
    return null
}
