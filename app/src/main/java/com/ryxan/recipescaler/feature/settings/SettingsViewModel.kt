package com.ryxan.recipescaler.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryxan.recipescaler.core.data.billing.ProRepository
import com.ryxan.recipescaler.core.domain.formatting.DisplayMode
import com.ryxan.recipescaler.core.domain.formatting.MeasurementSystem
import com.ryxan.recipescaler.core.domain.model.AppSettings
import com.ryxan.recipescaler.core.domain.model.ThemeMode
import com.ryxan.recipescaler.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val proRepository: ProRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val isPro: StateFlow<Boolean> = proRepository.isPro

    /** Debug-only "Pretend Pro" switch state (not persisted). */
    private val _debugPro = kotlinx.coroutines.flow.MutableStateFlow(false)
    val debugPro: StateFlow<Boolean> = _debugPro

    fun setDebugPro(enabled: Boolean) {
        _debugPro.value = enabled
        proRepository.setDebugPro(if (enabled) true else null)
    }

    fun setMeasurementSystem(v: MeasurementSystem) {
        viewModelScope.launch { repository.setMeasurementSystem(v) }
    }

    fun setDisplayMode(v: DisplayMode) {
        viewModelScope.launch { repository.setDisplayMode(v) }
    }

    fun setThemeMode(v: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(v) }
    }

    fun setDynamicColor(v: Boolean) {
        viewModelScope.launch { repository.setDynamicColor(v) }
    }
}
