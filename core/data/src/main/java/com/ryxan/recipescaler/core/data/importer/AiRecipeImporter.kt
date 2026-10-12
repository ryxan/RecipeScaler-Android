package com.ryxan.recipescaler.core.data.importer

import com.ryxan.recipescaler.core.domain.importer.AiIngredient
import com.ryxan.recipescaler.core.domain.importer.AiRecipe
import com.ryxan.recipescaler.core.domain.model.ParsedIngredient
import kotlinx.serialization.Serializable

/** Imports a recipe's ingredient list from a JPEG image. */
interface AiRecipeImporter {
    suspend fun import(jpeg: ByteArray): AiImportResult
}

/**
 * Result of a single AI import attempt. A free-import credit is spent only on
 * [Success] — every other variant is refunded by the caller.
 */
sealed interface AiImportResult {

    /** Parsed ingredients, plus the raw recipe fields (title/servings) for prefill. */
    data class Success(
        val recipe: AiRecipe,
        val items: List<ParsedIngredient>,
    ) : AiImportResult

    /** Model ran but found no readable ingredients (or returned an empty list). */
    data object NothingFound : AiImportResult

    /** Transport-level failure: no connectivity, timeout, or HTTP 5xx. */
    data object NetworkError : AiImportResult

    /** Blocked by safety filters, either the prompt/image or the response. */
    data object Blocked : AiImportResult

    /** The project's API quota was exhausted — distinct from the app's per-user caps. */
    data object RateLimited : AiImportResult

    /** Any other failure; [cause] is logged in debug builds only. */
    data class Failed(val cause: Throwable? = null) : AiImportResult
}

/**
 * Serialized form of the model's JSON response. Unknown keys are ignored and
 * every field is optional so a partial response still decodes.
 */
@Serializable
data class AiRecipeDto(
    val title: String? = null,
    val servings: String? = null,
    val ingredients: List<AiIngredientDto> = emptyList(),
) {
    fun toDomain() = AiRecipe(
        title = title,
        servings = servings,
        ingredients = ingredients.map { it.toDomain() },
    )
}

@Serializable
data class AiIngredientDto(
    val qty: String? = null,
    val unit: String? = null,
    val name: String? = null,
) {
    fun toDomain() = AiIngredient(qty = qty, unit = unit, name = name)
}
