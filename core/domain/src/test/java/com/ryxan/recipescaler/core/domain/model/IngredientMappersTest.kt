package com.ryxan.recipescaler.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class IngredientMappersTest {

    @Test
    fun `toScaleItem maps persisted fields`() {
        val item = Ingredient(id = 42, rawQuantity = "1 1/2", unit = KitchenUnit.CUP, name = "Flour")
            .toScaleItem()
        assertEquals("42", item.id)
        assertEquals("1 1/2", item.qtyString)
        assertEquals("cup", item.unit)
        assertEquals("Flour", item.name)
    }

    @Test
    fun `toIngredient maps editor fields with fresh id`() {
        val ingredient = IngredientItem(id = "7", qtyString = "2", unit = "tbsp", name = "Sugar")
            .toIngredient()
        assertEquals(0L, ingredient.id)
        assertEquals("2", ingredient.rawQuantity)
        assertEquals(KitchenUnit.TBSP, ingredient.unit)
        assertEquals("Sugar", ingredient.name)
    }

    @Test
    fun `toIngredient falls back to NONE for unknown unit`() {
        val ingredient = IngredientItem(id = "1", qtyString = "", unit = "smidgen", name = "Salt")
            .toIngredient()
        assertEquals(KitchenUnit.NONE, ingredient.unit)
    }

    @Test
    fun `formatYield whole numbers have no decimal point`() {
        assertEquals("4", formatYield(4.0))
        assertEquals("12", formatYield(12.0))
    }

    @Test
    fun `formatYield trims decimals`() {
        assertEquals("0.5", formatYield(0.5))
        assertEquals("1.25", formatYield(1.25))
        assertEquals("1.26", formatYield(1.256))
    }

    @Test
    fun `formatYield zero and negative render empty`() {
        assertEquals("", formatYield(0.0))
        assertEquals("", formatYield(-1.0))
    }
}
