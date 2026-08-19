package com.asadrao.clock.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.core.app.NotificationManagerCompat
import com.asadrao.clock.R
import com.asadrao.clock.domain.model.Alarm

/**
 * Notification channels and the ringing notification.
 *
 * The alarm channel is deliberately **silent and non-vibrating**. That looks wrong at first
 * glance, but the ringing service plays the alarm itself through `AudioAttributes` with
 * `USAGE_ALARM` and drives the vibrator directly. Letting the channel also make a sound would
 * either double it up or, worse, hand control of the sound to a channel setting the user can
 * change — and an alarm clock whose sound can be silenced by a notification setting is not an
 * alarm clock.
 *
 * The full-screen intent is what puts the ringing screen in front of the user over the lockscreen.
 * If that permission is revoked the system degrades it to a heads-up notification, which is why
 * this notification carries working Snooze and Dismiss actions rather than relying on the
 * activity appearing.
 */
object ClockNotifications {

    const val CHANNEL_ALARM = "alarm_ringing"
    const val CHANNEL_UPCOMING = "alarm_upcoming"

    const val NOTIFICATION_RINGING = 1001

    // A second notification id was tried here, on the theory that a foreground-service notification
    // is not treated as a fresh interruptive post and so its full-screen intent is ignored. It made
    // no difference to the launch and produced a visible duplicate notification, so it is gone.

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)

        val ringing = NotificationChannel(
            CHANNEL_ALARM,
            context.getString(R.string.channel_alarm_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_alarm_description)
            // See the class comment: the service owns the sound and the vibration.
            setSound(null, null)
            enableVibration(false)
            setBypassDnd(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setShowBadge(false)
        }

        val upcoming = NotificationChannel(
            CHANNEL_UPCOMING,
            context.getString(R.string.channel_upcoming_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.channel_upcoming_description)
            setShowBadge(false)
        }

        manager.createNotificationChannels(listOf(ringing, upcoming))
    }

    /**
     * The ringing notification, which doubles as the foreground-service notification and as the
     * fallback UI if the full-screen intent is not honoured.
     */
    fun ringing(
        context: Context,
        alarm: Alarm,
        timeText: String,
        snoozeAvailable: Boolean,
    ): android.app.Notification {
        val fullScreen = PendingIntent.getActivity(
            context,
            AlarmIntents.fireRequestCode(alarm.id),
            AlarmRingingActivity.intent(context, alarm.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_tab_alarm)
            .setContentTitle(alarm.label.ifBlank { context.getString(R.string.alarm_default_title) })
            .setContentText(timeText)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Not dismissible by swipe: an alarm the user can flick away without deciding is a
            // missed alarm.
            .setOngoing(true)
            .setAutoCancel(false)
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
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .addAction(
                0,
                context.getString(R.string.action_dismiss),
                servicePendingIntent(context, alarm.id, AlarmRingService.ACTION_DISMISS),
            )

        if (snoozeAvailable) {
            builder.addAction(
                0,
                context.getString(R.string.action_snooze),
                servicePendingIntent(context, alarm.id, AlarmRingService.ACTION_SNOOZE),
            )
        }
        return builder.build()
    }

    private fun servicePendingIntent(
        context: Context,
        alarmId: Long,
        action: String,
    ): PendingIntent {
        val intent = Intent(context, AlarmRingService::class.java).apply {
            this.action = action
            // Distinct data per action, since PendingIntent equality ignores extras.
            data = "samsungclock://service/$action/$alarmId".toUri()
            putExtra(AlarmIntents.EXTRA_ALARM_ID, alarmId)
        }
        return PendingIntent.getService(
            context,
            action.hashCode() + alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun cancelRinging(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_RINGING)
    }
}
