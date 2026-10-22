package com.ryxan.recipescaler.feature.recipes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
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
class RecipeListContentTest {

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
    ) {
        composeRule.setContent {
            RescaleTheme(dynamicColor = false) {
                RecipeListContent(
                    state = state,
                    onQueryChange = onQueryChange,
                    onOpenRecipe = onOpenRecipe,
                    onNewRecipe = onNewRecipe,
                    onQuickScale = onQuickScale,
                    onDelete = onDelete,
                    onUndo = onUndo,
                    onDismissUndo = onDismissUndo,
                    onNavigateToSettings = {},
                )
            }
        }
    }

    @Test
    fun `rows render title and supporting text`() {
        content(
            RecipeListUiState(
                isLoading = false,
                recipes = listOf(summary(1, "Bread", yieldAmount = 4.0, ingredientCount = 7)),
            ),
        )
        composeRule.onNodeWithText("Bread").assertIsDisplayed()
        composeRule.onNodeWithText("4 servings · 7 ingredients").assertIsDisplayed()
    }

    @Test
    fun `blank title falls back to untitled and yield omitted when non-positive`() {
        content(
            RecipeListUiState(
                isLoading = false,
                recipes = listOf(summary(1, "", yieldAmount = 0.0, ingredientCount = 1)),
            ),
        )
        composeRule.onNodeWithText("Untitled recipe").assertIsDisplayed()
        composeRule.onNodeWithText("1 ingredient").assertIsDisplayed()
    }

    @Test
    fun `avatar shows first letter of title`() {
        content(
            RecipeListUiState(
                isLoading = false,
                recipes = listOf(summary(1, "100 Good Cookies")),
            ),
        )
        composeRule.onNodeWithText("G").assertExists()
    }

    @Test
    fun `tapping a row calls onOpenRecipe with its id`() {
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
    fun `bottom nav fires tab callbacks`() {
        var scaler = false
        var settings = false
        composeRule.setContent {
            RescaleTheme(dynamicColor = false) {
                RecipeListContent(
                    state = RecipeListUiState(isLoading = false),
                    onQueryChange = {},
                    onOpenRecipe = {},
                    onNewRecipe = {},
                    onQuickScale = {},
                    onDelete = {},
                    onUndo = {},
                    onDismissUndo = {},
                    onNavigateToSettings = { settings = true },
                    onScalerClick = { scaler = true },
                )
            }
        }
        composeRule.onNode(hasText("My Recipes") and hasClickAction()).assertIsDisplayed()
        composeRule.onNodeWithText("Scaler").performClick()
        composeRule.onNode(hasText("Settings") and hasClickAction()).performClick()
        assertTrue(scaler)
        assertTrue(settings)
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
    fun `swiping a row calls onDelete`() {
        var deleted = -1L
        content(
            RecipeListUiState(isLoading = false, recipes = listOf(summary(7, "Cake"))),
            onDelete = { deleted = it },
        )
        composeRule.onNodeWithText("Cake").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        assertEquals(7L, deleted)
    }

    @Test
    fun `row exposes a delete accessibility action`() {
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

    private fun recipe(id: Long, title: String) = Recipe(
        id = id,
        title = title,
        yieldAmount = 4.0,
        yieldLabel = "servings",
        ingredients = emptyList(),
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun `no-results state shows for a non-blank query`() {
        content(RecipeListUiState(query = "zzz", isLoading = false))
        composeRule.onNodeWithText("No recipes match your search").assertIsDisplayed()
    }
}
