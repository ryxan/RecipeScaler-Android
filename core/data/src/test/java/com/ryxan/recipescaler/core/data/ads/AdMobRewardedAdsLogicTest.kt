package com.ryxan.recipescaler.core.data.ads

import com.ryxan.recipescaler.core.domain.importer.AiImportConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class AdMobRewardedAdsLogicTest {

    @Test
    fun `earned reward grants the rewarded-ad credit constant`() {
        assertEquals(AiImportConfig.SCANS_PER_REWARDED_AD, AdMobRewardedAds.creditsForReward())
    }

    @Test
    fun `retry backoff doubles until the cap`() {
        assertEquals(60_000L, AdMobRewardedAds.nextRetryDelayMs(30_000L))
        assertEquals(120_000L, AdMobRewardedAds.nextRetryDelayMs(60_000L))
    }

    @Test
    fun `retry backoff never exceeds the cap`() {
        assertEquals(600_000L, AdMobRewardedAds.nextRetryDelayMs(600_000L))
        assertEquals(600_000L, AdMobRewardedAds.nextRetryDelayMs(Long.MAX_VALUE / 4))
    }
}
