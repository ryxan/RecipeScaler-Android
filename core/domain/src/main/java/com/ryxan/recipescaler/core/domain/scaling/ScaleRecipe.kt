package com.ryxan.recipescaler.core.domain.scaling

import com.ryxan.recipescaler.core.domain.formatting.DisplayPrefs
import com.ryxan.recipescaler.core.domain.formatting.KitchenFormatter
import com.ryxan.recipescaler.core.domain.model.IngredientItem
import com.ryxan.recipescaler.core.domain.model.KitchenUnit
import com.ryxan.recipescaler.core.domain.model.ScaleStatus
import com.ryxan.recipescaler.core.domain.model.ScaledIngredient
import com.ryxan.recipescaler.core.domain.model.UnitConversions
import com.ryxan.recipescaler.core.domain.model.UnitFamily
import com.ryxan.recipescaler.core.domain.parsing.QuantityParser

sealed interface ScaleMode {
    data class ByYield(val original: Double, val target: Double) : ScaleMode
    data class ByIngredient(
        val ingredientId: String,
        val targetAmount: Double,
        val targetUnit: String,
    ) : ScaleMode
    data class ByFactor(val factor: Double) : ScaleMode
}

sealed interface ScaleError {
    data object InvalidYield : ScaleError
    data object InvalidFactor : ScaleError
    data object IngredientNotFound : ScaleError
    data object IngredientHasNoQuantity : ScaleError
    data class IncompatibleUnits(val from: String, val to: String) : ScaleError
}

sealed interface ScaleOutcome {
    data class Success(
        val factor: Double,
        val ingredients: List<ScaledIngredient>,
        val showScaleWarning: Boolean,
    ) : ScaleOutcome
    data class Failure(val error: ScaleError) : ScaleOutcome
}

/**
 * Scales a list of ingredients by yield, by a key ingredient, or by a direct
 * factor. Pure function — no Android, no I/O. See docs/plan.md §3.2.
 */
object ScaleRecipe {

    /** Failure thrown inside [factor]'s Result, carrying the [ScaleError]. */
    private class ScaleException(val error: ScaleError) : Exception(error.toString())

    /**
     * Compute the multiplication factor for [mode], or a failure carrying the
     * [ScaleError] (accessible via [ScaleException]).
     */
    fun factor(ingredients: List<IngredientItem>, mode: ScaleMode): Result<Double> = when (mode) {
        is ScaleMode.ByYield -> {
            if (mode.original <= 0.0 || mode.target <= 0.0) {
                Result.failure(ScaleException(ScaleError.InvalidYield))
            } else {
                Result.success(mode.target / mode.original)
            }
        }
        is ScaleMode.ByFactor -> {
            if (mode.factor <= 0.0) {
                Result.failure(ScaleException(ScaleError.InvalidFactor))
            } else {
                Result.success(mode.factor)
            }
        }
        is ScaleMode.ByIngredient -> byIngredientFactor(ingredients, mode)
    }

    /** Scale every ingredient row, formatting with [prefs]. */
    fun scale(
        ingredients: List<IngredientItem>,
        mode: ScaleMode,
        prefs: DisplayPrefs = DisplayPrefs(),
    ): ScaleOutcome {
        val factor = factor(ingredients, mode).getOrElse { e ->
            return ScaleOutcome.Failure((e as ScaleException).error)
        }
        val scaled = ingredients.map { scaleRow(it, factor, prefs) }
        return ScaleOutcome.Success(
            factor = factor,
            ingredients = scaled,
            showScaleWarning = factor > 4.0 || factor < 0.25,
        )
    }

    // ── ByIngredient ──────────────────────────────────────────────────────────

    private fun byIngredientFactor(
        ingredients: List<IngredientItem>,
        mode: ScaleMode.ByIngredient,
    ): Result<Double> {
        val ingredient = ingredients.find { it.id == mode.ingredientId }
            ?: return Result.failure(ScaleException(ScaleError.IngredientNotFound))
        val qty = QuantityParser.parse(ingredient.qtyString)
        if (qty == null || qty == 0.0) {
            return Result.failure(ScaleException(ScaleError.IngredientHasNoQuantity))
        }
        val converted = convert(mode.targetAmount, mode.targetUnit, ingredient.unit)
            ?: return Result.failure(
                ScaleException(
                    ScaleError.IncompatibleUnits(from = mode.targetUnit, to = ingredient.unit)
                )
            )
        return Result.success(converted / qty)
    }

    /**
     * Convert [amount] from [fromKey] units to [toKey] units (volume through mL,
     * weight through g, COUNT only between identical canonical keys). Null when
     * the units are in different families or unrecognised.
     */
    private fun convert(amount: Double, fromKey: String, toKey: String): Double? {
        val from = KitchenUnit.fromKey(fromKey)
        val to = KitchenUnit.fromKey(toKey)
        if (from == null || to == null) {
            // Unrecognised unit: treat as COUNT — only an identical
            // canonical key can convert.
            val canonicalFrom = RecipeScalerCalculator.canonicalUnit(fromKey)
            val canonicalTo = RecipeScalerCalculator.canonicalUnit(toKey)
            return if (canonicalFrom.equals(canonicalTo, ignoreCase = true)) amount else null
        }
        if (from.family != to.family) return null
        return when (from.family) {
            UnitFamily.VOLUME -> {
                val ml = UnitConversions.toMl(amount, from) ?: return null
                when (to) {
                    KitchenUnit.ML -> ml
                    KitchenUnit.L -> ml / UnitConversions.ML_PER_L
                    else -> UnitConversions.TSP_PER_UNIT[to]?.let { ml / UnitConversions.ML_PER_TSP / it }
                }
            }
            UnitFamily.WEIGHT -> {
                val g = UnitConversions.toG(amount, from) ?: return null
                when (to) {
                    KitchenUnit.MG -> g / 0.001
                    KitchenUnit.G -> g
                    KitchenUnit.KG -> g / 1000.0
                    KitchenUnit.OZ -> g / UnitConversions.G_PER_OZ
                    KitchenUnit.LB -> g / UnitConversions.G_PER_OZ / UnitConversions.OZ_PER_LB
                    else -> null
                }
            }
            UnitFamily.COUNT -> if (from == to) amount else null
        }
    }

    // ── Per-row scaling ───────────────────────────────────────────────────────

    private fun scaleRow(
        ingredient: IngredientItem,
        factor: Double,
        prefs: DisplayPrefs,
    ): ScaledIngredient {
        val raw = ingredient.qtyString
        if (raw.isBlank()) {
            return ScaledIngredient(
                id = ingredient.id, name = ingredient.name, unit = ingredient.unit,
                originalQty = null, scaledQty = null,
                displayText = "", originalDisplayText = "",
                status = ScaleStatus.BLANK,
            )
        }
        val qty = QuantityParser.parse(raw)
        if (qty == null) {
            return ScaledIngredient(
                id = ingredient.id, name = ingredient.name, unit = ingredient.unit,
                originalQty = null, scaledQty = null,
                displayText = raw, originalDisplayText = raw,
                status = ScaleStatus.UNPARSED,
            )
        }
        val status = if (QuantityParser.isRange(raw)) ScaleStatus.RANGE_LOW_END else ScaleStatus.SCALED
        return ScaledIngredient(
            id = ingredient.id, name = ingredient.name, unit = ingredient.unit,
            originalQty = qty, scaledQty = qty * factor,
            displayText = KitchenFormatter.format(qty * factor, ingredient.unit, prefs),
            originalDisplayText = KitchenFormatter.format(qty, ingredient.unit, prefs),
            status = status,
        )
    }
}
