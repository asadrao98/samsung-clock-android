package com.asadrao.clock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.asadrao.clock.ui.theme.ClockTheme

/**
 * The "More options" popup.
 *
 * A raw `Popup` rather than Material's `DropdownMenu`, because the differences are all in the
 * container: One UI uses a 26dp radius, generous padding, 48dp rows and **no dividers** between
 * them, on a surface a step lighter than the page. Material's menu is a small-radius, tightly
 * padded, elevated card.
 */
@Composable
fun OneUiPopupMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!expanded) return
    Popup(
        alignment = Alignment.TopEnd,
        // Anchored just inside the screen edge, below the button that opened it.
        offset = with(androidx.compose.ui.platform.LocalDensity.current) {
            androidx.compose.ui.unit.IntOffset(0, 48.dp.roundToPx())
        },
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = modifier
                .width(220.dp)
                .clip(ClockTheme.shapes.popup)
                .background(ClockTheme.colors.popupBackground)
                .padding(vertical = 20.dp),
            content = content,
        )
    }
}

@Composable
fun OneUiPopupMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = ClockTheme.dimens.popupRowMinHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = ClockTheme.dimens.screenMargin),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = ClockTheme.typography.body,
            color = ClockTheme.colors.textPrimary,
        )
    }
}
