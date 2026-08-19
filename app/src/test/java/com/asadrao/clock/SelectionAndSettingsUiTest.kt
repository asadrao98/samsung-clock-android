package com.asadrao.clock

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.asadrao.clock.di.LocalAppContainer
import com.asadrao.clock.ui.ClockApp
import com.asadrao.clock.ui.theme.SamsungClockTheme
import com.asadrao.clock.ui.theme.ThemeMode
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Selection mode and the Settings route, driven through the real UI.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w412dp-h915dp-xhdpi")
class SelectionAndSettingsUiTest {

    @get:Rule
    val compose = createComposeRule()

    private var container: com.asadrao.clock.di.AppContainer? = null

    /** Closes the database; an unclosed handle surfaces as a failure in an unrelated test. */
    @After
    fun tearDown() {
        container?.close()
    }

    private fun launch() {
        val container = testAppContainer(ApplicationProvider.getApplicationContext())
            .also { this.container = it }
        compose.setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                SamsungClockTheme(themeMode = ThemeMode.Light) { ClockApp() }
            }
        }
    }

    private fun awaitText(text: String) = compose.waitUntil(5_000) {
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    private fun awaitNoText(text: String) = compose.waitUntil(5_000) {
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
    }

    private fun addAlarm(name: String) {
        compose.onNode(hasContentDescription("Add alarm")).performClick()
        awaitText("Save")
        compose.onNodeWithText("Alarm name").performTextInput(name)
        compose.onNodeWithText("Save").performClick()
        awaitNoText("Save")
    }

    @Test
    fun the_overflow_menu_offers_settings() {
        launch()
        awaitText("No alarms")
        compose.onNode(hasContentDescription("More options")).performClick()
        awaitText("Settings")
    }

    @Test
    fun edit_is_offered_only_once_there_is_something_to_edit() {
        launch()
        awaitText("No alarms")
        compose.onNode(hasContentDescription("More options")).performClick()
        awaitText("Settings")
        // Nothing to select yet, so Edit would be a dead option.
        compose.onNodeWithText("Edit").assertDoesNotExist()
    }

    @Test
    fun edit_enters_selection_mode_with_nothing_selected() {
        // Entering via the menu is not a statement about any particular alarm, so pre-selecting
        // one would be surprising — and dangerous, sitting next to a Delete button.
        launch()
        addAlarm("Gym")
        awaitText("Gym")

        compose.onNode(hasContentDescription("More options")).performClick()
        awaitText("Edit")
        compose.onNodeWithText("Edit").performClick()

        awaitText("Select alarms")
        compose.onNodeWithText("1 selected").assertDoesNotExist()
        // With nothing selected there is no action bar, so Delete must not be reachable.
        compose.onNodeWithText("Delete").assertDoesNotExist()
    }

    @Test
    fun selection_mode_replaces_the_header_actions() {
        launch()
        addAlarm("Office")
        awaitText("Office")

        compose.onNode(hasContentDescription("More options")).performClick()
        awaitText("Edit")
        compose.onNodeWithText("Edit").performClick()
        awaitText("Select alarms")

        // Add and More options step aside while selecting.
        compose.onNode(hasContentDescription("Add alarm")).assertDoesNotExist()
        compose.onNode(hasContentDescription("More options")).assertDoesNotExist()
        compose.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun cancel_leaves_selection_mode() {
        launch()
        addAlarm("Dentist")
        awaitText("Dentist")

        compose.onNode(hasContentDescription("More options")).performClick()
        awaitText("Edit")
        compose.onNodeWithText("Edit").performClick()
        awaitText("Select alarms")

        compose.onNodeWithText("Cancel").performClick()
        awaitText("Alarm")
        compose.onNode(hasContentDescription("Add alarm")).assertIsDisplayed()
    }

    @Test
    fun settings_opens_and_shows_only_real_options() {
        launch()
        awaitText("No alarms")
        compose.onNode(hasContentDescription("More options")).performClick()
        awaitText("Settings")
        compose.onNodeWithText("Settings").performClick()

        awaitText("Theme")
        compose.onNodeWithText("Default snooze").assertIsDisplayed()
        compose.onNodeWithText("Vibrate when a timer finishes").assertIsDisplayed()
        // No 12/24-hour toggle: that belongs to the system and the app follows it.
        compose.onAllNodesWithText("Time format").assertCountEquals(0)
    }

    @Test
    fun the_theme_can_be_changed_from_settings() {
        launch()
        awaitText("No alarms")
        compose.onNode(hasContentDescription("More options")).performClick()
        awaitText("Settings")
        compose.onNodeWithText("Settings").performClick()
        awaitText("System default")

        compose.onNodeWithText("Theme").performClick()
        awaitText("Dark")
        compose.onNodeWithText("Dark").performClick()

        // The summary follows the stored preference.
        awaitText("Dark")
    }
}
