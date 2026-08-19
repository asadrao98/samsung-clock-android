package com.asadrao.clock

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToString
import com.asadrao.clock.ui.components.OneUiFilledButton
import com.asadrao.clock.ui.components.OneUiListRow
import com.asadrao.clock.ui.components.OneUiSwitch
import com.asadrao.clock.ui.theme.SamsungClockTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Behaviour and accessibility of the hand-built components. Their *appearance* cannot be checked
 * here — Robolectric never rasterises anything — so these cover the parts that a custom control
 * most often gets wrong: toggle semantics, disabled state, and having an accessible name.
 */
@RunWith(RobolectricTestRunner::class)
class OneUiComponentsTest {

    @get:Rule
    val compose = createComposeRule()

    // ---- switch ---------------------------------------------------------------------------

    @Test
    fun the_switch_reports_its_on_off_state_to_accessibility() {
        // A hand-rolled toggle drawn with two Boxes would look right and be completely
        // invisible to TalkBack. Role.Switch plus a toggleable value is what makes it a
        // control rather than decoration.
        compose.setContent {
            SamsungClockTheme {
                Column {
                    OneUiSwitch(checked = true, onCheckedChange = {})
                    OneUiSwitch(checked = false, onCheckedChange = {})
                }
            }
        }
        val switches = compose.onAllNodes(isToggleable())
        switches.assertCountEquals(2)
        switches[0].assertIsOn()
        switches[1].assertIsOff()
    }

    @Test
    fun tapping_the_switch_toggles_it() {
        // State is hoisted outside setContent deliberately: declaring it inside would rebuild
        // it on every recomposition, and the toggle would appear never to change.
        val checked = mutableStateOf(false)
        compose.setContent {
            SamsungClockTheme {
                OneUiListRow(
                    title = "Vibration",
                    trailing = {
                        OneUiSwitch(
                            checked = checked.value,
                            onCheckedChange = { checked.value = it },
                        )
                    },
                )
            }
        }

        val switch = compose.onNode(isToggleable())
        switch.assertIsOff()
        switch.performClick()
        switch.assertIsOn()
        assertTrue(checked.value)
        switch.performClick()
        switch.assertIsOff()
        assertFalse(checked.value)
    }

    @Test
    fun a_disabled_switch_is_reported_disabled_and_ignores_taps() {
        var changes = 0
        compose.setContent {
            SamsungClockTheme {
                OneUiSwitch(checked = false, onCheckedChange = { changes++ }, enabled = false)
            }
        }

        val switch = compose.onNode(isToggleable())
        switch.assertIsNotEnabled()
        switch.performClick()
        assertEquals("a disabled switch must not fire its callback", 0, changes)
        switch.assertIsOff()
    }

    @Test
    fun a_switch_with_no_callback_is_not_interactive() {
        // Used for read-only rows. It must still report its state, but must not invite a tap.
        compose.setContent {
            SamsungClockTheme {
                OneUiSwitch(checked = true, onCheckedChange = null)
            }
        }
        compose.onAllNodes(isToggleable()).assertCountEquals(0)
    }

    // ---- list row -------------------------------------------------------------------------

    @Test
    fun a_row_with_a_summary_shows_both_lines() {
        compose.setContent {
            SamsungClockTheme {
                OneUiListRow(title = "Snooze", summary = "5 minutes, 3 times")
            }
        }
        compose.onNodeWithText("Snooze").assertIsDisplayed()
        compose.onNodeWithText("5 minutes, 3 times").assertIsDisplayed()
    }

    @Test
    fun a_row_is_only_clickable_when_it_has_an_on_click() {
        compose.setContent {
            SamsungClockTheme {
                Column {
                    OneUiListRow(title = "Tappable", onClick = {})
                    OneUiListRow(title = "Inert")
                }
            }
        }
        compose.onNodeWithText("Tappable").assertHasClickAction()
        // "Inert" carries no click action, so a user is not invited to press something dead.
        val tree = compose.onRoot().printToString(maxDepth = 100)
        assertTrue(tree.contains("Inert"))
    }

    @Test
    fun a_disabled_row_does_not_invoke_its_on_click() {
        var clicks = 0
        compose.setContent {
            SamsungClockTheme {
                OneUiListRow(title = "Alarm sound", enabled = false, onClick = { clicks++ })
            }
        }
        compose.onNodeWithText("Alarm sound").performClick()
        assertEquals(0, clicks)
    }

    // ---- buttons --------------------------------------------------------------------------

    @Test
    fun a_filled_button_clicks() {
        var clicked = false
        compose.setContent {
            SamsungClockTheme {
                OneUiFilledButton(text = "Save", onClick = { clicked = true })
            }
        }
        compose.onNodeWithText("Save").assertHasClickAction().performClick()
        assertTrue(clicked)
    }

    @Test
    fun a_disabled_button_is_reported_disabled_and_does_not_click() {
        var clicked = false
        compose.setContent {
            SamsungClockTheme {
                OneUiFilledButton(text = "Save", onClick = { clicked = true }, enabled = false)
            }
        }
        compose.onNodeWithText("Save").assertIsNotEnabled().performClick()
        assertFalse(clicked)
    }

    @Test
    fun an_enabled_button_is_reported_enabled() {
        compose.setContent {
            SamsungClockTheme {
                OneUiFilledButton(text = "Start", onClick = {})
            }
        }
        compose.onNodeWithText("Start").assertIsEnabled()
    }
}
