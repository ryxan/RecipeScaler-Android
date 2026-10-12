package com.ryxan.recipescaler.di

import com.ryxan.recipescaler.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object FeatureFlagsModule {

    /** True when a google-services.json shipped — AI import can be offered. */
    @Provides
    @Named("aiImportAvailable")
    fun provideAiImportAvailable(): Boolean = BuildConfig.AI_IMPORT_ENABLED
}
