package com.ryxan.recipescaler

import com.ryxan.recipescaler.core.domain.model.AppSettings
import com.ryxan.recipescaler.core.domain.model.ThemeMode
import com.ryxan.recipescaler.testing.FakeSettingsRepository
import com.ryxan.recipescaler.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `exposes repository settings`() = runTest {
        val repo = FakeSettingsRepository()
        val vm = MainViewModel(repo)
        assertEquals(AppSettings(), vm.settings.value)

        repo.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, vm.settings.value.themeMode)
    }
}
