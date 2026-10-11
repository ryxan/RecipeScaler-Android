package com.ryxan.recipescaler.core.domain.scaling

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for what remains in [RecipeScalerCalculator] after the formatting and
 * scaling logic moved to [com.ryxan.recipescaler.core.domain.formatting.KitchenFormatter]
 * and [ScaleRecipe]: the unit catalogue, multiplier, and parseQuantity shim.
 * Formatting tests now live in KitchenFormatterTest.
 */
class RecipeScalerCalculatorTest {

    @Test fun `canonicalUnit maps accessory-bar labels to keys`() {
        assertEquals("tbsp", RecipeScalerCalculator.canonicalUnit("Tbsp"))
        assertEquals("ml",   RecipeScalerCalculator.canonicalUnit("mL"))
        assertEquals("L",    RecipeScalerCalculator.canonicalUnit("l"))
        assertEquals("pt",   RecipeScalerCalculator.canonicalUnit("pt"))
        assertEquals("none", RecipeScalerCalculator.canonicalUnit("none"))
    }

    @Test fun `canonicalUnit passes unknown keys through trimmed`() {
        assertEquals("smidgen", RecipeScalerCalculator.canonicalUnit(" smidgen "))
    }

    @Test fun `ALL_UNITS is derived from KitchenUnit in order`() {
        assertEquals(
            com.ryxan.recipescaler.core.domain.model.KitchenUnit.entries.map { it.key to it.label },
            RecipeScalerCalculator.ALL_UNITS
        )
    }

    @Test fun `calculateMultiplier divides desired by original`() {
        assertEquals(2.0, RecipeScalerCalculator.calculateMultiplier(2.0, 4.0), 0.0001)
        assertEquals(1.0, RecipeScalerCalculator.calculateMultiplier(0.0, 4.0), 0.0001)
    }

    @Test fun `parseQuantity returns zero on failure and value on success`() {
        assertEquals(0.0, RecipeScalerCalculator.parseQuantity("a few"), 0.0001)
        assertEquals(0.0, RecipeScalerCalculator.parseQuantity(""), 0.0001)
        assertEquals(1.5, RecipeScalerCalculator.parseQuantity("1½"), 0.0001)
    }
}
