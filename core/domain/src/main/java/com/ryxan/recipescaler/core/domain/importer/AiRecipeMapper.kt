package com.ryxan.recipescaler.core.domain.importer

import com.ryxan.recipescaler.core.domain.model.IngredientItem
import com.ryxan.recipescaler.core.domain.model.KitchenUnit
import com.ryxan.recipescaler.core.domain.model.ParsedIngredient
import com.ryxan.recipescaler.core.domain.parsing.QuantityParser
import java.util.UUID

/**
 * Maps an [AiRecipe] to [ParsedIngredient] rows for the editor.
 *
 * Pure function with no Android or Firebase dependencies — same testability
 * as [com.ryxan.recipescaler.core.domain.parsing.RecipeOcrParser].
 *
 * Rules:
 * - Units are resolved to canonical [KitchenUnit] keys. An unrecognised unit
 *   folds to "none" with the original unit text preserved at the start of the
 *   name so no information is lost.
 * - Fully blank rows are dropped; results are capped at [AiImportConfig.MAX_INGREDIENTS].
 * - A non-blank quantity that [QuantityParser] cannot parse flags the row
 *   `lowConfidence` so the review hint fires.
 */
object AiRecipeMapper {

    fun map(recipe: AiRecipe): List<ParsedIngredient> =
        recipe.ingredients
            .mapNotNull { mapIngredient(it) }
            .take(AiImportConfig.MAX_INGREDIENTS)

    private fun mapIngredient(raw: AiIngredient): ParsedIngredient? {
        val qty = raw.qty?.trim()?.take(AiImportConfig.MAX_QTY_LENGTH).orEmpty()
        val name = raw.name?.trim()?.take(AiImportConfig.MAX_NAME_LENGTH).orEmpty()
        if (qty.isEmpty() && name.isEmpty() && raw.unit.isNullOrBlank()) return null

        val (unit, finalName) = resolveUnit(raw.unit, name)
        val lowConfidence = qty.isNotEmpty() && QuantityParser.parse(qty) == null

        return ParsedIngredient(
            item = IngredientItem(
                id = UUID.randomUUID().toString(),
                qtyString = qty,
                unit = unit,
                name = finalName,
            ),
            lowConfidence = lowConfidence,
        )
    }

    private fun resolveUnit(rawUnit: String?, name: String): Pair<String, String> {
        val unit = rawUnit?.trim().orEmpty()
        if (unit.isEmpty()) return KitchenUnit.NONE.key to name

        KitchenUnit.fromKey(unit)?.let { return it.key to name }

        // Surface forms the model may emit despite the enum constraint:
        // plurals and dotted abbreviations ("cups", "oz.", "lbs").
        val folded = unit.lowercase().trimEnd('.', 's')
        KitchenUnit.fromKey(folded)?.let { return it.key to name }

        return KitchenUnit.NONE.key to "$unit $name".trim()
    }
}
