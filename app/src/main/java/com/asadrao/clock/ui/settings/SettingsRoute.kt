package com.asadrao.clock.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asadrao.clock.di.LocalAppContainer
import com.asadrao.clock.ui.theme.ClockTheme

/** Binds [SettingsViewModel] to [SettingsScreen]. */
@Composable
fun SettingsRoute(onClose: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: SettingsViewModel = viewModel(
        key = "settings",
        factory = SettingsViewModel.factory(container),
    )
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    SettingsScreen(
        settings = settings,
        onThemeModeChange = viewModel::setThemeMode,
        onDefaultSnoozeChange = viewModel::setDefaultSnooze,
        onTimerVibrationChange = viewModel::setTimerVibration,
        onClose = onClose,
        modifier = Modifier
            .fillMaxSize()
            .background(ClockTheme.colors.pageBackground),
    )
}
