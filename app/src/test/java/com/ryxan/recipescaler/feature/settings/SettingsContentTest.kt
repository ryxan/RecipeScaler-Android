package com.ryxan.recipescaler.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.ryxan.recipescaler.core.designsystem.RescaleTheme
import com.ryxan.recipescaler.core.domain.formatting.DisplayMode
import com.ryxan.recipescaler.core.domain.formatting.MeasurementSystem
import com.ryxan.recipescaler.core.domain.model.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun content(
        settings: AppSettings = AppSettings(),
        onSystem: (MeasurementSystem) -> Unit = {},
        onDisplayMode: (DisplayMode) -> Unit = {},
    ) {
        composeRule.setContent {
            RescaleTheme(dynamicColor = false) {
                SettingsContent(
                    settings = settings,
                    onSystem = onSystem,
                    onDisplayMode = onDisplayMode,
                    onThemeMode = {},
                    onDynamicColor = {},
                    showDynamicColor = true,
                    versionName = "1.0-test",
                )
            }
        }
    }

    @Test
    fun `selecting Metric calls onSystem`() {
        var selected: MeasurementSystem? = null
        content(onSystem = { selected = it })
        composeRule.onNodeWithText("Metric").performClick()
        assertEquals(MeasurementSystem.METRIC, selected)
    }

    @Test
    fun `pro row renders and shows Active when pro`() {
        var clicked = false
        composeRule.setContent {
            RescaleTheme(dynamicColor = false) {
                SettingsContent(
                    settings = AppSettings(),
                    onSystem = {},
                    onDisplayMode = {},
                    onThemeMode = {},
                    onDynamicColor = {},
                    showDynamicColor = false,
                    versionName = "1.0-test",
                    isPro = true,
                    onProClick = { clicked = true },
                )
            }
        }

        composeRule.onNodeWithText("Rescale Pro").assertIsDisplayed()
        composeRule.onNodeWithText("Active").assertIsDisplayed()
        composeRule.onNodeWithText("Rescale Pro").performClick()
        assertEquals(true, clicked)
    }

    @Test
    fun `sections and version render`() {
        content()
        composeRule.onNodeWithText("Measurement system").assertIsDisplayed()
        composeRule.onNodeWithText("Kitchen fractions").assertIsDisplayed()
        // Lower sections can sit below the fold on the small test display.
        composeRule.onNodeWithText("System default").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Version 1.0-test").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `selecting Decimals calls onDisplayMode`() {
        var selected: DisplayMode? = null
        content(onDisplayMode = { selected = it })
        composeRule.onNodeWithText("Decimals").performClick()
        assertEquals(DisplayMode.DECIMAL, selected)
    }
}
