package com.ryxan.recipescaler.feature.editor

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.ryxan.recipescaler.core.designsystem.RescaleTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScanSourceSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun sheet(state: EditorUiState) {
        composeRule.setContent {
            RescaleTheme(dynamicColor = false) {
                ScanSourceSheet(state = state, callbacks = EditorCallbacks())
            }
        }
    }

    @Test
    fun `sources render and pro sees the handwriting hint`() {
        sheet(EditorUiState(aiAvailable = true, isPro = true))

        composeRule.onNodeWithText("Scan").assertIsDisplayed()
        composeRule.onNodeWithText("Choose existing photo").assertIsDisplayed()
        // The sheet collapsed to one Scan entry: the document scanner already
        // covers camera capture and gallery import.
        composeRule.onAllNodesWithText("Take photo").assertCountEquals(0)
        composeRule.onAllNodesWithText("Scan document").assertCountEquals(0)
        composeRule.onAllNodesWithText("Choose from gallery").assertCountEquals(0)
        // The AI row can sit below the sheet's visible fold in tests — assert
        // it exists rather than that it's on screen.
        composeRule.onNodeWithText("Use AI import").assertExists()
        composeRule.onNodeWithText("Best for handwriting").assertExists()
    }

    @Test
    fun `free user with credits left sees the remaining count`() {
        sheet(EditorUiState(aiAvailable = true, isPro = false, aiScansRemaining = 2))

        composeRule.onNodeWithText("Best for handwriting · 2 left").assertExists()
    }

    @Test
    fun `free user with no credits sees the Pro upsell`() {
        sheet(EditorUiState(aiAvailable = true, isPro = false, aiScansRemaining = 0))

        composeRule.onNodeWithText("Pro feature").assertExists()
    }

    @Test
    fun `AI switch is hidden when AI is unavailable`() {
        sheet(EditorUiState(aiAvailable = false))

        composeRule.onAllNodesWithText("Use AI import").assertCountEquals(0)
    }
}
