package com.asadrao.clock

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A [DataStore] that keeps preferences in memory.
 *
 * Used by every test in place of the real, file-backed store. That is not just a speed choice — the
 * file-backed store owns a background coroutine scope for its writes, and a write still in flight
 * when a test's temporary directory is deleted throws on that scope with nothing to catch it.
 * `runTest` then reports the failure against whichever test happens to start next, which is a
 * genuinely misleading symptom to chase. Holding the values in memory removes the file, the scope
 * and the whole class of flakiness.
 *
 * The mutex preserves DataStore's contract that updates are serialised, so a test that writes
 * concurrently behaves the way production would.
 */
class InMemoryPreferenceDataStore : DataStore<Preferences> {

    private val state = MutableStateFlow(emptyPreferences())
    private val writeLock = Mutex()

    override val data: Flow<Preferences> = state.asStateFlow()

    override suspend fun updateData(
        transform: suspend (Preferences) -> Preferences,
    ): Preferences = writeLock.withLock {
        val updated = transform(state.value)
        state.value = updated
        updated
    }
}
