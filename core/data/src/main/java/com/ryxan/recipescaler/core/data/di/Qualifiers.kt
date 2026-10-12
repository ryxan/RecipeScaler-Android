package com.ryxan.recipescaler.core.data.di

import javax.inject.Qualifier

/** App-wide [kotlinx.coroutines.CoroutineScope] for work that outlives a screen. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
