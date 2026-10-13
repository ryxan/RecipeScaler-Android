package com.ryxan.recipescaler.core.data.ads

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.ryxan.recipescaler.core.data.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Runs the UMP consent flow once per process, then initialises the Mobile Ads
 * SDK and kicks off the first rewarded-ad load — only when
 * `canRequestAds()` is true. No-ops when ads are disabled or under Robolectric.
 */
@Singleton
class AdConsentManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: AdConfig,
    private val rewardedAds: Provider<AdMobRewardedAds>,
    @ApplicationScope private val scope: CoroutineScope,
) {

    private val adsInitialized = AtomicBoolean(false)

    /** Call once with an Activity (MainActivity.onCreate). Safe to call again. */
    fun gatherConsentAndMaybeInitAds(activity: Activity) {
        if (!config.adsEnabled || isRobolectric()) return
        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)
        consentInfo.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    maybeInitAds()
                }
            },
            { formError ->
                Log.w(TAG, "Consent info update failed: ${formError?.message}")
                maybeInitAds()
            },
        )
    }

    private fun maybeInitAds() {
        if (!UserMessagingPlatform.getConsentInformation(context).canRequestAds()) return
        if (adsInitialized.compareAndSet(false, true)) {
            scope.launch(Dispatchers.Default) {
                MobileAds.initialize(context) { }
            }
            rewardedAds.get().requestLoad()
        }
    }

    private fun isRobolectric(): Boolean =
        Build.FINGERPRINT?.contains("robolectric", ignoreCase = true) == true

    private companion object {
        const val TAG = "AdConsentManager"
    }
}
