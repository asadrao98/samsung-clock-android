package com.asadrao.clock

import com.asadrao.clock.data.prefs.SettingsStore
import com.asadrao.clock.domain.model.Alarm
import com.asadrao.clock.ui.theme.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Settings persistence. Each test gets its own preference file, so none of them can be influenced
 * by what another one wrote.
 */
class SettingsTest {

    private fun store() = SettingsStore(InMemoryPreferenceDataStore())

    @Test
    fun defaults_are_sensible_before_anything_is_chosen() = runTest {
        val settings = store().settings.first()
        assertEquals(ThemeMode.System, settings.themeMode)
        assertEquals(Alarm.DEFAULT_SNOOZE_MINUTES, settings.defaultSnoozeMinutes)
        assertEquals(Alarm.DEFAULT_SNOOZE_LIMIT, settings.defaultSnoozeRepeats)
        assertTrue(settings.timerVibration)
    }

    @Test
    fun the_theme_choice_persists() = runTest {
        val store = store()
        store.setThemeMode(ThemeMode.Dark)
        assertEquals(ThemeMode.Dark, store.settings.first().themeMode)

        store.setThemeMode(ThemeMode.Light)
        assertEquals(ThemeMode.Light, store.settings.first().themeMode)
    }

    @Test
    fun the_snooze_defaults_persist_together() = runTest {
        val store = store()
        store.setDefaultSnooze(minutes = 10, repeats = 5)
        val settings = store.settings.first()
        assertEquals(10, settings.defaultSnoozeMinutes)
        assertEquals(5, settings.defaultSnoozeRepeats)
    }

    @Test
    fun an_unlimited_snooze_repeat_is_stored_as_zero() = runTest {
        val store = store()
        store.setDefaultSnooze(minutes = 3, repeats = 0)
        assertEquals(0, store.settings.first().defaultSnoozeRepeats)
    }

    @Test
    fun the_timer_vibration_choice_persists() = runTest {
        val store = store()
        store.setTimerVibration(false)
        assertEquals(false, store.settings.first().timerVibration)
    }

    @Test
    fun every_theme_mode_round_trips() = runTest {
        val store = store()
        ThemeMode.entries.forEach { mode ->
            store.setThemeMode(mode)
            assertEquals(mode, store.settings.first().themeMode)
        }
    }
}
