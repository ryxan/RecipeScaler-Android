package com.ryxan.recipescaler.feature.recipes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ryxan.recipescaler.core.designsystem.RescaleTheme
import com.ryxan.recipescaler.core.domain.model.RecipeSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
    ) {
        composeRule.setContent {
            RescaleTheme(dynamicColor = false) {
                RecipeListContent(
                    state = state,
                    onQueryChange = onQueryChange,
                    onOpenRecipe = onOpenRecipe,
                    onNewRecipe = onNewRecipe,
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
        composeRule.onNodeWithText("Tap New recipe to add one").assertIsDisplayed()
    }

    @Test
    fun `no-results state shows for a non-blank query`() {
        content(RecipeListUiState(query = "zzz", isLoading = false))
        composeRule.onNodeWithText("No recipes match your search").assertIsDisplayed()
    }
}
