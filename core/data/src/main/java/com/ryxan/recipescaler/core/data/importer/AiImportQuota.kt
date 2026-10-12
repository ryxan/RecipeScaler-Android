package com.ryxan.recipescaler.core.data.importer

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ryxan.recipescaler.core.domain.importer.AiImportConfig
import com.ryxan.recipescaler.core.domain.time.Clock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Per-device quota for AI recipe imports. Callers reserve before the network
 * call and refund on any non-success so a failed import never costs a credit.
 */
interface AiImportQuota {
    /** Remaining lifetime imports for a free user (0 once exhausted). */
    val remainingFree: Flow<Int>

    /** Atomically reserves one import; false when the quota is exhausted (nothing written). */
    suspend fun tryReserve(isPro: Boolean): Boolean

    /** Returns a previously reserved credit after a failed import. */
    suspend fun refund(isPro: Boolean)
}

/**
 * Free users get [AiImportConfig.FREE_LIFETIME_LIMIT] lifetime imports; Pro
 * users get a hidden daily cap of [AiImportConfig.PREMIUM_DAILY_CAP].
 *
 * The counter is local and bypassable by clearing app data — acceptable at two
 * free uses; the real cost guards are App Check, the per-user RPM quota, and
 * budget alerts.
 */
@Singleton
class DataStoreAiImportQuota internal constructor(
    private val dataStore: DataStore<Preferences>,
    private val clock: Clock,
    private val zoneId: ZoneId,
) : AiImportQuota {

    @Inject
    constructor(dataStore: DataStore<Preferences>, clock: Clock) : this(
        dataStore,
        clock,
        ZoneId.systemDefault(),
    )

    private fun today(): String =
        Instant.ofEpochMilli(clock.now()).atZone(zoneId).toLocalDate().toString()

    override val remainingFree: Flow<Int> = dataStore.data.map { preferences ->
        (AiImportConfig.FREE_LIFETIME_LIMIT - (preferences[KEY_FREE_USED] ?: 0)).coerceAtLeast(0)
    }

    override suspend fun tryReserve(isPro: Boolean): Boolean {
        var granted = false
        dataStore.edit { preferences ->
            if (isPro) {
                val today = today()
                val dayCount = if (preferences[KEY_DAY] == today) preferences[KEY_DAY_COUNT] ?: 0 else 0
                if (dayCount < AiImportConfig.PREMIUM_DAILY_CAP) {
                    preferences[KEY_DAY] = today
                    preferences[KEY_DAY_COUNT] = dayCount + 1
                    granted = true
                }
            } else {
                val used = preferences[KEY_FREE_USED] ?: 0
                if (used < AiImportConfig.FREE_LIFETIME_LIMIT) {
                    preferences[KEY_FREE_USED] = used + 1
                    granted = true
                }
            }
        }
        return granted
    }

    override suspend fun refund(isPro: Boolean) {
        dataStore.edit { preferences ->
            if (isPro) {
                if (preferences[KEY_DAY] == today()) {
                    preferences[KEY_DAY_COUNT] =
                        ((preferences[KEY_DAY_COUNT] ?: 0) - 1).coerceAtLeast(0)
                }
            } else {
                preferences[KEY_FREE_USED] =
                    ((preferences[KEY_FREE_USED] ?: 0) - 1).coerceAtLeast(0)
            }
        }
    }

    private companion object {
        val KEY_FREE_USED = intPreferencesKey("ai_import_free_used")
        val KEY_DAY = stringPreferencesKey("ai_import_day")
        val KEY_DAY_COUNT = intPreferencesKey("ai_import_day_count")
    }
}
