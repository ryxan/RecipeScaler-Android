package com.ryxan.recipescaler.core.domain.repository

import com.ryxan.recipescaler.core.domain.model.SavedScaleInputs
import kotlinx.coroutines.flow.Flow

/**
 * Persists the last Scale View inputs (Serves + Scale To) per recipe, so
 * reopening a recipe restores the scaling the user left. Emits null for a
 * recipe the user has never scaled.
 */
interface ScaleInputsRepository {

    fun observe(recipeId: Long): Flow<SavedScaleInputs?>

    suspend fun save(recipeId: Long, inputs: SavedScaleInputs)
}
