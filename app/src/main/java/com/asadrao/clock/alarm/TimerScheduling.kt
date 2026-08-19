package com.asadrao.clock.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import com.asadrao.clock.ClockApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Wakes the device when a timer runs out.
 *
 * Uses `ELAPSED_REALTIME_WAKEUP`, which is an exact match for how a timer is modelled: the stored
 * deadline is already a monotonic reading, so there is no conversion to get wrong, and the timer is
 * immune to the wall clock being changed underneath it.
 *
 * `setExactAndAllowWhileIdle` rather than `setAlarmClock`, because a timer is not an alarm clock —
 * it should not light up the system's upcoming-alarm indicator or appear in Settings as the next
 * alarm. It is still exact and still fires in Doze.
 */
object TimerScheduling {

    const val ACTION_TIMER_FINISHED = "com.asadrao.clock.action.TIMER_FINISHED"
    const val EXTRA_TIMER_ID = "com.asadrao.clock.extra.TIMER_ID"

    private const val SCHEME = "samsungclock"

    fun schedule(context: Context, timerId: Long, endsAtRealtime: Long) {
        val manager = context.getSystemService(AlarmManager::class.java)
        val operation = pendingIntent(context, timerId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
            Log.w(TAG, "exact alarms not permitted; timer $timerId may finish late")
            manager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                endsAtRealtime,
                operation,
            )
            return
        }
        manager.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            endsAtRealtime,
            operation,
        )
    }

    fun cancel(context: Context, timerId: Long) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context, timerId))
    }

    private fun pendingIntent(context: Context, timerId: Long): PendingIntent {
        val intent = Intent(context, TimerReceiver::class.java).apply {
            action = ACTION_TIMER_FINISHED
            // In the data URI, not just an extra: PendingIntent equality ignores extras, so two
            // timers would otherwise share one pending intent and only the last would survive.
            data = "$SCHEME://timer/$timerId".toUri()
            putExtra(EXTRA_TIMER_ID, timerId)
        }
        return PendingIntent.getBroadcast(
            context,
            TIMER_REQUEST_BASE + timerId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private const val TAG = "TimerScheduling"
    private const val TIMER_REQUEST_BASE = 2_000_000
}

/** Receives a timer's deadline and starts it ringing. */
class TimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val timerId = intent.getLongExtra(TimerScheduling.EXTRA_TIMER_ID, -1L)
        if (timerId < 0L) return

        // The full-screen alert is raised by the notification's full-screen intent, posted by the
        // service — a receiver may not start an activity itself. Sound first, synchronously.
        context.startForegroundService(TimerRingService.startIntent(context, timerId))

        val app = context.applicationContext as? ClockApplication ?: return
        // goAsync() yields null when onReceive is invoked outside a real broadcast dispatch.
        // Calling finish() on that would throw on a background thread — in the alarm path, where a
        // crash is most costly — so the handle is treated as optional throughout.
        val pending: android.content.BroadcastReceiver.PendingResult? = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                // Park the timer at zero so the UI shows it finished rather than counting into
                // the negative or resurrecting a stale deadline.
                app.container.timerRepository.getTimer(timerId)?.let { timer ->
                    app.container.timerRepository.updateTimer(
                        timer.copy(endsAtRealtime = null, remainingWhenPausedMillis = 0L)
                    )
                }
            } catch (t: Throwable) {
                Log.e("TimerReceiver", "failed to settle timer $timerId", t)
            } finally {
                pending?.finish()
            }
        }
    }
}
