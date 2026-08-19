package com.asadrao.clock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.asadrao.clock.ui.theme.ClockTheme

/**
 * A One UI bottom sheet.
 *
 * Built on Material 3's `ModalBottomSheet` deliberately: the drag-to-dismiss physics, scrim
 * handling and predictive-back integration are genuinely hard to reproduce and are not what makes
 * a sheet look Material. Every *visual* slot is replaced — 26dp top corners, our elevated surface,
 * our 60% scrim, and our own grab handle in place of Material's.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneUiBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = ClockTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = ClockTheme.shapes.bottomSheet,
        containerColor = colors.elevatedBackground,
        contentColor = colors.textPrimary,
        scrimColor = colors.scrim,
        dragHandle = { OneUiSheetHandle() },
        modifier = modifier,
        content = content,
    )
}

@Composable
private fun OneUiSheetHandle() {
    val dimens = ClockTheme.dimens
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = dimens.sheetHandleWidth, height = dimens.sheetHandleHeight)
                .clip(ClockTheme.shapes.pill)
                .background(ClockTheme.colors.textTertiary.copy(alpha = 0.5f)),
        )
    }
}
