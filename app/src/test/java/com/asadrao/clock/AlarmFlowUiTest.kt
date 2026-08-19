package com.asadrao.clock

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
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
 * The whole Phase 2 loop, driven through the UI: add an alarm, see it listed, toggle it, edit it,
 * delete it. Nothing is stubbed — these run against a real Room database and the real view models.
 *
 * Because the database is real, every write lands on Room's background executor and the UI updates
 * a moment later. So each step waits for the result rather than asserting immediately;
 * `waitForIdle` only settles composition, not other coroutines. Tests that skipped the wait passed
 * or failed depending on timing, which is worse than either.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w412dp-h915dp-xhdpi")
class AlarmFlowUiTest {

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

    private fun tapAdd() = compose.onNode(hasContentDescription("Add alarm")).performClick()

    private fun awaitText(text: String) = compose.waitUntil(TIMEOUT) {
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    private fun awaitNoText(text: String) = compose.waitUntil(TIMEOUT) {
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
    }

    /**
     * The alarm's own toggle. Matched on the Switch role, because the day chips are also
     * toggleable — they are checkboxes — and a looser matcher picks up all seven of them.
     */
    private fun alarmSwitch() = compose.onNode(
        SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)
    )

    /** Adds an alarm, optionally named, and returns once it is on the list. */
    private fun addAlarm(name: String? = null) {
        tapAdd()
        awaitText("Save")
        if (name != null) compose.onNodeWithText("Alarm name").performTextInput(name)
        compose.onNodeWithText("Save").performClick()
        awaitNoText("Save")
    }

    @Test
    fun add_then_save_puts_an_alarm_in_the_list() {
        launch()
        awaitText("No alarms")

        addAlarm()

        awaitNoText("No alarms")
        alarmSwitch().assertExists()
    }

    @Test
    fun cancel_discards_the_new_alarm() {
        launch()
        tapAdd()
        awaitText("Cancel")
        compose.onNodeWithText("Cancel").performClick()
        // Nothing was written, so the empty state is still what the user sees.
        awaitText("No alarms")
    }

    @Test
    fun a_named_alarm_shows_its_name_in_the_list() {
        launch()
        addAlarm("Gym")
        awaitText("Gym")
    }

    @Test
    fun the_list_toggle_turns_an_alarm_off_and_on() {
        launch()
        addAlarm()

        // Saving always arms the alarm, so it lands enabled.
        alarmSwitch().assertIsOn()
        alarmSwitch().performClick()
        compose.waitUntil(TIMEOUT) {
            compose.onAllNodes(
                SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, androidx.compose.ui.state.ToggleableState.Off)
                    and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)
            ).fetchSemanticsNodes().isNotEmpty()
        }
        alarmSwitch().assertIsOff()

        alarmSwitch().performClick()
        compose.waitUntil(TIMEOUT) {
            compose.onAllNodes(
                SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, androidx.compose.ui.state.ToggleableState.On)
                    and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)
            ).fetchSemanticsNodes().isNotEmpty()
        }
        alarmSwitch().assertIsOn()
    }

    @Test
    fun tapping_a_row_opens_the_editor_with_a_delete_option() {
        launch()
        addAlarm("Office")
        awaitText("Office")

        compose.onNodeWithText("Office").performClick()
        // Delete is offered only for an existing alarm.
        awaitText("Delete")
        compose.onNodeWithText("Delete").assertIsDisplayed()
    }

    @Test
    fun a_new_alarm_offers_no_delete() {
        launch()
        tapAdd()
        awaitText("Save")
        compose.onNodeWithText("Delete").assertDoesNotExist()
    }

    @Test
    fun deleting_from_the_editor_removes_the_alarm() {
        launch()
        addAlarm("Dentist")
        awaitText("Dentist")

        compose.onNodeWithText("Dentist").performClick()
        awaitText("Delete")
        compose.onNodeWithText("Delete").performClick()

        awaitText("No alarms")
        compose.onNodeWithText("Dentist").assertDoesNotExist()
    }

    @Test
    fun the_editor_exposes_the_alarm_controls_in_one_ui_order() {
        launch()
        tapAdd()
        awaitText("Save")
        compose.onNodeWithText("Alarm name").assertIsDisplayed()
        compose.onNodeWithText("Alarm sound").assertIsDisplayed()
        compose.onNodeWithText("Vibration").assertIsDisplayed()
        compose.onNodeWithText("Snooze").assertIsDisplayed()
        compose.onNodeWithText("Ring once").assertIsDisplayed()
    }

    @Test
    fun the_editor_shows_three_picker_columns_in_twelve_hour_mode() {
        // A three-column drum — hour, minute, meridiem — is what makes it read as One UI rather
        // than as Material's clock face.
        launch()
        tapAdd()
        awaitText("Save")
        compose.onNode(hasContentDescription("Hour")).assertExists()
        compose.onNode(hasContentDescription("Minute")).assertExists()
        compose.onNode(hasContentDescription("AM or PM")).assertExists()
    }

    @Test
    fun choosing_repeat_days_replaces_the_ring_once_summary() {
        launch()
        tapAdd()
        awaitText("Ring once")

        // Chips announce the full day name; a single letter means nothing to a screen reader.
        compose.onNode(
            hasContentDescription("Mon") and
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
        ).performClick()

        awaitNoText("Ring once")
    }

    @Test
    fun selecting_every_day_collapses_the_summary_to_every_day() {
        launch()
        tapAdd()
        awaitText("Ring once")
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
            compose.onNode(
                hasContentDescription(day) and
                    SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
            ).performClick()
        }
        awaitText("Every day")
    }

    @Test
    fun two_alarms_both_appear_with_their_own_toggles() {
        launch()
        addAlarm("First")
        awaitText("First")
        addAlarm("Second")
        awaitText("Second")

        compose.onNodeWithText("First").assertExists()
        compose.onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)
        ).assertCountEquals(2)
    }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
