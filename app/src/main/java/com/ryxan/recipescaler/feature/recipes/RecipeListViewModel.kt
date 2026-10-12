package com.ryxan.recipescaler.feature.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryxan.recipescaler.core.domain.model.Recipe
import com.ryxan.recipescaler.core.domain.model.RecipeSummary
import com.ryxan.recipescaler.core.domain.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecipeListUiState(
    val query: String = "",
    val recipes: List<RecipeSummary> = emptyList(),
    val isLoading: Boolean = true,
    val pendingUndo: Recipe? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecipeListViewModel @Inject constructor(
    private val repository: RecipeRepository,
    private val undoController: UndoDeleteController,
) : ViewModel() {

    val query = MutableStateFlow("")

    val uiState: StateFlow<RecipeListUiState> = combine(
        query.flatMapLatest { repository.observeSummaries(it) },
        query,
        undoController.pending,
    ) { recipes, q, pending ->
        RecipeListUiState(
            query = q,
            recipes = recipes,
            isLoading = false,
            pendingUndo = pending,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeListUiState())

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    fun delete(id: Long) {
        viewModelScope.launch { undoController.delete(id) }
    }

    fun undoDelete() {
        undoController.undo()
    }

    fun dismissUndo() {
        undoController.dismiss()
    }
}
