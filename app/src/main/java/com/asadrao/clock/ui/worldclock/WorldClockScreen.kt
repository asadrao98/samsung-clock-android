package com.asadrao.clock.ui.worldclock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asadrao.clock.R
import com.asadrao.clock.domain.worldclock.City
import com.asadrao.clock.domain.worldclock.WorldClockTime
import com.asadrao.clock.ui.components.OneUiRowDivider
import com.asadrao.clock.ui.components.OneUiTextButton
import com.asadrao.clock.ui.format.AlarmFormat
import com.asadrao.clock.ui.theme.ClockTheme
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/**
 * The World clock list.
 *
 * Every row is derived from a single shared instant, ticked **once a minute on the minute** rather
 * than once a second. The rows show minutes, so a per-second tick would wake the device sixty times
 * for every visible change; aligning to the boundary also means the whole list rolls over together
 * instead of at sixty different moments.
 *
 * The home zone is read fresh on each tick, so a timezone change while the screen is open — the
 * exact case a world clock is used in — is picked up without anything having to invalidate a cache.
 */
@Composable
fun WorldClockContent(
    state: WorldClockUiState,
    is24Hour: Boolean,
    locale: Locale,
    onRemove: (String) -> Unit,
    onAddCity: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = ClockTheme.dimens
    val navInset = WindowInsets.navigationBars.asPaddingValues()

    // One clock for the whole list, aligned to the minute boundary.
    val now by produceState(initialValue = Instant.now()) {
        while (true) {
            val current = Instant.now()
            value = current
            val zoned = current.atZone(ZoneId.systemDefault())
            val millisToNextMinute =
                60_000L - (zoned.second * 1_000L + zoned.nano / 1_000_000L)
            delay(millisToNextMinute.coerceAtLeast(250L))
        }
    }
    val homeZone = ZoneId.systemDefault().id

    if (state.isEmpty) {
        EmptyCities(onAddCity = onAddCity, modifier = modifier.fillMaxSize())
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = dimens.navPillMargin,
            end = dimens.navPillMargin,
            top = 8.dp,
            bottom = dimens.contentBottomPadding(navInset.calculateBottomPadding()),
        ),
    ) {
        item(key = "card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ClockTheme.shapes.card)
                    .background(ClockTheme.colors.cardBackground),
            ) {
                state.cities.forEachIndexed { index, city ->
                    if (index > 0) OneUiRowDivider()
                    CityRow(
                        city = city,
                        now = now,
                        homeZone = homeZone,
                        is24Hour = is24Hour,
                        locale = locale,
                        onRemove = { onRemove(city.zoneId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CityRow(
    city: City,
    now: Instant,
    homeZone: String,
    is24Hour: Boolean,
    locale: Locale,
    onRemove: () -> Unit,
) {
    val colors = ClockTheme.colors
    val zoned = WorldClockTime.nowIn(city.zoneId, now)
    val isDay = WorldClockTime.isDaytime(city.zoneId, now)
    val dayOffset = WorldClockTime.dayOffset(city.zoneId, homeZone, now)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = ClockTheme.dimens.cardPadding,
                vertical = ClockTheme.dimens.rowPaddingVertical,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(if (isDay) R.drawable.ic_sun else R.drawable.ic_moon),
            contentDescription = if (isDay) "Daytime" else "Night-time",
            tint = if (isDay) colors.functionalOrange else colors.textSecondary,
            modifier = Modifier.size(ClockTheme.dimens.iconSmall),
        )
        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = city.cityName,
                style = ClockTheme.typography.listTitle,
                color = colors.textPrimary,
            )
            Text(
                text = buildString {
                    append(
                        WorldClockTime.offsetDescription(
                            zoneId = city.zoneId,
                            homeZoneId = homeZone,
                            instant = now,
                        )
                    )
                    when {
                        dayOffset > 0L -> append(" · Tomorrow")
                        dayOffset < 0L -> append(" · Yesterday")
                    }
                },
                style = ClockTheme.typography.alarmMeta,
                color = colors.textSecondary,
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = AlarmFormat.time(zoned.hour, zoned.minute, is24Hour),
                    style = ClockTheme.typography.worldClockTime,
                    color = colors.textPrimary,
                )
                AlarmFormat.meridiem(zoned.hour, locale, is24Hour)?.let {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = it,
                        style = ClockTheme.typography.alarmMeta,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))
        OneUiTextButton(text = "Remove", onClick = onRemove, color = colors.dangerText)
    }
}

@Composable
private fun EmptyCities(onAddCity: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ClockTheme.colors
    Column(
        modifier = modifier.padding(horizontal = ClockTheme.dimens.screenMargin),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_tab_world_clock),
            contentDescription = null,
            tint = colors.textTertiary.copy(alpha = 0.4f),
            modifier = Modifier.size(96.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "No cities",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Tap + to add a city",
            style = ClockTheme.typography.alarmMeta,
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(80.dp))
    }
}
