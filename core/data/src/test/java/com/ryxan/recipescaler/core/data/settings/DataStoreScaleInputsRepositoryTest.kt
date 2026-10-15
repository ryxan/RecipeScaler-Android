package com.ryxan.recipescaler.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ryxan.recipescaler.core.domain.model.SavedScaleInputs
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataStoreScaleInputsRepositoryTest {

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
    fun `no saved inputs yields null`() = runTest {
        val repo = DataStoreScaleInputsRepository(newDataStore(backgroundScope))
        assertNull(repo.observe(1L).first())
    }

    @Test
    fun `saved inputs round-trip per recipe`() = runTest {
        val repo = DataStoreScaleInputsRepository(newDataStore(backgroundScope))
        repo.save(1L, SavedScaleInputs("4", "6"))
        repo.save(2L, SavedScaleInputs("2", "8"))

        assertEquals(SavedScaleInputs("4", "6"), repo.observe(1L).first())
        assertEquals(SavedScaleInputs("2", "8"), repo.observe(2L).first())
    }

    @Test
    fun `saving one recipe does not disturb another`() = runTest {
        val repo = DataStoreScaleInputsRepository(newDataStore(backgroundScope))
        repo.save(1L, SavedScaleInputs("4", "6"))
        repo.save(1L, SavedScaleInputs("4", "10"))

        assertEquals(SavedScaleInputs("4", "10"), repo.observe(1L).first())
    }

    @Test
    fun `half-written keys read as absent`() = runTest {
        val dataStore = newDataStore(backgroundScope)
        dataStore.edit { it[stringPreferencesKey("scale_inputs_serves_1")] = "4" }

        val repo = DataStoreScaleInputsRepository(dataStore)
        assertNull(repo.observe(1L).first())
    }

    @Test
    fun `values persist across a new repository instance on same file`() = runTest {
        val dataStore = newDataStore(backgroundScope)
        val repo = DataStoreScaleInputsRepository(dataStore)
        repo.save(7L, SavedScaleInputs("3", "9"))

        val repo2 = DataStoreScaleInputsRepository(dataStore)
        assertEquals(SavedScaleInputs("3", "9"), repo2.observe(7L).first())
    }
}
