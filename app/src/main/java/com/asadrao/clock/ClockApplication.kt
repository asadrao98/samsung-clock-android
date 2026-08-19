package com.asadrao.clock

import android.app.Application
import com.asadrao.clock.alarm.ClockNotifications
import com.asadrao.clock.di.AppContainer

class ClockApplication : Application() {

    /** The app's single object graph. Built lazily, so nothing touches disk on startup. */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
        // Channels have to exist before the first notification is posted, and an alarm can fire
        // in a process started by AlarmManager rather than by the user opening the app — so this
        // belongs here rather than in an activity.
        ClockNotifications.ensureChannels(this)
    }
}
