package com.ryxan.recipescaler.testing

import com.ryxan.recipescaler.core.domain.model.SavedScaleInputs
import com.ryxan.recipescaler.core.domain.repository.ScaleInputsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeScaleInputsRepository : ScaleInputsRepository {

    private val stored = MutableStateFlow<Map<Long, SavedScaleInputs>>(emptyMap())

    override fun observe(recipeId: Long): Flow<SavedScaleInputs?> =
        stored.map { it[recipeId] }

    override suspend fun save(recipeId: Long, inputs: SavedScaleInputs) {
        stored.update { it + (recipeId to inputs) }
    }

    fun seed(recipeId: Long, inputs: SavedScaleInputs) {
        stored.value = stored.value + (recipeId to inputs)
    }

    fun savedFor(recipeId: Long): SavedScaleInputs? = stored.value[recipeId]
}
