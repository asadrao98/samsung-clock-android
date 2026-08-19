package com.asadrao.clock.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.asadrao.clock.domain.stopwatch.StopwatchState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persists the stopwatch so a running one survives the app being killed.
 *
 * Written only when the state actually changes — start, pause, lap, reset — never on the display
 * tick. The readout updates ~60 times a second and writing that to disk would be absurd; it does
 * not need to be stored, because it is derived from the monotonic clock on the way out.
 */
class StopwatchStore(private val dataStore: DataStore<Preferences>) {

    private val accumulated = longPreferencesKey("accumulated")
    private val runningSince = longPreferencesKey("running_since")
    private val bootMarker = longPreferencesKey("boot_marker")
    private val laps = stringPreferencesKey("laps")

    val state: Flow<StopwatchState> = dataStore.data.map { prefs ->
        StopwatchState(
            accumulatedMillis = prefs[accumulated] ?: 0L,
            // A stored -1 means "not running"; DataStore has no null for a Long key.
            runningSinceRealtime = prefs[runningSince]?.takeIf { it >= 0L },
            laps = prefs[laps]?.takeIf { it.isNotEmpty() }
                ?.split(',')
                ?.mapNotNull { it.toLongOrNull() }
                ?: emptyList(),
            bootMarker = prefs[bootMarker],
        )
    }

    suspend fun save(state: StopwatchState) {
        dataStore.edit { prefs ->
            prefs[accumulated] = state.accumulatedMillis
            prefs[runningSince] = state.runningSinceRealtime ?: -1L
            prefs[laps] = state.laps.joinToString(",")
            state.bootMarker?.let { prefs[bootMarker] = it }
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
