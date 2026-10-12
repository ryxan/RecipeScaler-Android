package com.ryxan.recipescaler.core.domain.importer

import com.ryxan.recipescaler.core.domain.model.KitchenUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiRecipeMapperTest {

    private fun recipe(vararg items: AiIngredient) = AiRecipe(ingredients = items.toList())

    @Test
    fun `maps qty unit and name to a ParsedIngredient`() {
        val items = AiRecipeMapper.map(recipe(AiIngredient(qty = "1 1/2", unit = "cup", name = "flour")))

        assertEquals(1, items.size)
        assertEquals("1 1/2", items[0].item.qtyString)
        assertEquals("cup", items[0].item.unit)
        assertEquals("flour", items[0].item.name)
        assertFalse(items[0].lowConfidence)
        assertTrue(items[0].item.id.isNotBlank())
    }

    @Test
    fun `canonicalizes unit case and plural variants`() {
        val items = AiRecipeMapper.map(
            recipe(
                AiIngredient(qty = "1", unit = "Tbsp", name = "a"),
                AiIngredient(qty = "2", unit = "Cups", name = "b"),
                AiIngredient(qty = "3", unit = "oz.", name = "c"),
                AiIngredient(qty = "4", unit = "lbs", name = "d"),
            ),
        )

        assertEquals(listOf("tbsp", "cup", "oz", "lb"), items.map { it.item.unit })
    }

    @Test
    fun `unknown unit folds to none and keeps original text in the name`() {
        val items = AiRecipeMapper.map(
            recipe(AiIngredient(qty = "2", unit = "bunches", name = "asparagus")),
        )

        assertEquals("none", items[0].item.unit)
        assertEquals("bunches asparagus", items[0].item.name)
    }

    @Test
    fun `blank unit becomes none`() {
        val items = AiRecipeMapper.map(
            recipe(
                AiIngredient(qty = "3", unit = null, name = "eggs"),
                AiIngredient(qty = "1", unit = "  ", name = "apple"),
            ),
        )

        assertEquals(listOf("none", "none"), items.map { it.item.unit })
        assertEquals("eggs", items[0].item.name)
        assertEquals("apple", items[1].item.name)
    }

    @Test
    fun `drops fully blank rows`() {
        val items = AiRecipeMapper.map(
            recipe(
                AiIngredient(qty = null, unit = null, name = null),
                AiIngredient(qty = "", unit = "", name = ""),
                AiIngredient(qty = "1", unit = "cup", name = "flour"),
            ),
        )

        assertEquals(1, items.size)
        assertEquals("flour", items[0].item.name)
    }

    @Test
    fun `caps results at MAX_INGREDIENTS`() {
        val many = List(AiImportConfig.MAX_INGREDIENTS + 10) {
            AiIngredient(qty = "1", unit = "cup", name = "ingredient $it")
        }

        assertEquals(
            AiImportConfig.MAX_INGREDIENTS,
            AiRecipeMapper.map(recipe(*many.toTypedArray())).size,
        )
    }

    @Test
    fun `unparseable qty flags lowConfidence`() {
        val items = AiRecipeMapper.map(
            recipe(
                AiIngredient(qty = "some", unit = "cup", name = "flour"),
                AiIngredient(qty = "1 1/2", unit = "cup", name = "sugar"),
                AiIngredient(qty = "", unit = "none", name = "salt to taste"),
            ),
        )

        assertTrue(items[0].lowConfidence)
        assertFalse(items[1].lowConfidence)
        // A blank qty is a name-only ingredient, not a confidence problem.
        assertFalse(items[2].lowConfidence)
    }

    @Test
    fun `unicode fraction qty parses without lowConfidence`() {
        val items = AiRecipeMapper.map(
            recipe(AiIngredient(qty = "½", unit = "cup", name = "milk")),
        )

        assertFalse(items[0].lowConfidence)
    }

    @Test
    fun `trims overlong fields`() {
        val items = AiRecipeMapper.map(
            recipe(
                AiIngredient(
                    qty = "1".repeat(AiImportConfig.MAX_QTY_LENGTH + 5),
                    unit = "cup",
                    name = "x".repeat(AiImportConfig.MAX_NAME_LENGTH + 10),
                ),
            ),
        )

        assertEquals(AiImportConfig.MAX_QTY_LENGTH, items[0].item.qtyString.length)
        assertEquals(AiImportConfig.MAX_NAME_LENGTH, items[0].item.name.length)
    }

    @Test
    fun `unit enum exposed to the model equals KitchenUnit keys`() {
        assertEquals(
            KitchenUnit.entries.map { it.key },
            AiImportConfig.ALLOWED_UNITS,
        )
        assertTrue(AiImportConfig.ALLOWED_UNITS.contains("none"))
    }
}
