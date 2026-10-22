package com.ryxan.recipescaler.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ryxan.recipescaler.core.domain.model.SavedScaleInputs
import com.ryxan.recipescaler.core.domain.repository.ScaleInputsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Stores the last Scale View inputs per recipe in the shared settings
 * DataStore, under one key pair per recipe id. Keys for deleted recipes are
 * left behind (tiny strings, never read again) rather than coupling recipe
 * deletion to this store.
 */
@Singleton
class DataStoreScaleInputsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ScaleInputsRepository {

    override fun observe(recipeId: Long): Flow<SavedScaleInputs?> = dataStore.data.map { prefs ->
        val serves = prefs[servesKey(recipeId)]
        val scaleTo = prefs[scaleToKey(recipeId)]
        if (serves == null || scaleTo == null) null else SavedScaleInputs(serves, scaleTo)
    }

    override suspend fun save(recipeId: Long, inputs: SavedScaleInputs) {
        dataStore.edit {
            it[servesKey(recipeId)] = inputs.servesText
            it[scaleToKey(recipeId)] = inputs.scaleToText
        }
    }

    private fun servesKey(recipeId: Long) =
        stringPreferencesKey("$SERVES_PREFIX$recipeId")

    private fun scaleToKey(recipeId: Long) =
        stringPreferencesKey("scale_inputs_to_$recipeId")

    private companion object {
        const val SERVES_PREFIX = "scale_inputs_serves_"
    }
}
