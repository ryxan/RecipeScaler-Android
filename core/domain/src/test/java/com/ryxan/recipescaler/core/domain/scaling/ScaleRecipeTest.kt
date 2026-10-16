package com.ryxan.recipescaler.core.domain.scaling

import com.ryxan.recipescaler.core.domain.formatting.DisplayMode
import com.ryxan.recipescaler.core.domain.formatting.DisplayPrefs
import com.ryxan.recipescaler.core.domain.model.IngredientItem
import com.ryxan.recipescaler.core.domain.model.KitchenUnit
import com.ryxan.recipescaler.core.domain.model.ScaleStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [ScaleRecipe] — factor computation for all three modes, the
 * >4× / <¼× warning, per-row statuses, and copy-list output. These restore the
 * behaviours previously covered by RecipeScalerUiStateTest.
 */
class ScaleRecipeTest {

    private fun ingredient(id: String, qty: String, unit: String, name: String = "item") =
        IngredientItem(id = id, qtyString = qty, unit = unit, name = name)

    private fun success(outcome: ScaleOutcome): ScaleOutcome.Success {
        assertTrue("expected Success, got $outcome", outcome is ScaleOutcome.Success)
        return outcome as ScaleOutcome.Success
    }

    private fun failure(outcome: ScaleOutcome): ScaleError {
        assertTrue("expected Failure, got $outcome", outcome is ScaleOutcome.Failure)
        return (outcome as ScaleOutcome.Failure).error
    }

    // ── ByYield ───────────────────────────────────────────────────────────────

    @Test fun `byYield doubles servings`() {
        val outcome = ScaleRecipe.scale(
            listOf(ingredient("1", "1", "cup", "flour")),
            ScaleMode.ByYield(original = 2.0, target = 4.0),
        )
        val s = success(outcome)
        assertEquals(2.0, s.factor, 0.0001)
        assertEquals(1, s.ingredients.size)
        assertEquals("flour", s.ingredients.first().name)
        assertEquals("2 cup", s.ingredients.first().displayText)
        assertEquals(ScaleStatus.SCALED, s.ingredients.first().status)
    }

    @Test fun `byYield zero target is InvalidYield`() {
        val outcome = ScaleRecipe.scale(
            listOf(ingredient("1", "1", "cup", "flour")),
            ScaleMode.ByYield(original = 4.0, target = 0.0),
        )
        assertEquals(ScaleError.InvalidYield, failure(outcome))
    }

    @Test fun `byYield zero original is InvalidYield`() {
        val outcome = ScaleRecipe.scale(
            listOf(ingredient("1", "1", "cup", "flour")),
            ScaleMode.ByYield(original = 0.0, target = 4.0),
        )
        assertEquals(ScaleError.InvalidYield, failure(outcome))
    }

    // ── Scale warning ─────────────────────────────────────────────────────────

    private fun warning(factor: Double) = success(
        ScaleRecipe.scale(
            listOf(ingredient("1", "1", "cup", "flour")),
            ScaleMode.ByFactor(factor),
        )
    ).showScaleWarning

    @Test fun `warning above 4x`() = assertTrue(warning(4.5))
    @Test fun `no warning at exactly 4x`() = assertFalse(warning(4.0))
    @Test fun `no warning at 3x`() = assertFalse(warning(3.0))
    @Test fun `warning below quarter`() = assertTrue(warning(0.2))
    @Test fun `no warning at exactly quarter`() = assertFalse(warning(0.25))

    // ── Display prefs propagate ───────────────────────────────────────────────

    @Test fun `4 tbsp in kitchen mode becomes quarter cup`() {
        val s = success(
            ScaleRecipe.scale(listOf(ingredient("1", "4", "tbsp", "sugar")), ScaleMode.ByFactor(1.0))
        )
        assertEquals("¼ cup", s.ingredients.first().displayText)
    }

    @Test fun `4 tbsp in keep-units stays tbsp`() {
        val s = success(
            ScaleRecipe.scale(
                listOf(ingredient("1", "4", "tbsp", "sugar")),
                ScaleMode.ByFactor(1.0),
                DisplayPrefs(mode = DisplayMode.KEEP_UNITS),
            )
        )
        assertEquals("4 Tbsp", s.ingredients.first().displayText)
    }

    // ── ByIngredient ──────────────────────────────────────────────────────────

    @Test fun `byIngredient same unit`() {
        val outcome = ScaleRecipe.scale(
            listOf(ingredient("1", "2", "cup", "flour")),
            ScaleMode.ByIngredient(ingredientId = "1", targetAmount = 3.0, targetUnit = "cup"),
        )
        assertEquals(1.5, success(outcome).factor, 0.0001)
    }

    @Test fun `byIngredient converts grams to lb`() {
        val outcome = ScaleRecipe.scale(
            listOf(ingredient("1", "500", "g", "flour")),
            ScaleMode.ByIngredient(ingredientId = "1", targetAmount = 1.0, targetUnit = "lb"),
        )
        assertEquals(0.90718474, success(outcome).factor, 0.000001)
    }

    @Test fun `byIngredient incompatible families`() {
        val outcome = ScaleRecipe.scale(
            listOf(ingredient("1", "2", "cup", "flour")),
            ScaleMode.ByIngredient(ingredientId = "1", targetAmount = 100.0, targetUnit = "g"),
        )
        assertEquals(ScaleError.IncompatibleUnits(from = "g", to = "cup"), failure(outcome))
    }

    @Test fun `byIngredient unknown id`() {
        val outcome = ScaleRecipe.scale(
            listOf(ingredient("1", "2", "cup", "flour")),
            ScaleMode.ByIngredient(ingredientId = "nope", targetAmount = 3.0, targetUnit = "cup"),
        )
        assertEquals(ScaleError.IngredientNotFound, failure(outcome))
    }

    @Test fun `byIngredient blank quantity`() {
        val outcome = ScaleRecipe.scale(
            listOf(ingredient("1", "", "cup", "flour")),
            ScaleMode.ByIngredient(ingredientId = "1", targetAmount = 3.0, targetUnit = "cup"),
        )
        assertEquals(ScaleError.IngredientHasNoQuantity, failure(outcome))
    }

    // ── ByFactor ──────────────────────────────────────────────────────────────

    @Test fun `byFactor zero is InvalidFactor`() {
        val outcome = ScaleRecipe.scale(
            listOf(ingredient("1", "1", "cup", "flour")),
            ScaleMode.ByFactor(0.0),
        )
        assertEquals(ScaleError.InvalidFactor, failure(outcome))
    }

    // ── Row statuses ──────────────────────────────────────────────────────────

    @Test fun `unparseable row passes through unscaled`() {
        val s = success(
            ScaleRecipe.scale(
                listOf(ingredient("1", "a few", "none", "garlic")),
                ScaleMode.ByFactor(2.0),
            )
        )
        val row = s.ingredients.first()
        assertEquals(ScaleStatus.UNPARSED, row.status)
        assertEquals("a few", row.displayText)
        assertEquals("a few", row.originalDisplayText)
    }

    @Test fun `blank row stays blank`() {
        val s = success(
            ScaleRecipe.scale(
                listOf(ingredient("1", "", "tsp", "salt")),
                ScaleMode.ByFactor(2.0),
            )
        )
        val row = s.ingredients.first()
        assertEquals(ScaleStatus.BLANK, row.status)
        assertEquals("", row.displayText)
    }

    @Test fun `range row scales the low end`() {
        val s = success(
            ScaleRecipe.scale(
                listOf(ingredient("1", "2-3", "clove", "garlic")),
                ScaleMode.ByFactor(2.0),
            )
        )
        val row = s.ingredients.first()
        assertEquals(ScaleStatus.RANGE_LOW_END, row.status)
        assertEquals("4 clove", row.displayText)
    }

    @Test fun `originalDisplayText shows the unscaled amount`() {
        val s = success(
            ScaleRecipe.scale(
                listOf(ingredient("1", "1/3", "cup", "sugar")),
                ScaleMode.ByFactor(3.0),
            )
        )
        val row = s.ingredients.first()
        assertEquals("⅓ cup", row.originalDisplayText)
        assertEquals("1 cup", row.displayText)
    }

    // ── "Promote to" overrides ────────────────────────────────────────────────

    @Test fun `promoted row displays in the target unit and keeps the original was text`() {
        val s = success(
            ScaleRecipe.scale(
                listOf(ingredient("a", "44", "cup", "stock")),
                ScaleMode.ByFactor(1.0),
                promoted = mapOf("a" to KitchenUnit.QUART),
            )
        )
        val row = s.ingredients.first()
        assertEquals("11 qt", row.displayText)
        assertEquals("44 cup", row.originalDisplayText)
        assertEquals(KitchenUnit.QUART, row.promotedUnit)
    }

    @Test fun `promotion rescales with the factor`() {
        val s = success(
            ScaleRecipe.scale(
                listOf(ingredient("a", "22", "cup", "stock")),
                ScaleMode.ByFactor(2.0),
                promoted = mapOf("a" to KitchenUnit.GALLON),
            )
        )
        assertEquals("2 gal 3 qt", s.ingredients.first().displayText)
    }

    @Test fun `promotion is ignored on non-volume rows`() {
        val s = success(
            ScaleRecipe.scale(
                listOf(ingredient("a", "2", "each", "eggs")),
                ScaleMode.ByFactor(2.0),
                promoted = mapOf("a" to KitchenUnit.QUART),
            )
        )
        val row = s.ingredients.first()
        assertEquals("4 each", row.displayText)
        assertEquals(null, row.promotedUnit)
    }

    @Test fun `promotion falls back when below one target unit`() {
        val s = success(
            ScaleRecipe.scale(
                listOf(ingredient("a", "1", "cup", "milk")),
                ScaleMode.ByFactor(0.5),
                promoted = mapOf("a" to KitchenUnit.QUART),
            )
        )
        assertEquals("½ cup", s.ingredients.first().displayText)
    }

    @Test fun `promotion is ignored outside kitchen mode`() {
        val s = success(
            ScaleRecipe.scale(
                listOf(ingredient("a", "44", "cup", "stock")),
                ScaleMode.ByFactor(1.0),
                prefs = DisplayPrefs(mode = DisplayMode.DECIMAL),
                promoted = mapOf("a" to KitchenUnit.QUART),
            )
        )
        val row = s.ingredients.first()
        assertEquals("44 cup", row.displayText)
        assertEquals(null, row.promotedUnit)
    }

    @Test fun `promote options are listed per target once each reaches one unit`() {
        val s = success(
            ScaleRecipe.scale(
                listOf(
                    ingredient("a", "1", "cup", "milk"),
                    ingredient("b", "44", "cup", "stock"),
                    ingredient("c", "2", "each", "eggs"),
                    ingredient("d", "3", "cup", "cream"),
                    ingredient("e", "4", "cup", "water"),
                ),
                ScaleMode.ByFactor(1.0),
            )
        )
        // Below 1 qt and 1 L: nothing to promote to, so no options (no icon).
        assertTrue(s.ingredients[0].promoteOptions.isEmpty())
        assertTrue(s.ingredients[3].promoteOptions.isEmpty())
        // 44 cup: qt, gal and L all ≥ 1 unit.
        assertEquals(
            listOf(KitchenUnit.QUART, KitchenUnit.GALLON, KitchenUnit.L),
            s.ingredients[1].promoteOptions.map { it.unit },
        )
        assertEquals("11 qt", s.ingredients[1].promoteOptions[0].previewText)
        // Non-volume rows get no options.
        assertTrue(s.ingredients[2].promoteOptions.isEmpty())
        // 4 cup = 1 qt: qt is offered; gal (<1) is not.
        assertEquals(listOf(KitchenUnit.QUART), s.ingredients[4].promoteOptions.map { it.unit })
        assertEquals("1 qt", s.ingredients[4].promoteOptions[0].previewText)
    }

    @Test fun `promote options drop entries identical to the Auto text`() {
        val s = success(
            ScaleRecipe.scale(
                listOf(
                    ingredient("a", "500", "ml", "water"),
                    ingredient("b", "1.5", "l", "stock"),
                    ingredient("c", "2", "qt", "broth"),
                ),
                ScaleMode.ByFactor(1.0),
            )
        )
        // 500 mL: below 1 qt and 1 L → no options.
        assertTrue(s.ingredients[0].promoteOptions.isEmpty())
        // 1.5 L: Auto is "1 L 500 mL", so the L option is a no-op and dropped.
        assertEquals(listOf(KitchenUnit.QUART), s.ingredients[1].promoteOptions.map { it.unit })
        // 2 qt: Auto is "2 qt", so the qt option is dropped; L remains.
        assertEquals(listOf(KitchenUnit.L), s.ingredients[2].promoteOptions.map { it.unit })
    }

    @Test fun `promotion to the unit the row already shows is inactive`() {
        val s = success(
            ScaleRecipe.scale(
                listOf(ingredient("a", "2", "qt", "broth")),
                ScaleMode.ByFactor(1.0),
                promoted = mapOf("a" to KitchenUnit.QUART),
            )
        )
        val row = s.ingredients.first()
        assertEquals("2 qt", row.displayText)
        assertEquals(null, row.promotedUnit)
    }

    @Test fun `stale promotion below its threshold is not reported as active`() {
        val s = success(
            ScaleRecipe.scale(
                listOf(ingredient("a", "3", "cup", "milk")),
                ScaleMode.ByFactor(1.0),
                promoted = mapOf("a" to KitchenUnit.QUART),
            )
        )
        val row = s.ingredients.first()
        assertEquals("3 cup", row.displayText)
        assertEquals(null, row.promotedUnit)
    }

    // ── Copy output with blank rows ───────────────────────────────────────────

    @Test fun `shopping list has no double space for blank rows`() {
        val s = success(
            ScaleRecipe.scale(
                listOf(ingredient("1", "", "none", "salt")),
                ScaleMode.ByFactor(1.0),
            )
        )
        assertEquals("• salt", RecipeScalerCalculator.generateShoppingList(s.ingredients))
    }
}
