package com.ryxan.recipescaler.feature.scale

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryxan.recipescaler.core.domain.formatting.DisplayPrefs
import com.ryxan.recipescaler.core.domain.model.Recipe
import com.ryxan.recipescaler.core.domain.model.SavedScaleInputs
import com.ryxan.recipescaler.core.domain.model.ScaledIngredient
import com.ryxan.recipescaler.core.domain.model.formatYield
import com.ryxan.recipescaler.core.domain.model.toScaleItem
import com.ryxan.recipescaler.core.domain.parsing.QuantityParser
import com.ryxan.recipescaler.core.domain.repository.RecipeRepository
import com.ryxan.recipescaler.core.domain.repository.ScaleInputsRepository
import com.ryxan.recipescaler.core.domain.repository.SettingsRepository
import com.ryxan.recipescaler.core.domain.scaling.CopyFormat
import com.ryxan.recipescaler.core.domain.scaling.RecipeScalerCalculator
import com.ryxan.recipescaler.core.domain.scaling.ScaleError
import com.ryxan.recipescaler.core.domain.scaling.ScaleMode
import com.ryxan.recipescaler.core.domain.scaling.ScaleOutcome
import com.ryxan.recipescaler.core.domain.scaling.ScaleRecipe
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ScaleInputs(
    val servesText: String = "",
    val scaleToText: String = "",
)

/** User-mappable errors/messages; the UI renders each to a string resource. */
sealed interface ScaleMessage {
    data class CantRead(val text: String) : ScaleMessage
    data object EnterAmountAboveZero : ScaleMessage
}

data class ScaleUiState(
    val isLoading: Boolean = true,
    val recipeId: Long = 0,
    val title: String = "",
    val yieldAmount: Double = 0.0,
    val yieldLabel: String = "",
    val inputs: ScaleInputs = ScaleInputs(),
    val message: ScaleMessage? = null,
    val factor: Double = 1.0,
    val derivedYieldAmount: Double? = null,
    val showScaleWarning: Boolean = false,
    val rows: List<ScaledIngredient> = emptyList(),
    val showOriginal: Boolean = false,
    val cookMode: Boolean = false,
    val checkedIds: Set<String> = emptySet(),
    /** True when the recipe is gone (deleted) — the screen shows an empty state. */
    val recipeMissing: Boolean = false,
)

/** Debounce for persisting Scale View inputs while the user is typing. */
private const val INPUTS_SAVE_DEBOUNCE_MS = 500L

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ScaleViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    repository: RecipeRepository,
    settingsRepository: SettingsRepository,
    private val scaleInputsRepository: ScaleInputsRepository,
) : ViewModel() {

    private val recipeId = savedStateHandle.get<Long>("recipeId") ?: 0L

    /** Null until the recipe first loads — defaults depend on its yield/ingredients. */
    private val inputs = MutableStateFlow<ScaleInputs?>(null)
    private val showOriginal = MutableStateFlow(false)
    private val cookMode = savedStateHandle.getStateFlow("cookMode", false)
    private val checkedIds =
        savedStateHandle.getStateFlow<List<String>>("checked", emptyList())

    init {
        // Restore the last inputs the user left for this recipe. Never clobber
        // values the user already typed if the load lands late.
        viewModelScope.launch {
            val saved = scaleInputsRepository.observe(recipeId).first()
            if (saved != null && inputs.value == null) {
                inputs.value = ScaleInputs(saved.servesText, saved.scaleToText)
            }
        }
        // Persist edits (debounced so typing doesn't write on every keystroke).
        viewModelScope.launch {
            inputs.filterNotNull()
                .debounce(INPUTS_SAVE_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect {
                    scaleInputsRepository.save(
                        recipeId,
                        SavedScaleInputs(it.servesText, it.scaleToText),
                    )
                }
        }
    }

    val uiState: StateFlow<ScaleUiState> = combine(
        repository.observe(recipeId),
        settingsRepository.settings,
        inputs,
        showOriginal,
        combine(cookMode, checkedIds) { c, ids -> c to ids.toSet() },
    ) { recipe, settings, inp, original, (cook, checked) ->
        buildState(recipe, settings.display, inp, original, cook, checked)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScaleUiState())

    // ── Inputs ────────────────────────────────────────────────────────────────

    private fun updateInputs(transform: (ScaleInputs) -> ScaleInputs) {
        val current = inputs.value ?: uiState.value.inputs
        inputs.value = transform(current)
    }

    fun setServes(text: String) = updateInputs { it.copy(servesText = text) }
    fun setScaleTo(text: String) = updateInputs { it.copy(scaleToText = text) }

    /** Sets "Scale to" to [n] servings; a blank/unreadable serves defaults to 1. */
    fun applyServings(n: Int) = updateInputs { inp ->
        val serves = QuantityParser.parse(inp.servesText)
        if (serves == null || serves <= 0) {
            inp.copy(servesText = "1", scaleToText = n.toString())
        } else {
            inp.copy(scaleToText = n.toString())
        }
    }

    fun setShowOriginal(v: Boolean) {
        showOriginal.value = v
    }

    /** Toggles cook/checklist mode; turning it off clears the checked rows. */
    fun toggleCookMode() {
        val on = !cookMode.value
        savedStateHandle["cookMode"] = on
        if (!on) savedStateHandle["checked"] = ArrayList<String>()
    }

    fun toggleChecked(id: String) {
        val current = checkedIds.value
        val next = if (id in current) current - id else current + id
        savedStateHandle["checked"] = ArrayList(next)
    }

    // ── State building ────────────────────────────────────────────────────────

    private fun buildState(
        recipe: Recipe?,
        prefs: DisplayPrefs,
        rawInputs: ScaleInputs?,
        original: Boolean,
        cook: Boolean,
        checked: Set<String>,
    ): ScaleUiState {
        if (recipe == null) {
            return ScaleUiState(
                isLoading = false,
                recipeId = recipeId,
                cookMode = cook,
                checkedIds = checked,
                recipeMissing = true,
            )
        }

        val items = recipe.ingredients.map { it.toScaleItem() }
        val inp = rawInputs ?: defaultInputs(recipe)

        val (mode, firstMessage) = modeFor(inp)
        var message = firstMessage
        val outcome = ScaleRecipe.scale(items, mode, prefs)

        val factor: Double
        val rows: List<ScaledIngredient>
        val warning: Boolean
        when (outcome) {
            is ScaleOutcome.Success -> {
                factor = outcome.factor
                rows = outcome.ingredients
                warning = outcome.showScaleWarning
            }
            is ScaleOutcome.Failure -> {
                factor = 1.0
                rows = (ScaleRecipe.scale(items, ScaleMode.ByFactor(1.0), prefs)
                        as ScaleOutcome.Success).ingredients
                warning = false
                message = message ?: when (outcome.error) {
                    ScaleError.InvalidYield, ScaleError.InvalidFactor ->
                        ScaleMessage.EnterAmountAboveZero
                    else -> null
                }
            }
        }

        val serves = QuantityParser.parse(inp.servesText)

        return ScaleUiState(
            isLoading = false,
            recipeId = recipe.id,
            title = recipe.title,
            yieldAmount = recipe.yieldAmount,
            yieldLabel = recipe.yieldLabel,
            inputs = inp,
            message = message,
            factor = factor,
            derivedYieldAmount = if (serves != null && serves > 0) serves * factor else null,
            showScaleWarning = warning,
            rows = rows,
            showOriginal = original,
            cookMode = cook,
            checkedIds = checked,
        )
    }

    private fun defaultInputs(recipe: Recipe): ScaleInputs {
        val base = if (recipe.yieldAmount > 0) formatYield(recipe.yieldAmount) else "1"
        return ScaleInputs(servesText = base, scaleToText = base)
    }

    private fun modeFor(inp: ScaleInputs): Pair<ScaleMode, ScaleMessage?> {
        if (inp.servesText.isBlank() || inp.scaleToText.isBlank()) {
            return ScaleMode.ByFactor(1.0) to null
        }
        val serves = QuantityParser.parse(inp.servesText)
            ?: return ScaleMode.ByFactor(1.0) to ScaleMessage.CantRead(inp.servesText)
        if (serves <= 0) return ScaleMode.ByFactor(1.0) to ScaleMessage.EnterAmountAboveZero
        val target = QuantityParser.parse(inp.scaleToText)
            ?: return ScaleMode.ByFactor(1.0) to ScaleMessage.CantRead(inp.scaleToText)
        if (target <= 0) return ScaleMode.ByFactor(1.0) to ScaleMessage.EnterAmountAboveZero
        return ScaleMode.ByYield(serves, target) to null
    }

    // ── Copy / share ──────────────────────────────────────────────────────────

    fun copyText(format: CopyFormat): String {
        val s = uiState.value
        val servings = s.derivedYieldAmount?.let { formatYield(it) }
            ?: "${formatYield(s.factor)}×"
        return RecipeScalerCalculator.generateFormattedRecipe(s.rows, servings, format)
    }
}
