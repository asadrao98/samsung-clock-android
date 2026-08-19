package com.asadrao.clock

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.asadrao.clock.alarm.AlarmIntents
import com.asadrao.clock.alarm.AlarmReceiver
import com.asadrao.clock.alarm.AlarmRingService
import com.asadrao.clock.alarm.ClockNotifications
import com.asadrao.clock.data.prefs.SnoozeTracker
import com.asadrao.clock.domain.model.Alarm
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Phase 3: what happens when an alarm actually goes off.
 *
 * The genuinely important assertion is the notification channel's silence — see the comment on
 * that test. The rest cover the routing between receiver, service and notification, which is
 * where a wiring mistake would leave an alarm that fires but never makes a sound.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w412dp-h915dp-xhdpi")
class RingingTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    /**
     * The receiver under test reaches for the application's own container, which opens the real
     * database. Left open it trips CloseGuard and fails a later, unrelated test.
     */
    @After
    fun tearDown() {
        (context as? ClockApplication)?.container?.close()
    }

    private fun alarm(
        id: Long = 1,
        label: String = "",
        snoozeEnabled: Boolean = true,
        snoozeLimit: Int = 3,
    ) = Alarm(
        id = id,
        hour = 7,
        minute = 30,
        label = label,
        snoozeEnabled = snoozeEnabled,
        snoozeRepeatLimit = snoozeLimit,
    )

    // ---- channels ------------------------------------------------------------------------

    @Test
    fun the_alarm_channel_is_created_at_high_importance() {
        ClockNotifications.ensureChannels(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = manager.getNotificationChannel(ClockNotifications.CHANNEL_ALARM)

        assertNotNull(channel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel!!.importance)
    }

    @Test
    fun the_alarm_channel_is_deliberately_silent_and_does_not_vibrate() {
        // This looks like a bug and is not. The ringing service plays the alarm itself with
        // USAGE_ALARM audio and drives the vibrator directly. If the channel also made a sound it
        // would either double up, or — much worse — hand control of whether an alarm is audible to
        // a per-channel setting the user can switch off. An alarm clock that a notification
        // setting can silence is not an alarm clock.
        ClockNotifications.ensureChannels(context)
        val channel = context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(ClockNotifications.CHANNEL_ALARM)!!

        assertEquals(null, channel.sound)
        assertFalse(channel.shouldVibrate())
    }

    @Test
    fun the_alarm_channel_bypasses_do_not_disturb() {
        ClockNotifications.ensureChannels(context)
        val channel = context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(ClockNotifications.CHANNEL_ALARM)!!
        assertTrue(channel.canBypassDnd())
    }

    // ---- the ringing notification --------------------------------------------------------

    @Test
    fun the_ringing_notification_is_an_alarm_that_cannot_be_swiped_away() {
        ClockNotifications.ensureChannels(context)
        val notification = ClockNotifications.ringing(
            context = context,
            alarm = alarm(label = "Wake up"),
            timeText = "7:30 AM",
            snoozeAvailable = true,
        )

        assertEquals(android.app.Notification.CATEGORY_ALARM, notification.category)
        // Ongoing: an alarm the user can flick away without deciding is a missed alarm.
        assertTrue(
            "ringing notification must be ongoing",
            notification.flags and android.app.Notification.FLAG_ONGOING_EVENT != 0,
        )
        assertNotNull("a full-screen intent is what shows the alarm over the lockscreen",
            notification.fullScreenIntent)
    }

    @Test
    fun the_ringing_notification_offers_snooze_and_dismiss() {
        // These actions are the fallback path: if the user has revoked the full-screen-intent
        // permission the system shows a heads-up notification instead of the alarm screen, and
        // these are then the only way to answer the alarm.
        ClockNotifications.ensureChannels(context)
        val notification = ClockNotifications.ringing(
            context = context,
            alarm = alarm(),
            timeText = "7:30 AM",
            snoozeAvailable = true,
        )
        val titles = notification.actions.orEmpty().map { it.title.toString() }
        assertTrue("expected Dismiss, got $titles", titles.contains("Dismiss"))
        assertTrue("expected Snooze, got $titles", titles.contains("Snooze"))
    }

    @Test
    fun the_notification_omits_snooze_when_it_is_unavailable() {
        ClockNotifications.ensureChannels(context)
        val notification = ClockNotifications.ringing(
            context = context,
            alarm = alarm(snoozeEnabled = false),
            timeText = "7:30 AM",
            snoozeAvailable = false,
        )
        val titles = notification.actions.orEmpty().map { it.title.toString() }
        assertFalse("a disabled action would be worse than none", titles.contains("Snooze"))
        assertTrue(titles.contains("Dismiss"))
    }

    @Test
    fun an_unnamed_alarm_falls_back_to_a_sensible_title() {
        ClockNotifications.ensureChannels(context)
        val notification = ClockNotifications.ringing(
            context = context,
            alarm = alarm(label = ""),
            timeText = "7:30 AM",
            snoozeAvailable = true,
        )
        val title = notification.extras.getCharSequence(android.app.Notification.EXTRA_TITLE)
        assertEquals("Alarm", title?.toString())
    }

    // ---- receiver routing ----------------------------------------------------------------

    @Test
    fun firing_an_alarm_starts_the_ringing_service() {
        // The single most important link in the chain: if the receiver does not start the
        // service, nothing makes a sound.
        val intent = AlarmIntents.fireIntent(context, 42L)
        AlarmReceiver().onReceive(context, intent)

        val started = shadowOf(context as android.app.Application).nextStartedService
        assertNotNull("the ringing service should have been started", started)
        assertEquals(AlarmRingService.ACTION_START, started!!.action)
        assertEquals(42L, started.getLongExtra(AlarmIntents.EXTRA_ALARM_ID, -1L))
    }

    @Test
    fun a_fire_intent_without_an_alarm_id_is_ignored() {
        val intent = android.content.Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmIntents.ACTION_FIRE
        }
        AlarmReceiver().onReceive(context, intent)
        assertEquals(null, shadowOf(context as android.app.Application).nextStartedService)
    }

    // ---- snooze budget -------------------------------------------------------------------

    @Test
    fun the_snooze_count_persists_and_resets() = runTest {
        // Persisted rather than held in the service, because the count has to survive the process
        // being killed between snoozes — otherwise the budget silently resets and snooze becomes
        // unlimited.
        val tracker = SnoozeTracker(InMemoryPreferenceDataStore())
        assertEquals(0, tracker.count(7L))
        assertEquals(1, tracker.increment(7L))
        assertEquals(2, tracker.increment(7L))
        assertEquals(2, tracker.count(7L))

        tracker.reset(7L)
        assertEquals(0, tracker.count(7L))
    }

    @Test
    fun snooze_counts_are_tracked_per_alarm() = runTest {
        val tracker = SnoozeTracker(InMemoryPreferenceDataStore())
        tracker.increment(1L)
        tracker.increment(1L)
        tracker.increment(2L)

        assertEquals(2, tracker.count(1L))
        assertEquals(1, tracker.count(2L))

        tracker.reset(1L)
        assertEquals(0, tracker.count(1L))
        assertEquals("resetting one alarm must not clear another", 1, tracker.count(2L))
    }
}
