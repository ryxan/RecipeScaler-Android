package com.ryxan.recipescaler.feature.editor

import androidx.lifecycle.SavedStateHandle
import com.ryxan.recipescaler.core.data.ads.NoopRewardedAds
import com.ryxan.recipescaler.core.domain.model.Ingredient
import com.ryxan.recipescaler.core.domain.model.KitchenUnit
import com.ryxan.recipescaler.core.domain.model.Recipe
import com.ryxan.recipescaler.feature.recipes.UndoDeleteController
import com.ryxan.recipescaler.testing.FakeProRepository
import com.ryxan.recipescaler.testing.FakeRecipeRepository
import com.ryxan.recipescaler.testing.MainDispatcherRule
import com.ryxan.recipescaler.testing.testScanImporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {

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
    fun `a new empty recipe is never inserted`() = runTest {
        val repo = FakeRecipeRepository()
        val handle = SavedStateHandle()
        val vm = EditorViewModel(handle, repo, UndoDeleteController(repo, backgroundScope), testScanImporter(), backgroundScope, FakeProRepository(), NoopRewardedAds())

        advanceTimeBy(1_000)
        assertTrue(repo.observeSummaries("").first().isEmpty())

        vm.flush()
        runCurrent()
        assertTrue(repo.observeSummaries("").first().isEmpty())
    }

    @Test
    fun `typing a title inserts once after debounce then updates the same id`() = runTest {
        val repo = FakeRecipeRepository()
        val handle = SavedStateHandle()
        val vm = EditorViewModel(handle, repo, UndoDeleteController(repo, backgroundScope), testScanImporter(), backgroundScope, FakeProRepository(), NoopRewardedAds())

        vm.onTitleChange("Bread")
        advanceTimeBy(600)
        var summaries = repo.observeSummaries("").first()
        assertEquals(1, summaries.size)
        val id = summaries[0].id
        assertEquals(id, handle.get<Long>("recipeId"))

        vm.onTitleChange("Bread v2")
        advanceTimeBy(600)
        summaries = repo.observeSummaries("").first()
        assertEquals(1, summaries.size)
        assertEquals("Bread v2", summaries[0].title)
        assertEquals(id, summaries[0].id)
    }

    @Test
    fun `yield text persists via QuantityParser`() = runTest {
        val repo = FakeRecipeRepository()
        val vm = EditorViewModel(SavedStateHandle(), repo, UndoDeleteController(repo, backgroundScope), testScanImporter(), backgroundScope, FakeProRepository(), NoopRewardedAds())
        // Yield alone doesn't make a recipe non-empty; give a row a qty first.
        vm.onRowQtyChange(vm.uiState.value.rows[0].key, "1")

        vm.onYieldTextChange("½")
        advanceTimeBy(600)
        val id = repo.observeSummaries("").first()[0].id
        assertEquals(0.5, repo.get(id)?.yieldAmount)

        vm.onYieldTextChange("4-6")
        advanceTimeBy(600)
        assertEquals(4.0, repo.get(id)?.yieldAmount)

        vm.onYieldTextChange("abc")
        advanceTimeBy(600)
        assertEquals(0.0, repo.get(id)?.yieldAmount)
        assertTrue(vm.uiState.value.yieldError)
    }

    @Test
    fun `opening an existing recipe loads it and onCleared flushes immediately`() = runTest {
        val repo = FakeRecipeRepository()
        val id = repo.save(
            recipe("Cookies").copy(
                ingredients = listOf(
                    Ingredient(rawQuantity = "2", unit = KitchenUnit.CUP, name = "Flour"),
                    Ingredient(rawQuantity = "1", unit = KitchenUnit.EACH, name = "Egg"),
                ),
            ),
        )
        val vm = EditorViewModel(SavedStateHandle(mapOf("recipeId" to id)), repo, UndoDeleteController(repo, backgroundScope), testScanImporter(), backgroundScope, FakeProRepository(), NoopRewardedAds())

        val state = vm.uiState.value
        assertEquals("Cookies", state.title)
        assertEquals("4", state.yieldText)
        assertEquals(listOf("Flour", "Egg"), state.rows.map { it.name })

        vm.onTitleChange("Cookies v2")
        vm.flush() // flush without waiting for the debounce
        runCurrent()
        assertEquals("Cookies v2", repo.get(id)?.title)
    }

    @Test
    fun `paste into blank rows replaces silently`() = runTest {
        val repo = FakeRecipeRepository()
        val vm = EditorViewModel(SavedStateHandle(), repo, UndoDeleteController(repo, backgroundScope), testScanImporter(), backgroundScope, FakeProRepository(), NoopRewardedAds())

        vm.importText("1 cup flour\n2 eggs")
        val s = vm.uiState.value
        assertNull(s.dialog)
        assertEquals(listOf("Flour", "Eggs"), s.rows.map { it.name })
        assertEquals(KitchenUnit.CUP, s.rows[0].unit)
    }

    @Test
    fun `paste into non-blank rows confirms then replaces or appends`() = runTest {
        val repo = FakeRecipeRepository()
        val vm = EditorViewModel(SavedStateHandle(), repo, UndoDeleteController(repo, backgroundScope), testScanImporter(), backgroundScope, FakeProRepository(), NoopRewardedAds())

        vm.importText("1 cup flour")            // silent: rows were blank
        vm.importText("2 eggs\n1 tsp salt")     // now non-blank → confirm dialog
        val dialog = vm.uiState.value.dialog
        assertTrue(dialog is EditorDialog.ConfirmImport)

        vm.confirmImportAppend()
        assertEquals(listOf("Flour", "Eggs", "Salt"), vm.uiState.value.rows.map { it.name })

        vm.importText("3 cups sugar")
        vm.confirmImportReplace()
        assertEquals(listOf("Sugar"), vm.uiState.value.rows.map { it.name })
    }

    @Test
    fun `append truncates at the cap and reports dropped rows`() = runTest {
        val repo = FakeRecipeRepository()
        val vm = EditorViewModel(SavedStateHandle(), repo, UndoDeleteController(repo, backgroundScope), testScanImporter(), backgroundScope, FakeProRepository(), NoopRewardedAds())

        val first = (1..48).joinToString("\n") { "1 cup ingredient$it" }
        vm.importText(first)
        assertEquals(48, vm.uiState.value.rows.size)

        val second = (1..5).joinToString("\n") { "2 eggs batch$it" }
        vm.importText(second)
        vm.confirmImportAppend()

        assertEquals(MAX_INGREDIENTS, vm.uiState.value.rows.size)
        val event = vm.events.first()
        assertEquals(EditorEvent.IngredientCapReached, event)
    }

    @Test
    fun `a session-created recipe emptied is deleted on flush but a pre-existing one is not`() = runTest {
        val repo = FakeRecipeRepository()

        // Created in session, then emptied → deleted.
        val vm = EditorViewModel(SavedStateHandle(), repo, UndoDeleteController(repo, backgroundScope), testScanImporter(), backgroundScope, FakeProRepository(), NoopRewardedAds())
        vm.onTitleChange("Temp")
        advanceTimeBy(600)
        val id = repo.observeSummaries("").first()[0].id
        vm.onTitleChange("")
        vm.flush()
        runCurrent()
        assertNull(repo.get(id))

        // Pre-existing, then emptied → kept.
        val existingId = repo.save(recipe("Keep me"))
        val vm2 = EditorViewModel(
            SavedStateHandle(mapOf("recipeId" to existingId)),
            repo,
            UndoDeleteController(repo, backgroundScope),
            testScanImporter(),
            backgroundScope,
            FakeProRepository(),
            NoopRewardedAds(),
        )
        val key = vm2.uiState.value.rows[0].key
        vm2.onTitleChange("")
        vm2.onRowQtyChange(key, "")
        vm2.onRowNameChange(key, "")
        vm2.flush()
        runCurrent()
        assertEquals("Keep me", repo.get(existingId)?.title)
    }

    @Test
    fun `delete removes the recipe and navigates up`() = runTest {
        val repo = FakeRecipeRepository()
        val vm = EditorViewModel(SavedStateHandle(), repo, UndoDeleteController(repo, backgroundScope), testScanImporter(), backgroundScope, FakeProRepository(), NoopRewardedAds())
        vm.onTitleChange("Gone")
        advanceTimeBy(600)
        val id = repo.observeSummaries("").first()[0].id

        vm.deleteRecipe()
        runCurrent()
        assertNull(repo.get(id))
        assertEquals(EditorEvent.NavigateUp, vm.events.first())
    }

    @Test
    fun `flush after delete does not resurrect the recipe or crash`() = runTest {
        val repo = FakeRecipeRepository()
        val vm = EditorViewModel(SavedStateHandle(), repo, UndoDeleteController(repo, backgroundScope), testScanImporter(), backgroundScope, FakeProRepository(), NoopRewardedAds())
        vm.onTitleChange("Gone")
        advanceTimeBy(600)
        val id = repo.observeSummaries("").first()[0].id

        vm.deleteRecipe()
        runCurrent()
        assertNull(repo.get(id))

        vm.flush() // simulates onCleared racing the delete
        runCurrent()
        assertNull(repo.get(id))
        assertTrue(repo.observeSummaries("").first().isEmpty())
    }

    @Test
    fun `a recipe deleted elsewhere is not resurrected by the debounced save`() = runTest {
        val repo = FakeRecipeRepository()
        val existingId = repo.save(recipe("Doomed"))
        val vm = EditorViewModel(
            SavedStateHandle(mapOf("recipeId" to existingId)),
            repo,
            UndoDeleteController(repo, backgroundScope),
            testScanImporter(),
            backgroundScope,
            FakeProRepository(),
            NoopRewardedAds(),
        )

        repo.delete(existingId)
        vm.onTitleChange("Doomed v2")
        advanceTimeBy(600)
        runCurrent()
        assertTrue(repo.observeSummaries("").first().isEmpty())

        vm.flush()
        runCurrent()
        assertTrue(repo.observeSummaries("").first().isEmpty())
    }

    @Test
    fun `prepareToScale flushes and returns the id or null when empty`() = runTest {
        val repo = FakeRecipeRepository()
        val vm = EditorViewModel(SavedStateHandle(), repo, UndoDeleteController(repo, backgroundScope), testScanImporter(), backgroundScope, FakeProRepository(), NoopRewardedAds())
        assertNull(vm.prepareToScale())

        vm.onTitleChange("Bread")
        val id = vm.prepareToScale()
        assertNotNull(id)
        assertNotNull(repo.get(id!!))
    }

    @Test
    fun `deleteRecipe saves latest edits first so undo restores them`() = runTest {
        val repo = FakeRecipeRepository()
        val undo = UndoDeleteController(repo, backgroundScope)
        val id = repo.save(recipe("Cookies"))
        val vm = EditorViewModel(
            SavedStateHandle(mapOf("recipeId" to id)),
            repo,
            undo,
            testScanImporter(),
            backgroundScope,
            FakeProRepository(),
            NoopRewardedAds(),
        )

        vm.onTitleChange("Cookies v2")
        vm.deleteRecipe()
        runCurrent()

        assertNull(repo.get(id))
        assertEquals("Cookies v2", undo.pending.value?.title)
        assertEquals(EditorEvent.NavigateUp, vm.events.first())

        undo.undo()
        runCurrent()
        assertEquals("Cookies v2", repo.get(id)?.title)
    }

    @Test
    fun `moveRow moves a row to the target position and ignores unknown keys`() = runTest {
        val repo = FakeRecipeRepository()
        val vm = EditorViewModel(
            SavedStateHandle(),
            repo,
            UndoDeleteController(repo, backgroundScope),
            testScanImporter(),
            backgroundScope,
            FakeProRepository(),
            NoopRewardedAds(),
        )
        vm.importText("1 cup flour\n2 eggs\n1 tsp salt")
        val keys = vm.uiState.value.rows.map { it.key }

        vm.moveRow(keys[2], keys[0])
        assertEquals(listOf("Salt", "Flour", "Eggs"), vm.uiState.value.rows.map { it.name })

        vm.moveRow(999L, keys[0])
        assertEquals(listOf("Salt", "Flour", "Eggs"), vm.uiState.value.rows.map { it.name })
    }

    @Test
    fun `inferFromName splits quantity and unit out of the name`() = runTest {
        val repo = FakeRecipeRepository()
        val vm = EditorViewModel(
            SavedStateHandle(),
            repo,
            UndoDeleteController(repo, backgroundScope),
            testScanImporter(),
            backgroundScope,
            FakeProRepository(),
            NoopRewardedAds(),
        )
        val key = vm.uiState.value.rows[0].key
        vm.onRowNameChange(key, "2 cups flour")
        vm.inferFromName(key)

        val row = vm.uiState.value.rows[0]
        assertEquals("2", row.rawQuantity)
        assertEquals(KitchenUnit.CUP, row.unit)
        assertEquals("Flour", row.name)
        assertTrue(row.key != key) // fresh key so the qty field picks up the value
    }

    @Test
    fun `inferFromName handles fractions and bare quantities`() = runTest {
        val repo = FakeRecipeRepository()
        val vm = EditorViewModel(
            SavedStateHandle(),
            repo,
            UndoDeleteController(repo, backgroundScope),
            testScanImporter(),
            backgroundScope,
            FakeProRepository(),
            NoopRewardedAds(),
        )
        val k1 = vm.uiState.value.rows[0].key
        vm.onRowNameChange(k1, "1 1/2 tsp salt")
        vm.inferFromName(k1)
        var row = vm.uiState.value.rows[0]
        assertEquals("1 1/2", row.rawQuantity)
        assertEquals(KitchenUnit.TSP, row.unit)
        assertEquals("Salt", row.name)

        val k2 = vm.addRow()
        vm.onRowNameChange(k2, "3 eggs")
        vm.inferFromName(k2)
        row = vm.uiState.value.rows[1]
        assertEquals("3", row.rawQuantity)
        assertEquals("Eggs", row.name)
    }

    @Test
    fun `inferFromName is a no-op without a quantity or with an existing qty`() = runTest {
        val repo = FakeRecipeRepository()
        val vm = EditorViewModel(
            SavedStateHandle(),
            repo,
            UndoDeleteController(repo, backgroundScope),
            testScanImporter(),
            backgroundScope,
            FakeProRepository(),
            NoopRewardedAds(),
        )
        val key = vm.uiState.value.rows[0].key
        vm.onRowNameChange(key, "flour")
        vm.inferFromName(key)
        var row = vm.uiState.value.rows[0]
        assertEquals("flour", row.name)
        assertEquals("", row.rawQuantity)
        assertEquals(key, row.key)

        vm.onRowNameChange(key, "salt to taste")
        vm.inferFromName(key)
        row = vm.uiState.value.rows[0]
        assertEquals("salt to taste", row.name)
        assertEquals("", row.rawQuantity)

        val k2 = vm.addRow()
        vm.onRowQtyChange(k2, "5")
        vm.onRowNameChange(k2, "2 cups flour")
        vm.inferFromName(k2)
        row = vm.uiState.value.rows[1]
        assertEquals("5", row.rawQuantity)
        assertEquals("2 cups flour", row.name)
        assertEquals(k2, row.key)
    }

    @Test
    fun `initialTitle prefills the title and autofocuses the first row without saving`() = runTest {
        val repo = FakeRecipeRepository()
        val handle = SavedStateHandle(mapOf("initialTitle" to "Quick scale"))
        val vm = EditorViewModel(
            handle,
            repo,
            UndoDeleteController(repo, backgroundScope),
            testScanImporter(),
            backgroundScope,
            FakeProRepository(),
            NoopRewardedAds(),
        )

        val s = vm.uiState.value
        assertEquals("Quick scale", s.title)
        assertEquals(s.rows[0].key, s.autoFocusKey)

        // Backing out without edits saves nothing.
        vm.flush()
        runCurrent()
        assertTrue(repo.observeSummaries("").first().isEmpty())
    }
}
