package com.ryxan.recipescaler.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ryxan.recipescaler.core.domain.formatting.DisplayMode
import com.ryxan.recipescaler.core.domain.formatting.DisplayPrefs
import com.ryxan.recipescaler.core.domain.formatting.MeasurementSystem
import com.ryxan.recipescaler.core.domain.model.AppSettings
import com.ryxan.recipescaler.core.domain.model.ThemeMode
import com.ryxan.recipescaler.core.data.DefaultMeasurementSystem
import com.ryxan.recipescaler.core.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @DefaultMeasurementSystem private val defaultSystem: MeasurementSystem,
) : SettingsRepository {

    private object Keys {
        val MEASUREMENT_SYSTEM = stringPreferencesKey("measurement_system")
        val DISPLAY_MODE = stringPreferencesKey("display_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }

    private val defaults = AppSettings()

    override val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            display = DisplayPrefs(
                system = prefs.enumOr(Keys.MEASUREMENT_SYSTEM, defaultSystem),
                mode = prefs.enumOr(Keys.DISPLAY_MODE, defaults.display.mode),
            ),
            themeMode = prefs.enumOr(Keys.THEME_MODE, defaults.themeMode),
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: defaults.dynamicColor,
        )
    }

    override suspend fun setMeasurementSystem(v: MeasurementSystem) {
        dataStore.edit { it[Keys.MEASUREMENT_SYSTEM] = v.name }
    }

    override suspend fun setDisplayMode(v: DisplayMode) {
        dataStore.edit { it[Keys.DISPLAY_MODE] = v.name }
    }

    override suspend fun setThemeMode(v: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = v.name }
    }

    override suspend fun setDynamicColor(v: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = v }
    }

    /** Corrupt or unknown stored values fall back to [default] instead of crashing. */
    private inline fun <reified T : Enum<T>> Preferences.enumOr(
        key: Preferences.Key<String>,
        default: T,
    ): T = this[key]?.let { raw -> runCatching { enumValueOf<T>(raw) }.getOrNull() } ?: default
}
