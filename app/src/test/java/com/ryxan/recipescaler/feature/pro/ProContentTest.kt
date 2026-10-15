package com.ryxan.recipescaler.feature.pro

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ryxan.recipescaler.core.data.billing.ProPlan
import com.ryxan.recipescaler.core.data.billing.ProProduct
import com.ryxan.recipescaler.core.designsystem.RescaleTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun content(
        state: ProUiState,
        onPurchase: (String) -> Unit = {},
        onRestore: () -> Unit = {},
        onTryAiImport: () -> Unit = {},
        onBackToRecipes: () -> Unit = {},
        onManageSubscription: () -> Unit = {},
    ) {
        composeRule.setContent {
            RescaleTheme(dynamicColor = false) {
                ProContent(
                    state = state,
                    onPurchase = onPurchase,
                    onRestore = onRestore,
                    onTryAiImport = onTryAiImport,
                    onBackToRecipes = onBackToRecipes,
                    onManageSubscription = onManageSubscription,
                )
            }
        }
    }

    private val products = listOf(
        ProProduct("rescale_pro_monthly", "Pro monthly", "$3.99", isSubscription = true),
        ProProduct("rescale_pro_lifetime", "Pro lifetime", "$14.99", isSubscription = false),
    )

    @Test
    fun `products render with their price buttons`() {
        content(ProUiState(products = products, loading = false))

        composeRule.onNodeWithText("Unlimited AI recipe import").assertIsDisplayed()
        composeRule.onNodeWithText("$3.99 / month").assertIsDisplayed()
        composeRule.onNodeWithText("$14.99 · lifetime").assertIsDisplayed()
        composeRule.onNodeWithText("Restore purchases").assertIsDisplayed()
    }

    @Test
    fun `tapping a product calls onPurchase with its id`() {
        var purchased: String? = null
        content(ProUiState(products = products, loading = false), onPurchase = { purchased = it })

        composeRule.onNodeWithText("$14.99 · lifetime").performClick()

        assertEquals("rescale_pro_lifetime", purchased)
    }

    @Test
    fun `empty products shows the unavailable message`() {
        content(ProUiState(products = emptyList(), loading = false))

        composeRule.onNodeWithText("Purchases aren't available right now").assertIsDisplayed()
    }

    @Test
    fun `pro user sees the celebration state with next steps`() {
        content(ProUiState(isPro = true, products = products, loading = false))

        composeRule.onNodeWithText("You're Pro!").assertIsDisplayed()
        composeRule.onNodeWithText("Thank you — everything is unlocked.").assertIsDisplayed()
        composeRule.onNodeWithText("What's unlocked").assertIsDisplayed()
        composeRule.onNodeWithText("No ads").assertIsDisplayed()
        composeRule.onNodeWithText("Scan your first recipe").assertIsDisplayed()
        composeRule.onNodeWithText("Back to recipes").assertIsDisplayed()
        composeRule.onNodeWithText("$3.99 / month").assertDoesNotExist()
        composeRule.onNodeWithText("Restore purchases").assertDoesNotExist()
    }

    @Test
    fun `monthly plan shows plan row and manage subscription`() {
        content(ProUiState(isPro = true, plan = ProPlan.Monthly, loading = false))

        composeRule.onNodeWithText("Monthly plan").assertIsDisplayed()
        composeRule.onNodeWithText("Manage subscription").assertIsDisplayed()
        composeRule.onNodeWithText("Lifetime access").assertDoesNotExist()
    }

    @Test
    fun `lifetime plan shows lifetime row and no manage link`() {
        content(ProUiState(isPro = true, plan = ProPlan.Lifetime, loading = false))

        composeRule.onNodeWithText("Lifetime access").assertIsDisplayed()
        composeRule.onNodeWithText("Manage subscription").assertDoesNotExist()
    }

    @Test
    fun `unknown plan hides the plan row`() {
        content(ProUiState(isPro = true, plan = null, loading = false))

        composeRule.onNodeWithText("Monthly plan").assertDoesNotExist()
        composeRule.onNodeWithText("Lifetime access").assertDoesNotExist()
    }

    @Test
    fun `tapping scan calls onTryAiImport`() {
        var called = false
        content(
            ProUiState(isPro = true, loading = false),
            onTryAiImport = { called = true },
        )

        composeRule.onNodeWithText("Scan your first recipe").performClick()

        assertTrue(called)
    }

    @Test
    fun `tapping back calls onBackToRecipes`() {
        var called = false
        content(
            ProUiState(isPro = true, loading = false),
            onBackToRecipes = { called = true },
        )

        composeRule.onNodeWithText("Back to recipes").performClick()

        assertTrue(called)
    }

    @Test
    fun `tapping manage calls onManageSubscription`() {
        var called = false
        content(
            ProUiState(isPro = true, plan = ProPlan.Monthly, loading = false),
            onManageSubscription = { called = true },
        )

        composeRule.onNodeWithText("Manage subscription").performClick()

        assertTrue(called)
    }

    @Test
    fun `pending purchase shows the pending state with explainer`() {
        content(ProUiState(isPending = true, loading = false))

        composeRule.onNodeWithText("Purchase pending").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Your payment is still processing. Pro will unlock automatically — nothing else to do.",
        ).assertIsDisplayed()
    }
}
