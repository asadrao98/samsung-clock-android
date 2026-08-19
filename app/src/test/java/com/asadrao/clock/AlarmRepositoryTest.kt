package com.asadrao.clock

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.asadrao.clock.data.db.ClockDatabase
import com.asadrao.clock.data.repository.RoomAlarmRepository
import com.asadrao.clock.domain.model.Alarm
import com.asadrao.clock.domain.model.RepeatDays
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.DayOfWeek

/**
 * Exercises the repository against a real Room database (in memory), not a fake, so the
 * mapping, the SQL and the ordering are all covered.
 */
@RunWith(RobolectricTestRunner::class)
class AlarmRepositoryTest {

    private lateinit var db: ClockDatabase
    private lateinit var repo: RoomAlarmRepository
    private var fakeNow = 1_000L

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, ClockDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = RoomAlarmRepository(db.alarmDao(), now = { fakeNow })
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun sample(hour: Int = 7, minute: Int = 30) = Alarm(
        hour = hour,
        minute = minute,
        label = "Wake up",
        repeatDays = RepeatDays.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
    )

    @Test
    fun an_added_alarm_comes_back_with_every_field_intact() = runTest {
        val id = repo.addAlarm(sample())
        val loaded = repo.getAlarm(id)!!

        assertEquals(7, loaded.hour)
        assertEquals(30, loaded.minute)
        assertEquals("Wake up", loaded.label)
        assertEquals(RepeatDays.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), loaded.repeatDays)
        assertTrue(loaded.enabled)
        assertTrue(loaded.vibrationEnabled)
        assertEquals(Alarm.DEFAULT_SNOOZE_MINUTES, loaded.snoozeDurationMinutes)
        assertNull(loaded.soundUri)
    }

    @Test
    fun add_stamps_both_timestamps_with_the_same_instant() = runTest {
        fakeNow = 5_000L
        val loaded = repo.getAlarm(repo.addAlarm(sample()))!!
        assertEquals(5_000L, loaded.createdAt)
        assertEquals(5_000L, loaded.updatedAt)
    }

    @Test
    fun add_ignores_a_caller_supplied_id_instead_of_overwriting_a_row() = runTest {
        val firstId = repo.addAlarm(sample(6, 0))
        // Hand it an alarm already carrying the first row's id.
        val secondId = repo.addAlarm(sample(8, 0).copy(id = firstId))

        assertTrue("a new row should have been created", secondId != firstId)
        assertEquals(2, repo.getAllAlarms().size)
        assertEquals(6, repo.getAlarm(firstId)!!.hour)
    }

    @Test
    fun update_changes_fields_and_moves_updated_at_only() = runTest {
        val id = repo.addAlarm(sample())
        val original = repo.getAlarm(id)!!
        fakeNow = 9_000L

        repo.updateAlarm(original.copy(label = "Gym", minute = 45))
        val updated = repo.getAlarm(id)!!

        assertEquals("Gym", updated.label)
        assertEquals(45, updated.minute)
        assertEquals(original.createdAt, updated.createdAt)
        assertEquals(9_000L, updated.updatedAt)
    }

    @Test
    fun set_enabled_toggles_without_disturbing_anything_else() = runTest {
        val id = repo.addAlarm(sample())
        repo.setEnabled(id, false)
        val disabled = repo.getAlarm(id)!!
        assertFalse(disabled.enabled)
        assertEquals("Wake up", disabled.label)
        assertEquals(RepeatDays.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), disabled.repeatDays)

        repo.setEnabled(id, true)
        assertTrue(repo.getAlarm(id)!!.enabled)
    }

    @Test
    fun delete_removes_only_the_named_alarm() = runTest {
        val keep = repo.addAlarm(sample(6, 0))
        val drop = repo.addAlarm(sample(9, 0))

        repo.deleteAlarm(drop)

        assertNull(repo.getAlarm(drop))
        assertEquals(listOf(keep), repo.getAllAlarms().map { it.id })
    }

    @Test
    fun deleting_an_absent_alarm_is_a_no_op_rather_than_a_failure() = runTest {
        repo.addAlarm(sample())
        repo.deleteAlarm(9_999L)
        assertEquals(1, repo.getAllAlarms().size)
    }

    @Test
    fun alarms_are_listed_by_time_of_day_not_insertion_order() = runTest {
        repo.addAlarm(sample(22, 15))
        repo.addAlarm(sample(6, 5))
        repo.addAlarm(sample(6, 0))
        repo.addAlarm(sample(13, 40))

        assertEquals(
            listOf(6 to 0, 6 to 5, 13 to 40, 22 to 15),
            repo.getAllAlarms().map { it.hour to it.minute },
        )
    }

    @Test
    fun alarms_at_the_same_time_keep_a_stable_order() = runTest {
        // Two alarms at 07:00 is an edge case the brief calls out; the list must not shuffle.
        val first = repo.addAlarm(sample(7, 0).copy(label = "A"))
        val second = repo.addAlarm(sample(7, 0).copy(label = "B"))
        assertEquals(listOf(first, second), repo.getAllAlarms().map { it.id })
        assertEquals(listOf(first, second), repo.getAllAlarms().map { it.id })
    }

    @Test
    fun get_enabled_returns_only_enabled_alarms() = runTest {
        val on = repo.addAlarm(sample(6, 0))
        val off = repo.addAlarm(sample(7, 0))
        repo.setEnabled(off, false)

        assertEquals(listOf(on), repo.getEnabledAlarms().map { it.id })
    }

    @Test
    fun the_alarm_list_flow_reflects_writes() = runTest {
        assertTrue(repo.observeAlarms().first().isEmpty())
        val id = repo.addAlarm(sample())
        assertEquals(listOf(id), repo.observeAlarms().first().map { it.id })
        repo.deleteAlarm(id)
        assertTrue(repo.observeAlarms().first().isEmpty())
    }

    @Test
    fun observing_a_single_alarm_yields_null_once_it_is_gone() = runTest {
        val id = repo.addAlarm(sample())
        assertEquals(id, repo.observeAlarm(id).first()!!.id)
        repo.deleteAlarm(id)
        assertNull(repo.observeAlarm(id).first())
    }

    @Test
    fun a_silent_alarm_and_a_default_sound_alarm_stay_distinguishable() = runTest {
        // null and the explicit silent marker mean different things and must not collapse.
        val silent = repo.addAlarm(sample().copy(soundUri = Alarm.SILENT_SOUND))
        val default = repo.addAlarm(sample(8, 0).copy(soundUri = null))

        assertTrue(repo.getAlarm(silent)!!.isSilent)
        assertFalse(repo.getAlarm(silent)!!.usesDefaultSound)
        assertTrue(repo.getAlarm(default)!!.usesDefaultSound)
        assertFalse(repo.getAlarm(default)!!.isSilent)
    }

    @Test
    fun a_non_repeating_alarm_persists_as_non_repeating() = runTest {
        val id = repo.addAlarm(sample().copy(repeatDays = RepeatDays.None))
        assertTrue(repo.getAlarm(id)!!.repeatDays.isEmpty)
        assertFalse(repo.getAlarm(id)!!.repeats)
    }
}
