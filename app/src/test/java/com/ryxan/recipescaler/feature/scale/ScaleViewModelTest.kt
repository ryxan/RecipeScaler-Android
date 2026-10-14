package com.ryxan.recipescaler.feature.scale

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ryxan.recipescaler.core.domain.formatting.DisplayMode
import com.ryxan.recipescaler.core.domain.formatting.MeasurementSystem
import com.ryxan.recipescaler.core.domain.model.Ingredient
import com.ryxan.recipescaler.core.domain.model.KitchenUnit
import com.ryxan.recipescaler.core.domain.model.Recipe
import com.ryxan.recipescaler.core.domain.scaling.CopyFormat
import com.ryxan.recipescaler.core.domain.scaling.RecipeScalerCalculator
import com.ryxan.recipescaler.testing.FakeRecipeRepository
import com.ryxan.recipescaler.testing.FakeSettingsRepository
import com.ryxan.recipescaler.testing.MainDispatcherRule
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

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
    ) = ScaleViewModel(SavedStateHandle(mapOf("recipeId" to id)), repo, settings)

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
        val vm = ScaleViewModel(handle, repo, settings)

        vm.toggleCookMode()
        val s = vm.uiState.filter { it.cookMode }.first()
        vm.toggleChecked(s.rows[0].id)
        vm.uiState.filter { it.checkedIds.isNotEmpty() }.first()

        // A new VM over the same handle (process death) restores both.
        val vm2 = ScaleViewModel(handle, repo, settings)
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
    fun `the stepper steps the factor on the half grid`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val vm = vm(repo, FakeSettingsRepository(), id)

        vm.uiState.filter { !it.isLoading }.first()
        vm.stepFactor(1)
        var s = vm.uiState.filter { !it.isLoading && it.factor == 1.5 }.first()
        assertEquals("6", s.inputs.scaleToText)

        vm.stepFactor(-1)
        s = vm.uiState.filter { !it.isLoading && it.factor == 1.0 }.first()
        assertEquals("4", s.inputs.scaleToText)
    }

    @Test
    fun `the stepper snaps an off-grid factor`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo, yieldAmount = 15.0)
        val vm = vm(repo, FakeSettingsRepository(), id)

        vm.uiState.filter { !it.isLoading }.first()
        vm.setScaleTo("16")
        vm.uiState.filter { abs(it.factor - 16.0 / 15) < 1e-9 }.first()

        vm.stepFactor(1)
        val s = vm.uiState.filter { !it.isLoading && it.factor == 1.5 }.first()
        assertEquals("22.5", s.inputs.scaleToText)
    }

    @Test
    fun `the stepper clamps at the minimum and maximum`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val vm = vm(repo, FakeSettingsRepository(), id)

        vm.uiState.filter { !it.isLoading }.first()
        vm.stepFactor(-1)
        var s = vm.uiState.filter { !it.isLoading && it.factor == 0.5 }.first()
        assertEquals("2", s.inputs.scaleToText)

        // Already at the minimum — no input write.
        vm.stepFactor(-1)
        s = vm.uiState.filter { !it.isLoading }.first()
        assertEquals(0.5, s.factor, 0.001)
        assertEquals("2", s.inputs.scaleToText)

        vm.setScaleTo("80")
        vm.uiState.filter { it.factor == 20.0 }.first()
        vm.stepFactor(1)
        s = vm.uiState.filter { !it.isLoading }.first()
        assertEquals(20.0, s.factor, 0.001)
        assertEquals("80", s.inputs.scaleToText)
    }

    @Test
    fun `the stepper resets an unreadable serves to 1`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val vm = vm(repo, FakeSettingsRepository(), id)

        vm.uiState.filter { !it.isLoading }.first()
        vm.setServes("abc")
        vm.uiState.filter { it.message is ScaleMessage.CantRead }.first()

        vm.stepFactor(1)
        val s = vm.uiState.filter { !it.isLoading && it.factor == 1.5 }.first()
        assertEquals("1", s.inputs.servesText)
        assertEquals("1.5", s.inputs.scaleToText)
    }

    @Test
    fun `inputs persist through the saved state handle`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val settings = FakeSettingsRepository()
        val handle = SavedStateHandle(mapOf("recipeId" to id))
        val vm = ScaleViewModel(handle, repo, settings)

        vm.uiState.filter { !it.isLoading }.first()
        vm.setScaleTo("8")
        vm.uiState.filter { it.factor == 2.0 }.first()

        // A new VM over the same handle restores the in-session factor.
        val vm2 = ScaleViewModel(handle, repo, settings)
        val s = vm2.uiState
            .filter { !it.isLoading && it.inputs.scaleToText == "8" }
            .first()
        assertEquals(2.0, s.factor, 0.001)
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
