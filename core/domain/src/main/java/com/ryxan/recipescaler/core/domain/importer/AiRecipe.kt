package com.ryxan.recipescaler.core.domain.importer

/**
 * The recipe fields returned by an AI import.
 *
 * Plain data classes — the kotlinx.serialization DTOs that decode the model's
 * JSON live in the data layer (`core.data.importer`) so this module stays free
 * of serialization dependencies. Field names mirror the response schema built
 * in FirebaseAiRecipeImporter; instructions are intentionally out of scope.
 */
data class AiRecipe(
    val title: String? = null,
    val servings: String? = null,
    val ingredients: List<AiIngredient> = emptyList(),
)

data class AiIngredient(
    val qty: String? = null,
    val unit: String? = null,
    val name: String? = null,
)
