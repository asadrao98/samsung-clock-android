package com.asadrao.clock

import com.asadrao.clock.domain.model.Alarm
import com.asadrao.clock.domain.model.RepeatDays
import com.asadrao.clock.domain.repository.AlarmRepository
import com.asadrao.clock.domain.schedule.AlarmScheduler
import com.asadrao.clock.domain.schedule.AlarmSchedulingCoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The scheduling *policy*: what gets armed, when, and what happens after a ring.
 *
 * Uses a recording fake scheduler, so these run as plain JVM tests. This is the behaviour the
 * brief cares most about — reboot survival, timezone changes, one-shot alarms switching
 * themselves off — and none of it needs a device to verify.
 */
class AlarmSchedulingCoordinatorTest {

    private val dubai = ZoneId.of("Asia/Dubai")

    // 19 Aug 2026 is a Wednesday. 06:00 Dubai.
    private val nowInstant: Instant = ZonedDateTime.of(2026, 8, 19, 6, 0, 0, 0, dubai).toInstant()

    private class FakeScheduler : AlarmScheduler {
        val scheduled = mutableMapOf<Long, Long>()
        val snoozes = mutableMapOf<Long, Long>()
        val cancelled = mutableListOf<Long>()
        val cancelledSnoozes = mutableListOf<Long>()
        var exactAllowed = true

        override fun canScheduleExact() = exactAllowed
        override fun schedule(alarm: Alarm, triggerAtMillis: Long) {
            scheduled[alarm.id] = triggerAtMillis
        }
        override fun scheduleSnooze(alarm: Alarm, triggerAtMillis: Long) {
            snoozes[alarm.id] = triggerAtMillis
        }
        override fun cancel(alarmId: Long) {
            cancelled += alarmId
            scheduled -= alarmId
        }
        override fun cancelSnooze(alarmId: Long) {
            cancelledSnoozes += alarmId
            snoozes -= alarmId
        }
    }

    private class FakeRepository(alarms: List<Alarm>) : AlarmRepository {
        val store = alarms.associateBy { it.id }.toMutableMap()
        override fun observeAlarms(): Flow<List<Alarm>> = flowOf(store.values.toList())
        override fun observeAlarm(id: Long): Flow<Alarm?> = flowOf(store[id])
        override suspend fun getAlarm(id: Long) = store[id]
        override suspend fun getAllAlarms() = store.values.sortedBy { it.id }
        override suspend fun getEnabledAlarms() = store.values.filter { it.enabled }
        override suspend fun addAlarm(alarm: Alarm): Long {
            store[alarm.id] = alarm; return alarm.id
        }
        override suspend fun updateAlarm(alarm: Alarm) { store[alarm.id] = alarm }
        override suspend fun deleteAlarm(id: Long) { store -= id }
        override suspend fun setEnabled(id: Long, enabled: Boolean) {
            store[id] = store.getValue(id).copy(enabled = enabled)
        }
    }

    private fun coordinator(
        alarms: List<Alarm>,
        zone: ZoneId = dubai,
    ): Triple<AlarmSchedulingCoordinator, FakeRepository, FakeScheduler> {
        val repo = FakeRepository(alarms)
        val scheduler = FakeScheduler()
        val clock = Clock.fixed(nowInstant, zone)
        return Triple(AlarmSchedulingCoordinator(repo, scheduler, clock), repo, scheduler)
    }

    private fun expectedMillis(
        year: Int, month: Int, day: Int, hour: Int, minute: Int, zone: ZoneId = dubai,
    ) = ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    // ---- reboot / bulk re-arm -------------------------------------------------------------

    @Test
    fun sync_all_arms_every_enabled_alarm() = runTest {
        val (coordinator, _, scheduler) = coordinator(
            listOf(
                Alarm(id = 1, hour = 7, minute = 0),
                Alarm(id = 2, hour = 8, minute = 30),
            )
        )
        coordinator.syncAll()

        assertEquals(2, scheduler.scheduled.size)
        assertEquals(expectedMillis(2026, 8, 19, 7, 0), scheduler.scheduled[1])
        assertEquals(expectedMillis(2026, 8, 19, 8, 30), scheduler.scheduled[2])
    }

    @Test
    fun sync_all_leaves_disabled_alarms_unarmed_and_clears_any_leftovers() = runTest {
        val (coordinator, _, scheduler) = coordinator(
            listOf(
                Alarm(id = 1, hour = 7, minute = 0, enabled = true),
                Alarm(id = 2, hour = 8, minute = 0, enabled = false),
            )
        )
        coordinator.syncAll()

        assertTrue(scheduler.scheduled.containsKey(1))
        assertFalse(scheduler.scheduled.containsKey(2))
        // Cancelled defensively, to clear anything left by a crash between write and schedule.
        assertTrue(scheduler.cancelled.contains(2))
        assertTrue(scheduler.cancelledSnoozes.contains(2))
    }

    @Test
    fun sync_all_with_no_alarms_does_nothing_and_does_not_throw() = runTest {
        val (coordinator, _, scheduler) = coordinator(emptyList())
        coordinator.syncAll()
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun sync_all_recomputes_against_the_current_timezone() = runTest {
        // The same 07:00 alarm maps to a different instant in New York than in Dubai. This is
        // what makes travelling work: re-running syncAll in the new zone is sufficient.
        val alarm = Alarm(id = 1, hour = 7, minute = 0, repeatDays = RepeatDays.EveryDay)
        val newYork = ZoneId.of("America/New_York")

        val (dubaiCoordinator, _, dubaiScheduler) = coordinator(listOf(alarm))
        dubaiCoordinator.syncAll()

        val (nyCoordinator, _, nyScheduler) = coordinator(listOf(alarm), zone = newYork)
        nyCoordinator.syncAll()

        val dubaiTrigger = dubaiScheduler.scheduled.getValue(1)
        val nyTrigger = nyScheduler.scheduled.getValue(1)
        assertTrue("the two zones must not resolve to the same instant", dubaiTrigger != nyTrigger)
        assertEquals(7, ZonedDateTime.ofInstant(Instant.ofEpochMilli(dubaiTrigger), dubai).hour)
        assertEquals(7, ZonedDateTime.ofInstant(Instant.ofEpochMilli(nyTrigger), newYork).hour)
    }

    // ---- single alarm sync ---------------------------------------------------------------

    @Test
    fun sync_arms_an_enabled_alarm() = runTest {
        val (coordinator, _, scheduler) = coordinator(listOf(Alarm(id = 1, hour = 9, minute = 15)))
        coordinator.sync(1)
        assertEquals(expectedMillis(2026, 8, 19, 9, 15), scheduler.scheduled[1])
    }

    @Test
    fun sync_cancels_a_disabled_alarm() = runTest {
        val (coordinator, _, scheduler) =
            coordinator(listOf(Alarm(id = 1, hour = 9, minute = 0, enabled = false)))
        coordinator.sync(1)
        assertTrue(scheduler.cancelled.contains(1))
        assertFalse(scheduler.scheduled.containsKey(1))
    }

    @Test
    fun sync_cancels_an_alarm_that_has_been_deleted() = runTest {
        // The row is gone but the system may still hold a pending trigger for it.
        val (coordinator, _, scheduler) = coordinator(emptyList())
        coordinator.sync(42)
        assertTrue(scheduler.cancelled.contains(42))
        assertTrue(scheduler.cancelledSnoozes.contains(42))
    }

    // ---- after the alarm rings ------------------------------------------------------------

    @Test
    fun a_repeating_alarm_is_re_armed_for_its_next_occurrence() = runTest {
        val alarm = Alarm(id = 1, hour = 6, minute = 0, repeatDays = RepeatDays.EveryDay)
        val (coordinator, repo, scheduler) = coordinator(listOf(alarm))

        // It is exactly 06:00 now, i.e. the alarm is firing this instant.
        coordinator.onAlarmFired(1)

        assertEquals(expectedMillis(2026, 8, 20, 6, 0), scheduler.scheduled[1])
        assertTrue("a repeating alarm stays enabled", repo.store.getValue(1).enabled)
    }

    @Test
    fun a_repeating_alarm_does_not_re_arm_onto_the_instant_it_just_fired() = runTest {
        // The infinite-ring bug: if "next" were computed inclusively it would return now.
        val alarm = Alarm(id = 1, hour = 6, minute = 0, repeatDays = RepeatDays.EveryDay)
        val (coordinator, _, scheduler) = coordinator(listOf(alarm))
        coordinator.onAlarmFired(1)
        assertTrue(scheduler.scheduled.getValue(1) > nowInstant.toEpochMilli())
    }

    @Test
    fun a_one_shot_alarm_switches_itself_off_but_is_not_deleted() = runTest {
        val (coordinator, repo, scheduler) =
            coordinator(listOf(Alarm(id = 1, hour = 6, minute = 0, label = "Dentist")))

        coordinator.onAlarmFired(1)

        val stored = repo.store.getValue(1)
        assertFalse("a fired one-shot must end up disabled", stored.enabled)
        assertEquals("it stays in the list, ready to re-enable", "Dentist", stored.label)
        assertTrue(scheduler.cancelled.contains(1))
        assertFalse(scheduler.scheduled.containsKey(1))
    }

    @Test
    fun firing_an_alarm_that_no_longer_exists_is_harmless() = runTest {
        val (coordinator, _, scheduler) = coordinator(emptyList())
        coordinator.onAlarmFired(7)
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun a_weekly_alarm_re_arms_a_week_out() = runTest {
        val alarm = Alarm(
            id = 1, hour = 6, minute = 0,
            repeatDays = RepeatDays.of(DayOfWeek.WEDNESDAY),
        )
        val (coordinator, _, scheduler) = coordinator(listOf(alarm))
        coordinator.onAlarmFired(1)
        assertEquals(expectedMillis(2026, 8, 26, 6, 0), scheduler.scheduled[1])
    }

    // ---- snooze and dismiss ---------------------------------------------------------------

    @Test
    fun snooze_arms_a_re_ring_and_reports_when() = runTest {
        val (coordinator, _, scheduler) = coordinator(
            listOf(Alarm(id = 1, hour = 6, minute = 0, snoozeDurationMinutes = 9))
        )
        val ringAt = coordinator.snooze(1)!!

        assertEquals(expectedMillis(2026, 8, 19, 6, 9), scheduler.snoozes[1])
        assertEquals(6, ringAt.hour)
        assertEquals(9, ringAt.minute)
    }

    @Test
    fun snooze_is_refused_when_the_alarm_has_snooze_switched_off() = runTest {
        val (coordinator, _, scheduler) = coordinator(
            listOf(Alarm(id = 1, hour = 6, minute = 0, snoozeEnabled = false))
        )
        assertNull(coordinator.snooze(1))
        assertTrue(scheduler.snoozes.isEmpty())
    }

    @Test
    fun snoozing_an_absent_alarm_returns_null() = runTest {
        val (coordinator, _, _) = coordinator(emptyList())
        assertNull(coordinator.snooze(99))
    }

    @Test
    fun dismiss_clears_the_snooze_but_leaves_tomorrow_armed() = runTest {
        val alarm = Alarm(id = 1, hour = 6, minute = 0, repeatDays = RepeatDays.EveryDay)
        val (coordinator, _, scheduler) = coordinator(listOf(alarm))

        coordinator.onAlarmFired(1)          // tomorrow is armed
        coordinator.snooze(1)                // user snoozes
        coordinator.dismiss(1)               // then dismisses

        assertTrue(scheduler.cancelledSnoozes.contains(1))
        assertTrue(scheduler.snoozes.isEmpty())
        assertEquals(
            "dismissing today must not cancel tomorrow",
            expectedMillis(2026, 8, 20, 6, 0),
            scheduler.scheduled[1],
        )
    }
}
