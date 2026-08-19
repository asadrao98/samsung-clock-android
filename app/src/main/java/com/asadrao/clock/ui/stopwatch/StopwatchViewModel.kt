package com.asadrao.clock.ui.stopwatch

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.asadrao.clock.data.prefs.StopwatchStore
import com.asadrao.clock.di.AppContainer
import com.asadrao.clock.domain.stopwatch.StopwatchState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Drives the stopwatch.
 *
 * The view model holds no ticking timer of its own. It owns the [StopwatchState] and the UI reads
 * the elapsed value from the monotonic clock on each frame it draws — so nothing here has to run
 * while the screen is off, and the readout is correct the instant it becomes visible again.
 */
class StopwatchViewModel(
    private val store: StopwatchStore,
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
    private val wallClock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _state = MutableStateFlow(StopwatchState())
    val state: StateFlow<StopwatchState> = _state.asStateFlow()

    /** An estimate of when the device booted, used to notice a restart. */
    private fun bootMarker(): Long = wallClock() - elapsedRealtime()

    init {
        viewModelScope.launch {
            // Reconciled against the current boot session before it is shown, so a stopwatch that
            // was running across a reboot does not briefly display a nonsense duration.
            _state.value = store.state.first().afterRestore(bootMarker())
        }
    }

    fun now(): Long = elapsedRealtime()

    fun startOrPause() {
        val current = _state.value
        update(
            if (current.isRunning) current.pause(elapsedRealtime())
            else current.start(elapsedRealtime(), bootMarker())
        )
    }

    fun lap() = update(_state.value.lap(elapsedRealtime()))

    fun reset() {
        _state.value = StopwatchState()
        viewModelScope.launch { store.clear() }
    }

    private fun update(next: StopwatchState) {
        _state.value = next
        viewModelScope.launch { store.save(next) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { StopwatchViewModel(container.stopwatchStore) }
        }
    }
}
