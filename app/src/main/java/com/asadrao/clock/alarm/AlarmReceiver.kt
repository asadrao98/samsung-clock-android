package com.asadrao.clock.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.asadrao.clock.ClockApplication
import com.asadrao.clock.domain.model.Alarm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives an alarm's scheduled wake-up.
 *
 * Two things happen, in this order:
 *
 * 1. **The ringing service is started immediately**, synchronously, before anything touches the
 *    database. A broadcast receiver has only a few seconds of guaranteed life, and starting the
 *    foreground service is the one step that absolutely must not be lost.
 * 2. The scheduling bookkeeping follows asynchronously — advancing a repeating alarm to its next
 *    occurrence, switching a one-shot off, and clearing the snooze budget for a fresh ring.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = AlarmIntents.alarmIdFrom(intent)
        if (alarmId == Alarm.NO_ID) {
            Log.w(TAG, "received ${intent.action} with no alarm id; ignoring")
            return
        }

        val app = context.applicationContext as? ClockApplication ?: return
        val container = app.container
        val coordinator = container.alarmSchedulingCoordinator

        // Bring up the ringing screen directly, before anything else.
        //
        // Not via the notification's full-screen intent: the system declines that for an app it
        // has not decided is an alarm app, and on a sideloaded build USE_FULL_SCREEN_INTENT is
        // denied by default — verified on a Pixel 8, where the appop logged a rejectTime at the
        // exact moment the alarm fired and no activity ever started.
        //
        // Launching straight from here works because an alarm scheduled with setAlarmClock arrives
        // with a background-activity-launch grant attached (visible in `dumpsys alarm` as
        // backgroundActivityAllowed). Getting the activity up first also makes the app visibly
        // foreground, which matters for the audio: Android's background-playback hardening was
        // observed muting USAGE_ALARM audio started while nothing of ours was on screen.
        //
        // The full-screen intent stays on the notification as a second route, and the
        // notification's own Snooze/Dismiss actions remain the fallback if both are refused.
        if (intent.action == AlarmIntents.ACTION_FIRE ||
            intent.action == AlarmIntents.ACTION_SNOOZE_FIRE
        ) {
            runCatching { context.startActivity(AlarmRingingActivity.intent(context, alarmId)) }
                .onFailure { Log.w(TAG, "could not start the ringing screen directly", it) }
        }

        // Then the sound. Everything else can be a moment late; this cannot.
        context.startForegroundService(AlarmRingService.startIntent(context, alarmId))

        // The work touches the database, so it has to outlive onReceive. goAsync keeps the
        // process alive for it; the receiver still has to finish well inside ~10 seconds.
        // goAsync() yields null when onReceive is invoked outside a real broadcast dispatch.
        // Calling finish() on that would throw on a background thread — in the alarm path, where a
        // crash is most costly — so the handle is treated as optional throughout.
        val pending: android.content.BroadcastReceiver.PendingResult? = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                when (intent.action) {
                    AlarmIntents.ACTION_FIRE -> {
                        // A scheduled ring is a fresh session, so the snooze budget resets.
                        container.snoozeTracker.reset(alarmId)
                        coordinator.onAlarmFired(alarmId)
                    }
                    // A snooze re-ring keeps the existing budget and has nothing to re-arm: the
                    // next scheduled occurrence was already set when the alarm first fired.
                    AlarmIntents.ACTION_SNOOZE_FIRE -> Unit
                    else -> Log.w(TAG, "unexpected action ${intent.action}")
                }
            } catch (t: Throwable) {
                // Never let a failure here take the process down: the alarm has already fired,
                // and a crash would lose the rest of the schedule too.
                Log.e(TAG, "failed handling ${intent.action} for alarm $alarmId", t)
            } finally {
                pending?.finish()
            }
        }
    }

    private companion object {
        const val TAG = "AlarmReceiver"
    }
}
