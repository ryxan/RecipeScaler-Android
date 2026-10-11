package com.ryxan.recipescaler.feature.settings

import com.ryxan.recipescaler.core.domain.formatting.DisplayMode
import com.ryxan.recipescaler.core.domain.formatting.MeasurementSystem
import com.ryxan.recipescaler.core.domain.model.ThemeMode
import com.ryxan.recipescaler.testing.FakeSettingsRepository
import com.ryxan.recipescaler.testing.MainDispatcherRule
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `setters delegate to the repository and update state`() = runTest {
        val repo = FakeSettingsRepository()
        val vm = SettingsViewModel(repo)

        vm.setMeasurementSystem(MeasurementSystem.METRIC)
        vm.setDisplayMode(DisplayMode.DECIMAL)
        vm.setThemeMode(ThemeMode.DARK)
        vm.setDynamicColor(false)

        val s = repo.settings.first()
        assertEquals(MeasurementSystem.METRIC, s.display.system)
        assertEquals(DisplayMode.DECIMAL, s.display.mode)
        assertEquals(ThemeMode.DARK, s.themeMode)
        assertFalse(s.dynamicColor)

        // WhileSubscribed: collecting the StateFlow picks up the repo's current value.
        val emitted = vm.settings.filter { it.themeMode == ThemeMode.DARK }.first()
        assertEquals(MeasurementSystem.METRIC, emitted.display.system)
        assertEquals(DisplayMode.DECIMAL, emitted.display.mode)
        assertFalse(emitted.dynamicColor)
    }
}
