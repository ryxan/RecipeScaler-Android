package com.ryxan.recipescaler.core.domain.repository

import com.ryxan.recipescaler.core.domain.formatting.DisplayMode
import com.ryxan.recipescaler.core.domain.formatting.MeasurementSystem
import com.ryxan.recipescaler.core.domain.model.AppSettings
import com.ryxan.recipescaler.core.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setMeasurementSystem(v: MeasurementSystem)
    suspend fun setDisplayMode(v: DisplayMode)
    suspend fun setThemeMode(v: ThemeMode)
    suspend fun setDynamicColor(v: Boolean)
    suspend fun setUiV2(v: Boolean)
    suspend fun setShowScaleWarning(v: Boolean)
}
