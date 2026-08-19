package com.asadrao.clock.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.asadrao.clock.MainActivity
import com.asadrao.clock.R

/**
 * A live analog clock for the home screen.
 *
 * Deliberately almost empty. The layout's `AnalogClock` is driven by the framework, so the hands
 * move without this provider being woken at all — `updatePeriodMillis` is 0 and there is no alarm or
 * job behind it. All this does is attach a tap target, once, when the widget is placed or resized.
 *
 * This is also the honest answer to wanting a live app icon: a launcher icon is a static drawable
 * and no API lets an app animate one or feed it the time. Samsung's clock icon showing live time is
 * a feature of Samsung's *launcher*, applied to its own built-in clock, and Pixel Launcher does the
 * same for Google Clock. A widget is the equivalent a third-party app can actually offer, and it is
 * larger and more useful besides.
 */
class AnalogClockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_analog_clock).apply {
                setOnClickPendingIntent(R.id.widget_root, openApp)
            }
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
