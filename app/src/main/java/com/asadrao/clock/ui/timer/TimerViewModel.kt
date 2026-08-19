package com.asadrao.clock.ui.timer

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.asadrao.clock.data.prefs.TimerRecentsStore
import com.asadrao.clock.di.AppContainer
import com.asadrao.clock.domain.repository.TimerRepository
import com.asadrao.clock.domain.timer.ClockTimer
import com.asadrao.clock.domain.timer.TimerPreset
import com.asadrao.clock.domain.timer.QuickDurations
import com.asadrao.clock.domain.timer.TimerScheduler
import com.asadrao.clock.domain.timer.durationMillisOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TimerUiState(
    val timers: List<ClockTimer> = emptyList(),
    val presets: List<TimerPreset> = emptyList(),
    /** One-tap durations, most-recently-used first. */
    val quickDurations: List<Long> = QuickDurations.DEFAULTS,
    val loaded: Boolean = false,
) {
    /** With nothing running, the tab shows the duration dials instead of a list. */
    val showEntry: Boolean get() = loaded && timers.isEmpty()
}

/**
 * Drives the Timer tab.
 *
 * Every state change writes to the database **and** re-arms or cancels the platform alarm that
 * fires when the timer runs out. Those two must move together: a stored timer with no scheduled
 * wake-up finishes silently, and a scheduled wake-up with no stored timer rings for something that
 * no longer exists.
 */
class TimerViewModel(
    private val repository: TimerRepository,
    private val scheduler: TimerScheduler,
    private val recentsStore: TimerRecentsStore,
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
) : ViewModel() {

    val uiState: StateFlow<TimerUiState> = combine(
        repository.observeTimers(),
        repository.observePresets(),
        recentsStore.recentDurations,
    ) { timers, presets, recents ->
        TimerUiState(
            timers = timers,
            presets = presets,
            quickDurations = QuickDurations.forDisplay(recents),
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimerUiState())

    fun now(): Long = elapsedRealtime()

    /** Creates a timer and starts it immediately, which is what dialling a duration implies. */
    fun addAndStart(hours: Int, minutes: Int, seconds: Int, label: String = "") =
        startDuration(durationMillisOf(hours, minutes, seconds), label)

    /** Starts a one-tap quick duration. */
    fun startQuickDuration(totalMillis: Long) = startDuration(totalMillis, label = "")

    private fun startDuration(total: Long, label: String) {
        if (total <= 0L) return
        viewModelScope.launch {
            // Recorded so the quick-duration row reorders to what the user actually uses.
            recentsStore.record(total)
            val id = repository.addTimer(total, label)
            repository.getTimer(id)?.let { start(it) }
        }
    }

    fun startPreset(preset: TimerPreset) {
        viewModelScope.launch {
            recentsStore.record(preset.totalMillis)
            val id = repository.addTimer(preset.totalMillis, preset.label)
            repository.getTimer(id)?.let { start(it) }
        }
    }

    fun startOrPause(timer: ClockTimer) {
        if (timer.isRunning) pause(timer) else start(timer)
    }

    private fun start(timer: ClockTimer) {
        val started = timer.start(elapsedRealtime())
        viewModelScope.launch {
            repository.updateTimer(started)
            started.endsAtRealtime?.let { scheduler.schedule(started.id, it) }
        }
    }

    private fun pause(timer: ClockTimer) {
        val paused = timer.pause(elapsedRealtime())
        viewModelScope.launch {
            repository.updateTimer(paused)
            // Cancel the wake-up too, or it fires while the timer sits paused.
            scheduler.cancel(timer.id)
        }
    }

    fun reset(timer: ClockTimer) {
        viewModelScope.launch {
            repository.updateTimer(timer.reset())
            scheduler.cancel(timer.id)
        }
    }

    fun addMinute(timer: ClockTimer) {
        val extended = timer.addMinute(elapsedRealtime())
        viewModelScope.launch {
            repository.updateTimer(extended)
            extended.endsAtRealtime?.let { scheduler.schedule(extended.id, it) }
        }
    }

    fun delete(timer: ClockTimer) {
        viewModelScope.launch {
            scheduler.cancel(timer.id)
            repository.deleteTimer(timer.id)
        }
    }

    fun savePreset(hours: Int, minutes: Int, seconds: Int, label: String) {
        val total = durationMillisOf(hours, minutes, seconds)
        if (total <= 0L) return
        viewModelScope.launch { repository.addPreset(total, label) }
    }

    fun deletePreset(preset: TimerPreset) {
        viewModelScope.launch { repository.deletePreset(preset.id) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TimerViewModel(
                    repository = container.timerRepository,
                    scheduler = container.timerScheduler,
                    recentsStore = container.timerRecentsStore,
                )
            }
        }
    }
}
