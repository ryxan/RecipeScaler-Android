package com.ryxan.recipescaler.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Insert
    suspend fun insertRecipe(recipe: RecipeEntity): Long

    @Update
    suspend fun updateRecipe(recipe: RecipeEntity)

    @Insert
    suspend fun insertIngredients(ingredients: List<IngredientEntity>)

    @Query("DELETE FROM ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredientsForRecipe(recipeId: Long)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipe(id: Long)

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getRecipeEntity(id: Long): RecipeEntity?

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    fun observeRecipe(id: Long): Flow<RecipeWithIngredients?>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getRecipe(id: Long): RecipeWithIngredients?

    @Query(
        """
        SELECT r.id, r.title, r.yieldAmount, r.yieldLabel, r.updatedAt,
            (SELECT COUNT(*) FROM ingredients i WHERE i.recipeId = r.id) AS ingredientCount
        FROM recipes r
        WHERE r.title LIKE '%' || :query || '%' ESCAPE '\'
        ORDER BY r.updatedAt DESC
        """
    )
    fun observeSummaries(query: String): Flow<List<RecipeSummaryTuple>>

    @Query("SELECT COUNT(*) FROM ingredients WHERE recipeId = :recipeId")
    suspend fun ingredientCount(recipeId: Long): Int

    /** Sets/clears the "Promote to" override on one ingredient row. */
    @Query(
        "UPDATE ingredients SET promotedUnitKey = :unitKey " +
            "WHERE id = :ingredientId AND recipeId = :recipeId"
    )
    suspend fun setPromotedUnit(recipeId: Long, ingredientId: Long, unitKey: String?)
}
