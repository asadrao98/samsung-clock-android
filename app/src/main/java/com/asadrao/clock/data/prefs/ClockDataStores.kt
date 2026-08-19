package com.asadrao.clock.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * The app's preference files, declared in one place.
 *
 * The stores themselves take a [DataStore] rather than a `Context` so they can be pointed at a
 * throwaway file in a test. DataStore permits only one active instance per file, and a test that
 * builds a second one over the same path fails in a way that looks like a bug in the store — so
 * making the file injectable is what keeps those tests honest and independent.
 */
internal val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("settings")
internal val Context.snoozeDataStore: DataStore<Preferences> by preferencesDataStore("snooze_counts")
internal val Context.stopwatchDataStore: DataStore<Preferences> by preferencesDataStore("stopwatch")
internal val Context.timerRecentsDataStore: DataStore<Preferences> by preferencesDataStore("timer_recents")
