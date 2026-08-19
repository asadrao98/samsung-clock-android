package com.asadrao.clock.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.asadrao.clock.BuildConfig
import com.asadrao.clock.data.prefs.ClockSettings
import com.asadrao.clock.ui.alarm.SnoozeSheet
import com.asadrao.clock.ui.components.OneUiBottomSheet
import com.asadrao.clock.ui.components.OneUiCard
import com.asadrao.clock.ui.components.OneUiListRow
import com.asadrao.clock.ui.components.OneUiRowDivider
import com.asadrao.clock.ui.components.OneUiSectionHeader
import com.asadrao.clock.ui.components.OneUiSwitch
import com.asadrao.clock.ui.theme.ClockTheme
import com.asadrao.clock.ui.theme.ThemeMode

/**
 * Settings.
 *
 * Short on purpose. Every row here changes something the user can see or hear; nothing is present
 * merely to fill the screen. In particular there is no 12/24-hour switch — that setting belongs to
 * the system, and the app follows it live.
 *
 * Notification and exact-alarm behaviour are the platform's to grant, so those rows hand off to the
 * system settings pages rather than pretending to own them.
 */
@Composable
fun SettingsScreen(
    settings: ClockSettings,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDefaultSnoozeChange: (Int, Int) -> Unit,
    onTimerVibrationChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ClockTheme.colors
    val dimens = ClockTheme.dimens
    val context = LocalContext.current
    var themeSheetOpen by remember { mutableStateOf(false) }
    var snoozeSheetOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(WindowInsets.statusBars.asPaddingValues()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimens.headerCollapsedHeight)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close",
                    tint = colors.textPrimary,
                )
            }
            Text(
                text = "Settings",
                style = ClockTheme.typography.screenTitleSmall,
                color = colors.textPrimary,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.navPillMargin),
        ) {
            OneUiSectionHeader("Appearance")
            OneUiCard {
                OneUiListRow(
                    title = "Theme",
                    summary = settings.themeMode.label(),
                    onClick = { themeSheetOpen = true },
                )
            }

            OneUiSectionHeader("Alarms")
            OneUiCard {
                OneUiListRow(
                    title = "Default snooze",
                    summary = snoozeSummary(settings),
                    onClick = { snoozeSheetOpen = true },
                )
                OneUiRowDivider()
                OneUiListRow(
                    title = "Notification settings",
                    summary = "Sound, importance and lock screen behaviour",
                    onClick = {
                        // Handed to the system: these are the platform's settings, not ours.
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                        )
                    },
                )
            }

            OneUiSectionHeader("Timer")
            OneUiCard {
                OneUiListRow(
                    title = "Vibrate when a timer finishes",
                    trailing = {
                        OneUiSwitch(
                            checked = settings.timerVibration,
                            onCheckedChange = onTimerVibrationChange,
                        )
                    },
                )
            }

            OneUiSectionHeader("About")
            OneUiCard {
                OneUiListRow(
                    title = "Clock",
                    summary = "Version ${BuildConfig.VERSION_NAME}",
                )
                OneUiRowDivider()
                OneUiListRow(
                    title = "Works entirely offline",
                    summary = "No account, no cloud, no analytics, and no network permission",
                )
            }

            Spacer(
                Modifier.height(
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 32.dp
                )
            )
        }
    }

    if (themeSheetOpen) {
        OneUiBottomSheet(onDismissRequest = { themeSheetOpen = false }) {
            OneUiSectionHeader("Theme")
            ThemeMode.entries.forEach { mode ->
                OneUiListRow(
                    title = mode.label(),
                    onClick = {
                        onThemeModeChange(mode)
                        themeSheetOpen = false
                    },
                    trailing = if (mode == settings.themeMode) {
                        { Text("✓", color = ClockTheme.colors.accent) }
                    } else {
                        null
                    },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (snoozeSheetOpen) {
        SnoozeSheet(
            durationMinutes = settings.defaultSnoozeMinutes,
            repeatLimit = settings.defaultSnoozeRepeats,
            onConfirm = { minutes, repeats ->
                onDefaultSnoozeChange(minutes, repeats)
                snoozeSheetOpen = false
            },
            onDismiss = { snoozeSheetOpen = false },
        )
    }
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.Light -> "Light"
    ThemeMode.Dark -> "Dark"
    ThemeMode.System -> "System default"
}

private fun snoozeSummary(settings: ClockSettings): String {
    val repeats = if (settings.defaultSnoozeRepeats == 0) "forever"
    else "${settings.defaultSnoozeRepeats} times"
    return "${settings.defaultSnoozeMinutes} minutes, $repeats"
}
