package com.ryxan.recipescaler.feature.scale

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ryxan.recipescaler.core.domain.formatting.DisplayMode
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
import org.junit.Assert.assertEquals
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
    fun `servings 8 on a 4-serving recipe gives factor 2`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val vm = vm(repo, FakeSettingsRepository(), id)

        vm.setTargetYield("8")
        val s = vm.uiState.filter { !it.isLoading && it.factor == 2.0 }.first()
        assertEquals("2 cup", s.rows[0].displayText)
        assertEquals(8.0, s.derivedYieldAmount!!, 0.001)
        assertNull(s.message)
    }

    @Test
    fun `servings half and range parse via QuantityParser`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val vm = vm(repo, FakeSettingsRepository(), id)

        vm.setTargetYield("½")
        var s = vm.uiState.filter { !it.isLoading && it.factor == 0.125 }.first()
        assertEquals(0.125, s.factor, 0.001)

        vm.setTargetYield("4-6")
        s = vm.uiState.filter { !it.isLoading && it.factor == 1.0 }.first()
        assertNull(s.message)
    }

    @Test
    fun `ingredient mode scales by target amount and converts units`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(
            repo,
            ingredients = listOf(
                Ingredient(rawQuantity = "100", unit = KitchenUnit.G, name = "Butter"),
                Ingredient(rawQuantity = "1", unit = KitchenUnit.CUP, name = "Flour"),
            ),
        )
        val vm = vm(repo, FakeSettingsRepository(), id)

        vm.setTab(ScaleTab.INGREDIENT)
        // Default selection is the first parseable row (the butter, 100 g).
        vm.setIngredientAmount("200")
        var s = vm.uiState.filter { !it.isLoading && it.factor == 2.0 }.first()
        assertEquals("200 g", s.rows[0].displayText)

        // ~7.05 oz ≈ 200 g → factor ≈ 2.
        vm.setIngredientUnit(KitchenUnit.OZ)
        vm.setIngredientAmount("7.05")
        s = vm.uiState.filter { !it.isLoading && kotlin.math.abs(it.factor - 2.0) < 0.02 }.first()
        assertEquals(2.0, s.factor, 0.02)
    }

    @Test
    fun `factor input and chips`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val vm = vm(repo, FakeSettingsRepository(), id)

        vm.setTab(ScaleTab.FACTOR)
        vm.setFactorText("3")
        var s = vm.uiState.filter { !it.isLoading && it.factor == 3.0 }.first()

        vm.setFactorText("1 1/2")
        s = vm.uiState.filter { it.factor == 1.5 }.first()

        vm.setFactorText("abc")
        s = vm.uiState.filter { it.message is ScaleMessage.CantRead }.first()
        assertEquals(1.0, s.factor, 0.001)
    }

    @Test
    fun `warning shows when factor is above 4 or below 0-25`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo)
        val vm = vm(repo, FakeSettingsRepository(), id)

        vm.setFactorText("5")
        vm.setTab(ScaleTab.FACTOR)
        var s = vm.uiState.filter { !it.isLoading && it.factor == 5.0 }.first()
        assertTrue(s.showScaleWarning)

        vm.setFactorText("0.1")
        s = vm.uiState.filter { it.factor == 0.1 }.first()
        assertTrue(s.showScaleWarning)
    }

    @Test
    fun `yield of zero defaults to factor tab and servings shows guidance`() = runTest {
        val repo = FakeRecipeRepository()
        val id = seeded(repo, yieldAmount = 0.0)
        val vm = vm(repo, FakeSettingsRepository(), id)

        var s = vm.uiState.filter { !it.isLoading }.first()
        assertEquals(ScaleTab.FACTOR, s.inputs.tab)

        vm.setTab(ScaleTab.SERVINGS)
        s = vm.uiState.filter { it.message == ScaleMessage.SetYieldInEditor }.first()
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
