package com.asadrao.clock.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.asadrao.clock.data.prefs.ClockSettings
import com.asadrao.clock.data.prefs.SettingsStore
import com.asadrao.clock.di.AppContainer
import com.asadrao.clock.ui.theme.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val store: SettingsStore) : ViewModel() {

    val settings: StateFlow<ClockSettings> = store.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClockSettings())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { store.setThemeMode(mode) }
    }

    fun setDefaultSnooze(minutes: Int, repeats: Int) {
        viewModelScope.launch { store.setDefaultSnooze(minutes, repeats) }
    }

    fun setTimerVibration(enabled: Boolean) {
        viewModelScope.launch { store.setTimerVibration(enabled) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(container.settingsStore) }
        }
    }
}
