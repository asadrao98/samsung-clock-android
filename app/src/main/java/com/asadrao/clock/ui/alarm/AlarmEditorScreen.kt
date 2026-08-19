package com.asadrao.clock.ui.alarm

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.IntentCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.asadrao.clock.ui.components.OneUiCard
import com.asadrao.clock.ui.components.OneUiDayChips
import com.asadrao.clock.ui.components.OneUiListRow
import com.asadrao.clock.ui.components.OneUiRowDivider
import com.asadrao.clock.ui.components.OneUiSwitch
import com.asadrao.clock.ui.components.OneUiTextButton
import com.asadrao.clock.ui.components.OneUiTimePicker
import com.asadrao.clock.ui.format.AlarmFormat
import com.asadrao.clock.ui.theme.ClockTheme
import java.util.Locale

/**
 * Add or edit an alarm. A full screen of its own, not a sheet or a dialog.
 *
 * The picker is pinned in a non-scrolling region at the top while the control list scrolls beneath
 * it. That split is deliberate: a drum picker inside a scrollable parent has its flings stolen by
 * the parent, so it must never be part of the scrolling content.
 *
 * Save and Cancel are plain text buttons filling the bottom bar half and half — no filled button,
 * no FAB, and no Save in the top bar.
 */
@Composable
fun AlarmEditorScreen(
    state: AlarmEditorUiState,
    is24Hour: Boolean,
    locale: Locale,
    onTimeChange: (Int, Int) -> Unit,
    onToggleDay: (java.time.DayOfWeek) -> Unit,
    onLabelChange: (String) -> Unit,
    onSoundChange: (String?, String) -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    onSnoozeEnabledChange: (Boolean) -> Unit,
    onSnoozeChange: (Int, Int) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ClockTheme.colors
    val dimens = ClockTheme.dimens
    var snoozeSheetOpen by remember { mutableStateOf(false) }

    val ringtonePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.let { data ->
                IntentCompat.getParcelableExtra(
                    data,
                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                    Uri::class.java,
                )
            }
            // A null URI is the picker's way of saying "Silent", which is a real choice and
            // distinct from "no sound chosen yet".
            onSoundChange(
                uri?.toString() ?: com.asadrao.clock.domain.model.Alarm.SILENT_SOUND,
                if (uri == null) "Silent" else "",
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.pageBackground)
            .padding(WindowInsets.statusBars.asPaddingValues()),
    ) {
        // Top row: close, and Delete for an existing alarm only.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimens.headerCollapsedHeight)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Cancel",
                    tint = colors.textPrimary,
                )
            }
            Spacer(Modifier.weight(1f))
            if (!state.isNew) {
                OneUiTextButton(
                    text = "Delete",
                    onClick = onDelete,
                    color = colors.dangerText,
                )
            }
        }

        // Pinned picker region. Never inside the scrollable below.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center,
        ) {
            OneUiTimePicker(
                hour = state.hour,
                minute = state.minute,
                is24Hour = is24Hour,
                onTimeChange = onTimeChange,
                locale = locale,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.navPillMargin),
        ) {
            // Summary line above the circles, which is how the "Every day" / "Weekdays" wording
            // is conveyed without inventing shortcut chips Samsung does not have.
            Text(
                text = AlarmFormat.repeatSummary(
                    repeatDays = state.repeatDays,
                    locale = locale,
                    everyDayLabel = "Every day",
                    weekdaysLabel = "Weekdays",
                    weekendsLabel = "Weekends",
                ) ?: "Ring once",
                style = ClockTheme.typography.alarmMeta,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
            )

            OneUiDayChips(
                selectedDays = state.selectedDays,
                onToggleDay = onToggleDay,
                locale = locale,
            )

            Spacer(Modifier.height(dimens.cardSpacing))

            OneUiCard {
                // Inline field, not a dialog — Samsung edits the name in place.
                AlarmNameField(value = state.label, onValueChange = onLabelChange)
                OneUiRowDivider()
                OneUiListRow(
                    title = "Alarm sound",
                    summary = soundSummary(state),
                    onClick = {
                        ringtonePicker.launch(ringtonePickerIntent(state.soundUri))
                    },
                )
                OneUiRowDivider()
                OneUiListRow(
                    title = "Vibration",
                    trailing = {
                        OneUiSwitch(
                            checked = state.vibrationEnabled,
                            onCheckedChange = onVibrationChange,
                        )
                    },
                )
                OneUiRowDivider()
                OneUiListRow(
                    title = "Snooze",
                    summary = snoozeSummary(state),
                    onClick = { snoozeSheetOpen = true },
                    trailing = {
                        OneUiSwitch(
                            checked = state.snoozeEnabled,
                            onCheckedChange = onSnoozeEnabledChange,
                        )
                    },
                )
            }

            Spacer(Modifier.height(24.dp))
        }

        // Bottom Cancel / Save bar.
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.dividerThickness)
                    .background(colors.divider),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WindowInsets.navigationBars.asPaddingValues())
                    .height(72.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                OneUiTextButton(
                    text = "Cancel",
                    onClick = onCancel,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                OneUiTextButton(
                    text = "Save",
                    onClick = onSave,
                    color = colors.accentText,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (snoozeSheetOpen) {
        SnoozeSheet(
            durationMinutes = state.snoozeDurationMinutes,
            repeatLimit = state.snoozeRepeatLimit,
            onConfirm = { duration, repeats ->
                onSnoozeChange(duration, repeats)
                snoozeSheetOpen = false
            },
            onDismiss = { snoozeSheetOpen = false },
        )
    }
}

@Composable
private fun AlarmNameField(value: String, onValueChange: (String) -> Unit) {
    val colors = ClockTheme.colors
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = "Alarm name",
                style = ClockTheme.typography.listTitle,
                color = colors.textTertiary,
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        textStyle = ClockTheme.typography.listTitle.copy(color = colors.textPrimary),
        colors = TextFieldDefaults.colors(
            // The field lives inside a card, so it must not draw its own container or the
            // Material underline.
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = colors.accent,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun soundSummary(state: AlarmEditorUiState): String = when {
    state.soundUri == com.asadrao.clock.domain.model.Alarm.SILENT_SOUND -> "Silent"
    state.soundName.isNotBlank() -> state.soundName
    state.soundUri == null -> "Default alarm sound"
    else -> "Custom sound"
}

private fun snoozeSummary(state: AlarmEditorUiState): String {
    val repeats = if (state.snoozeRepeatLimit == 0) "forever" else "${state.snoozeRepeatLimit} times"
    return "${state.snoozeDurationMinutes} minutes, $repeats"
}

/**
 * The system ringtone picker, scoped to alarm sounds.
 *
 * Using the platform picker rather than building our own list means the user's existing alarm
 * tones and any audio they have added are all available, with no storage permission needed.
 */
private fun ringtonePickerIntent(currentUri: String?): Intent =
    Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Alarm sound")
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        putExtra(
            RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
        )
        val existing = currentUri
            ?.takeIf { it != com.asadrao.clock.domain.model.Alarm.SILENT_SOUND }
            ?.let(Uri::parse)
        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing)
    }
