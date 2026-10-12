package com.ryxan.recipescaler.feature.pro

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ryxan.recipescaler.core.data.billing.ProProduct
import com.ryxan.recipescaler.core.designsystem.RescaleTheme
import org.junit.Assert.assertEquals
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
    ) {
        composeRule.setContent {
            RescaleTheme(dynamicColor = false) {
                ProContent(state = state, onPurchase = onPurchase, onRestore = onRestore)
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
    fun `pro user sees the thank-you state`() {
        content(ProUiState(isPro = true, products = products, loading = false))

        composeRule.onNodeWithText("You're Pro — thank you!").assertIsDisplayed()
        composeRule.onNodeWithText("$3.99 / month").assertDoesNotExist()
    }

    @Test
    fun `pending purchase shows the pending state`() {
        content(ProUiState(isPending = true, loading = false))

        composeRule.onNodeWithText("Purchase pending").assertIsDisplayed()
    }
}
