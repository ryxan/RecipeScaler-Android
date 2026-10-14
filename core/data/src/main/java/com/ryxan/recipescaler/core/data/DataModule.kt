package com.ryxan.recipescaler.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.ryxan.recipescaler.core.data.db.RecipeDao
import com.ryxan.recipescaler.core.data.db.RecipeDatabase
import com.ryxan.recipescaler.core.domain.time.Clock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.ryxan.recipescaler.core.data.ads.AdMobRewardedAds
import com.ryxan.recipescaler.core.data.ads.RewardedAds
import com.ryxan.recipescaler.core.data.billing.PlayBillingProRepository
import com.ryxan.recipescaler.core.data.billing.ProRepository
import com.ryxan.recipescaler.core.data.di.ApplicationScope
import com.ryxan.recipescaler.core.data.importer.AiImportQuota
import com.ryxan.recipescaler.core.data.importer.AiRecipeImporter
import com.ryxan.recipescaler.core.data.importer.AndroidImagePreprocessor
import com.ryxan.recipescaler.core.data.importer.DataStoreAiImportQuota
import com.ryxan.recipescaler.core.data.importer.FirebaseAiRecipeImporter
import com.ryxan.recipescaler.core.data.importer.ImagePreprocessor
import com.ryxan.recipescaler.core.data.importer.MlKitOcrEngine
import com.ryxan.recipescaler.core.data.importer.OcrEngine
import com.ryxan.recipescaler.core.data.settings.DataStoreSettingsRepository
import com.ryxan.recipescaler.core.domain.formatting.MeasurementSystem
import com.ryxan.recipescaler.core.domain.repository.RecipeRepository
import com.ryxan.recipescaler.core.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultMeasurementSystem

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideRecipeDatabase(@ApplicationContext context: Context): RecipeDatabase =
        Room.databaseBuilder(context, RecipeDatabase::class.java, "recipes.db").build()

    @Provides
    fun provideRecipeDao(db: RecipeDatabase): RecipeDao = db.recipeDao()

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { File(context.filesDir, "settings.preferences_pb") }
        )

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock { System.currentTimeMillis() }

    @Provides
    @Singleton
    @DefaultMeasurementSystem
    fun provideDefaultMeasurementSystem(): MeasurementSystem = MeasurementSystem.MATCH_RECIPE

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindingsModule {

    @Binds
    @Singleton
    abstract fun bindRecipeRepository(impl: RoomRecipeRepository): RecipeRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindOcrEngine(impl: MlKitOcrEngine): OcrEngine

    @Binds
    @Singleton
    abstract fun bindImagePreprocessor(impl: AndroidImagePreprocessor): ImagePreprocessor

    @Binds
    @Singleton
    abstract fun bindAiRecipeImporter(impl: FirebaseAiRecipeImporter): AiRecipeImporter

    @Binds
    @Singleton
    abstract fun bindAiImportQuota(impl: DataStoreAiImportQuota): AiImportQuota

    @Binds
    @Singleton
    abstract fun bindProRepository(impl: PlayBillingProRepository): ProRepository

    @Binds
    @Singleton
    abstract fun bindRewardedAds(impl: AdMobRewardedAds): RewardedAds
}
