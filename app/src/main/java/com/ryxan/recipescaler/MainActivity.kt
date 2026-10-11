package com.ryxan.recipescaler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ryxan.recipescaler.core.designsystem.RescaleTheme
import com.ryxan.recipescaler.navigation.RescaleNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RescaleTheme {
                RescaleNavHost()
            }
        }
    }
}
