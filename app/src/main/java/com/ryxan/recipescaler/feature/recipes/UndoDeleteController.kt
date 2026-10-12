package com.ryxan.recipescaler.feature.recipes

import com.ryxan.recipescaler.core.domain.model.Recipe
import com.ryxan.recipescaler.core.domain.repository.RecipeRepository
import com.ryxan.recipescaler.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Holds the most recently deleted recipe so it can be restored via the undo
 * snackbar. Singleton so a delete triggered on another screen (e.g. the editor)
 * still surfaces on the recipe list.
 */
@Singleton
class UndoDeleteController @Inject constructor(
    private val repository: RecipeRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {

    private val _pending = MutableStateFlow<Recipe?>(null)
    val pending: StateFlow<Recipe?> = _pending

    suspend fun delete(id: Long) {
        val recipe = repository.get(id) ?: return
        repository.delete(id)
        _pending.value = recipe
    }

    fun undo() {
        val recipe = _pending.value ?: return
        _pending.value = null
        scope.launch { repository.restore(recipe) }
    }

    fun dismiss() {
        _pending.value = null
    }
}
