package com.ryxan.recipescaler.core.domain.scaling

import com.ryxan.recipescaler.core.domain.model.KitchenUnit
import com.ryxan.recipescaler.core.domain.model.ScaledIngredient
import com.ryxan.recipescaler.core.domain.parsing.QuantityParser

object RecipeScalerCalculator {

    // ── Unit catalogue ────────────────────────────────────────────────────────
    // Canonical key → display label, derived from [KitchenUnit] so the two can
    // never drift. Used by the ingredient row display, the "was X unit" caption,
    // and the accessory bar lookup.
    val ALL_UNITS: List<Pair<String, String>> = KitchenUnit.entries.map { it.key to it.label }

    /** Display label for a canonical unit key (e.g. "tbsp" → "Tbsp"); unknown keys pass through. */
    private fun displayLabel(unit: String): String =
        ALL_UNITS.find { it.first == unit }?.second ?: unit

    /** Normalise a stored/UI unit string to a canonical [ALL_UNITS] key (e.g. "Tbsp" → "tbsp"). */
    fun canonicalUnit(unit: String): String =
        ALL_UNITS.find { it.first.equals(unit.trim(), ignoreCase = true) }?.first ?: unit.trim()

    // ── Multiplier ────────────────────────────────────────────────────────────

    fun calculateMultiplier(originalServings: Double, desiredServings: Double): Double {
        if (originalServings <= 0.0) return 1.0
        return desiredServings / originalServings
    }

    // ── Copy helpers ──────────────────────────────────────────────────────────

    /** Plain-text shopping list. Rows with a blank display collapse to the name. */
    fun generateShoppingList(scaledIngredients: List<ScaledIngredient>): String {
        if (scaledIngredients.isEmpty()) return ""
        return scaledIngredients.joinToString("\n") { "• ${rowText(it)}" }
    }

    /**
     * Generates a recipe in the chosen copy format.
     */
    fun generateFormattedRecipe(
        scaledIngredients: List<ScaledIngredient>,
        servings: String,
        format: CopyFormat
    ): String {
        if (scaledIngredients.isEmpty()) return ""
        return when (format) {
            CopyFormat.PLAIN -> {
                "Scaled Recipe (Serves $servings)\n\n" +
                    scaledIngredients.joinToString("\n") { rowText(it) }
            }
            CopyFormat.MARKDOWN -> {
                "## Recipe (Serves $servings)\n\n### Ingredients\n\n" +
                    scaledIngredients.joinToString("\n") { markdownRow(it) }
            }
            CopyFormat.SHOPPING_LIST -> {
                scaledIngredients.joinToString("\n") { "☐ ${rowText(it)}" }
            }
        }
    }

    /** "displayText name", or just "name" when the display text is blank. */
    private fun rowText(ingredient: ScaledIngredient): String =
        listOf(ingredient.displayText, ingredient.name)
            .filter { it.isNotBlank() }
            .joinToString(" ")

    private fun markdownRow(ingredient: ScaledIngredient): String =
        if (ingredient.displayText.isBlank()) "- ${ingredient.name}"
        else "- **${ingredient.displayText}** ${ingredient.name}"

    // ── Quantity parser ───────────────────────────────────────────────────────

    /**
     * Parses a quantity string, returning 0.0 when blank or unparseable.
     * Delegates to [QuantityParser]; see it for the supported forms.
     */
    fun parseQuantity(inputStr: String): Double = QuantityParser.parse(inputStr) ?: 0.0
}

enum class CopyFormat {
    PLAIN,
    MARKDOWN,
    SHOPPING_LIST
}
