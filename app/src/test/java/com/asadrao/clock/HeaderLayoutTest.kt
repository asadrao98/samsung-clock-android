package com.asadrao.clock

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import com.asadrao.clock.ui.components.OneUiCollapsingHeaderLayout
import com.asadrao.clock.ui.theme.SamsungClockTheme
import com.asadrao.clock.ui.theme.ThemeMode
import org.junit.Assert.assertTrue
import androidx.compose.ui.platform.testTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The header must hand its content exactly the space left below it — no more.
 *
 * This is a regression test for a layout bug that shipped to a device: the content box used
 * `fillMaxSize()`, which inside a `Column` resolves against the incoming max height rather than the
 * remaining height. The content therefore became a full screen tall while starting below a ~360dp
 * header, so its bottom 360dp fell off the screen and sat under the floating navigation pill. Every
 * tab was affected, and no existing test noticed because nothing asserted where the bottom of the
 * content landed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w412dp-h915dp-xhdpi")
class HeaderLayoutTest {

    @get:Rule
    val compose = createComposeRule()

    private fun show(collapseOnContentScroll: Boolean) {
        compose.setContent {
            SamsungClockTheme(themeMode = ThemeMode.Light) {
                OneUiCollapsingHeaderLayout(
                    title = "Timer",
                    collapseOnContentScroll = collapseOnContentScroll,
                ) {
                    Box(Modifier.fillMaxSize().testTag(CONTENT)) {
                        Text("TOP", modifier = Modifier.align(Alignment.TopCenter))
                        Text("BOTTOM", modifier = Modifier.align(Alignment.BottomCenter))
                    }
                }
            }
        }
    }

    @Test
    fun the_bottom_of_the_content_stays_on_screen_with_a_fixed_header() {
        show(collapseOnContentScroll = false)
        // Would fail with fillMaxSize(): the bottom-aligned element sat below the window.
        compose.onNodeWithText("BOTTOM").assertIsDisplayed()
    }

    @Test
    fun the_bottom_of_the_content_stays_on_screen_with_a_collapsing_header() {
        show(collapseOnContentScroll = true)
        compose.onNodeWithText("BOTTOM").assertIsDisplayed()
    }

    @Test
    fun the_content_slot_is_shorter_than_the_window_because_the_header_takes_space() {
        // The invariant that actually catches the bug. `fillMaxSize()` handed the content the full
        // window height even though the header already consumed several hundred dp above it, so the
        // slot was exactly as tall as the screen and its bottom fell off. Measuring the slot is
        // reliable; asserting on a bottom-aligned child is not, because unclipped bounds happily
        // report positions outside the window.
        show(collapseOnContentScroll = false)
        val window = compose.onRoot().getUnclippedBoundsInRoot()
        val windowHeight = window.bottom - window.top
        val slot = compose.onNodeWithTag(CONTENT).getUnclippedBoundsInRoot()
        val contentHeight = slot.bottom - slot.top
        assertTrue(
            "content slot ${contentHeight} must be shorter than the window ${windowHeight}",
            contentHeight < windowHeight,
        )
        assertTrue("content slot should still have real height", contentHeight.value > 100f)
    }

    @Test
    fun a_collapsing_header_leaves_the_same_invariant_intact() {
        show(collapseOnContentScroll = true)
        val window = compose.onRoot().getUnclippedBoundsInRoot()
        val windowHeight = window.bottom - window.top
        val slot = compose.onNodeWithTag(CONTENT).getUnclippedBoundsInRoot()
        val contentHeight = slot.bottom - slot.top
        assertTrue(contentHeight < windowHeight)
    }

    @Test
    fun a_collapsing_header_opens_as_a_tall_hero() {
        // Samsung's signature: roughly 40% of the screen, so content begins well down the page.
        show(collapseOnContentScroll = true)
        val topOfContent = compose.onNodeWithText("TOP").getUnclippedBoundsInRoot().top
        assertTrue(
            "hero header should push content well down, content began at $topOfContent",
            topOfContent.value > 200f,
        )
    }

    @Test
    fun a_fixed_header_is_compact_so_the_body_keeps_its_room() {
        // The Timer and Stopwatch opt out of collapsing, and must get the small title rather than
        // the hero. With the hero the dial fitted but its controls landed under the floating pill,
        // reachable only by scrolling a screen that is not meant to scroll.
        show(collapseOnContentScroll = false)
        val topOfContent = compose.onNodeWithText("TOP").getUnclippedBoundsInRoot().top
        assertTrue(
            "fixed header should be compact, content began at $topOfContent",
            topOfContent.value < 120f,
        )

        // And the body should therefore get most of the screen.
        val window = compose.onRoot().getUnclippedBoundsInRoot()
        val slot = compose.onNodeWithTag(CONTENT).getUnclippedBoundsInRoot()
        val ratio = (slot.bottom - slot.top).value / (window.bottom - window.top).value
        assertTrue("body should get most of the screen, got $ratio", ratio > 0.8f)
    }

    private companion object {
        const val CONTENT = "header-content-slot"
    }
}
