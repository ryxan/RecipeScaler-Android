package com.ryxan.recipescaler.feature.recipes

import app.cash.turbine.test
import com.ryxan.recipescaler.core.domain.model.Ingredient
import com.ryxan.recipescaler.core.domain.model.KitchenUnit
import com.ryxan.recipescaler.core.domain.model.Recipe
import com.ryxan.recipescaler.testing.FakeRecipeRepository
import com.ryxan.recipescaler.testing.MainDispatcherRule
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RecipeListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun recipe(title: String) = Recipe(
        title = title,
        yieldAmount = 4.0,
        yieldLabel = "servings",
        ingredients = listOf(
            Ingredient(rawQuantity = "1", unit = KitchenUnit.CUP, name = "Flour"),
        ),
    )

    @Test
    fun `emits saved summaries`() = runTest {
        val repo = FakeRecipeRepository()
        repo.save(recipe("Bread"))
        repo.save(recipe("Cake"))
        val vm = RecipeListViewModel(repo)

        val state = vm.uiState.filter { !it.isLoading }.first()
        // updatedAt desc: most recently saved first.
        assertEquals(listOf("Cake", "Bread"), state.recipes.map { it.title })
    }

    @Test
    fun `query filters summaries`() = runTest {
        val repo = FakeRecipeRepository()
        repo.save(recipe("Bread"))
        repo.save(recipe("Cake"))
        val vm = RecipeListViewModel(repo)

        vm.uiState.filter { !it.isLoading }.test {
            awaitItem() // initial, unfiltered

            vm.onQueryChange("bre")
            // A stale (old query, filtered list) emission is possible while the
            // flatMapLatest inner flow switches; wait for the settled state.
            var s = awaitItem()
            while (s.query != "bre" || s.recipes.map { it.title } != listOf("Bread")) {
                s = awaitItem()
            }
            assertEquals("bre", s.query)
            assertEquals(listOf("Bread"), s.recipes.map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty repository emits empty list`() = runTest {
        val vm = RecipeListViewModel(FakeRecipeRepository())

        val state = vm.uiState.filter { !it.isLoading }.first()
        assertTrue(state.recipes.isEmpty())
    }
}
