package com.asadrao.clock.ui.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.asadrao.clock.data.prefs.SettingsStore
import com.asadrao.clock.di.AppContainer
import com.asadrao.clock.domain.model.Alarm
import com.asadrao.clock.domain.model.RepeatDays
import com.asadrao.clock.domain.repository.AlarmRepository
import com.asadrao.clock.domain.schedule.AlarmSchedule
import com.asadrao.clock.domain.schedule.AlarmSchedulingCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.DayOfWeek
import java.time.ZonedDateTime

/**
 * The alarm being edited.
 *
 * A working copy held in memory: nothing touches the database until Save, so Cancel genuinely
 * discards. [isNew] drives whether Save inserts or updates, and whether Delete is offered.
 */
data class AlarmEditorUiState(
    val hour: Int = 7,
    val minute: Int = 0,
    val repeatDays: RepeatDays = RepeatDays.None,
    val label: String = "",
    val soundUri: String? = null,
    val soundName: String = "",
    val vibrationEnabled: Boolean = true,
    val snoozeEnabled: Boolean = true,
    val snoozeDurationMinutes: Int = Alarm.DEFAULT_SNOOZE_MINUTES,
    val snoozeRepeatLimit: Int = Alarm.DEFAULT_SNOOZE_LIMIT,
    val isNew: Boolean = true,
    val loaded: Boolean = false,
) {
    val selectedDays: Set<DayOfWeek> get() = repeatDays.toSet()
}

class AlarmEditorViewModel(
    private val alarmId: Long,
    private val repository: AlarmRepository,
    private val coordinator: AlarmSchedulingCoordinator,
    private val settingsStore: SettingsStore,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmEditorUiState())
    val uiState: StateFlow<AlarmEditorUiState> = _uiState.asStateFlow()

    /** Kept so Save can preserve createdAt and the id rather than inventing new ones. */
    private var original: Alarm? = null

    init {
        viewModelScope.launch {
            val existing = if (alarmId != Alarm.NO_ID) repository.getAlarm(alarmId) else null
            original = existing
            _uiState.value = if (existing != null) {
                AlarmEditorUiState(
                    hour = existing.hour,
                    minute = existing.minute,
                    repeatDays = existing.repeatDays,
                    label = existing.label,
                    soundUri = existing.soundUri,
                    vibrationEnabled = existing.vibrationEnabled,
                    snoozeEnabled = existing.snoozeEnabled,
                    snoozeDurationMinutes = existing.snoozeDurationMinutes,
                    snoozeRepeatLimit = existing.snoozeRepeatLimit,
                    isNew = false,
                    loaded = true,
                )
            } else {
                // A new alarm opens at the current wall-clock time, minutes unrounded, which is
                // what Samsung does — rounding to the next five would silently move the value the
                // user is about to accept.
                val now = ZonedDateTime.now(clock)
                // A new alarm starts from the user's configured defaults; an existing one keeps
                // whatever it was saved with.
                val defaults = settingsStore.settings.first()
                AlarmEditorUiState(
                    hour = now.hour,
                    minute = now.minute,
                    snoozeDurationMinutes = defaults.defaultSnoozeMinutes,
                    snoozeRepeatLimit = defaults.defaultSnoozeRepeats,
                    isNew = true,
                    loaded = true,
                )
            }
        }
    }

    fun setTime(hour: Int, minute: Int) = _uiState.update { it.copy(hour = hour, minute = minute) }

    fun toggleDay(day: DayOfWeek) =
        _uiState.update { it.copy(repeatDays = it.repeatDays.toggle(day)) }

    fun setLabel(label: String) = _uiState.update { it.copy(label = label) }

    fun setSound(uri: String?, displayName: String) =
        _uiState.update { it.copy(soundUri = uri, soundName = displayName) }

    fun setVibration(enabled: Boolean) = _uiState.update { it.copy(vibrationEnabled = enabled) }

    fun setSnoozeEnabled(enabled: Boolean) = _uiState.update { it.copy(snoozeEnabled = enabled) }

    fun setSnooze(durationMinutes: Int, repeatLimit: Int) = _uiState.update {
        it.copy(snoozeDurationMinutes = durationMinutes, snoozeRepeatLimit = repeatLimit)
    }

    /** Saves, schedules, and reports when the alarm will ring so the caller can confirm it. */
    fun save(onSaved: (ZonedDateTime) -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            val alarm = Alarm(
                id = original?.id ?: Alarm.NO_ID,
                hour = state.hour,
                minute = state.minute,
                // Saving an alarm always arms it. Editing a disabled alarm and pressing Save
                // means "I want this", so leaving it off would be surprising.
                enabled = true,
                repeatDays = state.repeatDays,
                label = state.label.trim(),
                soundUri = state.soundUri,
                vibrationEnabled = state.vibrationEnabled,
                snoozeEnabled = state.snoozeEnabled,
                snoozeDurationMinutes = state.snoozeDurationMinutes,
                snoozeRepeatLimit = state.snoozeRepeatLimit,
                createdAt = original?.createdAt ?: 0L,
            )
            val id = if (state.isNew) {
                repository.addAlarm(alarm)
            } else {
                repository.updateAlarm(alarm)
                alarm.id
            }
            coordinator.sync(id)
            onSaved(AlarmSchedule.nextTrigger(alarm, ZonedDateTime.now(clock)))
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val existing = original ?: return
        viewModelScope.launch {
            repository.deleteAlarm(existing.id)
            coordinator.sync(existing.id)
            onDeleted()
        }
    }

    companion object {
        fun factory(container: AppContainer, alarmId: Long): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    AlarmEditorViewModel(
                        alarmId = alarmId,
                        repository = container.alarmRepository,
                        coordinator = container.alarmSchedulingCoordinator,
                        settingsStore = container.settingsStore,
                        clock = Clock.systemDefaultZone(),
                    )
                }
            }
    }
}
