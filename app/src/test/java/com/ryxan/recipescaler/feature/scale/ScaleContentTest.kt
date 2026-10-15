package com.ryxan.recipescaler.feature.scale

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import com.ryxan.recipescaler.core.designsystem.RescaleTheme
import com.ryxan.recipescaler.core.domain.formatting.MeasurementSystem
import com.ryxan.recipescaler.core.domain.model.KitchenUnit
import com.ryxan.recipescaler.core.domain.model.PromoteOption
import com.ryxan.recipescaler.core.domain.model.ScaleStatus
import com.ryxan.recipescaler.core.domain.model.ScaledIngredient
import com.ryxan.recipescaler.core.domain.scaling.CopyFormat
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScaleContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun row(
        text: String,
        name: String,
        status: ScaleStatus = ScaleStatus.SCALED,
        promotedUnit: KitchenUnit? = null,
        promoteOptions: List<PromoteOption> = emptyList(),
    ) =
        ScaledIngredient(
            id = "1",
            name = name,
            unit = "cup",
            originalQty = 1.0,
            scaledQty = 2.0,
            displayText = text,
            originalDisplayText = "1 cup",
            status = status,
            promotedUnit = promotedUnit,
            promoteOptions = promoteOptions,
        )

    private fun state(
        factor: Double = 2.0,
        showWarning: Boolean = false,
        cookMode: Boolean = false,
        checkedIds: Set<String> = emptySet(),
        recipeMissing: Boolean = false,
        recipeId: Long = 7,
        measurementSystem: MeasurementSystem = MeasurementSystem.MATCH_RECIPE,
        unitsSelectable: Boolean = true,
    ) = ScaleUiState(
        isLoading = false,
        recipeId = recipeId,
        title = "Bread",
        yieldAmount = 4.0,
        yieldLabel = "servings",
        inputs = ScaleInputs(servesText = "4", scaleToText = "8"),
        factor = factor,
        derivedYieldAmount = 8.0,
        showScaleWarning = showWarning,
        rows = listOf(row("2 cup", "Flour")),
        cookMode = cookMode,
        checkedIds = checkedIds,
        measurementSystem = measurementSystem,
        unitsSelectable = unitsSelectable,
        recipeMissing = recipeMissing,
    )

    private fun content(state: ScaleUiState, callbacks: ScaleCallbacks = ScaleCallbacks()) {
        composeRule.setContent {
            RescaleTheme(dynamicColor = false) {
                ScaleContent(state = state, callbacks = callbacks)
            }
        }
    }

    @Test
    fun `renders title rows and summary`() {
        content(state())
        composeRule.onNodeWithText("Bread").assertIsDisplayed()
        composeRule.onNodeWithText("2 cup").assertIsDisplayed()
        composeRule.onNodeWithText("Flour").assertIsDisplayed()
        composeRule.onNodeWithText("×2 · Makes 8 servings").assertIsDisplayed()
    }

    @Test
    fun `shows the serves and scale to fields`() {
        content(state())
        composeRule.onNodeWithText("Serves").assertIsDisplayed()
        composeRule.onNodeWithText("Scale to (servings)").assertIsDisplayed()
    }

    @Test
    fun `the tab row is gone`() {
        content(state())
        composeRule.onAllNodesWithText("Servings").assertCountEquals(0)
        composeRule.onAllNodesWithText("Ingredient").assertCountEquals(0)
        composeRule.onAllNodesWithText("×Factor").assertCountEquals(0)
    }

    @Test
    fun `the 24 chip is reachable by scrolling`() {
        content(state())
        composeRule.onNode(
            hasScrollToIndexAction() and hasAnyAncestor(hasScrollToIndexAction()),
        ).performScrollToNode(hasText("24"))
        composeRule.onNodeWithText("24").assertIsDisplayed()
    }

    @Test
    fun `tapping the 3 chip fires the servings callback`() {
        var servings = 0
        content(
            state(factor = 1.0),
            ScaleCallbacks(onServings = { servings = it }),
        )
        composeRule.onNodeWithText("3").performClick()
        assertEquals(3, servings)
    }

    @Test
    fun `warning banner shows for large factors`() {
        content(state(factor = 5.0, showWarning = true))
        composeRule.onNodeWithText(
            "×5 is a big jump",
            substring = true,
        ).assertIsDisplayed()
    }

    @Test
    fun `warning banner shows for very small factors`() {
        content(state(factor = 0.1, showWarning = true))
        composeRule.onNodeWithText(
            "makes a very small batch",
            substring = true,
        ).assertIsDisplayed()
    }

    @Test
    fun `warning banner expands to detail and dismisses`() {
        content(state(factor = 5.0, showWarning = true))
        composeRule.onNodeWithText(
            "×5 is a big jump",
            substring = true,
        ).performClick()
        composeRule.onNodeWithText(
            "split across pans",
            substring = true,
        ).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Dismiss").performClick()
        composeRule.onAllNodesWithText(
            "×5 is a big jump",
            substring = true,
        ).assertCountEquals(0)
    }

    @Test
    fun `unreadable input marks the failing field and flags the summary`() {
        content(
            state().copy(
                inputs = ScaleInputs(servesText = "4", scaleToText = "abc"),
                message = ScaleMessage.CantRead("abc", ScaleField.SCALE_TO),
                factor = 1.0,
                derivedYieldAmount = null,
            ),
        )
        composeRule.onAllNodesWithText("Can't read 'abc'").assertCountEquals(1)
        composeRule.onNodeWithText("(input not read)").assertIsDisplayed()
        composeRule.onNodeWithText("×1").assertIsDisplayed()
    }

    @Test
    fun `show original lives in the overflow menu`() {
        var toggled: Boolean? = null
        content(
            state(),
            ScaleCallbacks(onShowOriginal = { toggled = it }),
        )
        composeRule.onAllNodesWithText("Show original amounts").assertCountEquals(0)
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Show original amounts").performClick()
        assertEquals(true, toggled)
    }

    @Test
    fun `cook mode hides inputs and shows the hint`() {
        content(state(cookMode = true))
        composeRule.onNodeWithText(
            "Tap an ingredient to check it off. The screen stays on.",
        ).assertIsDisplayed()
        composeRule.onAllNodesWithText("Serves").assertCountEquals(0)
        composeRule.onAllNodesWithText("Scale to (servings)").assertCountEquals(0)
        composeRule.onAllNodesWithText("3").assertCountEquals(0)
        composeRule.onAllNodesWithText("Show original amounts").assertCountEquals(0)
        // Summary and rows still visible.
        composeRule.onNodeWithText("×2 · Makes 8 servings").assertIsDisplayed()
        composeRule.onNodeWithText("Flour").assertIsDisplayed()
    }

    @Test
    fun `tapping a row in cook mode toggles it checked`() {
        var toggled = ""
        content(
            state(cookMode = true),
            ScaleCallbacks(onToggleChecked = { toggled = it }),
        )
        composeRule.onNodeWithText("Flour").performClick()
        assertEquals("1", toggled)
    }

    @Test
    fun `a missing recipe shows the empty message`() {
        content(state(recipeMissing = true))
        composeRule.onNodeWithText("This recipe no longer exists").assertIsDisplayed()
        composeRule.onAllNodesWithText("Flour").assertCountEquals(0)
    }

    @Test
    fun `the edit action fires with the recipe id`() {
        var edited = 0L
        content(
            state(recipeId = 42),
            ScaleCallbacks(onEdit = { edited = it }),
        )
        composeRule.onNodeWithContentDescription("Edit").performClick()
        assertEquals(42L, edited)
    }

    @Test
    fun `the edit fab fires with the recipe id`() {
        var edited = 0L
        content(
            state(recipeId = 42),
            ScaleCallbacks(onEdit = { edited = it }),
        )
        // The extended FAB merges its semantics — the label only exists in the
        // unmerged tree, but clicking its bounds still hits the button.
        composeRule.onNodeWithText("Edit", useUnmergedTree = true).performClick()
        assertEquals(42L, edited)
    }

    @Test
    fun `the edit fab hides in cook mode`() {
        content(state(cookMode = true))
        composeRule.onAllNodesWithText("Edit", useUnmergedTree = true)
            .assertCountEquals(0)
    }

    @Test
    fun `share and copy live in the overflow menu`() {
        var copied: CopyFormat? = null
        var shared = false
        content(
            state(),
            ScaleCallbacks(onCopy = { copied = it }, onShare = { shared = true }),
        )
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Share…").assertIsDisplayed()
        composeRule.onNodeWithText("Copy plain").assertIsDisplayed()
        composeRule.onNodeWithText("Copy Markdown").assertIsDisplayed()
        composeRule.onNodeWithText("Copy shopping list").performClick()
        assertEquals(CopyFormat.SHOPPING_LIST, copied)

        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Share…").performClick()
        assertEquals(true, shared)
    }

    @Test
    fun `the cook entry shows a label`() {
        content(state())
        composeRule.onNodeWithText("Cook").assertIsDisplayed()
    }

    @Test
    fun `the cook entry shows Done in cook mode`() {
        content(state(cookMode = true))
        composeRule.onNodeWithText("Done").assertIsDisplayed()
    }

    @Test
    fun `the stepper fires step callbacks`() {
        val steps = mutableListOf<Int>()
        content(state(), ScaleCallbacks(onStep = { steps += it }))
        composeRule.onNodeWithContentDescription("Increase servings").performClick()
        composeRule.onNodeWithContentDescription("Decrease servings").performClick()
        assertEquals(listOf(1, -1), steps)
    }

    @Test
    fun `the stepper shows the target servings`() {
        content(state())
        composeRule.onNodeWithContentDescription("Makes 8 servings").assertIsDisplayed()
    }

    @Test
    fun `the stepper minus disables at the minimum servings`() {
        content(
            state().copy(
                inputs = ScaleInputs(servesText = "4", scaleToText = "1"),
                factor = 0.25,
                derivedYieldAmount = 1.0,
            ),
        )
        composeRule.onNodeWithContentDescription("Decrease servings").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Increase servings").assertIsDisplayed()
    }

    @Test
    fun `the stepper plus disables at the maximum servings`() {
        content(
            state().copy(
                inputs = ScaleInputs(servesText = "4", scaleToText = "99"),
                factor = 24.75,
                derivedYieldAmount = 99.0,
            ),
        )
        composeRule.onNodeWithContentDescription("Increase servings").assertIsNotEnabled()
    }

    @Test
    fun `the measurement system lives in the overflow menu`() {
        var picked: MeasurementSystem? = null
        content(
            state(measurementSystem = MeasurementSystem.MATCH_RECIPE),
            ScaleCallbacks(onMeasurementSystem = { picked = it }),
        )
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Match recipe").assertIsDisplayed()
        composeRule.onNodeWithText("US customary").assertIsDisplayed()
        composeRule.onNodeWithText("Metric").performClick()
        assertEquals(MeasurementSystem.METRIC, picked)
    }

    @Test
    fun `the measurement system is hidden when it has no effect`() {
        content(state(unitsSelectable = false))
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onAllNodesWithText("Match recipe").assertCountEquals(0)
        composeRule.onAllNodesWithText("US customary").assertCountEquals(0)
        composeRule.onAllNodesWithText("Metric").assertCountEquals(0)
    }

    // ── "Promote to" ──────────────────────────────────────────────────────────

    private val promoteOptions = listOf(
        PromoteOption(KitchenUnit.QUART, "11 qt"),
        PromoteOption(KitchenUnit.GALLON, "2 gal 3 qt"),
        PromoteOption(KitchenUnit.L, "10 L 410 mL"),
    )

    private fun promotedState(promotedUnit: KitchenUnit? = null, unitsSelectable: Boolean = true) =
        state(unitsSelectable = unitsSelectable).copy(
            rows = listOf(
                row(
                    "44 cup", "Stock",
                    promotedUnit = promotedUnit,
                    promoteOptions = promoteOptions,
                ),
            ),
        )

    @Test
    fun `the promote menu lists targets and fires the callback`() {
        var picked: Pair<String, KitchenUnit?>? = null
        content(
            promotedState(),
            ScaleCallbacks(onPromote = { id, unit -> picked = id to unit }),
        )
        composeRule.onNodeWithContentDescription("Promote to a larger unit").performClick()
        composeRule.onNodeWithText("Quarts — 11 qt").assertIsDisplayed()
        composeRule.onNodeWithText("Gallons — 2 gal 3 qt").assertIsDisplayed()
        composeRule.onNodeWithText("Litres — 10 L 410 mL").performClick()
        assertEquals("1" to KitchenUnit.L, picked)
    }

    @Test
    fun `the promote menu hides auto without an override`() {
        content(promotedState())
        composeRule.onNodeWithContentDescription("Promote to a larger unit").performClick()
        composeRule.onAllNodesWithText("Auto").assertCountEquals(0)
    }

    @Test
    fun `the promote menu auto entry resets the override`() {
        var reset: Pair<String, KitchenUnit?>? = null
        content(
            promotedState(promotedUnit = KitchenUnit.QUART),
            ScaleCallbacks(onPromote = { id, unit -> reset = id to unit }),
        )
        composeRule.onNodeWithContentDescription("Promote to a larger unit").performClick()
        composeRule.onNodeWithText("Auto").performClick()
        assertEquals("1" to null, reset)
    }

    @Test
    fun `the promote button is hidden without options`() {
        content(state())
        composeRule.onAllNodesWithContentDescription("Promote to a larger unit")
            .assertCountEquals(0)
    }

    @Test
    fun `the promote button is hidden outside kitchen mode`() {
        content(promotedState(unitsSelectable = false))
        composeRule.onAllNodesWithContentDescription("Promote to a larger unit")
            .assertCountEquals(0)
    }
}
