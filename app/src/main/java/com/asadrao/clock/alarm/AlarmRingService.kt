package com.asadrao.clock.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.asadrao.clock.ClockApplication
import com.asadrao.clock.domain.model.Alarm
import com.asadrao.clock.ui.format.AlarmFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime

/**
 * Keeps an alarm ringing.
 *
 * A foreground service, because the sound has to continue while the screen is off and while the
 * user is in another app, and because the ringing outlives whatever process state the app happened
 * to be in when the alarm fired. Its notification carries the full-screen intent that brings up
 * [AlarmRingingActivity], and also carries working Snooze and Dismiss actions so the alarm is
 * still usable if the full-screen intent is not honoured.
 *
 * A stop is idempotent from every route — the notification action, the ringing screen, the volume
 * keys — because more than one of them can arrive for the same alarm.
 */
class AlarmRingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainScope = MainScope()
    private var audio: AlarmAudio? = null
    private var ringingAlarmId: Long = Alarm.NO_ID

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra(AlarmIntents.EXTRA_ALARM_ID, Alarm.NO_ID) ?: Alarm.NO_ID
        when (intent?.action) {
            ACTION_START -> startRinging(alarmId)
            ACTION_SNOOZE -> snooze(alarmId)
            ACTION_DISMISS -> dismiss(alarmId)
            else -> {
                Log.w(TAG, "unexpected action ${intent?.action}; stopping")
                stopEverything()
            }
        }
        // Not sticky: if the system kills us mid-ring we must not be restarted with a null intent
        // and start making a noise with no alarm behind it. AlarmManager owns the schedule.
        return START_NOT_STICKY
    }

    private fun startRinging(alarmId: Long) {
        if (alarmId == Alarm.NO_ID) {
            stopEverything()
            return
        }
        if (ringingAlarmId == alarmId && audio != null) return // already ringing this alarm

        val app = application as? ClockApplication ?: run { stopEverything(); return }
        val container = app.container

        scope.launch {
            val alarm = container.alarmRepository.getAlarm(alarmId)
            if (alarm == null) {
                Log.w(TAG, "alarm $alarmId no longer exists; not ringing")
                stopEverything()
                return@launch
            }

            // A fresh scheduled ring resets the snooze budget for the day.
            val snoozeCount = container.snoozeTracker.count(alarmId)
            val snoozeAvailable = alarm.snoozeEnabled &&
                (alarm.snoozeRepeatLimit == UNLIMITED_SNOOZE || snoozeCount < alarm.snoozeRepeatLimit)

            val now = ZonedDateTime.now()
            val timeText = AlarmFormat.time(alarm.hour, alarm.minute, is24Hour = false) +
                (AlarmFormat.meridiem(alarm.hour, java.util.Locale.getDefault(), false)
                    ?.let { " $it" } ?: "")

            withContext(Dispatchers.Main) {
                ringingAlarmId = alarmId
                ClockNotifications.ensureChannels(this@AlarmRingService)
                val notification = ClockNotifications.ringing(
                    context = this@AlarmRingService,
                    alarm = alarm,
                    timeText = timeText,
                    snoozeAvailable = snoozeAvailable,
                )

                // Posted through notify() *before* startForeground, and this order is the whole
                // trick. A full-screen intent is only acted on for a fresh, interruptive post; a
                // notification handed straight to startForeground() is not treated as one, so the
                // ringing screen never appeared. Verified on a Pixel 8 running Android 17, where
                // the USE_FULL_SCREEN_INTENT appop was noted as allowed and yet no activity was
                // ever started.
                //
                // startForeground with the same id then counts as an update, which deliberately
                // does not fire the intent a second time.
                // Guarded because the user can refuse notifications. Without the permission the
                // post is a no-op and there is no full-screen intent to raise the ringing screen,
                // so the alarm degrades to sound only — audible, which is the part that matters.
                val notifications = NotificationManagerCompat.from(this@AlarmRingService)
                if (notifications.areNotificationsEnabled()) {
                    notifications.notify(ClockNotifications.NOTIFICATION_RINGING, notification)
                }
                startForegroundCompat(notification)

                // Audio last, so the ringing screen is on its way up first.
                //
                // A note for anyone reading `dumpsys audio` here: every player logs
                // `event:muted updated source:none` right after starting. That is the mute-*source*
                // set being updated to none — the player is not muted. Android also logs
                // "AudioHardening background playback would be muted ... usage: USAGE_ALARM", which
                // is likewise advisory about a future policy rather than a description of what
                // happened. Confirmed audible on a Pixel 8 with STREAM_ALARM reporting
                // `Muted: false`. Both lines look alarming and neither is a fault.
                audio = AlarmAudio(this@AlarmRingService).also { it.start(alarm) }
            }
        }
    }

    private fun startForegroundCompat(notification: android.app.Notification) {
        // Android 14 requires a declared foreground-service type. mediaPlayback is the honest
        // description of what this service does: it plays audio the user must hear.
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            ClockNotifications.NOTIFICATION_RINGING,
            notification,
            type,
        )
    }

    private fun snooze(alarmId: Long) {
        val app = application as? ClockApplication ?: run { stopEverything(); return }
        val container = app.container
        scope.launch {
            val alarm = container.alarmRepository.getAlarm(alarmId)
            if (alarm != null && alarm.snoozeEnabled) {
                val used = container.snoozeTracker.increment(alarmId)
                val allowed = alarm.snoozeRepeatLimit == UNLIMITED_SNOOZE ||
                    used <= alarm.snoozeRepeatLimit
                if (allowed) {
                    container.alarmSchedulingCoordinator.snooze(alarmId)
                } else {
                    Log.i(TAG, "snooze budget for alarm $alarmId is used up; dismissing instead")
                }
            }
            stopEverything()
        }
    }

    private fun dismiss(alarmId: Long) {
        val app = application as? ClockApplication
        val container = app?.container
        scope.launch {
            container?.let {
                // Clears any pending snooze, and resets the budget so tomorrow starts fresh.
                it.alarmSchedulingCoordinator.dismiss(alarmId)
                it.snoozeTracker.reset(alarmId)
            }
            stopEverything()
        }
    }

    private fun stopEverything() {
        mainScope.launch {
            audio?.stop()
            audio = null
            ringingAlarmId = Alarm.NO_ID
            ServiceCompat.stopForeground(this@AlarmRingService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            NotificationManagerCompat.from(this@AlarmRingService)
                .cancel(ClockNotifications.NOTIFICATION_RINGING)
            stopSelf()
        }
    }

    override fun onDestroy() {
        audio?.stop()
        audio = null
        scope.cancel()
        mainScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AlarmRingService"

        const val ACTION_START = "com.asadrao.clock.action.RING_START"
        const val ACTION_SNOOZE = "com.asadrao.clock.action.RING_SNOOZE"
        const val ACTION_DISMISS = "com.asadrao.clock.action.RING_DISMISS"

        /** A snooze repeat limit of zero means "as many times as you like". */
        const val UNLIMITED_SNOOZE = 0

        fun startIntent(context: Context, alarmId: Long): Intent =
            Intent(context, AlarmRingService::class.java).apply {
                action = ACTION_START
                putExtra(AlarmIntents.EXTRA_ALARM_ID, alarmId)
            }

        fun snoozeIntent(context: Context, alarmId: Long): Intent =
            Intent(context, AlarmRingService::class.java).apply {
                action = ACTION_SNOOZE
                putExtra(AlarmIntents.EXTRA_ALARM_ID, alarmId)
            }

        fun dismissIntent(context: Context, alarmId: Long): Intent =
            Intent(context, AlarmRingService::class.java).apply {
                action = ACTION_DISMISS
                putExtra(AlarmIntents.EXTRA_ALARM_ID, alarmId)
            }
    }
}
