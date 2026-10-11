package com.ryxan.recipescaler.feature.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryxan.recipescaler.core.domain.model.Ingredient
import com.ryxan.recipescaler.core.domain.model.IngredientItem
import com.ryxan.recipescaler.core.domain.model.KitchenUnit
import com.ryxan.recipescaler.core.domain.model.Recipe
import com.ryxan.recipescaler.core.domain.model.formatYield
import com.ryxan.recipescaler.core.domain.parsing.QuantityParser
import com.ryxan.recipescaler.core.domain.parsing.RecipeOcrParser
import com.ryxan.recipescaler.core.domain.repository.RecipeRepository
import com.ryxan.recipescaler.di.ApplicationScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

const val MAX_INGREDIENTS = 50
private const val SAVE_DEBOUNCE_MS = 500L

data class EditorRow(
    val key: Long,
    val rawQuantity: String = "",
    val unit: KitchenUnit = KitchenUnit.NONE,
    val name: String = "",
) {
    val qtyError: Boolean
        get() = rawQuantity.isNotBlank() && QuantityParser.parse(rawQuantity) == null
}

sealed interface EditorDialog {
    data object PasteText : EditorDialog
    data class ConfirmImport(val items: List<IngredientItem>) : EditorDialog
    data object ConfirmDelete : EditorDialog
    data object ConfirmClear : EditorDialog
}

data class EditorUiState(
    val isLoading: Boolean = true,
    val recipeId: Long = 0,
    val title: String = "",
    val yieldText: String = "",
    val yieldLabel: String = "servings",
    val rows: List<EditorRow> = emptyList(),
    val dialog: EditorDialog? = null,
) {
    val yieldError: Boolean
        get() = yieldText.isNotBlank() && QuantityParser.parse(yieldText) == null

    val canScale: Boolean
        get() = rows.any { QuantityParser.parse(it.rawQuantity)?.let { q -> q > 0 } == true }
}

sealed interface EditorEvent {
    data object NothingToImport : EditorEvent
    data object IngredientCapReached : EditorEvent
    data object NavigateUp : EditorEvent
}

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: RecipeRepository,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState

    private val _events = Channel<EditorEvent>(Channel.BUFFERED)
    val events: Flow<EditorEvent> = _events.receiveAsFlow()

    private val saveMutex = Mutex()
    private var saveJob: Job? = null
    private var keyCounter = 0L

    /** True when this session inserted the recipe — only those are auto-deleted when emptied. */
    private var createdInSession = false

    /** True once the recipe was deleted — blocks every later save so it can't resurrect. */
    private var deleted = false

    init {
        viewModelScope.launch {
            val id = savedStateHandle.get<Long>("recipeId") ?: 0L
            val recipe = if (id != 0L) repository.get(id) else null
            if (recipe != null) {
                _uiState.value = EditorUiState(
                    isLoading = false,
                    recipeId = recipe.id,
                    title = recipe.title,
                    yieldText = formatYield(recipe.yieldAmount),
                    yieldLabel = recipe.yieldLabel,
                    rows = recipe.ingredients.map { it.toEditorRow() },
                )
            } else {
                // Unknown id (or 0): fall back to a new blank recipe.
                _uiState.value = EditorUiState(
                    isLoading = false,
                    recipeId = 0L,
                    rows = listOf(newRow()),
                )
            }
        }
    }

    private fun newRow() = EditorRow(key = ++keyCounter)

    private fun Ingredient.toEditorRow() = EditorRow(
        key = ++keyCounter,
        rawQuantity = rawQuantity,
        unit = unit,
        name = name,
    )

    // ── Field edits ───────────────────────────────────────────────────────────

    fun onTitleChange(v: String) = edit { it.copy(title = v) }
    fun onYieldTextChange(v: String) = edit { it.copy(yieldText = v) }
    fun onYieldLabelChange(v: String) = edit { it.copy(yieldLabel = v) }

    fun onRowQtyChange(key: Long, v: String) = editRows(key) { it.copy(rawQuantity = v) }
    fun onRowUnitChange(key: Long, v: KitchenUnit) = editRows(key) { it.copy(unit = v) }
    fun onRowNameChange(key: Long, v: String) = editRows(key) { it.copy(name = v) }

    private fun edit(transform: (EditorUiState) -> EditorUiState) {
        _uiState.update(transform)
        scheduleSave()
    }

    private fun editRows(key: Long, transform: (EditorRow) -> EditorRow) {
        edit { s -> s.copy(rows = s.rows.map { if (it.key == key) transform(it) else it }) }
    }

    /** Insert a blank row after [afterKey] (or at the end). Returns its key, or -1 at the cap. */
    fun addRow(afterKey: Long? = null): Long {
        if (_uiState.value.rows.size >= MAX_INGREDIENTS) return -1L
        val row = newRow()
        _uiState.update { s ->
            val idx = s.rows.indexOfFirst { it.key == afterKey }
            val rows = s.rows.toMutableList().apply {
                add(if (idx >= 0) idx + 1 else size, row)
            }
            s.copy(rows = rows)
        }
        scheduleSave()
        return row.key
    }

    fun removeRow(key: Long) {
        _uiState.update { s -> s.copy(rows = s.rows.filterNot { it.key == key }) }
        scheduleSave()
    }

    // ── Paste-text import ─────────────────────────────────────────────────────

    fun openPasteDialog() {
        _uiState.update { it.copy(dialog = EditorDialog.PasteText) }
    }

    fun importText(text: String) {
        // Parse past the cap so truncation can be detected.
        val items = RecipeOcrParser.parse(text, MAX_INGREDIENTS * 4).map { it.item }
        if (items.isEmpty()) {
            dismissDialog()
            _events.trySend(EditorEvent.NothingToImport)
            return
        }
        val allBlank = _uiState.value.rows.all { it.rawQuantity.isBlank() && it.name.isBlank() }
        if (allBlank) {
            replaceRows(items)
        } else {
            _uiState.update { it.copy(dialog = EditorDialog.ConfirmImport(items)) }
        }
    }

    fun confirmImportReplace() {
        val dialog = _uiState.value.dialog as? EditorDialog.ConfirmImport ?: return
        replaceRows(dialog.items)
    }

    fun confirmImportAppend() {
        val dialog = _uiState.value.dialog as? EditorDialog.ConfirmImport ?: return
        val s = _uiState.value
        val existing = s.rows.filter { it.rawQuantity.isNotBlank() || it.name.isNotBlank() }
        val room = MAX_INGREDIENTS - existing.size
        val kept = dialog.items.take(room.coerceAtLeast(0))
        val dropped = dialog.items.size - kept.size
        _uiState.value = s.copy(
            rows = existing + kept.map { it.toEditorRow() },
            dialog = null,
        )
        scheduleSave()
        if (dropped > 0) _events.trySend(EditorEvent.IngredientCapReached)
    }

    private fun replaceRows(items: List<IngredientItem>) {
        val kept = items.take(MAX_INGREDIENTS)
        _uiState.update { s ->
            s.copy(rows = kept.map { it.toEditorRow() }, dialog = null)
        }
        scheduleSave()
        if (items.size > kept.size) _events.trySend(EditorEvent.IngredientCapReached)
    }

    private fun IngredientItem.toEditorRow() = EditorRow(
        key = ++keyCounter,
        rawQuantity = qtyString,
        unit = KitchenUnit.fromKey(unit) ?: KitchenUnit.NONE,
        name = name,
    )

    // ── Clear / delete ────────────────────────────────────────────────────────

    fun requestClear() {
        _uiState.update { it.copy(dialog = EditorDialog.ConfirmClear) }
    }

    fun confirmClear() {
        _uiState.update { it.copy(rows = listOf(newRow()), dialog = null) }
        scheduleSave()
    }

    fun requestDelete() {
        _uiState.update { it.copy(dialog = EditorDialog.ConfirmDelete) }
    }

    fun confirmDelete() {
        _uiState.update { it.copy(dialog = null) }
        appScope.launch {
            saveMutex.withLock {
                saveJob?.cancel()
                deleted = true
                val id = _uiState.value.recipeId
                if (id != 0L) repository.delete(id)
                _events.send(EditorEvent.NavigateUp)
            }
        }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(dialog = null) }
    }

    // ── Saving ────────────────────────────────────────────────────────────────

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = appScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            saveMutex.withLock { doSave() }
        }
    }

    /**
     * Flush synchronously; returns the recipe id, or null when the editor is
     * empty (nothing was or should be saved).
     */
    suspend fun prepareToScale(): Long? {
        saveJob?.cancel()
        saveJob = null
        return saveMutex.withLock { doSave() }
    }

    private suspend fun doSave(): Long? {
        if (deleted) return null
        val s = _uiState.value
        if (s.isLoading) return s.recipeId.takeIf { it != 0L }
        val empty = s.title.isBlank() &&
            s.rows.all { it.rawQuantity.isBlank() && it.name.isBlank() }
        if (empty) {
            if (s.recipeId != 0L && createdInSession) {
                repository.delete(s.recipeId)
            }
            return s.recipeId.takeIf { it != 0L }
        }
        val id = try {
            repository.save(
                Recipe(
                    id = s.recipeId,
                    title = s.title,
                    yieldAmount = QuantityParser.parse(s.yieldText) ?: 0.0,
                    yieldLabel = s.yieldLabel,
                    ingredients = s.rows
                        .filter { it.rawQuantity.isNotBlank() || it.name.isNotBlank() }
                        .map {
                            Ingredient(
                                id = 0,
                                rawQuantity = it.rawQuantity,
                                unit = it.unit,
                                name = it.name,
                            )
                        },
                ),
            )
        } catch (e: IllegalArgumentException) {
            // The recipe was deleted elsewhere — don't resurrect it.
            deleted = true
            return null
        }
        if (id != s.recipeId) {
            savedStateHandle["recipeId"] = id
            createdInSession = true
            _uiState.update { it.copy(recipeId = id) }
        }
        return id
    }

    internal fun flush() {
        saveJob?.cancel()
        appScope.launch {
            saveMutex.withLock { doSave() }
        }
    }

    override fun onCleared() {
        flush()
    }
}
