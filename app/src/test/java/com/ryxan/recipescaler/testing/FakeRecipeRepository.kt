package com.ryxan.recipescaler.testing

import com.ryxan.recipescaler.core.domain.model.Recipe
import com.ryxan.recipescaler.core.domain.model.RecipeSummary
import com.ryxan.recipescaler.core.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [RecipeRepository] honoring the interface contract (see its KDoc). */
class FakeRecipeRepository : RecipeRepository {

    private val recipes = MutableStateFlow<Map<Long, Recipe>>(emptyMap())
    private var nextId = 1L
    private var now = 0L

    /** Deterministic, monotonically increasing clock so updatedAt ordering is stable. */
    private fun tick(): Long = ++now

    override fun observeSummaries(query: String): Flow<List<RecipeSummary>> =
        recipes.map { map ->
            map.values
                .filter { query.isBlank() || it.title.contains(query, ignoreCase = true) }
                .sortedByDescending { it.updatedAt }
                .map {
                    RecipeSummary(
                        id = it.id,
                        title = it.title,
                        yieldAmount = it.yieldAmount,
                        yieldLabel = it.yieldLabel,
                        ingredientCount = it.ingredients.size,
                        updatedAt = it.updatedAt,
                    )
                }
        }

    override fun observe(id: Long): Flow<Recipe?> = recipes.map { it[id] }

    override suspend fun get(id: Long): Recipe? = recipes.value[id]

    override suspend fun save(recipe: Recipe): Long {
        val current = recipes.value.toMutableMap()
        return if (recipe.id == 0L) {
            val id = nextId++
            val t = tick()
            current[id] = recipe.copy(
                id = id,
                createdAt = t,
                updatedAt = t,
                ingredients = recipe.ingredients.mapIndexed { i, ing ->
                    if (ing.id == 0L) ing.copy(id = id * 1000 + i + 1) else ing
                },
            )
            recipes.value = current
            id
        } else {
            val existing = current[recipe.id]
                ?: throw IllegalArgumentException("No recipe with id ${recipe.id}")
            current[recipe.id] = recipe.copy(createdAt = existing.createdAt, updatedAt = tick())
            recipes.value = current
            recipe.id
        }
    }

    override suspend fun delete(id: Long) {
        val current = recipes.value.toMutableMap()
        current.remove(id)
        recipes.value = current
    }
}
