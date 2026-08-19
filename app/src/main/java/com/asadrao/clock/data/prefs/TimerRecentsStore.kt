package com.asadrao.clock.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The durations the user has most recently timed.
 *
 * Samsung's quick-duration buttons are not a fixed list — they follow what you actually use, so the
 * timer you set yesterday is one tap away today. This keeps a short most-recent-first history to
 * drive that.
 *
 * A duration that is used again moves to the front rather than being duplicated, so the row does
 * not fill up with the same value.
 */
class TimerRecentsStore(private val dataStore: DataStore<Preferences>) {

    private val key = stringPreferencesKey("recent_durations")

    val recentDurations: Flow<List<Long>> = dataStore.data.map { prefs ->
        prefs[key]?.takeIf { it.isNotEmpty() }
            ?.split(',')
            ?.mapNotNull { it.toLongOrNull() }
            ?.filter { it > 0L }
            ?: emptyList()
    }

    suspend fun record(durationMillis: Long) {
        if (durationMillis <= 0L) return
        dataStore.edit { prefs ->
            val existing = prefs[key]?.split(',')?.mapNotNull { it.toLongOrNull() } ?: emptyList()
            // Move-to-front rather than append, and cap the history.
            val updated = (listOf(durationMillis) + existing.filter { it != durationMillis })
                .take(MAX_RECENTS)
            prefs[key] = updated.joinToString(",")
        }
    }

    private companion object {
        const val MAX_RECENTS = 4
    }
}
