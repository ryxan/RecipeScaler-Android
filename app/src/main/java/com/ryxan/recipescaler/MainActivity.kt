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
import com.ryxan.recipescaler.core.designsystem.RescaleTheme
import com.ryxan.recipescaler.core.domain.model.ThemeMode
import com.ryxan.recipescaler.navigation.RescaleNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

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
    }
}
