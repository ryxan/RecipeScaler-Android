package com.ryxan.recipescaler.feature.scale

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryxan.recipescaler.core.domain.formatting.DisplayPrefs
import com.ryxan.recipescaler.core.domain.model.IngredientItem
import com.ryxan.recipescaler.core.domain.model.KitchenUnit
import com.ryxan.recipescaler.core.domain.model.Recipe
import com.ryxan.recipescaler.core.domain.model.ScaledIngredient
import com.ryxan.recipescaler.core.domain.model.UnitFamily
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

enum class ScaleTab { SERVINGS, INGREDIENT, FACTOR }

data class ScaleInputs(
    val tab: ScaleTab = ScaleTab.SERVINGS,
    val targetYieldText: String = "",
    val ingredientId: String? = null,
    val ingredientAmountText: String = "",
    val ingredientUnit: KitchenUnit? = null,
    val factorText: String = "1",
)

data class IngredientChoice(
    val id: String,
    val label: String,
    val qtyText: String,
    val unit: KitchenUnit?,
)

/** User-mappable errors/messages; the UI renders each to a string resource. */
sealed interface ScaleMessage {
    data class CantRead(val text: String) : ScaleMessage
    data object EnterAmountAboveZero : ScaleMessage
    data object IngredientNoQuantity : ScaleMessage
    data class IncompatibleUnits(val from: String, val to: String) : ScaleMessage
    data object SetYieldInEditor : ScaleMessage
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
    val ingredientChoices: List<IngredientChoice> = emptyList(),
    val unitChoices: List<KitchenUnit> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ScaleViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    repository: RecipeRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val recipeId = savedStateHandle.get<Long>("recipeId") ?: 0L

    /** Null until the recipe first loads — defaults depend on its yield/ingredients. */
    private val inputs = MutableStateFlow<ScaleInputs?>(null)
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

    private fun updateInputs(transform: (ScaleInputs) -> ScaleInputs) {
        val current = inputs.value ?: uiState.value.inputs
        inputs.value = transform(current)
    }

    fun setTab(tab: ScaleTab) = updateInputs { it.copy(tab = tab) }
    fun setTargetYield(text: String) = updateInputs { it.copy(targetYieldText = text) }
    fun setIngredientAmount(text: String) = updateInputs { it.copy(ingredientAmountText = text) }
    fun setIngredientUnit(unit: KitchenUnit) = updateInputs { it.copy(ingredientUnit = unit) }
    fun setFactorText(text: String) = updateInputs { it.copy(factorText = text) }

    fun selectIngredient(id: String) = updateInputs { inp ->
        val choice = uiState.value.ingredientChoices.find { it.id == id }
        inp.copy(
            ingredientId = id,
            ingredientAmountText = choice?.qtyText ?: inp.ingredientAmountText,
            ingredientUnit = choice?.unit ?: inp.ingredientUnit,
        )
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
        val choices = items
            .filter { (QuantityParser.parse(it.qtyString) ?: 0.0) > 0.0 }
            .map {
                IngredientChoice(
                    id = it.id,
                    label = choiceLabel(it),
                    qtyText = it.qtyString,
                    unit = KitchenUnit.fromKey(it.unit),
                )
            }

        var inp = rawInputs ?: defaultInputs(recipe, choices)

        var (mode, message) = modeFor(inp, recipe, items, choices)
        var outcome = ScaleRecipe.scale(items, mode, prefs)

        // A stale selection should snap back to the default ingredient.
        if (outcome is ScaleOutcome.Failure && outcome.error is ScaleError.IngredientNotFound) {
            inp = defaultIngredient(choices, inp)
            val retry = modeFor(inp, recipe, items, choices)
            mode = retry.first
            message = retry.second
            outcome = ScaleRecipe.scale(items, mode, prefs)
            inputs.value = inp
        }

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
                message = message ?: when (val e = outcome.error) {
                    ScaleError.InvalidYield, ScaleError.InvalidFactor ->
                        ScaleMessage.EnterAmountAboveZero
                    ScaleError.IngredientHasNoQuantity -> ScaleMessage.IngredientNoQuantity
                    is ScaleError.IncompatibleUnits ->
                        ScaleMessage.IncompatibleUnits(e.from, e.to)
                    ScaleError.IngredientNotFound -> null
                }
            }
        }

        return ScaleUiState(
            isLoading = false,
            recipeId = recipe.id,
            title = recipe.title,
            yieldAmount = recipe.yieldAmount,
            yieldLabel = recipe.yieldLabel,
            inputs = inp,
            message = message,
            factor = factor,
            derivedYieldAmount = if (recipe.yieldAmount > 0) recipe.yieldAmount * factor else null,
            showScaleWarning = warning,
            rows = rows,
            showOriginal = original,
            cookMode = cook,
            checkedIds = checked,
            ingredientChoices = choices,
            unitChoices = unitChoicesFor(inp, items),
        )
    }

    private fun defaultInputs(recipe: Recipe, choices: List<IngredientChoice>): ScaleInputs {
        val base = ScaleInputs(
            tab = if (recipe.yieldAmount > 0) ScaleTab.SERVINGS else ScaleTab.FACTOR,
            targetYieldText = formatYield(recipe.yieldAmount),
            factorText = "1",
        )
        return defaultIngredient(choices, base)
    }

    private fun defaultIngredient(choices: List<IngredientChoice>, inp: ScaleInputs): ScaleInputs {
        val first = choices.firstOrNull() ?: return inp
        return inp.copy(
            ingredientId = first.id,
            ingredientAmountText = first.qtyText,
            ingredientUnit = first.unit,
        )
    }

    private fun modeFor(
        inp: ScaleInputs,
        recipe: Recipe,
        items: List<IngredientItem>,
        choices: List<IngredientChoice>,
    ): Pair<ScaleMode, ScaleMessage?> = when (inp.tab) {
        ScaleTab.SERVINGS -> {
            when {
                recipe.yieldAmount <= 0 ->
                    ScaleMode.ByFactor(1.0) to ScaleMessage.SetYieldInEditor
                inp.targetYieldText.isBlank() -> ScaleMode.ByFactor(1.0) to null
                else -> {
                    val target = QuantityParser.parse(inp.targetYieldText)
                    when {
                        target == null ->
                            ScaleMode.ByFactor(1.0) to ScaleMessage.CantRead(inp.targetYieldText)
                        target <= 0 ->
                            ScaleMode.ByFactor(1.0) to ScaleMessage.EnterAmountAboveZero
                        else -> ScaleMode.ByYield(recipe.yieldAmount, target) to null
                    }
                }
            }
        }
        ScaleTab.INGREDIENT -> {
            val id = inp.ingredientId ?: choices.firstOrNull()?.id
            val selected = items.find { it.id == id }
            when {
                id == null -> ScaleMode.ByFactor(1.0) to null
                inp.ingredientAmountText.isBlank() -> ScaleMode.ByFactor(1.0) to null
                else -> {
                    val amount = QuantityParser.parse(inp.ingredientAmountText)
                    when {
                        amount == null ->
                            ScaleMode.ByFactor(1.0) to
                                ScaleMessage.CantRead(inp.ingredientAmountText)
                        amount <= 0 ->
                            ScaleMode.ByFactor(1.0) to ScaleMessage.EnterAmountAboveZero
                        else -> ScaleMode.ByIngredient(
                            ingredientId = id,
                            targetAmount = amount,
                            targetUnit = (inp.ingredientUnit
                                ?: KitchenUnit.fromKey(selected?.unit ?: "")
                                ?: KitchenUnit.NONE).key,
                        ) to null
                    }
                }
            }
        }
        ScaleTab.FACTOR -> {
            when {
                inp.factorText.isBlank() -> ScaleMode.ByFactor(1.0) to null
                else -> {
                    val factor = QuantityParser.parse(inp.factorText)
                    when {
                        factor == null ->
                            ScaleMode.ByFactor(1.0) to ScaleMessage.CantRead(inp.factorText)
                        factor <= 0 ->
                            ScaleMode.ByFactor(1.0) to ScaleMessage.EnterAmountAboveZero
                        else -> ScaleMode.ByFactor(factor) to null
                    }
                }
            }
        }
    }

    private fun choiceLabel(item: IngredientItem): String {
        val unit = KitchenUnit.fromKey(item.unit)
        return listOfNotNull(
            item.qtyString.ifBlank { null },
            unit?.takeIf { it != KitchenUnit.NONE }?.label,
            item.name.ifBlank { null },
        ).joinToString(" ")
    }

    private fun unitChoicesFor(inp: ScaleInputs, items: List<IngredientItem>): List<KitchenUnit> {
        val id = inp.ingredientId ?: return emptyList()
        val item = items.find { it.id == id } ?: return emptyList()
        val unit = KitchenUnit.fromKey(item.unit) ?: return emptyList()
        return if (unit.family == UnitFamily.COUNT) {
            listOf(unit)
        } else {
            KitchenUnit.entries.filter { it.family == unit.family }
        }
    }

    // ── Copy / share ──────────────────────────────────────────────────────────

    fun copyText(format: CopyFormat): String {
        val s = uiState.value
        val servings = s.derivedYieldAmount?.let { formatYield(it) }
            ?: "${formatYield(s.factor)}×"
        return RecipeScalerCalculator.generateFormattedRecipe(s.rows, servings, format)
    }
}
