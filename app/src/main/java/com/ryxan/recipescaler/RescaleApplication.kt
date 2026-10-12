package com.ryxan.recipescaler

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RescaleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // google-services.json is optional (git-ignored); Firebase is only
        // initialised when the config file shipped, and App Check with it.
        // Provider is chosen per build type in src/debug|release/AppCheckProviders.kt.
        if (FirebaseApp.getApps(this).isNotEmpty()) {
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(appCheckProviderFactory())
        }
    }
}
