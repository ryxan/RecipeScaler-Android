package com.ryxan.recipescaler.feature.settings

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.ryxan.recipescaler.core.designsystem.RescaleTheme
import com.ryxan.recipescaler.core.domain.formatting.DisplayMode
import com.ryxan.recipescaler.core.domain.formatting.MeasurementSystem
import com.ryxan.recipescaler.core.domain.importer.AiImportConfig
import com.ryxan.recipescaler.core.domain.model.AppSettings
import com.ryxan.recipescaler.core.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsV2ContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun content(
        settings: AppSettings = AppSettings(),
        onSystem: (MeasurementSystem) -> Unit = {},
        onDisplayMode: (DisplayMode) -> Unit = {},
        onThemeMode: (ThemeMode) -> Unit = {},
        onUiV2: (Boolean) -> Unit = {},
        isPro: Boolean = false,
        onProClick: () -> Unit = {},
        showDebug: Boolean = false,
        aiScansRemaining: Int = 0,
        onSetAiScans: (Int) -> Unit = {},
        showDynamicColor: Boolean = true,
    ) {
        composeRule.setContent {
            RescaleTheme(dynamicColor = false, uiV2 = true) {
                SettingsV2Content(
                    settings = settings,
                    onSystem = onSystem,
                    onDisplayMode = onDisplayMode,
                    onThemeMode = onThemeMode,
                    onDynamicColor = {},
                    onUiV2 = onUiV2,
                    showUiV2 = true,
                    showDynamicColor = showDynamicColor,
                    versionName = "1.0-test",
                    isPro = isPro,
                    onProClick = onProClick,
                    showDebug = showDebug,
                    aiScansRemaining = aiScansRemaining,
                    onSetAiScans = onSetAiScans,
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
        content(isPro = true, onProClick = { clicked = true })
        composeRule.onNodeWithText("Rescale Pro").assertIsDisplayed()
        composeRule.onNodeWithText("Active").assertIsDisplayed()
        composeRule.onNodeWithText("Rescale Pro").performClick()
        assertEquals(true, clicked)
    }

    @Test
    fun `sections render uppercase and version shows`() {
        content()
        composeRule.onNodeWithText("MEASUREMENT SYSTEM").assertIsDisplayed()
        composeRule.onNodeWithText("DISPLAY").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("THEME").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("ABOUT").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Version 1.0-test")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `selecting Decimals calls onDisplayMode`() {
        var selected: DisplayMode? = null
        content(onDisplayMode = { selected = it })
        composeRule.onNodeWithText("Decimals").performScrollTo().performClick()
        assertEquals(DisplayMode.DECIMAL, selected)
    }

    @Test
    fun `selecting Dark calls onThemeMode`() {
        var selected: ThemeMode? = null
        content(onThemeMode = { selected = it })
        composeRule.onNodeWithText("Dark").performScrollTo().performClick()
        assertEquals(ThemeMode.DARK, selected)
    }

    @Test
    fun `experimental ui switch calls onUiV2`() {
        var toggled: Boolean? = null
        content(onUiV2 = { toggled = it })
        composeRule.onNodeWithText("New look (experimental)")
            .performScrollTo()
            .performClick()
        assertEquals(true, toggled)
    }

    @Test
    fun `debug AI scan controls hidden without showDebug`() {
        content()
        composeRule.onAllNodesWithText("DEBUG").assertCountEquals(0)
        composeRule.onAllNodesWithText("AI scans left: 0").assertCountEquals(0)
    }

    @Test
    fun `debug AI scan preset calls onSetAiScans`() {
        var setTo: Int? = null
        content(showDebug = true, aiScansRemaining = 7, onSetAiScans = { setTo = it })
        composeRule.onNodeWithText("AI scans left: 7").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("5").performScrollTo().performClick()
        assertEquals(5, setTo)
        composeRule.onNodeWithText("Reset").performClick()
        assertEquals(AiImportConfig.FREE_LIFETIME_LIMIT, setTo)
    }
}
