package com.ryxan.recipescaler.core.data.ads

/**
 * AdMob configuration supplied by the `:app` module — the ad unit ID lives in
 * app BuildConfig (test unit in debug, `ADMOB_REWARDED_UNIT_ID` from
 * local.properties in release) and the app ID is a manifest placeholder, so
 * this library never reads either directly.
 */
data class AdConfig(
    /** AdMob rewarded ad unit ID. */
    val rewardedAdUnitId: String,
    /** Master switch; false keeps the SDK fully idle (e.g. tests). */
    val adsEnabled: Boolean = true,
)
