package com.ryxan.recipescaler.di

import com.ryxan.recipescaler.BuildConfig
import com.ryxan.recipescaler.core.data.ads.AdConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AdsModule {

    /**
     * AdMob unit IDs live in :app BuildConfig — the test unit in debug,
     * `ADMOB_REWARDED_UNIT_ID` from local.properties in release. The app ID is
     * a manifest placeholder (`admobAppId`) in the same build types.
     */
    @Provides
    @Singleton
    fun provideAdConfig(): AdConfig = AdConfig(
        rewardedAdUnitId = BuildConfig.ADMOB_REWARDED_UNIT_ID,
    )
}
