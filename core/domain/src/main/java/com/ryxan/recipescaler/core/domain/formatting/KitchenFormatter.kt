package com.ryxan.recipescaler.core.domain.formatting

import com.ryxan.recipescaler.core.domain.model.KitchenUnit
import com.ryxan.recipescaler.core.domain.model.UnitConversions
import com.ryxan.recipescaler.core.domain.model.UnitFamily
import com.ryxan.recipescaler.core.domain.model.UnitSystem
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.floor

enum class MeasurementSystem { MATCH_RECIPE, US, METRIC }
enum class DisplayMode { KITCHEN, KEEP_UNITS, DECIMAL }

data class DisplayPrefs(
    val system: MeasurementSystem = MeasurementSystem.MATCH_RECIPE,
    val mode: DisplayMode = DisplayMode.KITCHEN,
)

/**
 * Formats a scaled quantity for display. See docs/plan.md §3.3.
 *
 * - KITCHEN (default): cup/Tbsp/tsp breakdown at ½-tsp precision with cup
 *   fractions, or per-unit rules for larger volumes, weights, and counts.
 * - KEEP_UNITS: same snapping but never changes the unit.
 * - DECIMAL: at most 2 decimals in the source unit.
 *
 * System conversion (KITCHEN only): when [DisplayPrefs.system] is US or METRIC
 * and differs from the unit's system, VOLUME and WEIGHT amounts convert first,
 * then render with the target system's rule. COUNT units never convert.
 */
object KitchenFormatter {

    fun format(amount: Double, unit: KitchenUnit, prefs: DisplayPrefs = DisplayPrefs()): String =
        formatInternal(amount, unit, unit.label, prefs)

    /** Unknown keys fall back to COUNT rules with the raw key as the label. */
    fun format(amount: Double, unitKey: String, prefs: DisplayPrefs = DisplayPrefs()): String {
        val unit = KitchenUnit.fromKey(unitKey)
        return formatInternal(amount, unit, unit?.label ?: unitKey.trim(), prefs)
    }

    // ── Dispatch ──────────────────────────────────────────────────────────────

    private fun formatInternal(
        amount: Double,
        unit: KitchenUnit?,
        label: String,
        prefs: DisplayPrefs,
    ): String {
        if (amount <= 0.0) {
            return if (unit == KitchenUnit.NONE || label.isBlank()) "0" else "0 $label"
        }
        return when (prefs.mode) {
            DisplayMode.DECIMAL -> stripDecimals(amount, 2) + suffix(unit, label)
            DisplayMode.KEEP_UNITS -> keepUnits(amount, unit, label)
            DisplayMode.KITCHEN -> kitchen(amount, unit, label, prefs)
        }
    }

    private fun suffix(unit: KitchenUnit?, label: String): String =
        if (unit == KitchenUnit.NONE || label.isBlank() || label == "-") "" else " $label"

    // ── KITCHEN mode ──────────────────────────────────────────────────────────

    private fun kitchen(
        amount: Double,
        unit: KitchenUnit?,
        label: String,
        prefs: DisplayPrefs,
    ): String {
        val family = unit?.family ?: UnitFamily.COUNT
        val system = unit?.system ?: UnitSystem.NONE

        // ── System conversion: explicit US/METRIC preference that differs from
        // the unit's own system converts VOLUME and WEIGHT; COUNT never does.
        if (family == UnitFamily.VOLUME && prefs.system != MeasurementSystem.MATCH_RECIPE &&
            system != UnitSystem.NONE && system.name != prefs.system.name
        ) {
            val ml = UnitConversions.toMl(amount, unit!!)
            return if (prefs.system == MeasurementSystem.US) {
                usVolume(ml!! / UnitConversions.ML_PER_TSP)
            } else {
                metricVolume(ml!!)
            }
        }
        if (family == UnitFamily.WEIGHT && prefs.system != MeasurementSystem.MATCH_RECIPE &&
            system != UnitSystem.NONE && system.name != prefs.system.name
        ) {
            val g = UnitConversions.toG(amount, unit!!)
            return if (prefs.system == MeasurementSystem.US) {
                usWeight(g!! / UnitConversions.G_PER_OZ)
            } else {
                metricWeight(g!!)
            }
        }

        return when (family) {
            UnitFamily.VOLUME -> when {
                system == UnitSystem.METRIC -> metricVolume(UnitConversions.toMl(amount, unit!!)!!)
                // Large US units stay in their own unit when the amount is a
                // natural measure; below 1 they drop to the tsp ladder.
                unit == KitchenUnit.PINT || unit == KitchenUnit.QUART || unit == KitchenUnit.GALLON ->
                    if (amount >= 1.0) renderQuarter(amount) + " ${unit.label}"
                    else usVolume(amount * UnitConversions.TSP_PER_UNIT[unit]!!)
                unit == KitchenUnit.FL_OZ ->
                    if (amount >= 1.0) renderHalf(amount) + " ${unit.label}"
                    else usVolume(amount * UnitConversions.TSP_PER_UNIT[unit]!!)
                else -> usVolume(amount * UnitConversions.TSP_PER_UNIT[unit]!!)
            }
            UnitFamily.WEIGHT ->
                if (system == UnitSystem.METRIC) {
                    metricWeight(UnitConversions.toG(amount, unit!!)!!)
                } else {
                    usWeight(UnitConversions.toOz(amount, unit!!)!!)
                }
            UnitFamily.COUNT -> countRule(amount, unit, label)
        }
    }

    // ── US volume: cup / Tbsp / tsp ladder ────────────────────────────────────
    //
    // Snap to half-teaspoons, then pick the rendering with the fewest parts
    // (then fewest scoops, then largest cup part). Tbsp-only renderings are
    // allowed only below ½ cup; with a cup part, Tbsp is capped at 3.

    private val CUP_FRACTION_TSP = doubleArrayOf(0.0, 12.0, 16.0, 24.0, 32.0, 36.0)
    private const val TSP_PER_CUP = 48.0
    private const val TSP_PER_TBSP = 3.0

    private fun usVolume(totalTsp: Double): String {
        if (totalTsp < 1.0 / 16.0) return "pinch"
        if (totalTsp < 1.0) {
            val snapped = nearest(totalTsp, doubleArrayOf(0.125, 0.25, 0.5, 0.75, 1.0))
            return renderFractional(snapped) + " tsp"
        }
        val h = snapHalf(totalTsp)

        var best: String? = null
        var bestParts = Int.MAX_VALUE
        var bestScoops = Int.MAX_VALUE
        var bestC = -1.0
        val maxWholeCups = floor(h / TSP_PER_CUP).toInt()
        for (w in 0..maxWholeCups) {
            for (f in CUP_FRACTION_TSP) {
                val c = w * TSP_PER_CUP + f
                if (c > h) continue
                val r = h - c
                val tbsp = floor(r / TSP_PER_TBSP).toInt()
                val tsp = r - tbsp * TSP_PER_TBSP

                val valid = (c > 0.0 && tbsp <= 3) || (c == 0.0 && h < TSP_PER_CUP / 2)
                if (!valid) continue

                var parts = 0
                if (c > 0.0) parts++
                if (tbsp > 0) parts++
                if (tsp > 0.0) parts++
                val scoops = w + (if (f > 0.0) 1 else 0) + tbsp +
                    floor(tsp).toInt() + (if (tsp > floor(tsp)) 1 else 0)

                if (parts < bestParts ||
                    (parts == bestParts && scoops < bestScoops) ||
                    (parts == bestParts && scoops == bestScoops && c > bestC)
                ) {
                    bestParts = parts
                    bestScoops = scoops
                    bestC = c
                    best = buildList {
                        if (c > 0.0) add(renderCup(w, f) + " cup")
                        if (tbsp > 0) add("$tbsp Tbsp")
                        if (tsp > 0.0) add(renderFractional(tsp) + " tsp")
                    }.joinToString(" ")
                }
            }
        }
        return best ?: renderFractional(h) + " tsp"
    }

    /** Render a whole-cup count + cup fraction (tsp): "¾", "1½", "20". */
    private fun renderCup(whole: Int, fracTsp: Double): String {
        if (fracTsp <= 0.0) return "$whole"
        val glyph = when (fracTsp) {
            12.0 -> "¼"
            16.0 -> "⅓"
            24.0 -> "½"
            32.0 -> "⅔"
            36.0 -> "¾"
            else -> error("unexpected cup fraction $fracTsp")
        }
        return if (whole == 0) glyph else "$whole$glyph"
    }

    // ── Other units ───────────────────────────────────────────────────────────

    /** Metric volume: whole mL (1 decimal below 10), promoting to L at 1000. */
    private fun metricVolume(totalMl: Double): String {
        if (totalMl < UnitConversions.ML_PER_TSP / 16.0) return "pinch"
        if (totalMl < 10.0) return stripDecimals(totalMl, 1) + " mL"
        val wholeMl = roundHalfUp(totalMl).toLong()
        return if (wholeMl >= 1000) {
            val litres = wholeMl / 1000
            val ml = wholeMl % 1000
            if (ml > 0) "$litres L $ml mL" else "$litres L"
        } else {
            "$wholeMl mL"
        }
    }

    /** Metric weight: mg below 1 g, 1-decimal g below 10, kg+g at 1000. */
    private fun metricWeight(totalG: Double): String {
        if (totalG < 1.0) return "${roundHalfUp(totalG * 1000)} mg"
        if (totalG < 10.0) return stripDecimals(totalG, 1) + " g"
        val wholeG = roundHalfUp(totalG).toLong()
        return if (wholeG >= 1000) {
            val kg = wholeG / 1000
            val g = wholeG % 1000
            if (g > 0) "$kg kg $g g" else "$kg kg"
        } else {
            "$wholeG g"
        }
    }

    /** US weight: nearest ¼ oz (min ¼), lb + oz at 16. */
    private fun usWeight(totalOz: Double): String {
        val snapped = snap(totalOz, 4.0).coerceAtLeast(0.25)
        if (snapped >= UnitConversions.OZ_PER_LB) {
            val lb = floor(snapped / UnitConversions.OZ_PER_LB).toInt()
            val oz = snapped - lb * UnitConversions.OZ_PER_LB
            return if (oz > 0.0) "$lb lb ${renderFractional(oz)} oz" else "$lb lb"
        }
        return renderFractional(snapped) + " oz"
    }

    /** Countable: pinch/dash round to whole (min 1); others to ½ (min ½). */
    private fun countRule(amount: Double, unit: KitchenUnit?, label: String): String {
        val text = if (unit == KitchenUnit.PINCH || unit == KitchenUnit.DASH) {
            "${roundHalfUp(amount).coerceAtLeast(1)}"
        } else {
            renderHalf(amount.coerceAtLeast(0.5))
        }
        return text + suffix(unit, label)
    }

    // ── KEEP_UNITS mode ───────────────────────────────────────────────────────

    private fun keepUnits(amount: Double, unit: KitchenUnit?, label: String): String {
        val text = when (unit) {
            KitchenUnit.TSP -> {
                if (amount < 1.0 / 16.0) return "pinch"
                if (amount < 1.0) {
                    renderFractional(nearest(amount, doubleArrayOf(0.125, 0.25, 0.5, 0.75, 1.0)))
                } else {
                    renderHalf(amount)
                }
            }
            KitchenUnit.TBSP, KitchenUnit.FL_OZ -> renderHalf(amount.coerceAtLeast(0.5))
            KitchenUnit.CUP -> renderCupKeep(amount)
            KitchenUnit.PINT, KitchenUnit.QUART, KitchenUnit.GALLON,
            KitchenUnit.OZ, KitchenUnit.LB -> renderQuarter(amount.coerceAtLeast(0.25))
            KitchenUnit.ML -> if (amount < 10.0) stripDecimals(amount, 1) else "${roundHalfUp(amount)}"
            KitchenUnit.L, KitchenUnit.KG -> stripDecimals(amount, 2)
            KitchenUnit.G -> if (amount < 10.0) stripDecimals(amount, 1) else "${roundHalfUp(amount)}"
            KitchenUnit.MG -> "${roundHalfUp(amount)}"
            else -> return countRule(amount, unit, label)
        }
        return text + suffix(unit, label)
    }

    /** cup in KEEP_UNITS: whole + one of {0, ¼, ⅓, ½, ⅔, ¾}, minimum ¼. */
    private fun renderCupKeep(amount: Double): String {
        val fracs = doubleArrayOf(0.0, 0.25, 1.0 / 3.0, 0.5, 2.0 / 3.0, 0.75)
        var best = 0.25
        var bestDist = Double.MAX_VALUE
        val wMax = floor(amount).toInt() + 1
        for (w in 0..wMax) {
            for (f in fracs) {
                val v = w + f
                if (v < 0.25) continue
                val d = kotlin.math.abs(v - amount)
                if (d < bestDist) {
                    bestDist = d
                    best = v
                }
            }
        }
        return renderFractional(best)
    }

    // ── Snapping / rendering helpers ──────────────────────────────────────────

    /** Round half-up to nearest 1/steps (e.g. steps=4 → nearest ¼). */
    private fun snap(value: Double, steps: Double): Double =
        floor(value * steps + 0.5) / steps

    private fun snapHalf(value: Double): Double = snap(value, 2.0)

    private fun roundHalfUp(value: Double): Long = floor(value + 0.5).toLong()

    private fun nearest(value: Double, options: DoubleArray): Double =
        options.minByOrNull { kotlin.math.abs(it - value) }!!

    /** Render a snapped non-negative number with Unicode fraction glyphs. */
    private fun renderFractional(value: Double): String {
        val whole = floor(value + 1e-9).toInt()
        val frac = value - whole
        val glyph = when {
            frac < 0.01 -> null
            kotlin.math.abs(frac - 0.125) < 0.01 -> "⅛"
            kotlin.math.abs(frac - 0.25) < 0.01 -> "¼"
            kotlin.math.abs(frac - 1.0 / 3.0) < 0.01 -> "⅓"
            kotlin.math.abs(frac - 0.5) < 0.01 -> "½"
            kotlin.math.abs(frac - 2.0 / 3.0) < 0.01 -> "⅔"
            kotlin.math.abs(frac - 0.75) < 0.01 -> "¾"
            else -> null
        }
        return when {
            glyph == null -> "$whole"
            whole == 0 -> glyph
            else -> "$whole$glyph"
        }
    }

    private fun renderHalf(value: Double): String = renderFractional(snapHalf(value))

    private fun renderQuarter(value: Double): String = renderFractional(snap(value, 4.0))

    /** At most [decimals] decimals, trailing zeros stripped: "1.5", "0.33", "2". */
    private fun stripDecimals(value: Double, decimals: Int): String =
        BigDecimal(value).setScale(decimals, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
}
