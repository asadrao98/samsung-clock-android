package com.asadrao.clock.domain.timer

/**
 * Arms and cancels the platform wake-up that fires when a timer runs out.
 *
 * Abstracted so [com.asadrao.clock.ui.timer.TimerViewModel] does not have to hold a `Context`. A
 * view model outlives the screen that created it, so keeping an Android context in one is a leak
 * waiting to happen — and it makes the scheduling policy untestable off-device.
 */
interface TimerScheduler {
    fun schedule(timerId: Long, endsAtRealtime: Long)
    fun cancel(timerId: Long)
}
