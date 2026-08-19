package com.asadrao.clock

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.asadrao.clock.di.LocalAppContainer
import com.asadrao.clock.ui.ClockApp
import com.asadrao.clock.ui.theme.SamsungClockTheme
import com.asadrao.clock.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exercises the shell for real: composes it, reads the tree, taps things.
 *
 * Runs on the JVM under Robolectric because this machine has no device or emulator. Layout is
 * measured but never rasterised, so these prove the shell composes, wires its clicks and swaps its
 * content — not that it *looks* like One UI. Visual fidelity stays unverified until Phase 8.
 *
 * The qualifier pins a tall screen on purpose: the expandable header is disabled below 580dp, and
 * Robolectric's default device is shorter than that, which would silently test the collapsed-only
 * path instead.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w412dp-h915dp-xhdpi")
class ClockShellUiTest {

    @get:Rule
    val compose = createComposeRule()

    private var container: com.asadrao.clock.di.AppContainer? = null

    /** A tab in the pill. Icon-only in 8.5, so its accessible name is a content description. */
    private fun tabNamed(label: String) =
        hasContentDescription(label) and
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    /**
     * Waits for text to appear. The alarm list is read from a real database, so its empty state
     * arrives a beat after composition — asserting immediately makes these tests timing-dependent.
     */
    private fun awaitText(text: String) = compose.waitUntil(5_000) {
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    /** Closes the database; an unclosed handle surfaces as a failure in an unrelated test. */
    @After
    fun tearDown() {
        container?.close()
    }

    private fun launchShell(themeMode: ThemeMode = ThemeMode.Light) {
        val container = testAppContainer(ApplicationProvider.getApplicationContext())
            .also { this.container = it }
        compose.setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                SamsungClockTheme(themeMode = themeMode) { ClockApp() }
            }
        }
    }

    @Test
    fun all_four_tabs_are_present() {
        launchShell()
        listOf("Alarm", "World clock", "Stopwatch", "Timer").forEach { label ->
            compose.onNode(tabNamed(label)).assertExists("tab '$label' is missing")
        }
    }

    @Test
    fun the_tabs_carry_no_visible_labels() {
        // One UI 8.5's pill is icon-only. A regression that re-added labels would still pass the
        // test above, since that matches on content description.
        launchShell()
        compose.onAllNodes(hasText("World clock")).assertCountEquals(0)
        compose.onAllNodes(hasText("Stopwatch")).assertCountEquals(0)
    }

    @Test
    fun alarm_is_the_landing_tab_and_shows_its_empty_state() {
        // A fresh database has no alarms, so the empty state is what should greet the user.
        launchShell()
        awaitText("No alarms")
        compose.onNodeWithText("Tap + to add an alarm").assertIsDisplayed()
    }

    @Test
    fun the_alarm_tab_offers_add_and_more_options() {
        launchShell()
        compose.onNode(hasContentDescription("Add alarm")).assertExists()
        compose.onNode(hasContentDescription("More options")).assertExists()
    }

    @Test
    fun tapping_a_tab_swaps_the_content() {
        launchShell()
        compose.onNode(tabNamed("Timer")).performClick()
        // With no timers running the Timer tab shows its duration drums.
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasContentDescription("Hours")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("No alarms").assertDoesNotExist()

        compose.onNode(tabNamed("World clock")).performClick()
        awaitText("No cities")
    }

    @Test
    fun returning_to_a_tab_restores_its_screen() {
        launchShell()
        compose.onNode(tabNamed("Stopwatch")).performClick()
        awaitText("Start")
        compose.onNode(tabNamed("Alarm")).performClick()
        awaitText("No alarms")
    }

    @Test
    fun the_shell_composes_in_dark_theme_too() {
        // Catches a palette role defined for only one theme, which would otherwise surface as a
        // crash the first time a user switched.
        launchShell(themeMode = ThemeMode.Dark)
        awaitText("No alarms")
    }

    @Test
    fun the_screen_title_is_exposed_to_accessibility_exactly_once() {
        // The header draws the title twice — large, and toolbar-sized — and cross-fades them.
        // Both copies sit in the tree regardless of opacity, so without hiding one TalkBack
        // announces the screen title twice over.
        launchShell()
        val headings = compose.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading) and hasText("Alarm")
        ).fetchSemanticsNodes()
        assertEquals(1, headings.size)
    }

    @Test
    fun the_header_title_names_the_current_tab() {
        launchShell()
        compose.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading) and hasText("Alarm")
        ).assertExists()

        compose.onNode(tabNamed("Timer")).performClick()
        compose.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading) and hasText("Timer")
        ).assertExists()
    }
}
