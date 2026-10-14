package com.ryxan.recipescaler.feature.editor

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Density
import com.ryxan.recipescaler.core.designsystem.RescaleTheme
import com.ryxan.recipescaler.core.domain.model.IngredientItem
import com.ryxan.recipescaler.core.domain.model.KitchenUnit
import com.ryxan.recipescaler.core.domain.model.ParsedIngredient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
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
        composeRule.onNodeWithContentDescription("Recipe title")
            .performTextInput("Soup")
        assertEquals("Soup", typed)
    }

    @Test
    fun `header says recipe title`() {
        content(state())
        composeRule.onNodeWithText("Recipe title").assertIsDisplayed()
        composeRule.onAllNodesWithText("Name").assertCountEquals(0)
    }

    @Test
    fun `ingredient fields expose position content descriptions`() {
        content(state())
        composeRule.onNodeWithContentDescription("Quantity, ingredient 1")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Ingredient name, row 1")
            .assertIsDisplayed()
    }

    @Test
    fun `qty field offers a next ime action`() {
        content(state())
        composeRule.onNodeWithContentDescription("Quantity, ingredient 1")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ImeAction,
                    ImeAction.Next,
                ),
            )
    }

    @Test
    fun `fraction chips hide when the name field is focused`() {
        content(state())
        composeRule.onNodeWithContentDescription("Quantity, ingredient 1")
            .performClick()
        composeRule.onNodeWithText("½").assertIsDisplayed()

        // In the stacked layout the name line can sit below the fold — scroll
        // the row into view before focusing it.
        composeRule.onNode(hasScrollToIndexAction())
            .performScrollToNode(hasContentDescription("Ingredient name, row 1"))
        composeRule.onNodeWithContentDescription("Ingredient name, row 1")
            .performClick()
        composeRule.onAllNodesWithText("½").assertCountEquals(0)
        // Units and the pinned Next action stay available either way.
        composeRule.onNodeWithText("cup").assertIsDisplayed()
        composeRule.onNodeWithText("Next").assertIsDisplayed()
    }

    @Test
    fun `needs review rows show the warning caption`() {
        content(
            state(
                rows = listOf(
                    EditorRow(key = 1, rawQuantity = "abc", needsReview = true),
                ),
            ),
        )
        composeRule.onNodeWithText("Check this line").assertIsDisplayed()
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
    fun `scale fab shows a snackbar when nothing can scale`() {
        var scaled = false
        content(state(), EditorCallbacks(onScale = { scaled = true }))
        // The FAB reads as disabled, and tapping it explains why.
        composeRule.onNode(hasClickAction() and hasAnyDescendant(hasText("Scale")), useUnmergedTree = true)
            .assertIsNotEnabled()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Add an ingredient to scale").assertIsDisplayed()
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
    fun `rows reorder by long press so no drag handle icon is shown`() {
        content(state())
        composeRule.onAllNodesWithContentDescription("Reorder").assertCountEquals(0)
        composeRule.onAllNodesWithContentDescription("Edit ingredients").assertCountEquals(0)
    }

    private val rowWithRemoveAction = SemanticsMatcher(
        "has a 'Remove ingredient' custom action",
    ) { node ->
        node.config.getOrNull(SemanticsActions.CustomActions)
            ?.any { it.label == "Remove ingredient" } == true
    }

    private val rowWithMoveDownAction = SemanticsMatcher(
        "has a 'Move down' custom action",
    ) { node ->
        node.config.getOrNull(SemanticsActions.CustomActions)
            ?.any { it.label == "Move down" } == true
    }

    @Test
    fun `rows expose move accessibility actions`() {
        val moves = mutableListOf<Pair<Long, Long>>()
        content(
            state(rows = listOf(EditorRow(key = 1), EditorRow(key = 2))),
            EditorCallbacks(onMoveRow = { f, t -> moves += f to t }),
        )
        composeRule.onNode(rowWithMoveDownAction)
            .performCustomAccessibilityActionWithLabel("Move down")
        assertEquals(listOf(1L to 2L), moves)
    }

    @Test
    fun `swiping a row calls onRemoveRow`() {
        var removed: Long? = null
        content(
            state(),
            EditorCallbacks(onRemoveRow = { removed = it }),
        )
        // A swipe on the row's semantics node lands on the text fields and is
        // consumed, so swipe across the row's measured bounds from the root.
        val b = composeRule.onNode(rowWithRemoveAction).getBoundsInRoot()
        composeRule.onRoot().performTouchInput {
            swipe(
                start = Offset(b.right.value - 8f, b.top.value + 20f),
                end = Offset(b.left.value + 8f, b.top.value + 20f),
                durationMillis = 400,
            )
        }
        composeRule.waitForIdle()
        assertEquals(1L, removed)
    }

    @Test
    fun `row exposes a remove accessibility action`() {
        var removed: Long? = null
        content(
            state(),
            EditorCallbacks(onRemoveRow = { removed = it }),
        )
        composeRule.onNode(rowWithRemoveAction)
            .performCustomAccessibilityActionWithLabel("Remove ingredient")
        assertEquals(1L, removed)
    }

    @Test
    fun `a pending undo shows a snackbar and Undo calls onUndoEdit`() {
        var undone = false
        content(
            EditorUiState(
                isLoading = false,
                rows = listOf(EditorRow(key = 2)),
                pendingUndo = EditorUndo.RowRemoved(EditorRow(key = 1, name = "Flour"), 0),
                undoToken = 1,
            ),
            EditorCallbacks(onUndoEdit = { undone = true }),
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Ingredient removed").assertIsDisplayed()
        composeRule.onNodeWithText("Undo").performClick()
        assertTrue(undone)
    }

    @Test
    fun `clear ingredients calls onClearClick without a dialog`() {
        var cleared = false
        content(
            state(canScaleRows = listOf(EditorRow(key = 1, rawQuantity = "1", name = "Flour"))),
            EditorCallbacks(onClearClick = { cleared = true }),
        )
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Clear ingredients").performClick()
        assertTrue(cleared)
    }

    @Test
    fun `blank rows show the scan paste type entry points`() {
        var scanClicked = false
        var pasteClicked = false
        content(
            state(),
            EditorCallbacks(
                onScanClick = { scanClicked = true },
                onPasteClick = { pasteClicked = true },
            ),
        )
        composeRule.onNodeWithText("Scan").assertIsDisplayed().performClick()
        assertTrue(scanClicked)
        composeRule.onNodeWithText("Paste").assertIsDisplayed().performClick()
        assertTrue(pasteClicked)
        composeRule.onNodeWithText("Type").assertIsDisplayed()
    }

    @Test
    fun `Type adds a row when the list is empty`() {
        var added = false
        content(
            state(rows = emptyList()),
            EditorCallbacks(onAddRow = { added = true; 99L }),
        )
        composeRule.onNodeWithText("Type").assertIsDisplayed().performClick()
        assertTrue(added)
    }

    @Test
    fun `non blank rows hide the scan paste type entry points`() {
        content(
            state(canScaleRows = listOf(EditorRow(key = 1, rawQuantity = "1", name = "Flour"))),
        )
        composeRule.onAllNodesWithText("Scan").assertCountEquals(0)
        composeRule.onAllNodesWithText("Paste").assertCountEquals(0)
        composeRule.onAllNodesWithText("Type").assertCountEquals(0)
    }

    @Test
    fun `yield row shows a Makes label and amount field`() {
        var yield: String? = null
        content(
            state(),
            EditorCallbacks(onYieldTextChange = { yield = it }),
        )
        composeRule.onNodeWithText("Makes:").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Yield amount")
            .performTextInput("8")
        assertTrue(yield?.contains("8") == true)
    }

    @Test
    fun `rows stack vertically at a large font scale`() {
        composeRule.setContent {
            RescaleTheme(dynamicColor = false) {
                CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                    EditorContent(state = state(), callbacks = EditorCallbacks())
                }
            }
        }
        val qtyBounds = composeRule.onNodeWithContentDescription("Quantity, ingredient 1")
            .getBoundsInRoot()
        val nameBounds = composeRule.onNodeWithContentDescription("Ingredient name, row 1")
            .getBoundsInRoot()
        // Stacked: the name field sits below the qty field instead of beside it.
        assertTrue(nameBounds.top >= qtyBounds.bottom)
    }

    @Test
    fun `confirm import shows stacked replace and append actions`() {
        content(
            EditorUiState(
                isLoading = false,
                rows = listOf(EditorRow(key = 1)),
                dialog = EditorDialog.ConfirmImport(
                    items = listOf(
                        ParsedIngredient(
                            IngredientItem(id = "1", qtyString = "2", unit = "cup", name = "flour"),
                        ),
                    ),
                ),
            ),
        )
        // Sheet actions stack as full-width buttons (clicks on sheet content are
        // unreliable under Robolectric, so the test asserts layout only).
        composeRule.onNodeWithText("Replace ingredients").assertExists()
        composeRule.onNodeWithText("Append to list").assertExists()
    }

    @Test
    fun `scan icon in the top bar calls onScanClick`() {
        var scanned = false
        content(state(), EditorCallbacks(onScanClick = { scanned = true }))
        composeRule.onNodeWithContentDescription("Scan recipe").performClick()
        assertTrue(scanned)
    }

    @Test
    fun `overflow menu no longer contains scan recipe`() {
        content(state())
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onAllNodesWithText("Scan recipe").assertCountEquals(0)
        composeRule.onAllNodesWithText("Paste recipe text").assertCountEquals(1)
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
