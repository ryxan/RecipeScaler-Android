package com.ryxan.recipescaler.core.domain.repository

import com.ryxan.recipescaler.core.domain.model.Recipe
import com.ryxan.recipescaler.core.domain.model.RecipeSummary
import kotlinx.coroutines.flow.Flow

interface RecipeRepository {

    /** Summaries sorted by updatedAt desc; [query] filters title LIKE %q% case-insensitive, blank = all. */
    fun observeSummaries(query: String = ""): Flow<List<RecipeSummary>>

    /** Emits null when the recipe is missing or deleted. */
    fun observe(id: Long): Flow<Recipe?>

    suspend fun get(id: Long): Recipe?

    /**
     * id == 0 inserts (createdAt = updatedAt = now); otherwise updates: replaces the
     * ingredient rows, keeps createdAt, sets updatedAt = now. Transactional.
     * Returns the recipe id.
     *
     * @throws IllegalArgumentException if id != 0 but no such recipe exists.
     */
    suspend fun save(recipe: Recipe): Long

    /** Cascades to ingredients; no-op if missing. */
    suspend fun delete(id: Long)

    /**
     * Re-inserts a previously deleted recipe with its original id, createdAt and
     * updatedAt (so it returns to the same list position) and its ingredients in
     * order. No-op if a recipe with that id already exists.
     */
    suspend fun restore(recipe: Recipe)
}
