package com.ryxan.recipescaler.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.ryxan.recipescaler.core.data.db.IngredientEntity
import com.ryxan.recipescaler.core.data.db.RecipeDao
import com.ryxan.recipescaler.core.data.db.RecipeDatabase
import com.ryxan.recipescaler.core.domain.model.Ingredient
import com.ryxan.recipescaler.core.domain.model.KitchenUnit
import com.ryxan.recipescaler.core.domain.model.Recipe
import com.ryxan.recipescaler.core.domain.time.Clock
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomRecipeRepositoryTest {

    private lateinit var db: RecipeDatabase
    private lateinit var dao: RecipeDao
    private lateinit var repo: RoomRecipeRepository
    private var now = 0L
    private val clock = Clock { now }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.recipeDao()
        now = 1_000L
        repo = RoomRecipeRepository(dao, db, clock)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun recipe(
        title: String = "Pancakes",
        yieldAmount: Double = 4.0,
        yieldLabel: String = "servings",
        ingredients: List<Ingredient> = listOf(
            Ingredient(rawQuantity = "1 1/2", unit = KitchenUnit.CUP, name = "flour"),
            Ingredient(rawQuantity = "½", unit = KitchenUnit.TSP, name = "salt", note = "fine sea"),
            Ingredient(rawQuantity = "2", unit = KitchenUnit.EACH, name = "eggs", note = null),
        ),
    ) = Recipe(
        title = title,
        yieldAmount = yieldAmount,
        yieldLabel = yieldLabel,
        ingredients = ingredients,
    )

    @Test
    fun `insert returns id and persists ingredients in order`() = runTest {
        val id = repo.save(recipe())
        assertNotEquals(0L, id)

        val loaded = repo.get(id)!!
        assertEquals("Pancakes", loaded.title)
        assertEquals(4.0, loaded.yieldAmount, 0.0)
        assertEquals("servings", loaded.yieldLabel)
        assertEquals(1_000L, loaded.createdAt)
        assertEquals(1_000L, loaded.updatedAt)
        assertEquals(listOf("flour", "salt", "eggs"), loaded.ingredients.map { it.name })
        assertEquals("1 1/2", loaded.ingredients[0].rawQuantity)
        assertEquals("½", loaded.ingredients[1].rawQuantity)
        assertEquals(KitchenUnit.TSP, loaded.ingredients[1].unit)
        assertEquals("fine sea", loaded.ingredients[1].note)
        assertNull(loaded.ingredients[2].note)
        assertTrue(loaded.ingredients.all { it.id != 0L })
    }

    @Test
    fun `observe emits updates after save and null after delete`() = runTest {
        val id = repo.save(recipe())
        repo.observe(id).test {
            assertEquals("Pancakes", awaitItem()!!.title)

            now = 2_000L
            repo.save(repo.get(id)!!.copy(title = "Waffles"))
            assertEquals("Waffles", awaitItem()!!.title)

            repo.delete(id)
            assertNull(awaitItem())
        }
    }

    @Test
    fun `observe emits null for missing id`() = runTest {
        repo.observe(42).test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun `update replaces ingredients keeps createdAt and bumps updatedAt`() = runTest {
        val id = repo.save(recipe())
        val original = repo.get(id)!!

        now = 5_000L
        val updated = original.copy(
            ingredients = listOf(
                Ingredient(rawQuantity = "3", unit = KitchenUnit.CUP, name = "oats"),
                Ingredient(rawQuantity = "1", unit = KitchenUnit.TBSP, name = "butter", note = "melted"),
            ),
        )
        repo.save(updated)

        val loaded = repo.get(id)!!
        assertEquals(1_000L, loaded.createdAt)
        assertEquals(5_000L, loaded.updatedAt)
        assertEquals(listOf("oats", "butter"), loaded.ingredients.map { it.name })
        assertEquals(2, dao.ingredientCount(id))
    }

    @Test
    fun `update of nonexistent id throws`() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { repo.save(recipe().copy(id = 99)) }
        }
    }

    @Test
    fun `delete cascades to ingredients and is no-op for missing id`() = runTest {
        val id = repo.save(recipe())
        assertEquals(3, dao.ingredientCount(id))

        repo.delete(id)
        assertNull(repo.get(id))
        assertEquals(0, dao.ingredientCount(id))

        repo.delete(id)
        repo.delete(12345)
    }

    @Test
    fun `summaries ordered by updatedAt desc with ingredient counts`() = runTest {
        val a = repo.save(recipe(title = "Alpha"))
        val b = repo.save(recipe(title = "Beta", ingredients = emptyList()))
        now = 2_000L
        repo.save(repo.get(a)!!.copy(title = "Alpha v2"))

        repo.observeSummaries().test {
            val list = awaitItem()
            assertEquals(listOf(a, b), list.map { it.id })
            assertEquals(3, list[0].ingredientCount)
            assertEquals(0, list[1].ingredientCount)
            assertEquals(2_000L, list[0].updatedAt)
        }
    }

    @Test
    fun `search is case-insensitive`() = runTest {
        repo.save(recipe(title = "Banana Bread"))
        repo.save(recipe(title = "pancakes"))

        repo.observeSummaries("BANANA").test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("Banana Bread", list[0].title)
        }
    }

    @Test
    fun `query containing LIKE wildcards matches literally`() = runTest {
        repo.save(recipe(title = "100% whole wheat"))
        repo.save(recipe(title = "1000 island dressing"))
        repo.save(recipe(title = "a_b test"))

        repo.observeSummaries("100%").test {
            val list = awaitItem()
            assertEquals(listOf("100% whole wheat"), list.map { it.title })
        }
        repo.observeSummaries("a_b").test {
            val list = awaitItem()
            assertEquals(listOf("a_b test"), list.map { it.title })
        }
    }

    @Test
    fun `unknown unitKey maps to NONE`() = runTest {
        val id = repo.save(recipe(ingredients = emptyList()))
        dao.insertIngredients(
            listOf(
                IngredientEntity(
                    recipeId = id,
                    position = 0,
                    rawQuantity = "1",
                    unitKey = "furlong",
                    name = "mystery",
                    note = null,
                )
            )
        )
        val loaded = repo.get(id)!!
        assertEquals(KitchenUnit.NONE, loaded.ingredients[0].unit)
    }
}
