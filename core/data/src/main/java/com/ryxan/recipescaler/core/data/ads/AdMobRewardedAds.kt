package com.ryxan.recipescaler.core.data.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.UserMessagingPlatform
import com.ryxan.recipescaler.core.data.di.ApplicationScope
import com.ryxan.recipescaler.core.data.importer.AiImportQuota
import com.ryxan.recipescaler.core.domain.importer.AiImportConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * AdMob-backed [RewardedAds]. Keeps one [RewardedAd] preloaded behind
 * [isAvailable]; [showForScans] presents it and grants
 * [AiImportConfig.SCANS_PER_REWARDED_AD] credits only when the user earns the
 * reward. Loads are gated on UMP [com.google.android.ump.ConsentInformation.canRequestAds]
 * — [AdConsentManager] drives consent, then calls [requestLoad].
 */
@Singleton
class AdMobRewardedAds @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: AdConfig,
    private val quota: AiImportQuota,
    @ApplicationScope private val scope: CoroutineScope,
) : RewardedAds {

    private val _isAvailable = MutableStateFlow(false)
    override val isAvailable: StateFlow<Boolean> = _isAvailable

    private var rewardedAd: RewardedAd? = null
    private val loading = AtomicBoolean(false)
    private var retryDelayMs = INITIAL_RETRY_MS

    /** Starts a load if consent allows and no ad is loaded or in flight. */
    fun requestLoad() {
        if (!config.adsEnabled) return
        scope.launch(Dispatchers.Main) { loadOnMain() }
    }

    private fun loadOnMain() {
        if (rewardedAd != null || !loading.compareAndSet(false, true)) return
        if (!UserMessagingPlatform.getConsentInformation(context).canRequestAds()) {
            loading.set(false)
            return
        }
        RewardedAd.load(
            context,
            config.rewardedAdUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    retryDelayMs = INITIAL_RETRY_MS
                    loading.set(false)
                    _isAvailable.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Rewarded ad failed to load: ${error.message}")
                    loading.set(false)
                    _isAvailable.value = false
                    scheduleRetry()
                }
            },
        )
    }

    private fun scheduleRetry() {
        val delayMs = retryDelayMs
        retryDelayMs = nextRetryDelayMs(delayMs)
        scope.launch {
            delay(delayMs)
            requestLoad()
        }
    }

    override suspend fun showForScans(activity: Activity): Int {
        val ad = rewardedAd ?: run {
            requestLoad()
            return 0
        }
        rewardedAd = null
        _isAvailable.value = false
        return suspendCancellableCoroutine { continuation ->
            var earned = 0
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    requestLoad()
                    if (continuation.isActive) continuation.resume(earned)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "Rewarded ad failed to show: ${adError.message}")
                    requestLoad()
                    if (continuation.isActive) continuation.resume(0)
                }
            }
            ad.show(activity) {
                earned = creditsForReward()
                scope.launch { quota.grantCredits(earned) }
            }
        }
    }

    internal companion object {
        const val TAG = "AdMobRewardedAds"
        const val INITIAL_RETRY_MS = 30_000L
        const val MAX_RETRY_MS = 10 * 60_000L

        /** Credits granted for an earned reward, regardless of the SDK reward amount. */
        fun creditsForReward(): Int = AiImportConfig.SCANS_PER_REWARDED_AD

        /** Doubles the previous delay, capped at [MAX_RETRY_MS]. */
        fun nextRetryDelayMs(currentMs: Long): Long =
            if (currentMs >= MAX_RETRY_MS / 2) MAX_RETRY_MS else currentMs * 2
    }
}
