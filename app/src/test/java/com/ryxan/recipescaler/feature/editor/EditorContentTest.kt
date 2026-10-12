package com.ryxan.recipescaler.feature.editor

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.ryxan.recipescaler.core.designsystem.RescaleTheme
import com.ryxan.recipescaler.core.domain.model.KitchenUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditorContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun state(
        recipeId: Long = 0,
        rows: List<EditorRow> = listOf(EditorRow(key = 1)),
        canScaleRows: List<EditorRow>? = null,
    ) = EditorUiState(
        isLoading = false,
        recipeId = recipeId,
        rows = canScaleRows ?: rows,
    )

    private fun content(state: EditorUiState, callbacks: EditorCallbacks = EditorCallbacks()) {
        composeRule.setContent {
            RescaleTheme(dynamicColor = false) {
                EditorContent(state = state, callbacks = callbacks)
            }
        }
    }

    @Test
    fun `typing in the title field fires the callback`() {
        var typed = ""
        content(
            state(),
            EditorCallbacks(onTitleChange = { typed = it }),
        )
        composeRule.onNodeWithText("Recipe title").performTextInput("Soup")
        assertEquals("Soup", typed)
    }

    @Test
    fun `typing in qty and name fields fires callbacks with the row key`() {
        var qty: Pair<Long, String>? = null
        var name: Pair<Long, String>? = null
        content(
            state(),
            EditorCallbacks(
                onQtyChange = { k, v -> qty = k to v },
                onNameChange = { k, v -> name = k to v },
            ),
        )
        composeRule.onNodeWithText("Qty").performTextInput("2")
        assertEquals(1L to "2", qty)

        composeRule.onNodeWithText("Ingredient").performTextInput("flour")
        assertEquals(1L to "flour", name)
    }

    @Test
    fun `scale fab is disabled when nothing can scale`() {
        var scaled = false
        content(state(), EditorCallbacks(onScale = { scaled = true }))
        // The FAB is marked disabled and ignores clicks while canScale is false.
        composeRule.onNode(hasClickAction() and hasAnyDescendant(hasText("Scale")), useUnmergedTree = true)
            .assertIsNotEnabled()
            .performClick()
        assertTrue(!scaled)
    }

    @Test
    fun `scale fab calls onScale when a row has a quantity`() {
        var scaled = false
        content(
            state(
                canScaleRows = listOf(
                    EditorRow(key = 1, rawQuantity = "1", unit = KitchenUnit.CUP, name = "Flour"),
                ),
            ),
            EditorCallbacks(onScale = { scaled = true }),
        )
        composeRule.onNode(hasClickAction() and hasAnyDescendant(hasText("Scale")), useUnmergedTree = true).performClick()
        assertTrue(scaled)
    }

    @Test
    fun `delete menu item calls onDelete immediately without a dialog`() {
        var deleted = false
        content(
            state(recipeId = 5),
            EditorCallbacks(onDelete = { deleted = true }),
        )
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Delete recipe").performClick()
        assertTrue(deleted)
        // No confirmation dialog appears.
        composeRule.onAllNodesWithText("Delete recipe?").assertCountEquals(0)
    }
}
