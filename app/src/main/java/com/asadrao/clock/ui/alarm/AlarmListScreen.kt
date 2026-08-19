package com.asadrao.clock.ui.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asadrao.clock.R
import com.asadrao.clock.domain.model.Alarm
import com.asadrao.clock.ui.components.OneUiRowDivider
import com.asadrao.clock.ui.format.AlarmFormat
import com.asadrao.clock.ui.theme.ClockTheme
import java.time.ZonedDateTime
import java.util.Locale

/**
 * The alarm list.
 *
 * The signature One UI 8.5 change here is the container logic: **one large rounded card holds
 * every alarm**, separated by hairlines, rather than a card per alarm. Getting that wrong is the
 * fastest way to make the screen look like a generic Material list.
 *
 * The "rings in 8 hr 43 min" summary is carried once, above the card — not repeated on each row.
 */
@Composable
fun AlarmListContent(
    state: AlarmListUiState,
    is24Hour: Boolean,
    locale: Locale,
    now: ZonedDateTime,
    nextTriggerFor: (Alarm) -> ZonedDateTime?,
    onAlarmClick: (Alarm) -> Unit,
    onAlarmLongClick: (Alarm) -> Unit,
    onToggle: (Alarm, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = ClockTheme.dimens
    val navInset = WindowInsets.navigationBars.asPaddingValues()

    if (state.isEmpty) {
        EmptyAlarms(modifier = modifier.fillMaxSize())
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        // As contentPadding, not Modifier.padding, so overscroll and the scrollbar still reach
        // the true edge while the last row can still clear the floating tab pill.
        contentPadding = PaddingValues(
            start = dimens.navPillMargin,
            end = dimens.navPillMargin,
            top = 8.dp,
            bottom = dimens.contentBottomPadding(navInset.calculateBottomPadding()),
        ),
    ) {
        if (state.nextTrigger != null) {
            item(key = "summary") {
                NextAlarmSummary(nextTrigger = state.nextTrigger, now = now, locale = locale, is24Hour = is24Hour)
            }
        }

        item(key = "card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ClockTheme.shapes.card)
                    .background(ClockTheme.colors.cardBackground),
            ) {
                state.alarms.forEachIndexed { index, alarm ->
                    if (index > 0) OneUiRowDivider()
                    AlarmRow(
                        alarm = alarm,
                        is24Hour = is24Hour,
                        locale = locale,
                        now = now,
                        nextTrigger = nextTriggerFor(alarm),
                        selectionMode = state.selectionMode,
                        selected = alarm.id in state.selectedIds,
                        onClick = { onAlarmClick(alarm) },
                        onLongClick = { onAlarmLongClick(alarm) },
                        onToggle = { onToggle(alarm, it) },
                    )
                }
            }
        }
    }
}

/**
 * How long until the next alarm rings, carried once for the whole screen.
 *
 * Hidden entirely when nothing is enabled, rather than showing a placeholder — the space
 * collapses with it.
 */
@Composable
private fun NextAlarmSummary(
    nextTrigger: ZonedDateTime,
    now: ZonedDateTime,
    locale: Locale,
    is24Hour: Boolean,
) {
    val colors = ClockTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 16.dp),
    ) {
        Text(
            text = "Alarm in " + AlarmFormat.timeUntil(
                nextTrigger = nextTrigger,
                now = now,
                hourUnit = "hr",
                minuteUnit = "min",
                lessThanAMinute = "less than a minute",
            ),
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
        )
        Spacer(Modifier.size(4.dp))
        Text(
            text = buildString {
                append(AlarmFormat.oneShotDate(nextTrigger, now, locale, "Today", "Tomorrow"))
                append("  ")
                append(AlarmFormat.time(nextTrigger.hour, nextTrigger.minute, is24Hour))
                AlarmFormat.meridiem(nextTrigger.hour, locale, is24Hour)?.let {
                    append(" ")
                    append(it)
                }
            },
            style = ClockTheme.typography.alarmMeta,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun EmptyAlarms(modifier: Modifier = Modifier) {
    val colors = ClockTheme.colors
    Column(
        modifier = modifier.padding(horizontal = ClockTheme.dimens.screenMargin),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_tab_alarm),
            contentDescription = null,
            tint = colors.textTertiary.copy(alpha = 0.4f),
            modifier = Modifier.size(96.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "No alarms",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Tap + to add an alarm",
            style = ClockTheme.typography.alarmMeta,
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
        )
        // Sits slightly above true centre, which reads better than dead centre.
        Spacer(Modifier.height(80.dp))
    }
}
