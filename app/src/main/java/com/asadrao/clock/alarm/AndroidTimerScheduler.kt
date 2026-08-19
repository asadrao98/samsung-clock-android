package com.asadrao.clock.alarm

import android.content.Context
import com.asadrao.clock.domain.timer.TimerScheduler

/** [TimerScheduler] backed by `AlarmManager`, via [TimerScheduling]. */
class AndroidTimerScheduler(private val context: Context) : TimerScheduler {
    override fun schedule(timerId: Long, endsAtRealtime: Long) =
        TimerScheduling.schedule(context, timerId, endsAtRealtime)

    override fun cancel(timerId: Long) = TimerScheduling.cancel(context, timerId)
}
