package com.ryxan.recipescaler.feature.recipes

import com.ryxan.recipescaler.core.domain.model.Ingredient
import com.ryxan.recipescaler.core.domain.model.KitchenUnit
import com.ryxan.recipescaler.core.domain.model.Recipe
import com.ryxan.recipescaler.testing.FakeRecipeRepository
import com.ryxan.recipescaler.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UndoDeleteControllerTest {

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
    fun `delete removes the recipe and exposes it as pending`() = runTest {
        val repo = FakeRecipeRepository()
        val controller = UndoDeleteController(repo, backgroundScope)
        val id = repo.save(recipe("Bread"))

        controller.delete(id)

        assertNull(repo.get(id))
        assertEquals("Bread", controller.pending.value?.title)
    }

    @Test
    fun `undo restores the deleted recipe and clears pending`() = runTest {
        val repo = FakeRecipeRepository()
        val controller = UndoDeleteController(repo, backgroundScope)
        val id = repo.save(recipe("Bread"))
        val before = repo.get(id)!!

        controller.delete(id)
        controller.undo()
        runCurrent()

        assertNull(controller.pending.value)
        val restored = repo.get(id)
        assertNotNull(restored)
        assertEquals(before.createdAt, restored!!.createdAt)
        assertEquals(before.updatedAt, restored.updatedAt)
    }

    @Test
    fun `dismiss clears pending without restoring`() = runTest {
        val repo = FakeRecipeRepository()
        val controller = UndoDeleteController(repo, backgroundScope)
        val id = repo.save(recipe("Bread"))

        controller.delete(id)
        controller.dismiss()

        assertNull(controller.pending.value)
        assertNull(repo.get(id))
    }

    @Test
    fun `deleting a missing id is a no-op`() = runTest {
        val repo = FakeRecipeRepository()
        val controller = UndoDeleteController(repo, backgroundScope)

        controller.delete(42)

        assertNull(controller.pending.value)
    }
}
