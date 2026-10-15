package com.ryxan.recipescaler

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ryxan.recipescaler.core.data.ads.AdConsentManager
import com.ryxan.recipescaler.core.designsystem.RescaleTheme
import com.ryxan.recipescaler.core.domain.model.ThemeMode
import com.ryxan.recipescaler.navigation.RescaleNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var adConsentManager: AdConsentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Every screen has the (dark) brand gradient behind the status bar, so
        // status-bar icons stay light in both themes.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            RescaleTheme(
                darkTheme = when (settings.themeMode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                },
                dynamicColor = settings.dynamicColor,
            ) {
                RescaleNavHost()
            }
        }
        // UMP does decor/window work on a worker thread during the consent-info
        // request; if it runs while the decor is still being installed (notably
        // during a relaunch on rotation) the two threads race inside
        // PhoneWindow.generateLayout and the activity dies with "Window
        // couldn't find content container view". Deferring to the decor view
        // guarantees the window is fully laid out first.
        window.decorView.post {
            if (!isFinishing && !isDestroyed) {
                adConsentManager.gatherConsentAndMaybeInitAds(this)
            }
        }
    }
}
