package com.asadrao.clock.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first

/**
 * How many times each alarm has been snoozed in its current ringing session.
 *
 * Kept on disk rather than in the ringing service, because the count has to survive the service
 * being killed between snoozes — an alarm snoozed twice must still know that when it rings again
 * twenty minutes later, in a fresh process. An in-memory counter would silently reset and give the
 * user unlimited snoozes.
 *
 * The count is cleared when an alarm rings on schedule, so each morning starts fresh.
 */
class SnoozeTracker(private val dataStore: DataStore<Preferences>) {

    private fun key(alarmId: Long) = intPreferencesKey("snooze_count_$alarmId")

    suspend fun count(alarmId: Long): Int =
        dataStore.data.first()[key(alarmId)] ?: 0

    suspend fun increment(alarmId: Long): Int {
        var updated = 0
        dataStore.edit { prefs ->
            updated = (prefs[key(alarmId)] ?: 0) + 1
            prefs[key(alarmId)] = updated
        }
        return updated
    }

    suspend fun reset(alarmId: Long) {
        dataStore.edit { it.remove(key(alarmId)) }
    }
}
