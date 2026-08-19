package com.asadrao.clock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asadrao.clock.data.prefs.ClockSettings
import com.asadrao.clock.di.LocalAppContainer
import com.asadrao.clock.ui.ClockApp
import com.asadrao.clock.ui.theme.SamsungClockTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Called before super so the window is already edge-to-edge for the first frame.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as ClockApplication).container
        // Read once here and shared, so the whole app repaints together when the preference
        // changes rather than each screen resolving the theme for itself.
        val settingsFlow = container.settingsStore.settings
            .stateIn(lifecycleScope, SharingStarted.Eagerly, ClockSettings())

        setContent {
            val settings by settingsFlow.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalAppContainer provides container) {
                SamsungClockTheme(themeMode = settings.themeMode) {
                    ClockApp()
                }
            }
        }
    }
}
