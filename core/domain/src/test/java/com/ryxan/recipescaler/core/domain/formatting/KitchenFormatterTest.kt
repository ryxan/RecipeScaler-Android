package com.ryxan.recipescaler.core.domain.formatting

import com.ryxan.recipescaler.core.domain.model.KitchenUnit
import com.ryxan.recipescaler.core.domain.scaling.RecipeScalerCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Spec-table tests for [KitchenFormatter] (docs/plan.md §3.3), plus the
 * formatting tests migrated from the old RecipeScalerCalculator spec.
 */
class KitchenFormatterTest {

    private val us = DisplayPrefs(system = MeasurementSystem.US)
    private val metric = DisplayPrefs(system = MeasurementSystem.METRIC)
    private val keep = DisplayPrefs(mode = DisplayMode.KEEP_UNITS)
    private val decimal = DisplayPrefs(mode = DisplayMode.DECIMAL)

    private fun format(amount: Double, unit: String, prefs: DisplayPrefs = DisplayPrefs()) =
        KitchenFormatter.format(amount, unit, prefs)

    // ── US volume, KITCHEN: spec table ────────────────────────────────────────

    @Test fun `20 tsp is 6 Tbsp 2 tsp`() = assertEquals("6 Tbsp 2 tsp", format(20.0, "tsp"))
    @Test fun `20 cup is 20 cup`() = assertEquals("20 cup", format(20.0, "cup"))
    @Test fun `70 cup is 70 cup`() = assertEquals("70 cup", format(70.0, "cup"))
    @Test fun `5 tsp is 1 Tbsp 2 tsp`() = assertEquals("1 Tbsp 2 tsp", format(5.0, "tsp"))
    @Test fun `50 tsp is 1 cup 2 tsp`() = assertEquals("1 cup 2 tsp", format(50.0, "tsp"))
    @Test fun `half tsp`() = assertEquals("½ tsp", format(0.5, "tsp"))
    @Test fun `quarter tsp`() = assertEquals("¼ tsp", format(0.25, "tsp"))
    @Test fun `eighth tsp`() = assertEquals("⅛ tsp", format(0.125, "tsp"))
    @Test fun `tiny tsp is pinch`() = assertEquals("pinch", format(0.03125, "tsp"))
    @Test fun `1·5 tsp`() = assertEquals("1½ tsp", format(1.5, "tsp"))
    @Test fun `half tbsp is 1·5 tsp`() = assertEquals("1½ tsp", format(0.5, "tbsp"))
    @Test fun `third cup`() = assertEquals("⅓ cup", format(1.0 / 3.0, "cup"))
    @Test fun `half cup`() = assertEquals("½ cup", format(0.5, "cup"))
    @Test fun `three-quarter cup`() = assertEquals("¾ cup", format(0.75, "cup"))
    @Test fun `0·4 cup is third cup 1 Tbsp`() = assertEquals("⅓ cup 1 Tbsp", format(0.4, "cup"))
    @Test fun `40 tsp is ¾ cup 1 Tbsp 1 tsp`() = assertEquals("¾ cup 1 Tbsp 1 tsp", format(40.0, "tsp"))
    @Test fun `47 tsp is ¾ cup 3 Tbsp 2 tsp`() = assertEquals("¾ cup 3 Tbsp 2 tsp", format(47.0, "tsp"))
    @Test fun `1·5 cup`() = assertEquals("1½ cup", format(1.5, "cup"))
    @Test fun `12 tsp is ¼ cup`() = assertEquals("¼ cup", format(12.0, "tsp"))
    @Test fun `19 tsp is ⅓ cup 1 Tbsp`() = assertEquals("⅓ cup 1 Tbsp", format(19.0, "tsp"))
    @Test fun `0·9 tsp snaps to 1`() = assertEquals("1 tsp", format(0.9, "tsp"))
    @Test fun `0·3 tsp is ¼ tsp`() = assertEquals("¼ tsp", format(0.3, "tsp"))
    @Test fun `1·2 tsp is 1 tsp`() = assertEquals("1 tsp", format(1.2, "tsp"))
    @Test fun `0 tsp is 0 tsp`() = assertEquals("0 tsp", format(0.0, "tsp"))

    // ── Other units, KITCHEN, MATCH_RECIPE ────────────────────────────────────

    @Test fun `1·5 qt`() = assertEquals("1½ qt", format(1.5, "qt"))
    @Test fun `half qt is 2 cup`() = assertEquals("2 cup", format(0.5, "qt"))
    @Test fun `2·3 fl oz`() = assertEquals("2½ fl oz", format(2.3, "fl oz"))
    @Test fun `half fl oz is 1 Tbsp`() = assertEquals("1 Tbsp", format(0.5, "fl oz"))
    @Test fun `1500 ml is 1 L 500 mL`() = assertEquals("1 L 500 mL", format(1500.0, "ml"))
    @Test fun `2·5 ml`() = assertEquals("2.5 mL", format(2.5, "ml"))
    @Test fun `0·75 L is 750 mL`() = assertEquals("750 mL", format(0.75, "L"))
    @Test fun `1·5 kg is 1 kg 500 g`() = assertEquals("1 kg 500 g", format(1.5, "kg"))
    @Test fun `half g is 500 mg`() = assertEquals("500 mg", format(0.5, "g"))
    @Test fun `4·24 g is 4·2 g`() = assertEquals("4.2 g", format(4.24, "g"))
    @Test fun `1·5 lb is 1 lb 8 oz`() = assertEquals("1 lb 8 oz", format(1.5, "lb"))
    @Test fun `17·3 oz is 1 lb 1¼ oz`() = assertEquals("1 lb 1¼ oz", format(17.3, "oz"))
    @Test fun `0·75 oz`() = assertEquals("¾ oz", format(0.75, "oz"))
    @Test fun `2·5 each`() = assertEquals("2½ each", format(2.5, "each"))
    @Test fun `2·5 none has no label`() = assertEquals("2½", format(2.5, "none"))
    @Test fun `3 pinch`() = assertEquals("3 pinch", format(3.0, "pinch"))
    @Test fun `0·2 each is half`() = assertEquals("½ each", format(0.2, "each"))
    @Test fun `0·2 ml is pinch`() = assertEquals("pinch", format(0.2, "ml"))
    @Test fun `0·5 ml stays mL`() = assertEquals("0.5 mL", format(0.5, "ml"))
    @Test fun `tiny tsp to metric is pinch`() =
        assertEquals("pinch", format(1.0 / 32.0, "tsp", metric))

    // ── System conversion, KITCHEN ────────────────────────────────────────────

    @Test fun `1 cup to metric`() = assertEquals("237 mL", format(1.0, "cup", metric))
    @Test fun `250 ml to US`() = assertEquals("1 cup 2½ tsp", format(250.0, "ml", us))
    @Test fun `100 g to US`() = assertEquals("3½ oz", format(100.0, "g", us))
    @Test fun `1 lb to metric`() = assertEquals("454 g", format(1.0, "lb", metric))
    @Test fun `2 each never converts`() = assertEquals("2 each", format(2.0, "each", metric))

    // ── KEEP_UNITS ────────────────────────────────────────────────────────────

    @Test fun `keep quarter cup stays quarter cup`() =
        assertEquals("¼ cup", format(0.25, "cup", keep))
    @Test fun `keep 2·5 tbsp`() = assertEquals("2½ Tbsp", format(2.5, "tbsp", keep))
    @Test fun `keep 0·3 cup`() = assertEquals("⅓ cup", format(0.3, "cup", keep))
    @Test fun `keep 4 tbsp`() = assertEquals("4 Tbsp", format(4.0, "tbsp", keep))
    @Test fun `keep 1·5 L`() = assertEquals("1.5 L", format(1.5, "L", keep))
    @Test fun `keep 1·5 lb`() = assertEquals("1½ lb", format(1.5, "lb", keep))
    @Test fun `keep 50 tsp`() = assertEquals("50 tsp", format(50.0, "tsp", keep))

    // ── DECIMAL ───────────────────────────────────────────────────────────────

    @Test fun `decimal third cup`() = assertEquals("0.33 cup", format(1.0 / 3.0, "cup", decimal))
    @Test fun `decimal 2 tsp`() = assertEquals("2 tsp", format(2.0, "tsp", decimal))

    // ── Migrated: unchanged expectations ──────────────────────────────────────

    /** 2 tsp × 2.5 → 5 tsp → 1 Tbsp 2 tsp */
    @Test fun `2 tsp scaled by 2·5 is 1 Tbsp 2 tsp`() =
        assertEquals("1 Tbsp 2 tsp", format(5.0, "tsp"))

    /** 12 cups flour → 12 cups */
    @Test fun `12 cups stays as 12 cups`() = assertEquals("12 cup", format(12.0, "cup"))

    @Test fun `48 tsp is exactly 1 cup`() = assertEquals("1 cup", format(48.0, "tsp"))
    @Test fun `250 ml times 2 is 500 mL`() = assertEquals("500 mL", format(500.0, "ml"))
    @Test fun `250 ml times 6 is 1 L 500 mL`() = assertEquals("1 L 500 mL", format(1500.0, "ml"))
    @Test fun `900 ml times 1·1 is 990 mL`() = assertEquals("990 mL", format(990.0, "ml"))
    @Test fun `999 ml stays as mL`() = assertEquals("999 mL", format(999.0, "ml"))
    @Test fun `1000 ml becomes 1 L`() = assertEquals("1 L", format(1000.0, "ml"))
    @Test fun `2000 ml becomes 2 L`() = assertEquals("2 L", format(2000.0, "ml"))
    @Test fun `quarter litre formats as 250 mL`() = assertEquals("250 mL", format(0.25, "L"))
    @Test fun `1·5 L formats as 1 L 500 mL`() = assertEquals("1 L 500 mL", format(1.5, "L"))
    @Test fun `integer g quantity`() = assertEquals("5 g", format(5.0, "g"))
    @Test fun `2 lb is 2 lb`() = assertEquals("2 lb", format(2.0, "lb"))

    // ── Migrated: expectations updated to the new spec ───────────────────────

    /** Was `one third cup formats as 5 Tbsp 1 tsp` — cup fractions now win. */
    @Test fun `one third cup formats as ⅓ cup`() = assertEquals("⅓ cup", format(16.0, "tsp"))

    /** Was `quarter cup times 3 is 12 Tbsp not 1 cup` — now rolls into ¾ cup. */
    @Test fun `quarter cup times 3 is ¾ cup`() = assertEquals("¾ cup", format(0.75, "cup"))

    /** Was `47 tsp does not roll into a cup` — now `¾ cup 3 Tbsp 2 tsp`. */
    @Test fun `47 tsp rolls into a cup part`() =
        assertEquals("¾ cup 3 Tbsp 2 tsp", format(47.0, "tsp"))

    /** Was `tiny amount under 1 tsp shows pinch` (0.4) — now snaps to ½ tsp. */
    @Test fun `0·4 tsp snaps to half tsp`() = assertEquals("½ tsp", format(0.4, "tsp"))

    /** Was `amount just above 0 5 tsp rounds to 1 tsp not pinch` (0.6) — now ½ tsp. */
    @Test fun `0·6 tsp snaps to half tsp`() = assertEquals("½ tsp", format(0.6, "tsp"))

    /** Was `2·5 each rounds up to 3` — COUNT now snaps to nearest ½. */
    @Test fun `2·5 each rounds to half`() = assertEquals("2½ each", format(2.5, "each"))

    /** Was `tiny nonzero other unit shows 1 not 0` (0.3 g) — now drops to mg below 1 g. */
    @Test fun `0·3 g shows as 300 mg`() = assertEquals("300 mg", format(0.3, "g"))

    /** Was `oz rounds to whole` (3.4 → 3 oz) — now nearest ¼. */
    @Test fun `oz snaps to quarter`() = assertEquals("3½ oz", format(3.4, "oz"))

    /** Was `keep original units rounds 2·5 tbsp to 3 Tbsp` — now 2½ Tbsp. */
    @Test fun `keep 2·5 tbsp shows fraction`() =
        assertEquals("2½ Tbsp", format(2.5, "tbsp", keep))

    /** Was `legacy Tbsp unit key still scales as US volume` — 2.5 Tbsp → 7.5 tsp. */
    @Test fun `legacy Tbsp unit key still formats as US volume`() =
        assertEquals("2 Tbsp 1½ tsp", format(2.5, "Tbsp"))

    // ── canonicalUnit (retained from the calculator spec) ────────────────────

    @Test fun `canonicalUnit maps accessory-bar labels to keys`() {
        assertEquals("tbsp", RecipeScalerCalculator.canonicalUnit("Tbsp"))
        assertEquals("ml",   RecipeScalerCalculator.canonicalUnit("mL"))
        assertEquals("L",    RecipeScalerCalculator.canonicalUnit("l"))
        assertEquals("pt",   RecipeScalerCalculator.canonicalUnit("pt"))
    }

    // ── formatIn ("Promote to") ───────────────────────────────────────────────

    private fun inUnit(amount: Double, unit: KitchenUnit, target: KitchenUnit) =
        KitchenFormatter.formatIn(amount, unit, target)

    @Test fun `44 cups promote to 11 qt`() =
        assertEquals("11 qt", inUnit(44.0, KitchenUnit.CUP, KitchenUnit.QUART))

    @Test fun `44 cups promote to 2 gal 3 qt`() =
        assertEquals("2 gal 3 qt", inUnit(44.0, KitchenUnit.CUP, KitchenUnit.GALLON))

    @Test fun `7 and a third cups promote to 1 qt 3⅓ cup`() =
        assertEquals(
            "1 qt 3⅓ cup",
            inUnit(7.0 + 1.0 / 3.0, KitchenUnit.CUP, KitchenUnit.QUART),
        )

    @Test fun `promote skips pints`() =
        assertEquals("1 qt 1 cup", inUnit(5.0, KitchenUnit.CUP, KitchenUnit.QUART))

    @Test fun `below one target unit is not offered`() {
        assertEquals(null, inUnit(3.0, KitchenUnit.CUP, KitchenUnit.QUART))
        assertEquals(null, inUnit(0.5, KitchenUnit.CUP, KitchenUnit.GALLON))
        assertEquals(null, inUnit(500.0, KitchenUnit.ML, KitchenUnit.L))
    }

    @Test fun `mL is not a promote target`() {
        assertEquals(null, inUnit(2.5, KitchenUnit.L, KitchenUnit.ML))
        assertEquals(null, inUnit(2.0, KitchenUnit.CUP, KitchenUnit.ML))
    }

    @Test fun `L threshold uses the rounded mL`() {
        // 999.6 mL renders as 1 L, so it counts as reaching 1 L.
        assertEquals("1 L", inUnit(999.6, KitchenUnit.ML, KitchenUnit.L))
        // Float noise just under 1 L (e.g. from qty * factor) still counts.
        assertEquals("1 L", inUnit(0.9999999999999999, KitchenUnit.L, KitchenUnit.L))
        // 999.4 mL rounds to 999 mL — below 1 L.
        assertEquals(null, inUnit(999.4, KitchenUnit.ML, KitchenUnit.L))
    }

    @Test fun `metric L promotes to L plus mL`() =
        assertEquals("10 L 416 mL", inUnit(10416.0, KitchenUnit.ML, KitchenUnit.L))

    @Test fun `us ingredient can promote to metric`() =
        assertEquals("3 L 785 mL", inUnit(1.0, KitchenUnit.GALLON, KitchenUnit.L))

    @Test fun `metric ingredient can promote to us units`() =
        // 946.353 mL ≈ 192 tsp = 1 qt exactly.
        assertEquals("1 qt", inUnit(946.353, KitchenUnit.ML, KitchenUnit.QUART))

    @Test fun `promote ignores non-volume units and targets`() {
        assertEquals(null, inUnit(2.0, KitchenUnit.KG, KitchenUnit.QUART))
        assertEquals(null, inUnit(2.0, KitchenUnit.EACH, KitchenUnit.L))
        assertEquals(null, inUnit(2000.0, KitchenUnit.ML, KitchenUnit.KG))
        assertEquals(null, inUnit(4.0, KitchenUnit.CUP, KitchenUnit.PINT))
    }

    @Test fun `remainder snaps to half-tsp precision`() =
        assertEquals(
            "1 qt 2 Tbsp",
            inUnit(198.0, KitchenUnit.TSP, KitchenUnit.QUART),
        )
}
