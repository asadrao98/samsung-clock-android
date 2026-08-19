package com.asadrao.clock.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing and component sizing. See `docs/ONEUI_DESIGN_SPEC.md` for provenance and confidence.
 *
 * One UI's generosity with whitespace is a large part of why it feels like One UI — a 24dp screen
 * margin against Material's 16dp, tall rows, and a great deal of air under a very large title.
 * Tightening these to Material defaults collapses the resemblance even when every colour is right.
 *
 * Every height here is a **minimum**, applied with `defaultMinSize`, never a fixed `height`, so
 * rows grow rather than clip when the user raises the system font scale.
 */
@Immutable
data class ClockDimensions(
    val grid: Dp,

    /** Horizontal page margin, for titles, section headers and card content alike. */
    val screenMargin: Dp,
    /** Horizontal padding inside a card. */
    val cardPadding: Dp,
    /** Vertical padding of a text row inside a card. */
    val rowPaddingVertical: Dp,
    /** Slightly tighter, for a row whose trailing control is a switch. */
    val rowPaddingVerticalSwitch: Dp,
    /** Gap between two cards. */
    val cardSpacing: Dp,
    /** Gap above a section header that follows a card. */
    val sectionGap: Dp,

    /**
     * The expanded header as a fraction of total screen height. Samsung's published guideline is
     * 39.67%, which is one of the very few real numbers they document — hence a fraction rather
     * than a dp, so it scales across devices.
     */
    val headerExpandedFraction: Float,
    /** Floor for the expanded header, for short screens where the fraction reads too small. */
    val headerExpandedMin: Dp,
    /** Below this screen height the expandable header is disabled entirely, e.g. landscape. */
    val headerExpandableMinScreenHeight: Dp,
    /** The pinned toolbar row. */
    val headerCollapsedHeight: Dp,

    /** The floating navigation pill. */
    val navPillHeight: Dp,
    val navPillMargin: Dp,
    /** Gap between the pill and the gesture-navigation inset. */
    val navPillBottomGap: Dp,
    /** Diameter of the neutral circle behind the selected tab icon. */
    val navSelectedCircleSize: Dp,

    val alarmRowMinHeight: Dp,
    val listRowMinHeight: Dp,
    val listRowTwoLineMinHeight: Dp,
    val listRowSwitchMinHeight: Dp,
    val popupRowMinHeight: Dp,

    /** Inline/dialog pill button. */
    val buttonHeight: Dp,
    /** Standalone primary action. */
    val buttonHeightLarge: Dp,
    val buttonPaddingHorizontal: Dp,
    val buttonMinWidth: Dp,
    /** The large circular stopwatch/timer control. */
    val circleButtonSize: Dp,
    val dayChipSize: Dp,

    /** Toggle geometry. Noticeably smaller than Material 3's switch. */
    val switchWidth: Dp,
    val switchHeight: Dp,
    val switchThumbSize: Dp,
    val switchThumbInset: Dp,
    val switchThumbStroke: Dp,

    val iconSize: Dp,
    val iconSmall: Dp,
    /** Leading icon in a preference-style row. */
    val iconLeadingLarge: Dp,
    val iconStroke: Dp,

    val dividerThickness: Dp,
    /** Divider inset from the card's start edge. */
    val dividerInset: Dp,

    val dialogPaddingHorizontal: Dp,
    val sheetHandleWidth: Dp,
    val sheetHandleHeight: Dp,

    /** Never build an interactive target smaller than this. */
    val minTouchTarget: Dp,
) {
    /**
     * Bottom padding a scrollable needs so its last item can clear the floating pill.
     *
     * Apply as `contentPadding`, never as a `Modifier.padding` on the list, or the scrollbar and
     * overscroll stop short of the true edge.
     */
    fun contentBottomPadding(navigationBarInset: Dp): Dp =
        navigationBarInset + navPillBottomGap + navPillHeight + cardSpacing
}

val ClockDimensionTokens = ClockDimensions(
    grid = 4.dp,

    screenMargin = 24.dp,
    cardPadding = 24.dp,
    rowPaddingVertical = 14.dp,
    rowPaddingVerticalSwitch = 12.dp,
    cardSpacing = 12.dp,
    sectionGap = 24.dp,

    headerExpandedFraction = 0.3967f,
    headerExpandedMin = 300.dp,
    headerExpandableMinScreenHeight = 580.dp,
    headerCollapsedHeight = 56.dp,

    navPillHeight = 64.dp,
    navPillMargin = 16.dp,
    navPillBottomGap = 8.dp,
    navSelectedCircleSize = 44.dp,

    alarmRowMinHeight = 80.dp,
    listRowMinHeight = 48.dp,
    listRowTwoLineMinHeight = 72.dp,
    listRowSwitchMinHeight = 56.dp,
    popupRowMinHeight = 48.dp,

    buttonHeight = 36.dp,
    buttonHeightLarge = 48.dp,
    buttonPaddingHorizontal = 20.dp,
    buttonMinWidth = 120.dp,
    circleButtonSize = 76.dp,
    dayChipSize = 40.dp,

    switchWidth = 35.dp,
    switchHeight = 22.dp,
    switchThumbSize = 18.dp,
    switchThumbInset = 2.dp,
    switchThumbStroke = 1.dp,

    iconSize = 24.dp,
    iconSmall = 20.dp,
    iconLeadingLarge = 36.dp,
    iconStroke = 2.dp,

    dividerThickness = 1.dp,
    dividerInset = 24.dp,

    dialogPaddingHorizontal = 24.dp,
    sheetHandleWidth = 40.dp,
    sheetHandleHeight = 4.dp,

    minTouchTarget = 48.dp,
)
