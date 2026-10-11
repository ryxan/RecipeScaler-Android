package com.ryxan.recipescaler.core.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/** Editable-row view of a persisted [Ingredient]. */
fun Ingredient.toScaleItem(): IngredientItem = IngredientItem(
    id = id.toString(),
    qtyString = rawQuantity,
    unit = unit.key,
    name = name,
)

/** Persisted form of an editor row; id 0 means "insert a new row". */
fun IngredientItem.toIngredient(): Ingredient = Ingredient(
    id = 0,
    rawQuantity = qtyString,
    unit = KitchenUnit.fromKey(unit) ?: KitchenUnit.NONE,
    name = name,
)

/**
 * Compact yield display: whole numbers without ".0" ("4"), otherwise up to
 * 2 decimals with trailing zeros stripped ("0.5", "1.25"). Non-positive
 * amounts render as "".
 */
fun formatYield(amount: Double): String {
    if (amount <= 0.0) return ""
    return BigDecimal(amount)
        .setScale(2, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
        .let { if (it == "0") "" else it }
}
