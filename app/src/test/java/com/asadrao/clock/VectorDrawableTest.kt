package com.asadrao.clock

import android.content.Context
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Every icon in this app is hand-authored vector XML, so a malformed path or a typo in a tag
 * is a real and easy mistake — and one that only shows up at inflation time, not at compile
 * time. This inflates each of them for real.
 */
@RunWith(RobolectricTestRunner::class)
class VectorDrawableTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun inflate(resId: Int, name: String) {
        val drawable = ResourcesCompat.getDrawable(context.resources, resId, null)
        assertNotNull("$name failed to inflate", drawable)
        assertTrue(
            "$name inflated with no intrinsic size",
            drawable!!.intrinsicWidth > 0 && drawable.intrinsicHeight > 0,
        )
    }

    @Test
    fun every_navigation_icon_inflates() {
        inflate(R.drawable.ic_tab_alarm, "ic_tab_alarm")
        inflate(R.drawable.ic_tab_world_clock, "ic_tab_world_clock")
        inflate(R.drawable.ic_tab_stopwatch, "ic_tab_stopwatch")
        inflate(R.drawable.ic_tab_timer, "ic_tab_timer")
    }

    @Test
    fun the_launcher_foreground_inflates() {
        inflate(R.drawable.ic_launcher_foreground, "ic_launcher_foreground")
    }

    @Test
    fun the_action_icons_inflate() {
        inflate(R.drawable.ic_add, "ic_add")
        inflate(R.drawable.ic_more_vertical, "ic_more_vertical")
        inflate(R.drawable.ic_search, "ic_search")
        inflate(R.drawable.ic_sun, "ic_sun")
        inflate(R.drawable.ic_moon, "ic_moon")
    }

    @Test
    fun the_widget_dial_and_hands_inflate() {
        // The widget's hands are rotated by the framework about their own centre, so each one is a
        // full-size square canvas. A malformed path here would only fail when the launcher drew it.
        inflate(R.drawable.widget_clock_dial, "widget_clock_dial")
        inflate(R.drawable.widget_clock_hand_hour, "widget_clock_hand_hour")
        inflate(R.drawable.widget_clock_hand_minute, "widget_clock_hand_minute")
        inflate(R.drawable.widget_clock_hand_second, "widget_clock_hand_second")
    }

    @Test
    fun the_widget_hands_are_square_so_rotation_is_centred() {
        // AnalogClock spins each hand about the drawable's centre. A non-square hand would appear
        // to wobble as it went round.
        listOf(
            R.drawable.widget_clock_hand_hour,
            R.drawable.widget_clock_hand_minute,
            R.drawable.widget_clock_hand_second,
        ).forEach { id ->
            val drawable = androidx.core.content.res.ResourcesCompat
                .getDrawable(context.resources, id, null)!!
            assertEquals(
                "hand drawables must be square",
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
            )
        }
    }
}
