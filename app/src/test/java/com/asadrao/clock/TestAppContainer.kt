package com.asadrao.clock

import android.content.Context
import androidx.room.Room
import com.asadrao.clock.data.db.ClockDatabase
import com.asadrao.clock.di.AppContainer

/**
 * An [AppContainer] wired entirely to throwaway storage: an in-memory database and in-memory
 * preferences. No test can see what another one wrote, and nothing touches the file system.
 */
fun testAppContainer(context: Context): AppContainer = AppContainer(
    context = context,
    settingsPreferences = InMemoryPreferenceDataStore(),
    snoozePreferences = InMemoryPreferenceDataStore(),
    stopwatchPreferences = InMemoryPreferenceDataStore(),
    databaseBuilder = {
        Room.inMemoryDatabaseBuilder(it, ClockDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    },
)
