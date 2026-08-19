package com.asadrao.clock.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.asadrao.clock.domain.model.Alarm
import com.asadrao.clock.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The user's preferences. */
data class ClockSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    /** Applied to newly created alarms, not to existing ones. */
    val defaultSnoozeMinutes: Int = Alarm.DEFAULT_SNOOZE_MINUTES,
    val defaultSnoozeRepeats: Int = Alarm.DEFAULT_SNOOZE_LIMIT,
    val timerVibration: Boolean = true,
)

/**
 * Persists settings.
 *
 * Deliberately small. Every entry here changes something real: the theme repaints the app, the
 * snooze defaults seed new alarms, and the timer vibration is read when a timer finishes. There is
 * no setting present only to make the screen look fuller — and notably no 12/24-hour toggle, because
 * that belongs to the system and the app follows it.
 */
class SettingsStore(private val dataStore: DataStore<Preferences>) {

    private val themeKey = stringPreferencesKey("theme_mode")
    private val snoozeMinutesKey = intPreferencesKey("default_snooze_minutes")
    private val snoozeRepeatsKey = intPreferencesKey("default_snooze_repeats")
    private val timerVibrationKey = booleanPreferencesKey("timer_vibration")

    val settings: Flow<ClockSettings> = dataStore.data.map { prefs ->
        ClockSettings(
            themeMode = prefs[themeKey]?.let { stored ->
                // An unrecognised stored value falls back rather than crashing — it can happen if
                // a preference is renamed in a later version.
                runCatching { ThemeMode.valueOf(stored) }.getOrDefault(ThemeMode.System)
            } ?: ThemeMode.System,
            defaultSnoozeMinutes = prefs[snoozeMinutesKey] ?: Alarm.DEFAULT_SNOOZE_MINUTES,
            defaultSnoozeRepeats = prefs[snoozeRepeatsKey] ?: Alarm.DEFAULT_SNOOZE_LIMIT,
            timerVibration = prefs[timerVibrationKey] ?: true,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[themeKey] = mode.name }
    }

    suspend fun setDefaultSnooze(minutes: Int, repeats: Int) {
        dataStore.edit {
            it[snoozeMinutesKey] = minutes
            it[snoozeRepeatsKey] = repeats
        }
    }

    suspend fun setTimerVibration(enabled: Boolean) {
        dataStore.edit { it[timerVibrationKey] = enabled }
    }
}
