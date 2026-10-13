package com.ryxan.recipescaler.core.data.ads

import android.app.Activity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Source of rewarded ads that grant bonus AI scans on completion.
 *
 * The Google Mobile Ads SDK is not integrated yet — [NoopRewardedAds] is bound
 * so the paywall renders the option in a "coming soon" state. When AdMob lands
 * (app ID in the manifest, AD_ID permission, UMP consent), bind a real
 * implementation that loads a RewardedAd behind [isAvailable] and returns the
 * earned credits from [showForScans].
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

/** No-op provider bound until the Mobile Ads SDK is integrated. */
@Singleton
class NoopRewardedAds @Inject constructor() : RewardedAds {
    override val isAvailable: Flow<Boolean> = flowOf(false)
    override suspend fun showForScans(activity: Activity): Int = 0
}
