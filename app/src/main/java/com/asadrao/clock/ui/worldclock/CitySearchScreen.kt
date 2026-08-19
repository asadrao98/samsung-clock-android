package com.asadrao.clock.ui.worldclock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.asadrao.clock.R
import com.asadrao.clock.domain.worldclock.City
import com.asadrao.clock.domain.worldclock.WorldClockTime
import com.asadrao.clock.ui.components.OneUiRowDivider
import com.asadrao.clock.ui.format.AlarmFormat
import com.asadrao.clock.ui.theme.ClockTheme
import java.time.Instant
import java.util.Locale

/**
 * Add a city.
 *
 * Searches the on-device time-zone catalogue, so it works with no network. Results show each city's
 * current time, which is usually what the user is checking in the first place, and a city already on
 * the list is marked rather than hidden — hiding it makes people think the search is broken.
 */
@Composable
fun CitySearchScreen(
    state: CitySearchUiState,
    is24Hour: Boolean,
    locale: Locale,
    onQueryChange: (String) -> Unit,
    onAdd: (City) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ClockTheme.colors
    val dimens = ClockTheme.dimens
    val now = Instant.now()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.pageBackground)
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
                text = "Add city",
                style = ClockTheme.typography.screenTitleSmall,
                color = colors.textPrimary,
            )
        }

        TextField(
            value = state.query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    text = "Search city, country or zone",
                    style = ClockTheme.typography.body,
                    color = colors.textTertiary,
                )
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(dimens.iconSize),
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            textStyle = ClockTheme.typography.body.copy(color = colors.textPrimary),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = colors.cardBackground,
                unfocusedContainerColor = colors.cardBackground,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = colors.accent,
            ),
            shape = ClockTheme.shapes.pill,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.navPillMargin, vertical = 8.dp),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = dimens.navPillMargin,
                end = dimens.navPillMargin,
                top = 8.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp,
            ),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ClockTheme.shapes.card)
                        .background(colors.cardBackground),
                ) {
                    state.results.forEachIndexed { index, city ->
                        if (index > 0) OneUiRowDivider()
                        CityResultRow(
                            city = city,
                            alreadyAdded = city.zoneId in state.alreadyAdded,
                            now = now,
                            is24Hour = is24Hour,
                            locale = locale,
                            onAdd = { onAdd(city) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CityResultRow(
    city: City,
    alreadyAdded: Boolean,
    now: Instant,
    is24Hour: Boolean,
    locale: Locale,
    onAdd: () -> Unit,
) {
    val colors = ClockTheme.colors
    val zoned = WorldClockTime.nowIn(city.zoneId, now)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // An already-added city is inert rather than absent, so the search does not look
            // as though it has failed to find somewhere the user knows is there.
            .then(if (alreadyAdded) Modifier else Modifier.clickable(onClick = onAdd))
            .padding(
                horizontal = ClockTheme.dimens.cardPadding,
                vertical = ClockTheme.dimens.rowPaddingVertical,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = city.cityName,
                style = ClockTheme.typography.listTitle,
                color = if (alreadyAdded) colors.textTertiary else colors.textPrimary,
            )
            Text(
                text = city.countryName,
                style = ClockTheme.typography.alarmMeta,
                color = colors.textSecondary,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (alreadyAdded) {
            Text(
                text = "Added",
                style = ClockTheme.typography.alarmMeta,
                color = colors.textTertiary,
            )
        } else {
            Text(
                text = AlarmFormat.time(zoned.hour, zoned.minute, is24Hour) +
                    (AlarmFormat.meridiem(zoned.hour, locale, is24Hour)?.let { " $it" } ?: ""),
                style = ClockTheme.typography.alarmMeta,
                color = colors.textSecondary,
            )
        }
    }
}
