package com.ryxan.recipescaler

import androidx.compose.ui.test.junit4.createComposeRule
import com.ryxan.recipescaler.core.designsystem.RescaleTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UiV2FlagTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `uiV2 flag flows through RescaleTheme`() {
        val seen = mutableListOf<Boolean>()
        composeRule.setContent {
            RescaleTheme(dynamicColor = false, uiV2 = false) {
                seen += RescaleTheme.uiV2
            }
            RescaleTheme(dynamicColor = false, uiV2 = true) {
                seen += RescaleTheme.uiV2
            }
        }
        composeRule.waitForIdle()
        assertEquals(listOf(false, true), seen.distinct())
    }
}
