package com.ryxan.recipescaler.core.domain.model

/**
 * Persisted recipe model (docs/plan.md §3.1). [Ingredient.rawQuantity] is the
 * source of truth — the text the user typed — and is re-parsed on read rather
 * than stored. Ingredient order is list order, persisted as `position`.
 */
data class Ingredient(
    val id: Long = 0,
    val rawQuantity: String,
    val unit: KitchenUnit,
    val name: String,
    val note: String? = null,
    /** Manual "Promote to" unit override on the Scale screen; null = Auto. */
    val promotedUnit: KitchenUnit? = null,
)

data class Recipe(
    val id: Long = 0,
    val title: String,
    val yieldAmount: Double,
    val yieldLabel: String,
    val ingredients: List<Ingredient>,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

data class RecipeSummary(
    val id: Long,
    val title: String,
    val yieldAmount: Double,
    val yieldLabel: String,
    val ingredientCount: Int,
    val updatedAt: Long,
)
