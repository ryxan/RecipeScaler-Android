package com.ryxan.recipescaler.feature.scale

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ryxan.recipescaler.core.domain.formatting.DisplayMode
import com.ryxan.recipescaler.core.domain.formatting.MeasurementSystem
import com.ryxan.recipescaler.core.domain.model.Ingredient
import com.ryxan.recipescaler.core.domain.model.KitchenUnit
import com.ryxan.recipescaler.core.domain.model.Recipe
import com.ryxan.recipescaler.core.domain.model.SavedScaleInputs
import com.ryxan.recipescaler.core.domain.scaling.CopyFormat
import com.ryxan.recipescaler.core.domain.scaling.RecipeScalerCalculator
import com.ryxan.recipescaler.testing.FakeRecipeRepository
import com.ryxan.recipescaler.testing.FakeScaleInputsRepository
import com.ryxan.recipescaler.testing.FakeSettingsRepository
import com.ryxan.recipescaler.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScaleViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private suspend fun seeded(
        repo: FakeRecipeRepository,
        yieldAmount: Double = 4.0,
        yieldLabel: String = "servings",
        ingredients: List<Ingredient> = listOf(
            Ingredient(rawQuantity = "1", unit = KitchenUnit.CUP, name = "Flour"),
        ),
        title: String = "Bread",
    ): Long = repo.save(
        Recipe(
            title = title,
            yieldAmount = yieldAmount,
            yieldLabel = yieldLabel,
            ingredients = ingredients,
        ),
    )

    private fun vm(
        repo: FakeRecipeRepository,
        settings: FakeSettingsRepository,
        id: Long,
        scaleInputs: FakeScaleInputsRepository = FakeScaleInputsRepository(),
        appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
    ) = ScaleViewModel(
        SavedStateHandle(mapOf("recipeId" to id)), repo, settings, scaleInputs, appScope,
    )

    @Test
    fun `defaults come from the recipe yield`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val vm = vm(repo, FakeSettingsRepository(), id)

        val s = vm.uiState.filter { !it.isLoading }.first()
        assertEquals("4", s.inputs.servesText)
        assertEquals("4", s.inputs.scaleToText)
        assertEquals(1.0, s.factor, 0.001)
        assertNull(s.message)
    }

    @Test
    fun `a recipe with no yield defaults to 1`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo, yieldAmount = 0.0)
        val vm = vm(repo, FakeSettingsRepository(), id)

        val s = vm.uiState.filter { !it.isLoading }.first()
        assertEquals("1", s.inputs.servesText)
        assertEquals("1", s.inputs.scaleToText)
        assertEquals(1.0, s.factor, 0.001)
    }

    @Test
    fun `scale to 6 on a 4-serving recipe gives factor 1-5`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val vm = vm(repo, FakeSettingsRepository(), id)

        vm.uiState.filter { !it.isLoading }.first()
        vm.setScaleTo("6")
        val s = vm.uiState.filter { !it.isLoading && it.factor == 1.5 }.first()
        assertEquals(6.0, s.derivedYieldAmount!!, 0.001)
        assertNull(s.message)
    }

    @Test
    fun `editing serves changes the factor`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val vm = vm(repo, FakeSettingsRepository(), id)

        vm.uiState.filter { !it.isLoading }.first()
        vm.setServes("2")
        val s = vm.uiState.filter { !it.isLoading && it.factor == 2.0 }.first()
        assertEquals("2 cup", s.rows[0].displayText)
        assertEquals(4.0, s.derivedYieldAmount!!, 0.001)
    }

    @Test
    fun `servings chip sets the scale-to target and keeps serves`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo, yieldAmount = 15.0)
        val vm = vm(repo, FakeSettingsRepository(), id)

        vm.uiState.filter { !it.isLoading }.first()
        vm.applyServings(1)
        val s = vm.uiState.filter { abs(it.factor - 1.0 / 15) < 1e-9 }.first()
        assertEquals("15", s.inputs.servesText)
        assertEquals("1", s.inputs.scaleToText)
    }

    @Test
    fun `servings chip on a blank serves defaults serves to 1`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val vm = vm(repo, FakeSettingsRepository(), id)

        vm.uiState.filter { !it.isLoading }.first()
        vm.setServes("")
        vm.applyServings(3)
        val s = vm.uiState.filter {
            !it.isLoading && it.inputs.servesText == "1" && it.inputs.scaleToText == "3"
        }.first()
        assertEquals(3.0, s.factor, 0.001)
    }

    @Test
    fun `unreadable text shows CantRead and falls back to 1x`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val vm = vm(repo, FakeSettingsRepository(), id)

        vm.uiState.filter { !it.isLoading }.first()
        vm.setScaleTo("abc")
        val s = vm.uiState.filter { it.message is ScaleMessage.CantRead }.first()
        assertEquals(1.0, s.factor, 0.001)
    }

    @Test
    fun `zero shows an above-zero error`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val vm = vm(repo, FakeSettingsRepository(), id)

        vm.uiState.filter { !it.isLoading }.first()
        vm.setScaleTo("0")
        val s = vm.uiState.filter {
            it.message == ScaleMessage.EnterAmountAboveZero(ScaleField.SCALE_TO)
        }.first()
        assertEquals(1.0, s.factor, 0.001)
    }

    @Test
    fun `fraction and range inputs parse via QuantityParser`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val vm = vm(repo, FakeSettingsRepository(), id)

        vm.uiState.filter { !it.isLoading }.first()
        vm.setScaleTo("½")
        var s = vm.uiState.filter { !it.isLoading && it.factor == 0.125 }.first()
        assertEquals(0.125, s.factor, 0.001)

        vm.setScaleTo("4-6")
        s = vm.uiState.filter { !it.isLoading && it.factor == 1.0 }.first()
        assertNull(s.message)
    }

    @Test
    fun `warning shows when factor is above 4 or below 0-25`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val vm = vm(repo, FakeSettingsRepository(), id)

        vm.uiState.filter { !it.isLoading }.first()
        vm.setScaleTo("20")
        var s = vm.uiState.filter { !it.isLoading && it.factor == 5.0 }.first()
        assertTrue(s.showScaleWarning)

        vm.setScaleTo("0.4")
        s = vm.uiState.filter { it.factor == 0.1 }.first()
        assertTrue(s.showScaleWarning)
    }

    @Test
    fun `cook mode toggles checks and turning it off clears them`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val vm = vm(repo, FakeSettingsRepository(), id)

        vm.toggleCookMode()
        var s = vm.uiState.filter { it.cookMode }.first()
        assertTrue(s.cookMode)

        val rowId = s.rows[0].id
        vm.toggleChecked(rowId)
        s = vm.uiState.filter { rowId in it.checkedIds }.first()
        assertTrue(rowId in s.checkedIds)

        vm.toggleChecked(rowId)
        s = vm.uiState.filter { it.checkedIds.isEmpty() }.first()

        vm.toggleChecked(rowId)
        vm.uiState.filter { it.checkedIds.isNotEmpty() }.first()
        vm.toggleCookMode()
        s = vm.uiState.filter { !it.cookMode }.first()
        assertTrue(s.checkedIds.isEmpty())
    }

    @Test
    fun `cook mode and checks persist in the saved state handle`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val settings = FakeSettingsRepository()
        val handle = SavedStateHandle(mapOf("recipeId" to id))
        val vm = ScaleViewModel(
            handle, repo, settings, FakeScaleInputsRepository(),
            CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        )

        vm.toggleCookMode()
        val s = vm.uiState.filter { it.cookMode }.first()
        vm.toggleChecked(s.rows[0].id)
        vm.uiState.filter { it.checkedIds.isNotEmpty() }.first()

        // A new VM over the same handle (process death) restores both.
        val vm2 = ScaleViewModel(
            handle, repo, settings, FakeScaleInputsRepository(),
            CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        )
        val s2 = vm2.uiState.filter { !it.isLoading }.first()
        assertTrue(s2.cookMode)
        assertEquals(setOf(s.rows[0].id), s2.checkedIds)
    }

    @Test
    fun `a deleted recipe reports recipeMissing`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val vm = vm(repo, FakeSettingsRepository(), id)

        vm.uiState.filter { !it.isLoading }.first()
        repo.delete(id)

        val s = vm.uiState.filter { it.recipeMissing }.first()
        assertTrue(s.recipeMissing)
    }

    @Test
    fun `copyText matches generateFormattedRecipe`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val vm = vm(repo, FakeSettingsRepository(), id)

        val s = vm.uiState.filter { !it.isLoading }.first()
        val expected = RecipeScalerCalculator.generateFormattedRecipe(
            s.rows, "4", CopyFormat.PLAIN,
        )
        assertEquals(expected, vm.copyText(CopyFormat.PLAIN))
    }

    @Test
    fun `the stepper steps the target servings`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val vm = vm(repo, FakeSettingsRepository(), id)

        vm.uiState.filter { !it.isLoading }.first()
        vm.stepServings(1)
        var s = vm.uiState.filter { !it.isLoading && it.inputs.scaleToText == "5" }.first()
        assertEquals(1.25, s.factor, 0.001)

        vm.stepServings(-1)
        s = vm.uiState.filter { !it.isLoading && it.inputs.scaleToText == "4" }.first()
        assertEquals(1.0, s.factor, 0.001)
    }

    @Test
    fun `the stepper snaps a fractional target in the step direction`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val vm = vm(repo, FakeSettingsRepository(), id)

        vm.uiState.filter { !it.isLoading }.first()
        vm.setScaleTo("4.5")
        vm.uiState.filter { it.factor == 1.125 }.first()

        vm.stepServings(1)
        var s = vm.uiState.filter { it.inputs.scaleToText == "5" }.first()
        assertEquals(1.25, s.factor, 0.001)

        vm.setScaleTo("4.5")
        vm.uiState.filter { it.inputs.scaleToText == "4.5" }.first()
        vm.stepServings(-1)
        s = vm.uiState.filter { it.inputs.scaleToText == "4" }.first()
        assertEquals(1.0, s.factor, 0.001)
    }

    @Test
    fun `the stepper clamps at the minimum and maximum servings`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val vm = vm(repo, FakeSettingsRepository(), id)

        vm.uiState.filter { !it.isLoading }.first()
        vm.setScaleTo("1")
        vm.uiState.filter { it.factor == 0.25 }.first()

        // Already at the minimum — no input write.
        vm.stepServings(-1)
        var s = vm.uiState.filter { !it.isLoading }.first()
        assertEquals("1", s.inputs.scaleToText)

        vm.setScaleTo("99")
        vm.uiState.filter { it.factor == 24.75 }.first()
        vm.stepServings(1)
        s = vm.uiState.filter { !it.isLoading }.first()
        assertEquals("99", s.inputs.scaleToText)
    }

    @Test
    fun `the stepper resets an unreadable serves to 1`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val vm = vm(repo, FakeSettingsRepository(), id)

        vm.uiState.filter { !it.isLoading }.first()
        vm.setServes("abc")
        vm.uiState.filter { it.message is ScaleMessage.CantRead }.first()

        vm.stepServings(1)
        val s = vm.uiState.filter { it.inputs.servesText == "1" }.first()
        assertEquals("5", s.inputs.scaleToText)
        assertEquals(5.0, s.factor, 0.001)
    }

    @Test
    fun `the stepper restarts from serves when the target is unreadable`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val vm = vm(repo, FakeSettingsRepository(), id)

        vm.uiState.filter { !it.isLoading }.first()
        vm.setScaleTo("abc")
        vm.uiState.filter { it.message is ScaleMessage.CantRead }.first()

        vm.stepServings(1)
        val s = vm.uiState.filter { it.inputs.scaleToText == "5" }.first()
        assertNull(s.message)
        assertEquals(1.25, s.factor, 0.001)
    }

    @Test
    fun `inputs persist through the saved state handle`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val settings = FakeSettingsRepository()
        val handle = SavedStateHandle(mapOf("recipeId" to id))
        val vm = ScaleViewModel(
            handle, repo, settings, FakeScaleInputsRepository(),
            CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        )

        vm.uiState.filter { !it.isLoading }.first()
        vm.setScaleTo("8")
        vm.uiState.filter { it.factor == 2.0 }.first()

        // A new VM over the same handle restores the in-session factor.
        val vm2 = ScaleViewModel(
            handle, repo, settings, FakeScaleInputsRepository(),
            CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        )
        val s = vm2.uiState
            .filter { !it.isLoading && it.inputs.scaleToText == "8" }
            .first()
        assertEquals(2.0, s.factor, 0.001)
    }

    @Test
    fun `saved scale inputs restore when reopening the recipe`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val scaleInputs = FakeScaleInputsRepository()
        scaleInputs.seed(id, SavedScaleInputs("4", "9"))
        val vm = vm(repo, FakeSettingsRepository(), id, scaleInputs)

        val s = vm.uiState.filter { !it.isLoading }.first()
        assertEquals("4", s.inputs.servesText)
        assertEquals("9", s.inputs.scaleToText)
        assertEquals(2.25, s.factor, 0.001)
    }

    @Test
    fun `saved inputs are per recipe`() = runTest {
        val repo = FakeRecipeRepository()
        val id1 = seeded(repo, title = "One")
        val id2 = seeded(repo, title = "Two")
        val scaleInputs = FakeScaleInputsRepository()
        scaleInputs.seed(id1, SavedScaleInputs("4", "9"))

        val vm2 = vm(repo, FakeSettingsRepository(), id2, scaleInputs)
        val s2 = vm2.uiState.filter { !it.isLoading }.first()
        assertEquals("4", s2.inputs.servesText)
        assertEquals("4", s2.inputs.scaleToText)
    }

    @Test
    fun `saved state handle restore wins over the store`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val scaleInputs = FakeScaleInputsRepository()
        scaleInputs.seed(id, SavedScaleInputs("4", "9"))
        // Process death left newer in-session values in the handle.
        val handle = SavedStateHandle(
            mapOf("recipeId" to id, "scaleServes" to "3", "scaleScaleTo" to "7"),
        )
        val vm = ScaleViewModel(
            handle, repo, FakeSettingsRepository(), scaleInputs,
            CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        )

        val s = vm.uiState.filter { !it.isLoading }.first()
        assertEquals("3", s.inputs.servesText)
        assertEquals("7", s.inputs.scaleToText)
    }

    @Test
    fun `typing scale-to persists it for the recipe`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val scaleInputs = FakeScaleInputsRepository()
        val vm = vm(repo, FakeSettingsRepository(), id, scaleInputs)

        vm.uiState.filter { !it.isLoading }.first()
        vm.setScaleTo("6")
        advanceUntilIdle()

        assertEquals(SavedScaleInputs("4", "6"), scaleInputs.savedFor(id))
    }

    @Test
    fun `typing serves persists the pair`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val scaleInputs = FakeScaleInputsRepository()
        val vm = vm(repo, FakeSettingsRepository(), id, scaleInputs)

        vm.uiState.filter { !it.isLoading }.first()
        vm.setServes("2")
        advanceUntilIdle()

        assertEquals(SavedScaleInputs("2", "4"), scaleInputs.savedFor(id))
    }

    @Test
    fun `servings chip persists the new target`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val scaleInputs = FakeScaleInputsRepository()
        val vm = vm(repo, FakeSettingsRepository(), id, scaleInputs)

        vm.uiState.filter { !it.isLoading }.first()
        vm.applyServings(8)
        advanceUntilIdle()

        assertEquals(SavedScaleInputs("4", "8"), scaleInputs.savedFor(id))
    }

    @Test
    fun `a pending edit is flushed when the view model is cleared`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val scaleInputs = FakeScaleInputsRepository()
        val vm = vm(repo, FakeSettingsRepository(), id, scaleInputs)

        vm.uiState.filter { !it.isLoading }.first()
        // Faster than the debounce window — the store would lose this without the flush.
        vm.setScaleTo("6")
        vm.flushPendingInputs()

        assertEquals(SavedScaleInputs("4", "6"), scaleInputs.savedFor(id))
    }

    @Test
    fun `measurement system changes flow into the state`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val settings = FakeSettingsRepository()
        val vm = vm(repo, settings, id)

        vm.uiState.filter { !it.isLoading }.first()
        vm.setMeasurementSystem(MeasurementSystem.METRIC)
        val s = vm.uiState
            .filter { it.measurementSystem == MeasurementSystem.METRIC }
            .first()
        assertTrue(s.unitsSelectable)
    }

    @Test
    fun `units are not selectable outside kitchen mode`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val settings = FakeSettingsRepository()
        val vm = vm(repo, settings, id)

        vm.uiState.filter { !it.isLoading }.first()
        settings.setDisplayMode(DisplayMode.DECIMAL)
        val s = vm.uiState.filter { !it.isLoading && !it.unitsSelectable }.first()
        assertFalse(s.unitsSelectable)
    }

    // ── "Promote to" ──────────────────────────────────────────────────────────

    @Test
    fun `promote persists the override and re-renders the row`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(
            repo,
            ingredients = listOf(
                Ingredient(rawQuantity = "44", unit = KitchenUnit.CUP, name = "Stock"),
            ),
        )
        val vm = vm(repo, FakeSettingsRepository(), id)

        val s = vm.uiState.filter { !it.isLoading }.first()
        val rowId = s.rows[0].id
        assertEquals("44 cup", s.rows[0].displayText)

        vm.promote(rowId, KitchenUnit.QUART)
        val promoted = vm.uiState
            .filter { it.rows[0].promotedUnit == KitchenUnit.QUART }
            .first()
        assertEquals("11 qt", promoted.rows[0].displayText)

        // The choice lives on the ingredient row, not session state.
        assertEquals(KitchenUnit.QUART, repo.get(id)!!.ingredients[0].promotedUnit)
    }

    @Test
    fun `promotion survives a scale-to change and auto resets`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(
            repo,
            ingredients = listOf(
                Ingredient(rawQuantity = "22", unit = KitchenUnit.CUP, name = "Stock"),
            ),
        )
        val vm = vm(repo, FakeSettingsRepository(), id)

        val s = vm.uiState.filter { !it.isLoading }.first()
        val rowId = s.rows[0].id
        vm.promote(rowId, KitchenUnit.GALLON)
        vm.uiState.filter { it.rows[0].promotedUnit == KitchenUnit.GALLON }.first()
        // 22 cup = 1056 tsp = 1 gal + 1 qt + 2 cup.
        assertEquals("1 gal 1 qt 2 cup", vm.uiState.value.rows[0].displayText)

        // Re-scaling keeps the override (22 cup → 44 cup = 2 gal 3 qt).
        vm.setScaleTo("8")
        val doubled = vm.uiState.filter { it.factor == 2.0 }.first()
        assertEquals("2 gal 3 qt", doubled.rows[0].displayText)

        // "Auto" clears it.
        vm.promote(rowId, null)
        val auto = vm.uiState.filter { it.rows[0].promotedUnit == null }.first()
        assertEquals("44 cup", auto.rows[0].displayText)
        assertNull(repo.get(id)!!.ingredients[0].promotedUnit)
    }

    @Test
    fun `promote ignores an unknown ingredient id`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val vm = vm(repo, FakeSettingsRepository(), id)

        vm.uiState.filter { !it.isLoading }.first()
        vm.promote("not-a-number", KitchenUnit.QUART)
        vm.promote("9999", KitchenUnit.QUART)
        runCurrent()
        assertNull(repo.get(id)!!.ingredients[0].promotedUnit)
    }

    @Test
    fun `promote options flow into the rows`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(
            repo,
            ingredients = listOf(
                Ingredient(rawQuantity = "44", unit = KitchenUnit.CUP, name = "Stock"),
                Ingredient(rawQuantity = "1", unit = KitchenUnit.TSP, name = "Salt"),
            ),
        )
        val vm = vm(repo, FakeSettingsRepository(), id)

        val s = vm.uiState.filter { !it.isLoading }.first()
        assertEquals(
            listOf(KitchenUnit.QUART, KitchenUnit.GALLON, KitchenUnit.ML, KitchenUnit.L),
            s.rows[0].promoteOptions.map { it.unit },
        )
        assertTrue(s.rows[1].promoteOptions.isEmpty())
    }

    @Test
    fun `copy text reflects the promoted unit`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(
            repo,
            ingredients = listOf(
                Ingredient(rawQuantity = "44", unit = KitchenUnit.CUP, name = "Stock"),
            ),
        )
        val vm = vm(repo, FakeSettingsRepository(), id)

        val s = vm.uiState.filter { !it.isLoading }.first()
        vm.promote(s.rows[0].id, KitchenUnit.QUART)
        vm.uiState.filter { it.rows[0].promotedUnit == KitchenUnit.QUART }.first()

        assertTrue(vm.copyText(CopyFormat.PLAIN).contains("11 qt"))
    }

    @Test
    fun `display mode change re-derives rows`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(
            repo,
            ingredients = listOf(
                Ingredient(rawQuantity = "0.5", unit = KitchenUnit.CUP, name = "Sugar"),
            ),
        )
        val settings = FakeSettingsRepository()
        val vm = vm(repo, settings, id)

        vm.uiState.filter { !it.isLoading }.test {
            val kitchen = awaitItem()
            assertEquals("½ cup", kitchen.rows[0].displayText)

            settings.setDisplayMode(DisplayMode.DECIMAL)
            val decimal = awaitItem()
            assertEquals("0.5 cup", decimal.rows[0].displayText)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
