package com.ryxan.recipescaler.core.domain.model

data class IngredientItem(
    val id: String,
    val qtyString: String,
    val unit: String,
    val name: String,
)

/** An [IngredientItem] produced by OCR parsing, with its recognition-confidence flag. */
data class ParsedIngredient(
    val item: IngredientItem,
    val lowConfidence: Boolean = false,
)

data class UnitOption(
    val value: String,
    val label: String
)

enum class ScaleStatus { SCALED, RANGE_LOW_END, UNPARSED, BLANK }

/** A "Promote to" menu entry: target unit plus the amount rendered in it. */
data class PromoteOption(
    val unit: KitchenUnit,
    val previewText: String,
)

data class ScaledIngredient(
    val id: String,
    val name: String,
    val unit: String,
    val originalQty: Double?,
    val scaledQty: Double?,
    val displayText: String,
    val originalDisplayText: String,
    val status: ScaleStatus,
    /** Active "Promote to" override for this row; null = Auto. */
    val promotedUnit: KitchenUnit? = null,
    /** Volume targets this row can be promoted to (empty = hide the menu). */
    val promoteOptions: List<PromoteOption> = emptyList(),
)

data class SavedRecipe(
    val name: String,
    val servings: String,
    val ingredients: List<SavedRecipeIngredient>
)

data class SavedRecipeIngredient(
    val qtyString: String,
    val unit: String,
    val name: String
)
