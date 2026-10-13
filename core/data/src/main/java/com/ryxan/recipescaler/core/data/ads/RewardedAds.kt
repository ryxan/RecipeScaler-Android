package com.ryxan.recipescaler.core.data.ads

import android.app.Activity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Source of rewarded ads that grant bonus AI scans on completion.
 *
 * [AdMobRewardedAds] is bound in production: it keeps a RewardedAd preloaded
 * behind [isAvailable] and grants the earned credits from [showForScans].
 * [NoopRewardedAds] remains for tests and as a fallback.
 */
interface RewardedAds {

    /** True while a rewarded ad is loaded and can be shown. */
    val isAvailable: Flow<Boolean>

    /**
     * Shows a rewarded ad for bonus AI scans.
     *
     * Returns the number of credits earned — 0 when the user skips the ad or
     * no ad is available. Implementations grant the credits themselves
     * (via [com.ryxan.recipescaler.core.data.importer.AiImportQuota.grantCredits]);
     * the caller only surfaces the result.
     */
    suspend fun showForScans(activity: Activity): Int
}

/** No-op provider for tests — ads disabled, never grants credits. */
@Singleton
class NoopRewardedAds @Inject constructor() : RewardedAds {
    override val isAvailable: Flow<Boolean> = flowOf(false)
    override suspend fun showForScans(activity: Activity): Int = 0
}
