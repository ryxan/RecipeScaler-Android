package com.ryxan.recipescaler.core.data

import androidx.room.withTransaction
import com.ryxan.recipescaler.core.data.db.RecipeDao
import com.ryxan.recipescaler.core.data.db.RecipeDatabase
import com.ryxan.recipescaler.core.data.db.RecipeEntity
import com.ryxan.recipescaler.core.data.db.toDomain
import com.ryxan.recipescaler.core.data.db.toEntity
import com.ryxan.recipescaler.core.domain.model.Recipe
import com.ryxan.recipescaler.core.domain.model.RecipeSummary
import com.ryxan.recipescaler.core.domain.repository.RecipeRepository
import com.ryxan.recipescaler.core.domain.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomRecipeRepository @Inject constructor(
    private val dao: RecipeDao,
    private val db: RecipeDatabase,
    private val clock: Clock,
) : RecipeRepository {

    override fun observeSummaries(query: String): Flow<List<RecipeSummary>> =
        dao.observeSummaries(escapeLike(query)).map { list -> list.map { it.toDomain() } }

    override fun observe(id: Long): Flow<Recipe?> =
        dao.observeRecipe(id).map { it?.toDomain() }

    override suspend fun get(id: Long): Recipe? = dao.getRecipe(id)?.toDomain()

    override suspend fun save(recipe: Recipe): Long = db.withTransaction {
        val now = clock.now()
        if (recipe.id == 0L) {
            val newId = dao.insertRecipe(
                RecipeEntity(
                    title = recipe.title,
                    yieldAmount = recipe.yieldAmount,
                    yieldLabel = recipe.yieldLabel,
                    createdAt = now,
                    updatedAt = now,
                )
            )
            dao.insertIngredients(
                recipe.ingredients.mapIndexed { i, ing -> ing.copy(id = 0).toEntity(newId, i) }
            )
            newId
        } else {
            val existing = dao.getRecipeEntity(recipe.id)
                ?: throw IllegalArgumentException("Recipe ${recipe.id} does not exist")
            dao.updateRecipe(
                existing.copy(
                    title = recipe.title,
                    yieldAmount = recipe.yieldAmount,
                    yieldLabel = recipe.yieldLabel,
                    updatedAt = now,
                )
            )
            dao.deleteIngredientsForRecipe(recipe.id)
            dao.insertIngredients(
                recipe.ingredients.mapIndexed { i, ing -> ing.copy(id = 0).toEntity(recipe.id, i) }
            )
            recipe.id
        }
    }

    override suspend fun delete(id: Long) {
        dao.deleteRecipe(id)
    }

    override suspend fun restore(recipe: Recipe) = db.withTransaction {
        if (dao.getRecipeEntity(recipe.id) != null) return@withTransaction
        dao.insertRecipe(
            RecipeEntity(
                id = recipe.id,
                title = recipe.title,
                yieldAmount = recipe.yieldAmount,
                yieldLabel = recipe.yieldLabel,
                createdAt = recipe.createdAt,
                updatedAt = recipe.updatedAt,
            )
        )
        dao.insertIngredients(
            recipe.ingredients.mapIndexed { i, ing -> ing.copy(id = 0).toEntity(recipe.id, i) }
        )
    }

    private companion object {
        /** Escape LIKE wildcards so queries containing `%`, `_`, or `\` match literally. */
        fun escapeLike(query: String): String = query
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
    }
}
