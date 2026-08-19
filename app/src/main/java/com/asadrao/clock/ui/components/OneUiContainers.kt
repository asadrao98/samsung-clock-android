package com.asadrao.clock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.asadrao.clock.ui.theme.ClockTheme

/**
 * The rounded container that groups related rows — One UI's basic unit of page structure.
 *
 * Depth comes from colour and rounding, never from a shadow: in light theme the card is white on
 * a light grey page, in dark theme a barely-lighter block on black. No `Modifier.shadow` and no
 * Material elevation anywhere, because a drop shadow immediately reads as Material.
 *
 * The clip is applied *before* any clickable inside, so the global press tint follows the
 * rounded corners instead of squaring them off.
 */
@Composable
fun OneUiCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(ClockTheme.shapes.card)
            .background(ClockTheme.colors.cardBackground),
        content = content,
    )
}

/**
 * A small accent-coloured label above a card group. One UI uses these constantly, and they are
 * noticeably smaller and more tinted than a Material section header.
 */
@Composable
fun OneUiSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = ClockTheme.typography.sectionHeader,
        // Tertiary grey and sentence case — not accent-coloured, and never all-caps.
        color = ClockTheme.colors.textTertiary,
        modifier = modifier.padding(
            start = ClockTheme.dimens.screenMargin,
            end = ClockTheme.dimens.screenMargin,
            top = ClockTheme.dimens.sectionGap,
            bottom = 6.dp,
        ),
    )
}

/**
 * A settings-style row: a title, an optional summary beneath it, and an optional trailing
 * control.
 *
 * Height is a *minimum* rather than fixed, so the row grows instead of clipping when the user
 * raises the system font size.
 */
@Composable
fun OneUiListRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = ClockTheme.colors
    val dimens = ClockTheme.dimens

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null && enabled) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .defaultMinSize(
                minHeight = when {
                    trailing != null && summary == null -> dimens.listRowSwitchMinHeight
                    summary != null -> dimens.listRowTwoLineMinHeight
                    else -> dimens.listRowMinHeight
                }
            )
            .padding(
                horizontal = dimens.cardPadding,
                vertical = if (trailing != null) dimens.rowPaddingVerticalSwitch
                else dimens.rowPaddingVertical,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = ClockTheme.typography.listTitle,
                color = if (enabled) colors.textPrimary else colors.disabled(colors.textSecondary),
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = ClockTheme.typography.listSummary,
                    color = if (enabled) colors.textSecondary else colors.disabled(colors.textSecondary),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (trailing != null) {
            Box(
                modifier = Modifier.padding(start = 12.dp),
                contentAlignment = Alignment.CenterEnd,
                content = { trailing() },
            )
        }
    }
}

/**
 * The hairline between two rows inside one card.
 *
 * Inset from the card's edges rather than running its full width — a full-bleed divider inside a
 * rounded container collides with the corner radius and looks like a mistake.
 */
@Composable
fun OneUiRowDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(start = ClockTheme.dimens.dividerInset, end = ClockTheme.dimens.dividerInset)
            .fillMaxWidth()
            .height(ClockTheme.dimens.dividerThickness)
            .background(ClockTheme.colors.divider),
    )
}
