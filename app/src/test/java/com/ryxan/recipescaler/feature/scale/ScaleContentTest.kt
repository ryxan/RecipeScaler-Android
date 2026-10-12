package com.ryxan.recipescaler.feature.scale

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ryxan.recipescaler.core.designsystem.RescaleTheme
import com.ryxan.recipescaler.core.domain.model.ScaleStatus
import com.ryxan.recipescaler.core.domain.model.ScaledIngredient
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

    private fun row(text: String, name: String, status: ScaleStatus = ScaleStatus.SCALED) =
        ScaledIngredient(
            id = "1",
            name = name,
            unit = "cup",
            originalQty = 1.0,
            scaledQty = 2.0,
            displayText = text,
            originalDisplayText = "1 cup",
            status = status,
        )

    private fun state(
        tab: ScaleTab = ScaleTab.FACTOR,
        factor: Double = 2.0,
        showWarning: Boolean = false,
        cookMode: Boolean = false,
        checkedIds: Set<String> = emptySet(),
        recipeMissing: Boolean = false,
    ) = ScaleUiState(
        isLoading = false,
        title = "Bread",
        yieldAmount = 4.0,
        yieldLabel = "servings",
        inputs = ScaleInputs(tab = tab, factorText = "2"),
        factor = factor,
        derivedYieldAmount = 8.0,
        showScaleWarning = showWarning,
        rows = listOf(row("2 cup", "Flour")),
        cookMode = cookMode,
        checkedIds = checkedIds,
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
    fun `tapping the 2x chip fires the factor callback`() {
        var factor = ""
        content(
            state(factor = 1.0),
            ScaleCallbacks(onFactorText = { factor = it }),
        )
        composeRule.onNodeWithText("2×").performClick()
        assertEquals("2", factor)
    }

    @Test
    fun `warning banner shows for large factors`() {
        content(state(factor = 5.0, showWarning = true))
        composeRule.onNodeWithText(
            "×5 is a big jump.",
            substring = true,
        ).assertIsDisplayed()
    }

    @Test
    fun `warning banner shows for very small factors`() {
        content(state(factor = 0.1, showWarning = true))
        composeRule.onNodeWithText(
            "makes a very small batch.",
            substring = true,
        ).assertIsDisplayed()
    }

    @Test
    fun `cook mode hides inputs and shows the hint`() {
        content(state(cookMode = true))
        composeRule.onNodeWithText(
            "Tap an ingredient to check it off. The screen stays on.",
        ).assertIsDisplayed()
        composeRule.onAllNodesWithText("Custom multiplier").assertCountEquals(0)
        composeRule.onAllNodesWithText("Show original amounts").assertCountEquals(0)
        composeRule.onAllNodesWithText("×Factor").assertCountEquals(0)
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
}
