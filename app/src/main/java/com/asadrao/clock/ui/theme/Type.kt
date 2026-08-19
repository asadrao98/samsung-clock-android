package com.asadrao.clock.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * One UI's real typefaces (SamsungOne for text, a geometric face for the big numerals) are
 * proprietary and cannot ship here, so everything routes through this one declaration.
 * Swapping in a bundled open font later means editing this single line.
 *
 * On Pixel this resolves to Roboto. Roboto's numerals are narrower and less round than
 * Samsung's, which is the single most visible difference between this app and the original.
 */
val ClockFontFamily: FontFamily = FontFamily.SansSerif

/**
 * Tabular figures. Every readout that changes while you watch it — clock, stopwatch, timer,
 * lap splits — must use this, or the text reflows on each tick as digit widths change.
 * Roboto ships proportional figures by default, so this is not optional.
 */
private const val TABULAR = "tnum"

/** Trims the extra leading Compose adds, so large type sits where the design expects. */
private val TightLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

@Immutable
data class ClockTypography(
    /** The big title of an expanded One UI header. */
    val screenTitleLarge: TextStyle,
    /** The same title after the header collapses into the app bar. */
    val screenTitleSmall: TextStyle,
    /** Small, accent-coloured group label above a card. */
    val sectionHeader: TextStyle,

    /** Alarm row: the time. */
    val alarmTime: TextStyle,
    /** Alarm row: the AM/PM beside the time. */
    val alarmMeridiem: TextStyle,
    /** Alarm row: label, repeat days, next-ring text. */
    val alarmMeta: TextStyle,

    /** World clock row: the city's current time. */
    val worldClockTime: TextStyle,

    /** The dominating numeric readout on stopwatch, timer and the ringing screen. */
    val numericDisplay: TextStyle,
    /** Hundredths, shown smaller and trailing the main readout. */
    val numericDisplaySmall: TextStyle,

    val dialogTitle: TextStyle,
    val dialogBody: TextStyle,

    val body: TextStyle,
    val listTitle: TextStyle,
    val listSummary: TextStyle,
    val buttonLabel: TextStyle,
    val caption: TextStyle,
)

val ClockTypographyTokens = ClockTypography(
    screenTitleLarge = TextStyle(
        fontFamily = ClockFontFamily,
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.6).sp,
        lineHeight = 40.sp,
        lineHeightStyle = TightLineHeight,
    ),
    screenTitleSmall = TextStyle(
        fontFamily = ClockFontFamily,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
        lineHeight = 24.sp,
        lineHeightStyle = TightLineHeight,
    ),
    sectionHeader = TextStyle(
        fontFamily = ClockFontFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 18.sp,
    ),

    alarmTime = TextStyle(
        fontFamily = ClockFontFamily,
        fontSize = 30.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = (-0.3).sp,
        lineHeight = 34.sp,
        lineHeightStyle = TightLineHeight,
        fontFeatureSettings = TABULAR,
    ),
    alarmMeridiem = TextStyle(
        fontFamily = ClockFontFamily,
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 22.sp,
    ),
    alarmMeta = TextStyle(
        fontFamily = ClockFontFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 19.sp,
    ),

    worldClockTime = TextStyle(
        fontFamily = ClockFontFamily,
        fontSize = 30.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = (-0.4).sp,
        lineHeight = 34.sp,
        lineHeightStyle = TightLineHeight,
        fontFeatureSettings = TABULAR,
    ),

    numericDisplay = TextStyle(
        fontFamily = ClockFontFamily,
        fontSize = 64.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = (-1.5).sp,
        lineHeight = 70.sp,
        lineHeightStyle = TightLineHeight,
        fontFeatureSettings = TABULAR,
    ),
    numericDisplaySmall = TextStyle(
        fontFamily = ClockFontFamily,
        fontSize = 34.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = (-0.6).sp,
        lineHeight = 38.sp,
        lineHeightStyle = TightLineHeight,
        fontFeatureSettings = TABULAR,
    ),

    dialogTitle = TextStyle(
        fontFamily = ClockFontFamily,
        fontSize = 17.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 22.sp,
    ),
    dialogBody = TextStyle(
        fontFamily = ClockFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 20.sp,
    ),

    body = TextStyle(
        fontFamily = ClockFontFamily,
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 22.sp,
    ),
    listTitle = TextStyle(
        fontFamily = ClockFontFamily,
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 22.sp,
    ),
    listSummary = TextStyle(
        fontFamily = ClockFontFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 18.sp,
    ),
    buttonLabel = TextStyle(
        fontFamily = ClockFontFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 20.sp,
    ),
    caption = TextStyle(
        fontFamily = ClockFontFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 16.sp,
    ),
)
