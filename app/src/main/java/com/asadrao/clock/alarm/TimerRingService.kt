package com.asadrao.clock.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.net.toUri
import com.asadrao.clock.ClockApplication
import com.asadrao.clock.R
import com.asadrao.clock.domain.model.Alarm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Sounds a finished timer.
 *
 * Deliberately separate from the alarm's ringing service and channel: a timer going off is a
 * different event from an alarm going off, and the user should be able to tune them independently.
 * It reuses [AlarmAudio], so the sound is played with `USAGE_ALARM` and survives Do Not Disturb —
 * a kitchen timer nobody hears is useless.
 *
 * The notification offers Stop and +1 min rather than Snooze; a timer has no schedule to defer to.
 */
class TimerRingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainScope = MainScope()
    private var audio: AlarmAudio? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val timerId = intent?.getLongExtra(TimerScheduling.EXTRA_TIMER_ID, -1L) ?: -1L
        when (intent?.action) {
            ACTION_START -> start(timerId)
            ACTION_STOP -> stopEverything()
            ACTION_ADD_MINUTE -> addMinute(timerId)
            else -> stopEverything()
        }
        return START_NOT_STICKY
    }

    private fun start(timerId: Long) {
        if (timerId < 0L) {
            stopEverything()
            return
        }
        val app = application as? ClockApplication ?: run { stopEverything(); return }
        scope.launch {
            val timer = app.container.timerRepository.getTimer(timerId)
            val label = timer?.label.orEmpty()
            val vibrate = app.container.settingsStore.settings.first().timerVibration
            withContext(Dispatchers.Main) {
                ensureChannel()
                val notification = notification(timerId, label)
                // notify() before startForeground, for the same reason as the alarm: a
                // notification handed straight to startForeground is not treated as a fresh
                // interruptive post, so its full-screen intent is never acted on.
                val notifications = NotificationManagerCompat.from(this@TimerRingService)
                if (notifications.areNotificationsEnabled()) {
                    notifications.notify(NOTIFICATION_ID, notification)
                }
                startForegroundCompat(notification)
                // The timer's own sound preference is a Phase 7 setting; until then it uses the
                // system alarm sound, which is the right default for something that must be heard.
                audio = AlarmAudio(this@TimerRingService).also {
                    it.start(
                        Alarm(
                            id = 0L,
                            hour = 0,
                            minute = 0,
                            vibrationEnabled = vibrate,
                            soundUri = null,
                        )
                    )
                }
            }
        }
    }

    private fun addMinute(timerId: Long) {
        val app = application as? ClockApplication ?: run { stopEverything(); return }
        scope.launch {
            app.container.timerRepository.getTimer(timerId)?.let { timer ->
                val extended = timer
                    .copy(remainingWhenPausedMillis = 0L)
                    .addMinute(android.os.SystemClock.elapsedRealtime())
                    .start(android.os.SystemClock.elapsedRealtime())
                app.container.timerRepository.updateTimer(extended)
                extended.endsAtRealtime?.let {
                    TimerScheduling.schedule(this@TimerRingService, timer.id, it)
                }
            }
            stopEverything()
        }
    }

    private fun startForegroundCompat(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_TIMER,
            getString(R.string.channel_timer_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.channel_timer_description)
            // The service owns the sound, for the same reason as the alarm channel.
            setSound(null, null)
            enableVibration(false)
            setBypassDnd(true)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun notification(timerId: Long, label: String): Notification {
        // Points at the timer's own full-screen alert rather than the app's main screen, so tapping
        // the notification lands on Stop and +1 min.
        val open = PendingIntent.getActivity(
            this,
            timerId.toInt(),
            TimerRingingActivity.intent(this, timerId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_tab_timer)
            .setContentTitle(label.ifBlank { getString(R.string.timer_finished) })
            .setContentText(getString(R.string.timer_finished_text))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            // Deliberately NOT setSilent(true).
            //
            // That flag looked harmless — the channel already sets sound and vibration to null, so
            // the notification makes no noise either way — but it marks the notification as
            // non-interruptive, and the system only launches a full-screen intent from the
            // interruptive path. The result was an alarm whose intent was correctly attached and
            // even allowlisted by NotificationManagerService, yet never launched. Verified on a
            // Pixel 8: the record read
            // `flags=ONGOING_EVENT|NO_CLEAR|FOREGROUND_SERVICE|HIGH_PRIORITY|SILENT` alongside
            // `fullscreenIntent=PendingIntent{... allowlist: +30s .../NotificationManagerService}`.
            //
            // Silence stays the channel's job, which is where it belongs.
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .addAction(0, getString(R.string.action_stop), action(ACTION_STOP, timerId))
            .addAction(0, getString(R.string.action_add_minute), action(ACTION_ADD_MINUTE, timerId))
            .build()
    }

    private fun action(actionName: String, timerId: Long): PendingIntent {
        val intent = Intent(this, TimerRingService::class.java).apply {
            action = actionName
            data = "samsungclock://timer-service/$actionName/$timerId".toUri()
            putExtra(TimerScheduling.EXTRA_TIMER_ID, timerId)
        }
        return PendingIntent.getService(
            this,
            actionName.hashCode() + timerId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun stopEverything() {
        mainScope.launch {
            audio?.stop()
            audio = null
            ServiceCompat.stopForeground(this@TimerRingService, ServiceCompat.STOP_FOREGROUND_REMOVE)
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
        private const val TAG = "TimerRingService"
        const val CHANNEL_TIMER = "timer_finished"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START = "com.asadrao.clock.action.TIMER_RING_START"
        const val ACTION_STOP = "com.asadrao.clock.action.TIMER_RING_STOP"
        const val ACTION_ADD_MINUTE = "com.asadrao.clock.action.TIMER_ADD_MINUTE"

        fun startIntent(context: Context, timerId: Long): Intent =
            Intent(context, TimerRingService::class.java).apply {
                action = ACTION_START
                putExtra(TimerScheduling.EXTRA_TIMER_ID, timerId)
            }
    }
}
