package com.ryxan.recipescaler.feature.scale

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryxan.recipescaler.core.domain.formatting.DisplayMode
import com.ryxan.recipescaler.core.domain.formatting.DisplayPrefs
import com.ryxan.recipescaler.core.domain.formatting.MeasurementSystem
import com.ryxan.recipescaler.core.domain.model.KitchenUnit
import com.ryxan.recipescaler.core.domain.model.Recipe
import com.ryxan.recipescaler.core.domain.model.ScaledIngredient
import com.ryxan.recipescaler.core.domain.model.formatYield
import com.ryxan.recipescaler.core.domain.model.toScaleItem
import com.ryxan.recipescaler.core.domain.parsing.QuantityParser
import com.ryxan.recipescaler.core.domain.repository.RecipeRepository
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor

data class ScaleInputs(
    val servesText: String = "",
    val scaleToText: String = "",
)

/** Which text field a [ScaleMessage] belongs to. */
enum class ScaleField { SERVES, SCALE_TO }

/** User-mappable errors/messages; the UI renders each to a string resource. */
sealed interface ScaleMessage {
    val field: ScaleField
    data class CantRead(val text: String, override val field: ScaleField) : ScaleMessage
    data class EnterAmountAboveZero(override val field: ScaleField) : ScaleMessage
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
    val measurementSystem: MeasurementSystem = MeasurementSystem.MATCH_RECIPE,
    /** False in display modes that ignore the unit system (Decimals, Keep units). */
    val unitsSelectable: Boolean = true,
    /** True when the recipe is gone (deleted) — the screen shows an empty state. */
    val recipeMissing: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ScaleViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: RecipeRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val recipeId = savedStateHandle.get<Long>("recipeId") ?: 0L

    /** Null until the recipe first loads — defaults depend on its yield/ingredients. */
    private val inputs = MutableStateFlow(restoredInputs())
    private val showOriginal = MutableStateFlow(false)
    private val cookMode = savedStateHandle.getStateFlow("cookMode", false)
    private val checkedIds =
        savedStateHandle.getStateFlow<List<String>>("checked", emptyList())

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

    private fun restoredInputs(): ScaleInputs? {
        val serves = savedStateHandle.get<String>(KEY_SERVES) ?: return null
        return ScaleInputs(serves, savedStateHandle.get<String>(KEY_SCALE_TO) ?: "")
    }

    private fun updateInputs(transform: (ScaleInputs) -> ScaleInputs) {
        val current = inputs.value ?: uiState.value.inputs
        val next = transform(current)
        inputs.value = next
        savedStateHandle[KEY_SERVES] = next.servesText
        savedStateHandle[KEY_SCALE_TO] = next.scaleToText
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

    /**
     * Steps the "Scale to" servings target by 1, clamped to
     * [MIN_SERVINGS]..[MAX_SERVINGS]. A fractional target snaps to the next
     * integer in the step direction; an unreadable target restarts from the
     * serves count, and an unreadable serves resets to 1 (same as
     * [applyServings]).
     */
    fun stepServings(direction: Int) {
        val state = uiState.value
        if (state.isLoading || state.recipeMissing) return
        updateInputs { inp ->
            val serves = QuantityParser.parse(inp.servesText)
            val current = QuantityParser.parse(inp.scaleToText)?.takeIf { it > 0 }
                ?: serves?.takeIf { it > 0 }
                ?: MIN_SERVINGS
            val stepped = if (direction > 0) {
                floor(current) + 1.0
            } else {
                ceil(current) - 1.0
            }
            val next = stepped.coerceIn(MIN_SERVINGS, MAX_SERVINGS)
            when {
                next == current -> inp
                serves == null || serves <= 0.0 ->
                    inp.copy(servesText = "1", scaleToText = formatYield(next))
                else -> inp.copy(scaleToText = formatYield(next))
            }
        }
    }

    fun setShowOriginal(v: Boolean) {
        showOriginal.value = v
    }

    fun setMeasurementSystem(system: MeasurementSystem) {
        viewModelScope.launch { settingsRepository.setMeasurementSystem(system) }
    }

    /**
     * Sets/clears a row's "Promote to" unit override (null = Auto). Persisted on
     * the ingredient row; the state re-derives through [RecipeRepository.observe].
     */
    fun promote(ingredientId: String, unit: KitchenUnit?) {
        val id = ingredientId.toLongOrNull() ?: return
        viewModelScope.launch { repository.setPromotedUnit(recipeId, id, unit) }
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

        val promoted = recipe.ingredients.mapNotNull { ing ->
            ing.promotedUnit?.let { ing.id.toString() to it }
        }.toMap()

        val (mode, firstMessage) = modeFor(inp)
        var message = firstMessage
        val outcome = ScaleRecipe.scale(items, mode, prefs, promoted)

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
                rows = (ScaleRecipe.scale(items, ScaleMode.ByFactor(1.0), prefs, promoted)
                        as ScaleOutcome.Success).ingredients
                warning = false
                message = message ?: when (outcome.error) {
                    ScaleError.InvalidYield ->
                        ScaleMessage.EnterAmountAboveZero(ScaleField.SERVES)
                    ScaleError.InvalidFactor ->
                        ScaleMessage.EnterAmountAboveZero(ScaleField.SCALE_TO)
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
            measurementSystem = prefs.system,
            unitsSelectable = prefs.mode == DisplayMode.KITCHEN,
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
            ?: return ScaleMode.ByFactor(1.0) to
                ScaleMessage.CantRead(inp.servesText, ScaleField.SERVES)
        if (serves <= 0) {
            return ScaleMode.ByFactor(1.0) to
                ScaleMessage.EnterAmountAboveZero(ScaleField.SERVES)
        }
        val target = QuantityParser.parse(inp.scaleToText)
            ?: return ScaleMode.ByFactor(1.0) to
                ScaleMessage.CantRead(inp.scaleToText, ScaleField.SCALE_TO)
        if (target <= 0) {
            return ScaleMode.ByFactor(1.0) to
                ScaleMessage.EnterAmountAboveZero(ScaleField.SCALE_TO)
        }
        return ScaleMode.ByYield(serves, target) to null
    }

    // ── Copy / share ──────────────────────────────────────────────────────────

    fun copyText(format: CopyFormat): String {
        val s = uiState.value
        val servings = s.derivedYieldAmount?.let { formatYield(it) }
            ?: "${formatYield(s.factor)}×"
        return RecipeScalerCalculator.generateFormattedRecipe(s.rows, servings, format)
    }

    companion object {
        /** Stepper bounds for the "Scale to" servings target. */
        const val MIN_SERVINGS = 1.0
        const val MAX_SERVINGS = 99.0

        private const val KEY_SERVES = "scaleServes"
        private const val KEY_SCALE_TO = "scaleScaleTo"
    }
}
