package com.ryxan.recipescaler.feature.recipes

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import com.ryxan.recipescaler.core.designsystem.RescaleTheme
import com.ryxan.recipescaler.core.domain.model.Recipe
import com.ryxan.recipescaler.core.domain.model.RecipeSummary
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
class RecipeListV2ContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun summary(
        id: Long,
        title: String,
        yieldAmount: Double = 4.0,
        yieldLabel: String = "servings",
        ingredientCount: Int = 7,
    ) = RecipeSummary(
        id = id,
        title = title,
        yieldAmount = yieldAmount,
        yieldLabel = yieldLabel,
        ingredientCount = ingredientCount,
        updatedAt = id,
    )

    private fun content(
        state: RecipeListUiState,
        onQueryChange: (String) -> Unit = {},
        onOpenRecipe: (Long) -> Unit = {},
        onNewRecipe: () -> Unit = {},
        onQuickScale: () -> Unit = {},
        onDelete: (Long) -> Unit = {},
        onUndo: () -> Unit = {},
        onDismissUndo: () -> Unit = {},
        onNavigateToSettings: () -> Unit = {},
    ) {
        composeRule.setContent {
            RescaleTheme(dynamicColor = false, uiV2 = true) {
                RecipeListV2Content(
                    state = state,
                    onQueryChange = onQueryChange,
                    onOpenRecipe = onOpenRecipe,
                    onNewRecipe = onNewRecipe,
                    onQuickScale = onQuickScale,
                    onDelete = onDelete,
                    onUndo = onUndo,
                    onDismissUndo = onDismissUndo,
                    onNavigateToSettings = onNavigateToSettings,
                )
            }
        }
    }

    @Test
    fun `cards render title and meta pills`() {
        content(
            RecipeListUiState(
                isLoading = false,
                recipes = listOf(summary(1, "Bread", yieldAmount = 4.0, ingredientCount = 7)),
            ),
        )
        composeRule.onNodeWithText("Bread").assertIsDisplayed()
        composeRule.onNodeWithText("4 servings").assertIsDisplayed()
        composeRule.onNodeWithText("7 ingredients").assertIsDisplayed()
    }

    @Test
    fun `blank title falls back to untitled and yield pill omitted when non-positive`() {
        content(
            RecipeListUiState(
                isLoading = false,
                recipes = listOf(summary(1, "", yieldAmount = 0.0, ingredientCount = 1)),
            ),
        )
        composeRule.onNodeWithText("Untitled recipe").assertIsDisplayed()
        composeRule.onNodeWithText("1 ingredient").assertIsDisplayed()
        composeRule.onAllNodesWithText("servings", substring = true).assertCountEquals(0)
    }

    @Test
    fun `monogram tile shows first letter of title`() {
        content(
            RecipeListUiState(
                isLoading = false,
                recipes = listOf(summary(1, "100 Good Cookies")),
            ),
        )
        composeRule.onNodeWithText("G").assertExists()
    }

    @Test
    fun `tapping a card calls onOpenRecipe with its id`() {
        var opened = -1L
        content(
            RecipeListUiState(
                isLoading = false,
                recipes = listOf(summary(42, "Cake")),
            ),
            onOpenRecipe = { opened = it },
        )
        composeRule.onNodeWithText("Cake").performClick()
        assertEquals(42L, opened)
    }

    @Test
    fun `cards have no scale chip — the card itself opens the recipe`() {
        content(
            RecipeListUiState(
                isLoading = false,
                recipes = listOf(summary(5, "Stew")),
            ),
        )
        composeRule.onAllNodesWithText("Scale").assertCountEquals(0)
    }

    @Test
    fun `fab calls onNewRecipe`() {
        var clicked = false
        content(RecipeListUiState(isLoading = false), onNewRecipe = { clicked = true })
        composeRule.onNodeWithText("New recipe", useUnmergedTree = true).performClick()
        assertTrue(clicked)
    }

    @Test
    fun `empty state shows when list is empty`() {
        content(RecipeListUiState(isLoading = false))
        composeRule.onNodeWithText("No recipes yet").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Save a recipe once, then scale it to any batch size.",
        ).assertIsDisplayed()
    }

    @Test
    fun `empty state quick scale button calls onQuickScale`() {
        var clicked = false
        content(RecipeListUiState(isLoading = false), onQuickScale = { clicked = true })
        composeRule.onNodeWithText("Quick scale").performClick()
        assertTrue(clicked)
    }

    @Test
    fun `top bar quick scale icon calls onQuickScale`() {
        var clicked = false
        content(
            RecipeListUiState(isLoading = false, recipes = listOf(summary(1, "Bread"))),
            onQuickScale = { clicked = true },
        )
        composeRule.onNodeWithContentDescription("Quick scale").performClick()
        assertTrue(clicked)
    }

    @Test
    fun `swiping a card reveals delete but does not delete`() {
        var deleted = -1L
        content(
            RecipeListUiState(isLoading = false, recipes = listOf(summary(7, "Cake"))),
            onDelete = { deleted = it },
        )
        composeRule.onNodeWithText("Cake").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        assertEquals(-1L, deleted)
        composeRule.onNodeWithContentDescription("Delete").assertIsDisplayed()
    }

    @Test
    fun `tapping the revealed delete strip calls onDelete`() {
        var deleted = -1L
        content(
            RecipeListUiState(isLoading = false, recipes = listOf(summary(7, "Cake"))),
            onDelete = { deleted = it },
        )
        composeRule.onNodeWithText("Cake").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Delete")
            .performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(7L, deleted)
    }

    @Test
    fun `card exposes a delete accessibility action`() {
        var deleted = -1L
        content(
            RecipeListUiState(isLoading = false, recipes = listOf(summary(7, "Cake"))),
            onDelete = { deleted = it },
        )
        composeRule.onNode(
            hasAnyDescendant(hasText("Cake")) and hasClickAction(),
            useUnmergedTree = true,
        ).performCustomAccessibilityActionWithLabel("Delete")
        assertEquals(7L, deleted)
    }

    @Test
    fun `pending undo shows a snackbar and Undo calls onUndo`() {
        var undo = false
        content(
            RecipeListUiState(
                isLoading = false,
                pendingUndo = recipe(id = 9, title = "Cake"),
            ),
            onUndo = { undo = true },
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Deleted “Cake”").assertIsDisplayed()
        composeRule.onNodeWithText("Undo").performClick()
        assertTrue(undo)
    }

    @Test
    fun `no-results state shows for a non-blank query`() {
        content(RecipeListUiState(query = "zzz", isLoading = false))
        composeRule.onNodeWithText("No recipes match your search").assertIsDisplayed()
    }

    @Test
    fun `bottom bar settings tab calls onNavigateToSettings`() {
        var opened = false
        content(
            RecipeListUiState(isLoading = false, recipes = listOf(summary(1, "Bread"))),
            onNavigateToSettings = { opened = true },
        )
        composeRule.onNodeWithText("Settings").performClick()
        assertTrue(opened)
    }

    private fun recipe(id: Long, title: String) = Recipe(
        id = id,
        title = title,
        yieldAmount = 4.0,
        yieldLabel = "servings",
        ingredients = emptyList(),
        createdAt = 1L,
        updatedAt = 1L,
    )
}
