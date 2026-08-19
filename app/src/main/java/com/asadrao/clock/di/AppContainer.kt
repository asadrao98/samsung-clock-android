package com.asadrao.clock.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.asadrao.clock.alarm.AndroidAlarmScheduler
import com.asadrao.clock.alarm.AndroidTimerScheduler
import com.asadrao.clock.data.db.ClockDatabase
import com.asadrao.clock.data.prefs.SnoozeTracker
import com.asadrao.clock.data.prefs.SettingsStore
import com.asadrao.clock.data.prefs.settingsDataStore
import com.asadrao.clock.data.prefs.snoozeDataStore
import com.asadrao.clock.data.prefs.TimerRecentsStore
import com.asadrao.clock.data.prefs.stopwatchDataStore
import com.asadrao.clock.data.prefs.timerRecentsDataStore
import com.asadrao.clock.data.prefs.StopwatchStore
import com.asadrao.clock.data.repository.RoomAlarmRepository
import com.asadrao.clock.data.repository.RoomTimerRepository
import com.asadrao.clock.data.repository.RoomWorldClockRepository
import com.asadrao.clock.domain.repository.AlarmRepository
import com.asadrao.clock.domain.repository.TimerRepository
import com.asadrao.clock.domain.repository.WorldClockRepository
import com.asadrao.clock.domain.timer.TimerScheduler
import com.asadrao.clock.domain.worldclock.CityCatalog
import com.asadrao.clock.domain.schedule.AlarmScheduler
import com.asadrao.clock.domain.schedule.AlarmSchedulingCoordinator
import java.time.Clock

/**
 * The object graph, wired by hand.
 *
 * No DI framework: the graph is small, and keeping it as plain lazy properties means the whole
 * thing can be read in one screen and swapped wholesale in a test.
 */
class AppContainer(
    private val context: Context,
    /**
     * The preference files are injectable so a test can point them at throwaway paths. DataStore
     * permits only one live instance per file, and several tests each building a container over the
     * production paths trips that in a way that surfaces as an unrelated-looking failure.
     */
    settingsPreferences: DataStore<Preferences> = context.settingsDataStore,
    snoozePreferences: DataStore<Preferences> = context.snoozeDataStore,
    stopwatchPreferences: DataStore<Preferences> = context.stopwatchDataStore,
    timerRecentsPreferences: DataStore<Preferences> = context.timerRecentsDataStore,
    /** Injectable so tests can use an in-memory database instead of a file. */
    databaseBuilder: (Context) -> ClockDatabase = ClockDatabase::build,
) {

    private val databaseDelegate = lazy { databaseBuilder(context) }
    private val database: ClockDatabase by databaseDelegate

    val alarmRepository: AlarmRepository by lazy { RoomAlarmRepository(database.alarmDao()) }

    val alarmScheduler: AlarmScheduler by lazy { AndroidAlarmScheduler(context) }

    val snoozeTracker: SnoozeTracker by lazy { SnoozeTracker(snoozePreferences) }

    val stopwatchStore: StopwatchStore by lazy { StopwatchStore(stopwatchPreferences) }

    val settingsStore: SettingsStore by lazy { SettingsStore(settingsPreferences) }

    val timerRepository: TimerRepository by lazy { RoomTimerRepository(database.timerDao()) }

    val timerScheduler: TimerScheduler by lazy { AndroidTimerScheduler(context) }

    val timerRecentsStore: TimerRecentsStore by lazy { TimerRecentsStore(timerRecentsPreferences) }

    val worldClockRepository: WorldClockRepository by lazy {
        RoomWorldClockRepository(database.worldCityDao())
    }

    /** Several hundred zones, built once and shared — not per screen. */
    val cityCatalog: CityCatalog by lazy { CityCatalog() }

    /**
     * Releases the database handle.
     *
     * The app itself never calls this — the database lives as long as the process, which is correct
     * for a clock. It exists for tests: an unclosed SQLite handle trips Robolectric's CloseGuard,
     * and the resulting asynchronous failure gets reported against whichever test happens to run
     * next, which is a very confusing symptom to trace back to a missing close.
     *
     * Guarded on the lazy so a container that never touched the database does not open one just to
     * close it.
     */
    fun close() {
        if (databaseDelegate.isInitialized()) database.close()
    }

    /**
     * Built with [Clock.systemDefaultZone] rather than a captured zone, so it re-reads the
     * device's timezone on every call and a user who flies somewhere gets correct alarms.
     */
    val alarmSchedulingCoordinator: AlarmSchedulingCoordinator by lazy {
        AlarmSchedulingCoordinator(
            repository = alarmRepository,
            scheduler = alarmScheduler,
            clock = Clock.systemDefaultZone(),
        )
    }
}
