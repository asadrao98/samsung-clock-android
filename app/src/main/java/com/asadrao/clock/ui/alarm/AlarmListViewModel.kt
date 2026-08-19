package com.asadrao.clock.ui.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.asadrao.clock.di.AppContainer
import com.asadrao.clock.domain.model.Alarm
import com.asadrao.clock.domain.repository.AlarmRepository
import com.asadrao.clock.domain.schedule.AlarmSchedule
import com.asadrao.clock.domain.schedule.AlarmSchedulingCoordinator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.ZonedDateTime

/** What the alarm list screen renders. */
data class AlarmListUiState(
    val alarms: List<Alarm> = emptyList(),
    val nextTrigger: ZonedDateTime? = null,
    val selectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val loaded: Boolean = false,
) {
    val isEmpty: Boolean get() = loaded && alarms.isEmpty()
    val allSelected: Boolean get() = alarms.isNotEmpty() && selectedIds.size == alarms.size
}

/** A one-shot message for the snackbar, e.g. confirming when an alarm will ring. */
sealed interface AlarmListEvent {
    data class AlarmEnabled(val ringsAt: ZonedDateTime) : AlarmListEvent
    data class AlarmsDeleted(val count: Int) : AlarmListEvent
}

class AlarmListViewModel(
    private val repository: AlarmRepository,
    private val coordinator: AlarmSchedulingCoordinator,
    private val clock: Clock,
) : ViewModel() {

    private val selection = MutableStateFlow(SelectionState())

    private val events = Channel<AlarmListEvent>(Channel.BUFFERED)
    val eventFlow: Flow<AlarmListEvent> = events.receiveAsFlow()

    val uiState: StateFlow<AlarmListUiState> =
        combine(repository.observeAlarms(), selection) { alarms, sel ->
            AlarmListUiState(
                alarms = alarms,
                nextTrigger = soonestTrigger(alarms),
                selectionMode = sel.active,
                // Drop ids that no longer exist, so a deletion cannot leave a phantom selected.
                selectedIds = sel.ids.intersect(alarms.map { it.id }.toSet()),
                loaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlarmListUiState())

    /**
     * The soonest ring across all enabled alarms — which is not simply the first row, because the
     * list is ordered by time of day and a 06:00 alarm that only repeats on Sundays may be days
     * further off than an 08:00 daily one.
     */
    private fun soonestTrigger(alarms: List<Alarm>): ZonedDateTime? {
        val now = ZonedDateTime.now(clock)
        return alarms.filter { it.enabled }
            .map { AlarmSchedule.nextTrigger(it, now) }
            .minByOrNull { it.toInstant() }
    }

    fun setEnabled(alarm: Alarm, enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(alarm.id, enabled)
            coordinator.sync(alarm.id)
            if (enabled) {
                val ringsAt = AlarmSchedule.nextTrigger(
                    alarm.copy(enabled = true),
                    ZonedDateTime.now(clock),
                )
                events.send(AlarmListEvent.AlarmEnabled(ringsAt))
            }
        }
    }

    // ---- selection mode ----------------------------------------------------------------

    /**
     * Enters selection mode with nothing chosen — the ⋮ → Edit route.
     *
     * Distinct from [startSelectionWith]: entering via the menu is not a statement about any
     * particular alarm, so pre-selecting one would be both surprising and dangerous next to a
     * Delete button.
     */
    fun enterSelection() {
        selection.value = SelectionState(active = true, ids = emptySet())
    }

    /** Enters selection mode with one alarm chosen — the long-press route. */
    fun startSelectionWith(alarmId: Long) {
        selection.value = SelectionState(active = true, ids = setOf(alarmId))
    }

    fun toggleSelected(alarmId: Long) {
        val current = selection.value
        val ids = if (alarmId in current.ids) current.ids - alarmId else current.ids + alarmId
        selection.value = current.copy(ids = ids)
    }

    fun selectAll(all: Boolean) {
        val ids = if (all) uiState.value.alarms.map { it.id }.toSet() else emptySet()
        selection.value = selection.value.copy(ids = ids)
    }

    fun exitSelection() {
        selection.value = SelectionState()
    }

    fun deleteSelected() {
        val ids = selection.value.ids
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { id ->
                repository.deleteAlarm(id)
                // Cancel after the row is gone: sync() reads the row back and, finding nothing,
                // clears both the alarm and any pending snooze.
                coordinator.sync(id)
            }
            selection.value = SelectionState()
            events.send(AlarmListEvent.AlarmsDeleted(ids.size))
        }
    }

    fun setSelectedEnabled(enabled: Boolean) {
        val ids = selection.value.ids
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { id ->
                repository.setEnabled(id, enabled)
                coordinator.sync(id)
            }
            selection.value = SelectionState()
        }
    }

    private data class SelectionState(
        val active: Boolean = false,
        val ids: Set<Long> = emptySet(),
    )

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AlarmListViewModel(
                    repository = container.alarmRepository,
                    coordinator = container.alarmSchedulingCoordinator,
                    clock = Clock.systemDefaultZone(),
                )
            }
        }
    }
}
