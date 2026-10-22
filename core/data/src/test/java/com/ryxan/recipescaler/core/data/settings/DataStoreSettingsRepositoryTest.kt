package com.ryxan.recipescaler.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ryxan.recipescaler.core.domain.formatting.DisplayMode
import com.ryxan.recipescaler.core.domain.formatting.MeasurementSystem
import com.ryxan.recipescaler.core.domain.model.AppSettings
import com.ryxan.recipescaler.core.domain.model.ThemeMode
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataStoreSettingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var file: File

    @Before
    fun setUp() {
        file = File(tempFolder.root, "settings.preferences_pb")
    }

    private fun newDataStore(scope: CoroutineScope): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope) { file }

    @Test
    fun `defaults when nothing stored`() = runTest {
        val repo = DataStoreSettingsRepository(newDataStore(backgroundScope), MeasurementSystem.MATCH_RECIPE)
        assertEquals(AppSettings(), repo.settings.first())
    }

    @Test
    fun `each setter round-trips`() = runTest {
        val repo = DataStoreSettingsRepository(newDataStore(backgroundScope), MeasurementSystem.MATCH_RECIPE)
        repo.setMeasurementSystem(MeasurementSystem.METRIC)
        repo.setDisplayMode(DisplayMode.DECIMAL)
        repo.setThemeMode(ThemeMode.DARK)
        repo.setDynamicColor(false)
        repo.setUiV2(true)
        repo.setShowScaleWarning(true)

        val s = repo.settings.first()
        assertEquals(MeasurementSystem.METRIC, s.display.system)
        assertEquals(DisplayMode.DECIMAL, s.display.mode)
        assertEquals(ThemeMode.DARK, s.themeMode)
        assertEquals(false, s.dynamicColor)
        assertEquals(true, s.uiV2)
        assertEquals(true, s.showScaleWarning)
    }

    @Test
    fun `values persist across a new repository instance on same file`() = runTest {
        val dataStore = newDataStore(backgroundScope)
        val repo = DataStoreSettingsRepository(dataStore, MeasurementSystem.MATCH_RECIPE)
        repo.setThemeMode(ThemeMode.LIGHT)

        val repo2 = DataStoreSettingsRepository(dataStore, MeasurementSystem.MATCH_RECIPE)
        assertEquals(ThemeMode.LIGHT, repo2.settings.first().themeMode)
    }

    @Test
    fun `garbage stored value falls back to default`() = runTest {
        val dataStore = newDataStore(backgroundScope)
        dataStore.edit { it[stringPreferencesKey("theme_mode")] = "PLAID" }

        val repo = DataStoreSettingsRepository(dataStore, MeasurementSystem.MATCH_RECIPE)
        assertEquals(ThemeMode.SYSTEM, repo.settings.first().themeMode)
    }

    @Test
    fun `absent measurement system key yields injected default`() = runTest {
        val repo = DataStoreSettingsRepository(
            newDataStore(backgroundScope),
            MeasurementSystem.US,
        )
        assertEquals(MeasurementSystem.US, repo.settings.first().display.system)
    }

    @Test
    fun `corrupt measurement system key yields injected default`() = runTest {
        val dataStore = newDataStore(backgroundScope)
        dataStore.edit { it[stringPreferencesKey("measurement_system")] = "GALLONS" }

        val repo = DataStoreSettingsRepository(dataStore, MeasurementSystem.US)
        assertEquals(MeasurementSystem.US, repo.settings.first().display.system)
    }
}
