package com.asadrao.clock.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Semantic colour roles. Screens never name a raw colour — they ask for a role, so light and
 * dark stay in step and a palette correction is one edit here.
 *
 * Values come from `docs/ONEUI_DESIGN_SPEC.md`, which records how confident we are in each one.
 * They are derived from Samsung's published design guidance and from press coverage of the 8.5
 * Clock redesign — **not** transcribed from Samsung's shipped resources. Treat them as a close
 * reconstruction, not as sampled truth.
 *
 * There is no elevation scale on purpose. One UI separates surfaces with a single step of
 * colour plus heavy rounding; the page is the frame and the card is the lighter surface, in both
 * themes. A drop shadow immediately reads as Material.
 */
@Immutable
data class ClockColors(
    /** The page. Also the collapsed app bar, so no seam appears when the header closes. */
    val pageBackground: Color,
    /** Rounded container grouping related rows. */
    val cardBackground: Color,
    val cardBackgroundPressed: Color,
    /** Dialogs and bottom sheets — a step off the page. */
    val elevatedBackground: Color,
    /** Overflow and context menus, which sit a further step up. */
    val popupBackground: Color,

    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,

    /**
     * Reserved for controls: switches, checkboxes, selected states, primary buttons.
     * Deliberately **not** used for the selected tab icon or for app-bar icons — high-contrast
     * neutral in those places is a One UI signature and the clearest visual break from
     * Material 3.
     */
    val accent: Color,
    val accentPressed: Color,
    val onAccent: Color,
    /**
     * Accent used as *text*. On dark surfaces the control blue is only ~4:1 against a card, so
     * accent text needs the lighter variant to stay legible.
     */
    val accentText: Color,
    /** Accent at low alpha, for tonal button and selected chip fills. */
    val accentContainer: Color,

    val divider: Color,

    val switchTrackOn: Color,
    val switchTrackOff: Color,
    val switchThumb: Color,
    /** One UI rings the off-state thumb with a hairline; on-state has none. */
    val switchThumbStrokeOff: Color,

    /** Neutral, non-accent pill button fill. */
    val buttonSurface: Color,

    /** Destructive fill, e.g. a Delete pill. */
    val danger: Color,
    /** Destructive *text*, which needs to be lighter on dark. */
    val dangerText: Color,

    /** Behind dialogs and sheets. The scrim is the separation; there is no shadow. */
    val scrim: Color,

    /** Running/complete states on timer and stopwatch. */
    val functionalGreen: Color,
    val functionalOrange: Color,

    val isDark: Boolean,
) {
    /** Applied to an enabled colour rather than defining separate disabled hexes. */
    fun disabled(color: Color): Color = color.copy(alpha = DISABLED_ALPHA)

    companion object {
        const val DISABLED_ALPHA = 0.4f
    }
}

/** Light: a light-grey page carrying near-white cards. */
val ClockLightColors = ClockColors(
    pageBackground = Color(0xFFF1F1F3),
    cardBackground = Color(0xFFFCFCFF),
    cardBackgroundPressed = Color(0xFFF1F1F3),
    elevatedBackground = Color(0xFFFAFAFF),
    popupBackground = Color(0xFFFCFCFF),

    textPrimary = Color(0xFF010102),
    textSecondary = Color(0xFF636368),
    textTertiary = Color(0xFF848487),

    accent = Color(0xFF0381FE),
    accentPressed = Color(0xFF0072DE),
    onAccent = Color(0xFFFAFAFA),
    accentText = Color(0xFF0381FE),
    accentContainer = Color(0x140381FE),

    divider = Color(0xFFE4E4E7),

    switchTrackOn = Color(0xFF0381FE),
    switchTrackOff = Color(0xFF99999E),
    switchThumb = Color(0xFFFCFCFF),
    switchThumbStrokeOff = Color(0xFF8C8C8C),

    buttonSurface = Color(0xFFE4E4E7),

    danger = Color(0xFFD93E36),
    dangerText = Color(0xFFD93E36),

    scrim = Color(0x99000000),

    functionalGreen = Color(0xFF11A85F),
    functionalOrange = Color(0xFFE65B17),

    isDark = false,
)

/**
 * Dark: a near-black grey page carrying dark-grey cards.
 *
 * Not pure black. One UI 8.5's dark surfaces reportedly moved lighter than earlier releases, so
 * these target retail 8.5; `#010102` / `#17171A` would be the 8.0 look. That is the single swap
 * to make if this reads too light on a device.
 */
val ClockDarkColors = ClockColors(
    pageBackground = Color(0xFF101013),
    cardBackground = Color(0xFF1D1D20),
    cardBackgroundPressed = Color(0xFF2D2D30),
    elevatedBackground = Color(0xFF252528),
    popupBackground = Color(0xFF3A3A3D),

    textPrimary = Color(0xFFFAFAFF),
    textSecondary = Color(0xFFB7B7BB),
    textTertiary = Color(0xFF99999E),

    accent = Color(0xFF0381FE),
    accentPressed = Color(0xFF5AA0FF),
    onAccent = Color(0xFFFAFAFA),
    accentText = Color(0xFF3E91FF),
    accentContainer = Color(0x1F3E91FF),

    divider = Color(0xFF3A3A3D),

    switchTrackOn = Color(0xFF0381FE),
    switchTrackOff = Color(0xFF636368),
    switchThumb = Color(0xFFFCFCFF),
    switchThumbStrokeOff = Color(0xFF8F8F8F),

    buttonSurface = Color(0xFF2D2D30),

    danger = Color(0xFFD93E36),
    dangerText = Color(0xFFFC6C65),

    scrim = Color(0x99000000),

    functionalGreen = Color(0xFF58DB9C),
    functionalOrange = Color(0xFFFC864C),

    isDark = true,
)
