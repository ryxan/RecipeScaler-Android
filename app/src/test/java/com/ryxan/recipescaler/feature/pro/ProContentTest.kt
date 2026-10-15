package com.ryxan.recipescaler.feature.pro

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
        onRetry: () -> Unit = {},
    ) {
        composeRule.setContent {
            RescaleTheme(dynamicColor = false) {
                ProContent(
                    state = state,
                    onPurchase = onPurchase,
                    onRestore = onRestore,
                    onRetry = onRetry,
                )
            }
        }
    }

    private val products = listOf(
        ProProduct("rescale_pro_monthly", "Pro monthly", "$3.99", isSubscription = true),
        ProProduct("rescale_pro_lifetime", "Pro lifetime", "$14.99", isSubscription = false),
    )

    @Test
    fun `plans render with prices, the primary action and restore`() {
        content(ProUiState(products = products, loading = false))

        composeRule.onNodeWithText("AI recipe import, every day").assertIsDisplayed()
        composeRule.onNodeWithText("$14.99").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("$3.99 / month").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Unlock for $14.99").assertIsDisplayed()
        composeRule.onNodeWithText("Restore purchases").assertIsDisplayed()
    }

    @Test
    fun `lifetime is preselected and the action buys it`() {
        var purchased: String? = null
        content(ProUiState(products = products, loading = false), onPurchase = { purchased = it })

        composeRule.onNodeWithText("Unlock for $14.99").performClick()

        assertEquals("rescale_pro_lifetime", purchased)
    }

    @Test
    fun `selecting monthly changes the action and buys monthly`() {
        var purchased: String? = null
        content(ProUiState(products = products, loading = false), onPurchase = { purchased = it })

        composeRule.onNodeWithText("Monthly").performScrollTo().performClick()
        composeRule.onNodeWithText("Subscribe for $3.99 / month").performClick()

        assertEquals("rescale_pro_monthly", purchased)
    }

    @Test
    fun `the AI scan pack is not shown as a Pro plan`() {
        val withPack = products + ProProduct("rescale_ai_scans_100", "100 scans", "$4.99", isSubscription = false)
        content(ProUiState(products = withPack, loading = false))

        composeRule.onNodeWithText("$4.99").assertDoesNotExist()
    }

    @Test
    fun `empty products shows the unavailable message and retry`() {
        var retried = false
        content(ProUiState(products = emptyList(), loading = false), onRetry = { retried = true })

        composeRule.onNodeWithText("Purchases aren't available right now").assertIsDisplayed()
        composeRule.onNodeWithText("Try again").performScrollTo().performClick()

        assertEquals(true, retried)
    }

    @Test
    fun `pro user sees the thank-you state and no purchase UI`() {
        content(ProUiState(isPro = true, products = products, loading = false))

        composeRule.onNodeWithText("You're Pro").assertIsDisplayed()
        composeRule.onNodeWithText("Unlock for $14.99").assertDoesNotExist()
        composeRule.onNodeWithText("Restore purchases").assertDoesNotExist()
    }

    @Test
    fun `pending purchase shows the pending state and hides the action`() {
        content(ProUiState(isPending = true, products = products, loading = false))

        composeRule.onNodeWithText("Purchase pending").assertIsDisplayed()
        composeRule.onNodeWithText("Unlock for $14.99").assertDoesNotExist()
        composeRule.onNodeWithText("Restore purchases").assertIsDisplayed()
    }

    @Test
    fun `verifying purchase hides the action and shows the verifying note`() {
        content(ProUiState(products = products, loading = false, verifying = true))

        composeRule.onNodeWithText("Verifying purchase…").assertIsDisplayed()
        composeRule.onNodeWithText("Unlock for $14.99").assertDoesNotExist()
        composeRule.onNodeWithText("Restore purchases").assertIsDisplayed()
    }

    @Test
    fun `usage meter shows scans left, and the empty message at zero`() {
        content(ProUiState(products = products, loading = false, freeScansLeft = 7))
        composeRule.onNodeWithText("7 AI scans left").assertIsDisplayed()
    }

    @Test
    fun `usage meter explains the out-of-scans case`() {
        content(ProUiState(products = products, loading = false, freeScansLeft = 0))
        composeRule.onNodeWithText("No AI scans left. On-device scanning is always free.").assertIsDisplayed()
    }

    @Test
    fun `restore calls onRestore`() {
        var restored = false
        content(ProUiState(products = products, loading = false), onRestore = { restored = true })

        composeRule.onNodeWithText("Restore purchases").performClick()

        assertEquals(true, restored)
    }
}
