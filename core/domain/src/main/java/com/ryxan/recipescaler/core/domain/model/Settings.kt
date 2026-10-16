package com.ryxan.recipescaler.core.domain.model

import com.ryxan.recipescaler.core.domain.formatting.DisplayPrefs

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val display: DisplayPrefs = DisplayPrefs(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val uiV2: Boolean = false,
)
