package com.asadrao.clock

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.asadrao.clock.alarm.AlarmIntents
import com.asadrao.clock.alarm.AlarmReceiver
import com.asadrao.clock.alarm.AndroidAlarmScheduler
import com.asadrao.clock.domain.model.Alarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager

/**
 * Checks what actually reaches [AlarmManager].
 *
 * The case worth the most here is the second test. `PendingIntent` equality ignores extras, so
 * an implementation that distinguished alarms only by an id extra would look completely correct
 * — and every alarm the user set would quietly overwrite the one before it. That failure is
 * invisible in code review and brutal in use, so it gets a test.
 */
@RunWith(RobolectricTestRunner::class)
class AndroidAlarmSchedulerTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var shadow: ShadowAlarmManager
    private lateinit var scheduler: AndroidAlarmScheduler

    private val t1 = 1_800_000_000_000L
    private val t2 = 1_800_000_060_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(AlarmManager::class.java)
        shadow = shadowOf(alarmManager)
        scheduler = AndroidAlarmScheduler(context)
        // Robolectric's shadow defaults canScheduleExactAlarms() to false and does not model
        // the install-time USE_EXACT_ALARM grant this app gets on API 33+. Turn it on so the
        // tests below exercise the real setAlarmClock path; the fallback gets its own test.
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    private fun alarm(id: Long) = Alarm(id = id, hour = 7, minute = 0)

    @Test
    fun scheduling_registers_a_wake_up_alarm_at_the_requested_time() {
        scheduler.schedule(alarm(1), t1)

        val scheduled = shadow.scheduledAlarms
        assertEquals(1, scheduled.size)
        assertEquals(AlarmManager.RTC_WAKEUP, scheduled[0].type)
        assertEquals(t1, scheduled[0].triggerAtTime)
    }

    @Test
    fun two_different_alarms_do_not_overwrite_each_other() {
        scheduler.schedule(alarm(1), t1)
        scheduler.schedule(alarm(2), t2)

        assertEquals(
            "each alarm needs its own pending intent identity",
            2,
            shadow.scheduledAlarms.size,
        )
        val times = shadow.scheduledAlarms.map { it.triggerAtTime }.sorted()
        assertEquals(listOf(t1, t2), times)
    }

    @Test
    fun many_alarms_all_survive_together() {
        (1L..10L).forEach { id -> scheduler.schedule(alarm(id), t1 + id * 60_000) }
        assertEquals(10, shadow.scheduledAlarms.size)
    }

    @Test
    fun rescheduling_the_same_alarm_replaces_rather_than_duplicates() {
        scheduler.schedule(alarm(1), t1)
        scheduler.schedule(alarm(1), t2)

        assertEquals(1, shadow.scheduledAlarms.size)
        assertEquals(t2, shadow.scheduledAlarms[0].triggerAtTime)
    }

    @Test
    fun an_alarm_and_its_own_snooze_coexist() {
        // They must be separately cancellable, so they cannot share an identity either.
        scheduler.schedule(alarm(1), t1)
        scheduler.scheduleSnooze(alarm(1), t2)
        assertEquals(2, shadow.scheduledAlarms.size)
    }

    @Test
    fun cancelling_removes_only_that_alarm() {
        scheduler.schedule(alarm(1), t1)
        scheduler.schedule(alarm(2), t2)

        scheduler.cancel(1)

        assertEquals(1, shadow.scheduledAlarms.size)
        assertEquals(t2, shadow.scheduledAlarms[0].triggerAtTime)
    }

    @Test
    fun cancelling_a_snooze_leaves_the_alarm_itself_scheduled() {
        scheduler.schedule(alarm(1), t1)
        scheduler.scheduleSnooze(alarm(1), t2)

        scheduler.cancelSnooze(1)

        assertEquals(1, shadow.scheduledAlarms.size)
        assertEquals(t1, shadow.scheduledAlarms[0].triggerAtTime)
    }

    @Test
    fun cancelling_something_never_scheduled_is_harmless() {
        scheduler.cancel(99)
        assertTrue(shadow.scheduledAlarms.isEmpty())
    }

    @Test
    fun a_permitted_exact_alarm_is_registered_as_a_real_alarm_clock() {
        // getNextAlarmClock() is only populated by setAlarmClock, so it is the discriminator
        // between the good path and the degraded one. This is what puts the alarm icon in the
        // status bar and exempts the alarm from Doze.
        scheduler.schedule(alarm(1), t1)

        val info = alarmManager.nextAlarmClock
        assertNotNull("setAlarmClock should have been used", info)
        assertEquals(t1, info!!.triggerTime)
    }

    @Test
    fun a_denied_exact_alarm_still_schedules_a_wake_up_rather_than_being_dropped() {
        // Only reachable on Android 12/12L. Degraded, but the alarm must still exist: firing
        // a few minutes late beats not firing at all.
        ShadowAlarmManager.setCanScheduleExactAlarms(false)

        scheduler.schedule(alarm(1), t1)

        assertFalse(scheduler.canScheduleExact())
        assertEquals(1, shadow.scheduledAlarms.size)
        assertEquals(AlarmManager.RTC_WAKEUP, shadow.scheduledAlarms[0].type)
        assertEquals(t1, shadow.scheduledAlarms[0].triggerAtTime)
        assertNull(
            "the fallback must not masquerade as an exact alarm clock",
            alarmManager.nextAlarmClock,
        )
    }

    @Test
    fun the_scheduled_intent_names_this_apps_receiver_and_the_right_alarm() {
        // Closes the seam between scheduling and receiving: if the intent were missing its id,
        // or aimed at the wrong component, everything above would still pass.
        scheduler.schedule(alarm(7), t1)

        val intent = shadowOf(shadow.scheduledAlarms[0].operation).savedIntent
        assertEquals(AlarmIntents.ACTION_FIRE, intent.action)
        assertEquals(7L, AlarmIntents.alarmIdFrom(intent))
        assertEquals(AlarmReceiver::class.java.name, intent.component?.className)
        // The id also has to be in the data URI, since that is what makes the PendingIntent
        // distinct. Extras alone would not.
        assertTrue(intent.data.toString().endsWith("/7"))
    }

    @Test
    fun a_snooze_intent_is_distinguishable_from_a_fire_intent() {
        scheduler.scheduleSnooze(alarm(7), t1)

        val intent = shadowOf(shadow.scheduledAlarms[0].operation).savedIntent
        assertEquals(AlarmIntents.ACTION_SNOOZE_FIRE, intent.action)
        assertEquals(7L, AlarmIntents.alarmIdFrom(intent))
        assertTrue(
            "fire and snooze must not share a data URI",
            AlarmIntents.fireIntent(context, 7).data != intent.data,
        )
        assertTrue(
            "fire and snooze must not share a request code",
            AlarmIntents.fireRequestCode(7) != AlarmIntents.snoozeRequestCode(7),
        )
    }
}
