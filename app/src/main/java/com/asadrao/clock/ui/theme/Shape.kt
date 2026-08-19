package com.asadrao.clock.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Corner radii. One UI is aggressively rounded — noticeably rounder than Material 3 — and
 * getting these too small is one of the fastest ways to lose the look.
 */
@Immutable
data class ClockShapes(
    /** Rounded container grouping a set of rows. */
    val card: Shape,
    /** The pressed/selected highlight drawn behind a row inside a card. */
    val row: Shape,
    /** Dialogs. */
    val dialog: Shape,
    /** Overflow and context menus. */
    val popup: Shape,
    /** Bottom sheets: top corners only. */
    val bottomSheet: Shape,
    /** Fully rounded, for pill buttons, chips and toggles. Height-independent. */
    val pill: Shape,
    /** Day-of-week selector chips and other small round targets. */
    val chip: Shape,
    /** The circular stopwatch/timer control buttons. */
    val circle: Shape,
)

val ClockShapeTokens = ClockShapes(
    card = RoundedCornerShape(26.dp),
    row = RoundedCornerShape(8.dp),
    dialog = RoundedCornerShape(26.dp),
    popup = RoundedCornerShape(26.dp),
    bottomSheet = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
    pill = RoundedCornerShape(percent = 50),
    chip = RoundedCornerShape(percent = 50),
    circle = RoundedCornerShape(percent = 50),
)
