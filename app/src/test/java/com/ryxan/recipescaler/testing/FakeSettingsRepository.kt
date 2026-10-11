package com.ryxan.recipescaler.testing

import com.ryxan.recipescaler.core.domain.formatting.DisplayMode
import com.ryxan.recipescaler.core.domain.formatting.MeasurementSystem
import com.ryxan.recipescaler.core.domain.model.AppSettings
import com.ryxan.recipescaler.core.domain.model.ThemeMode
import com.ryxan.recipescaler.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeSettingsRepository : SettingsRepository {

    private val _settings = MutableStateFlow(AppSettings())
    override val settings: Flow<AppSettings> = _settings

    override suspend fun setMeasurementSystem(v: MeasurementSystem) {
        _settings.update { it.copy(display = it.display.copy(system = v)) }
    }

    override suspend fun setDisplayMode(v: DisplayMode) {
        _settings.update { it.copy(display = it.display.copy(mode = v)) }
    }

    override suspend fun setThemeMode(v: ThemeMode) {
        _settings.update { it.copy(themeMode = v) }
    }

    override suspend fun setDynamicColor(v: Boolean) {
        _settings.update { it.copy(dynamicColor = v) }
    }
}
