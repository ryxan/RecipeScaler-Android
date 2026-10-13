package com.ryxan.recipescaler.core.data.importer

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.ryxan.recipescaler.core.domain.importer.AiImportConfig
import com.ryxan.recipescaler.core.domain.time.Clock
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataStoreAiImportQuotaTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private var millis = LocalDate.parse(DAY).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    private var fileCounter = 0

    private fun newQuota(scope: CoroutineScope): DataStoreAiImportQuota =
        DataStoreAiImportQuota(
            PreferenceDataStoreFactory.create(scope = scope) {
                File(tempFolder.root, "quota_${fileCounter++}.preferences_pb")
            },
            Clock { millis },
            ZoneId.of("UTC"),
        )

    private fun advanceDay() {
        millis += 86_400_000L
    }

    @Test
    fun `fresh install reports the full free limit`() = runTest {
        val quota = newQuota(backgroundScope)
        assertEquals(AiImportConfig.FREE_LIFETIME_LIMIT, quota.remainingFree.first())
    }

    @Test
    fun `free user reserves until the limit then fails without writing`() = runTest {
        val quota = newQuota(backgroundScope)

        repeat(AiImportConfig.FREE_LIFETIME_LIMIT) {
            assertTrue(quota.tryReserve(isPro = false))
        }
        assertFalse(quota.tryReserve(isPro = false))
        assertEquals(0, quota.remainingFree.first())

        // The failed reserve wrote nothing, so one refund restores a credit.
        quota.refund(isPro = false)
        assertEquals(1, quota.remainingFree.first())
    }

    @Test
    fun `refund restores a free credit`() = runTest {
        val quota = newQuota(backgroundScope)

        assertTrue(quota.tryReserve(isPro = false))
        quota.refund(isPro = false)

        assertEquals(AiImportConfig.FREE_LIFETIME_LIMIT, quota.remainingFree.first())
    }

    @Test
    fun `refund never drops below zero`() = runTest {
        val quota = newQuota(backgroundScope)

        quota.refund(isPro = false)
        quota.refund(isPro = false)

        assertEquals(AiImportConfig.FREE_LIFETIME_LIMIT, quota.remainingFree.first())
    }

    @Test
    fun `bonus credits are spent only after the free allowance`() = runTest {
        val quota = newQuota(backgroundScope)
        quota.grantCredits(5)

        repeat(AiImportConfig.FREE_LIFETIME_LIMIT) {
            assertTrue(quota.tryReserve(isPro = false))
        }
        // Free allowance spent — the bonus balance is untouched so far.
        assertEquals(5, quota.bonusCredits.first())

        repeat(5) { assertTrue(quota.tryReserve(isPro = false)) }
        assertFalse(quota.tryReserve(isPro = false))
        assertEquals(0, quota.bonusCredits.first())
    }

    @Test
    fun `refund restores a bonus credit when the last debit was bonus`() = runTest {
        val quota = newQuota(backgroundScope)
        quota.grantCredits(2)
        repeat(AiImportConfig.FREE_LIFETIME_LIMIT) {
            assertTrue(quota.tryReserve(isPro = false))
        }

        assertTrue(quota.tryReserve(isPro = false)) // spends a bonus credit
        assertEquals(1, quota.bonusCredits.first())
        quota.refund(isPro = false)

        assertEquals(2, quota.bonusCredits.first())
        assertEquals(0, quota.remainingFree.first())
    }

    @Test
    fun `pro user reserves up to the daily cap`() = runTest {
        val quota = newQuota(backgroundScope)

        repeat(AiImportConfig.PREMIUM_DAILY_CAP) {
            assertTrue(quota.tryReserve(isPro = true))
        }
        assertFalse(quota.tryReserve(isPro = true))
    }

    @Test
    fun `pro cap rolls over on a new day`() = runTest {
        val quota = newQuota(backgroundScope)

        repeat(AiImportConfig.PREMIUM_DAILY_CAP) {
            assertTrue(quota.tryReserve(isPro = true))
        }
        assertFalse(quota.tryReserve(isPro = true))

        advanceDay()
        assertTrue(quota.tryReserve(isPro = true))
    }

    @Test
    fun `pro refund on a new day does not touch the counts`() = runTest {
        val quota = newQuota(backgroundScope)

        assertTrue(quota.tryReserve(isPro = true))
        advanceDay()
        quota.refund(isPro = true) // different day — must not write

        // A full day-2 budget is still available: the refund didn't corrupt
        // yesterday's count and today's count is independent anyway.
        repeat(AiImportConfig.PREMIUM_DAILY_CAP) {
            assertTrue(quota.tryReserve(isPro = true))
        }
        assertFalse(quota.tryReserve(isPro = true))
    }

    @Test
    fun `debugSetRemaining splits the balance between free and bonus`() = runTest {
        val quota = newQuota(backgroundScope)

        quota.debugSetRemaining(0)
        assertEquals(0, quota.remainingFree.first())
        assertEquals(0, quota.bonusCredits.first())
        assertFalse(quota.tryReserve(isPro = false))

        quota.debugSetRemaining(3)
        assertEquals(3, quota.remainingFree.first())
        assertEquals(0, quota.bonusCredits.first())
        assertTrue(quota.tryReserve(isPro = false))
        assertEquals(2, quota.remainingFree.first())

        val bonus = 7
        quota.debugSetRemaining(AiImportConfig.FREE_LIFETIME_LIMIT + bonus)
        assertEquals(AiImportConfig.FREE_LIFETIME_LIMIT, quota.remainingFree.first())
        assertEquals(bonus, quota.bonusCredits.first())

        // Free allowance is spent first, then bonus credits.
        repeat(AiImportConfig.FREE_LIFETIME_LIMIT) {
            assertTrue(quota.tryReserve(isPro = false))
        }
        assertEquals(0, quota.remainingFree.first())
        assertTrue(quota.tryReserve(isPro = false))
        assertEquals(bonus - 1, quota.bonusCredits.first())
    }

    private companion object {
        const val DAY = "2026-10-10"
    }
}
