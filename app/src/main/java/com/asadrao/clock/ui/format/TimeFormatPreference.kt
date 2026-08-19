package com.asadrao.clock.ui.format

import android.content.Context
import android.database.ContentObserver
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Whether the device is set to 24-hour time, kept live.
 *
 * Reading `DateFormat.is24HourFormat` once is not enough: changing the setting does not
 * necessarily trigger a configuration change, so a screen that read it at composition would keep
 * showing the old format until it was recreated. This watches the setting itself, so the alarm
 * list and the time picker follow the system immediately.
 */
@Composable
fun rememberIs24HourFormat(): Boolean {
    val context = LocalContext.current
    var is24Hour by remember { mutableStateOf(DateFormat.is24HourFormat(context)) }

    DisposableEffect(context) {
        val uri = Settings.System.getUriFor(Settings.System.TIME_12_24)
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                is24Hour = DateFormat.is24HourFormat(context)
            }
        }
        context.contentResolver.registerContentObserver(uri, false, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    return is24Hour
}

/** The system's current locale, for day names and AM/PM strings. */
@Composable
fun rememberLocale(): java.util.Locale {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    return remember(configuration) {
        androidx.core.os.ConfigurationCompat.getLocales(configuration)[0]
            ?: java.util.Locale.getDefault()
    }
}
