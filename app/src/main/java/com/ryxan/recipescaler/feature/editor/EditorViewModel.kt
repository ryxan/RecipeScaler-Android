package com.ryxan.recipescaler.feature.editor

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryxan.recipescaler.core.domain.importer.splitYield
import com.ryxan.recipescaler.core.domain.model.Ingredient
import com.ryxan.recipescaler.core.domain.model.KitchenUnit
import com.ryxan.recipescaler.core.domain.model.ParsedIngredient
import com.ryxan.recipescaler.core.domain.model.Recipe
import com.ryxan.recipescaler.core.domain.model.formatYield
import com.ryxan.recipescaler.core.domain.parsing.QuantityParser
import com.ryxan.recipescaler.core.domain.parsing.RecipeOcrParser
import com.ryxan.recipescaler.core.domain.repository.RecipeRepository
import com.ryxan.recipescaler.di.ApplicationScope
import com.ryxan.recipescaler.feature.importer.ScanImporter
import com.ryxan.recipescaler.feature.importer.ScanOutcome
import com.ryxan.recipescaler.feature.importer.ScanError
import com.ryxan.recipescaler.feature.recipes.UndoDeleteController
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
    /** Imported with an unparseable quantity; cleared on any edit. Not persisted. */
    val needsReview: Boolean = false,
) {
    val qtyError: Boolean
        get() = rawQuantity.isNotBlank() && QuantityParser.parse(rawQuantity) == null
}

/** Title/servings detected by a scan, applied to blank fields on import. */
data class ImportPrefill(val title: String?, val servings: String?)

enum class ImportBusy { ON_DEVICE, AI }

sealed interface EditorDialog {
    data object PasteText : EditorDialog
    data class ConfirmImport(
        val items: List<ParsedIngredient>,
        val prefill: ImportPrefill? = null,
    ) : EditorDialog
    data object ConfirmClear : EditorDialog
    data object ScanSource : EditorDialog
    data class ScanFailed(
        val error: ScanError,
        val canRetryWithAi: Boolean,
        val canUseOnDevice: Boolean,
    ) : EditorDialog
    data object AiPaywall : EditorDialog
}

data class EditorUiState(
    val isLoading: Boolean = true,
    val recipeId: Long = 0,
    val title: String = "",
    val yieldText: String = "",
    val yieldLabel: String = "servings",
    val rows: List<EditorRow> = emptyList(),
    val dialog: EditorDialog? = null,
    /** Row key that should grab qty focus once on open (Quick scale). */
    val autoFocusKey: Long? = null,
    /** Non-null while a scan/import is in flight — new scan requests are ignored. */
    val importBusy: ImportBusy? = null,
    /** Route scans through the AI importer instead of on-device OCR. Session only. */
    val useAiImport: Boolean = false,
    val aiAvailable: Boolean = false,
    val isPro: Boolean = false,
    val aiFreeRemaining: Int = 0,
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
    /** A scan applied but looked poor — the UI offers "Try AI import". */
    data object ScanLooksPoor : EditorEvent
}

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: RecipeRepository,
    private val undoController: UndoDeleteController,
    private val scanImporter: ScanImporter,
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

    /** False until the user edits — a prefilled Quick scale alone never inserts. */
    private var dirty = false

    /**
     * Set when a parsed scan result was poor and AI retry is possible; consumed
     * (as [EditorEvent.ScanLooksPoor]) once the result is actually applied.
     */
    private var pendingOfferAiRetry = false

    init {
        _uiState.update { it.copy(aiAvailable = scanImporter.aiAvailable) }
        viewModelScope.launch {
            scanImporter.remainingFree.collect { remaining ->
                _uiState.update { it.copy(aiFreeRemaining = remaining) }
            }
        }
        viewModelScope.launch {
            scanImporter.isPro.collect { pro ->
                _uiState.update { it.copy(isPro = pro) }
            }
        }
        viewModelScope.launch {
            val id = savedStateHandle.get<Long>("recipeId") ?: 0L
            val recipe = if (id != 0L) repository.get(id) else null
            if (recipe != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        recipeId = recipe.id,
                        title = recipe.title,
                        yieldText = formatYield(recipe.yieldAmount),
                        yieldLabel = recipe.yieldLabel,
                        rows = recipe.ingredients.map { i -> i.toEditorRow() },
                    )
                }
            } else {
                // Unknown id (or 0): fall back to a new blank recipe.
                val initialTitle = savedStateHandle.get<String>("initialTitle")
                val row = newRow()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        recipeId = 0L,
                        title = initialTitle.orEmpty(),
                        rows = listOf(row),
                        autoFocusKey = if (initialTitle != null) row.key else null,
                    )
                }
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

    fun onRowQtyChange(key: Long, v: String) = editRows(key) { it.copy(rawQuantity = v, needsReview = false) }
    fun onRowUnitChange(key: Long, v: KitchenUnit) = editRows(key) { it.copy(unit = v, needsReview = false) }
    fun onRowNameChange(key: Long, v: String) = editRows(key) { it.copy(name = v, needsReview = false) }

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

    /** Moves the row [fromKey] to the position of row [toKey]; unknown keys are ignored. */
    fun moveRow(fromKey: Long, toKey: Long) {
        var moved = false
        _uiState.update { s ->
            val from = s.rows.indexOfFirst { it.key == fromKey }
            val to = s.rows.indexOfFirst { it.key == toKey }
            if (from < 0 || to < 0 || from == to) return@update s
            moved = true
            val rows = s.rows.toMutableList().apply { add(to, removeAt(from)) }
            s.copy(rows = rows)
        }
        if (moved) scheduleSave()
    }

    /**
     * Splits a quantity/unit off the name when the qty field is blank, e.g.
     * "2 cups flour" → qty "2", unit CUP, name "flour". The row gets a fresh
     * key so the cached qty TextFieldValue picks up the new value. No-op when
     * the row already has a quantity or the name carries none.
     */
    fun inferFromName(key: Long) {
        val row = _uiState.value.rows.firstOrNull { it.key == key } ?: return
        if (row.rawQuantity.isNotBlank() || row.name.isBlank()) return
        val item = RecipeOcrParser.parse(row.name, 1).firstOrNull()?.item ?: return
        if (item.qtyString.isBlank()) return
        val inferred = row.copy(
            key = ++keyCounter,
            rawQuantity = item.qtyString,
            unit = KitchenUnit.fromKey(item.unit)?.takeIf { it != KitchenUnit.NONE } ?: row.unit,
            name = item.name,
        )
        _uiState.update { s ->
            s.copy(rows = s.rows.map { if (it.key == key) inferred else it })
        }
        scheduleSave()
    }

    // ── Paste-text import ─────────────────────────────────────────────────────

    fun openPasteDialog() {
        _uiState.update { it.copy(dialog = EditorDialog.PasteText) }
    }

    fun importText(text: String) {
        // Parse past the cap so truncation can be detected.
        val items = RecipeOcrParser.parse(text, MAX_INGREDIENTS * 4)
        if (items.isEmpty()) {
            dismissDialog()
            _events.trySend(EditorEvent.NothingToImport)
            return
        }
        pendingOfferAiRetry = false
        applyImport(items, prefill = null)
    }

    private fun applyImport(items: List<ParsedIngredient>, prefill: ImportPrefill?) {
        val allBlank = _uiState.value.rows.all { it.rawQuantity.isBlank() && it.name.isBlank() }
        if (allBlank) {
            replaceRows(items, prefill)
        } else {
            _uiState.update { it.copy(dialog = EditorDialog.ConfirmImport(items, prefill)) }
        }
    }

    fun confirmImportReplace() {
        val dialog = _uiState.value.dialog as? EditorDialog.ConfirmImport ?: return
        replaceRows(dialog.items, dialog.prefill)
    }

    fun confirmImportAppend() {
        val dialog = _uiState.value.dialog as? EditorDialog.ConfirmImport ?: return
        val s = _uiState.value
        val existing = s.rows.filter { it.rawQuantity.isNotBlank() || it.name.isNotBlank() }
        val room = MAX_INGREDIENTS - existing.size
        val kept = dialog.items.take(room.coerceAtLeast(0))
        val dropped = dialog.items.size - kept.size
        _uiState.value = applyPrefill(
            s.copy(
                rows = existing + kept.map { it.toEditorRow() },
                dialog = null,
            ),
            dialog.prefill,
        )
        scheduleSave()
        if (dropped > 0) _events.trySend(EditorEvent.IngredientCapReached)
        consumeOfferAiRetry()
    }

    private fun replaceRows(items: List<ParsedIngredient>, prefill: ImportPrefill?) {
        val kept = items.take(MAX_INGREDIENTS)
        _uiState.update { s ->
            applyPrefill(
                s.copy(rows = kept.map { it.toEditorRow() }, dialog = null),
                prefill,
            )
        }
        scheduleSave()
        if (items.size > kept.size) _events.trySend(EditorEvent.IngredientCapReached)
        consumeOfferAiRetry()
    }

    /** Applies scan-detected title/servings to blank fields only. */
    private fun applyPrefill(s: EditorUiState, prefill: ImportPrefill?): EditorUiState {
        if (prefill == null) return s
        var next = s
        if (next.title.isBlank() && !prefill.title.isNullOrBlank()) {
            next = next.copy(title = prefill.title.trim())
        }
        if (next.yieldText.isBlank() && !prefill.servings.isNullOrBlank()) {
            splitYield(prefill.servings)?.let { split ->
                next = next.copy(
                    yieldText = split.amountText,
                    yieldLabel = split.label ?: next.yieldLabel,
                )
            }
        }
        return next
    }

    private fun consumeOfferAiRetry() {
        if (pendingOfferAiRetry) {
            pendingOfferAiRetry = false
            _events.trySend(EditorEvent.ScanLooksPoor)
        }
    }

    private fun ParsedIngredient.toEditorRow() = EditorRow(
        key = ++keyCounter,
        rawQuantity = item.qtyString,
        unit = KitchenUnit.fromKey(item.unit) ?: KitchenUnit.NONE,
        name = item.name,
        needsReview = lowConfidence,
    )

    // ── Scan import ───────────────────────────────────────────────────────────

    fun openScanSheet() {
        if (_uiState.value.importBusy != null) return
        _uiState.update { it.copy(dialog = EditorDialog.ScanSource) }
    }

    fun setUseAiImport(enabled: Boolean) {
        _uiState.update { it.copy(useAiImport = enabled) }
    }

    /** A FileProvider URI for TakePicture, or null when capture is impossible. */
    fun newCameraUri(): Uri? = scanImporter.newCaptureUri()

    /**
     * Entry point for every image source (document scanner, camera, gallery).
     * [deleteWhenDone] is true for temp files we own (scanner output, camera
     * capture) and false for gallery picks. Ignored while an import is busy.
     */
    fun onImageCaptured(uri: Uri, deleteWhenDone: Boolean) {
        if (_uiState.value.importBusy != null) return
        viewModelScope.launch {
            val useAi = _uiState.value.useAiImport && _uiState.value.aiAvailable
            _uiState.update {
                it.copy(
                    dialog = null,
                    importBusy = if (useAi) ImportBusy.AI else ImportBusy.ON_DEVICE,
                )
            }
            val outcome = scanImporter.scan(uri, useAi = useAi, deleteWhenDone = deleteWhenDone)
            applyOutcome(outcome)
        }
    }

    /** "Try AI import" — reuses the JPEG cached during the failed/poor scan. */
    fun retryAiImport() {
        if (_uiState.value.importBusy != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(dialog = null, importBusy = ImportBusy.AI) }
            val outcome = scanImporter.retryWithAi()
            if (outcome != null) applyOutcome(outcome) else _uiState.update { it.copy(importBusy = null) }
        }
    }

    /** "Scan on device" — on-device OCR on the cached JPEG after an AI network error. */
    fun useOnDevice() {
        if (_uiState.value.importBusy != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(dialog = null, importBusy = ImportBusy.ON_DEVICE) }
            val outcome = scanImporter.useOnDevice()
            if (outcome != null) applyOutcome(outcome) else _uiState.update { it.copy(importBusy = null) }
        }
    }

    /** Dismiss the scan-failed dialog and drop the cached JPEG. */
    fun dismissScanFailed() {
        scanImporter.clear()
        _uiState.update { it.copy(dialog = null) }
    }

    private fun applyOutcome(outcome: ScanOutcome) {
        when (outcome) {
            is ScanOutcome.Parsed -> {
                pendingOfferAiRetry = outcome.offerAiRetry
                _uiState.update { it.copy(importBusy = null) }
                applyImport(
                    outcome.items,
                    ImportPrefill(title = outcome.title, servings = outcome.servings),
                )
            }
            is ScanOutcome.Failed -> _uiState.update {
                it.copy(
                    importBusy = null,
                    dialog = EditorDialog.ScanFailed(
                        error = outcome.error,
                        canRetryWithAi = outcome.canRetryWithAi,
                        canUseOnDevice = outcome.canUseOnDevice,
                    ),
                )
            }
            ScanOutcome.PaywallRequired -> _uiState.update {
                it.copy(importBusy = null, dialog = EditorDialog.AiPaywall)
            }
        }
    }

    // ── Clear / delete ────────────────────────────────────────────────────────

    fun requestClear() {
        _uiState.update { it.copy(dialog = EditorDialog.ConfirmClear) }
    }

    fun confirmClear() {
        _uiState.update { it.copy(rows = listOf(newRow()), dialog = null) }
        scheduleSave()
    }

    /** Saves latest edits, deletes via [undoController] (undoable), then navigates up. */
    fun deleteRecipe() {
        appScope.launch {
            saveMutex.withLock {
                saveJob?.cancel()
                doSave()
                deleted = true
                val id = _uiState.value.recipeId
                if (id != 0L) undoController.delete(id)
                _events.send(EditorEvent.NavigateUp)
            }
        }
    }

    fun dismissDialog() {
        if (_uiState.value.dialog is EditorDialog.ScanFailed) scanImporter.clear()
        pendingOfferAiRetry = false
        _uiState.update { it.copy(dialog = null) }
    }

    // ── Saving ────────────────────────────────────────────────────────────────

    private fun scheduleSave() {
        dirty = true
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
        if (!dirty && s.recipeId == 0L) return null
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
        scanImporter.clear()
        flush()
    }
}
