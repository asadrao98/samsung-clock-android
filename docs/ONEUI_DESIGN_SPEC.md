# One UI 8.5 Clock — implementation spec

The single source of truth for how this app should look and behave. Written before the UI was
built, and updated as things are verified.

## How to read this, and what is actually verified

Nobody on this project has held a Galaxy device running One UI 8.5. **Every number here is
either published by Samsung, described in press coverage of the 8.5 Clock redesign, or an
honest reconstruction.** Confidence is marked on each claim:

- **(high)** — published by Samsung, or corroborated across several independent write-ups
- **(medium)** — a long-standing One UI pattern, extrapolated to 8.5 or to this screen
- **(low)** — reconstruction or design judgement filling a gap. Proportion is likely right; the
  absolute value is a guess.

Treat every `dp`, `sp`, hex and `ms` marked (low) as a placeholder to check against hardware.
Getting the **proportions and hierarchy** right matters far more than matching any single value.

### Sources used

- **Samsung's published *One UI Design Guidelines*** (Mobile UX Center) — the only source for
  real Samsung numbers: the 39.67% expanded-header proportion, the 24dp side margin, the
  26/20/12dp radius scale.
- **Press coverage of the 8.5 Clock redesign** — SamMobile, SammyGuru, GizChina, and Samsung's
  own regional support pages. This is where the 8.5-specific changes come from, and it is the
  reason this spec departs from earlier One UI in several places.
- **AOSP resources** (Apache 2.0) for platform-level defaults.

### What was deliberately NOT used

Decompiled Samsung library resources (`oui_des_*`, from Samsung's One UI Design / SESL
libraries) were available during research and are **deliberately excluded**. No value in this
document or in the code was transcribed from them. Copying exact tokens out of Samsung's
shipped resources is what rule 4 of the brief forbids, and it would make this a port rather
than a recreation. Everything below is derived from published documentation, public description,
or our own judgement.

Samsung's typefaces (SamsungOne, Samsung Sans, SamsungSharpSans) are likewise never shipped.

---

## The five things that make it read as One UI

In rough order of how much each one carries the resemblance:

1. **An enormous expanded header** with the title low and centred, which collapses to a small
   toolbar — and **snaps** to open or closed, never resting halfway. (high)
2. **26dp corner rounding everywhere** on grouping containers, with **zero elevation**.
   Separation comes from a one-step colour difference between page and card, never a shadow.
   (high)
3. **A floating, pill-shaped, icon-only bottom navigation island** — new in 8.5, and a hard
   break from every earlier One UI, which used full-width text tabs. (high)
4. **Per-tab gradient colour** living in the expanded header area — also new in 8.5. (high)
5. **Short, hard-decelerating motion** — 150/220ms with Samsung's own sine-based curves. One UI
   does **not** overshoot; the springy feel comes from aggressive deceleration. (medium)

---

## Colour

Page is the frame; the card is the lighter surface in both themes.

| Role | Light | Dark | Conf |
|---|---|---|---|
| `pageBackground` | `#F1F1F3` | `#101013` | medium |
| `cardBackground` | `#FCFCFF` | `#1D1D20` | medium |
| `cardBackgroundPressed` | `#F1F1F3` | `#2D2D30` | low |
| `elevatedBackground` (dialog, sheet) | `#FAFAFF` | `#252528` | medium |
| `popupBackground` (overflow menu) | `#FCFCFF` | `#3A3A3D` | medium |
| `textPrimary` | `#010102` | `#FAFAFF` | high |
| `textSecondary` | `#636368` | `#B7B7BB` | high |
| `textTertiary` | `#848487` | `#99999E` | high |
| `divider` | `#E4E4E7` | `#3A3A3D` | high |
| `accent` | `#0381FE` | `#3E91FF` | medium |
| `accentPressed` | `#0072DE` | `#5AA0FF` | low |
| `onAccent` | `#FAFAFA` | `#FAFAFA` | high |
| `switchTrackOff` | `#99999E` | `#636368` | high |
| `switchThumb` | `#FCFCFF` | `#FCFCFF` | high |
| `danger` (fill) | `#D93E36` | `#D93E36` | high |
| `dangerText` | `#D93E36` | `#FC6C65` | high |
| `buttonSurface` (neutral pill) | `#E4E4E7` | `#2D2D30` | high |
| `scrim` | black 60% | black 60% | high |

Notes:

- **Dark is a near-black grey, not pure black** (medium). 8.5's dark surfaces reportedly moved
  lighter than earlier builds. `#101013`/`#1D1D20` targets retail 8.5; `#010102`/`#17171A`
  would be 8.0. One swap at the top of the dark palette.
- **Two sources disagree on the accent**: `#0381FE` (classic Samsung blue) vs `#387AFF`. We use
  `#0381FE`. One token, one-line correction. (medium)
- `disabledAlpha = 0.4f` applied to the enabled colour — no separate disabled hexes. (high)
- **Accent is never used for the selected tab icon or for app-bar icons.** High-contrast neutral
  there is the One UI signature and the main visual difference from Material 3. (medium)
- On dark, accent-coloured *text* uses the lighter `#3E91FF`: `#0381FE` on `#1D1D20` is about
  4.0:1, which fails for body text. (high)
- **Dynamic colour (Material You) is off.** The fixed palette is the product identity.

## Type

Roboto (the platform face). Samsung's own regular face is a modified Roboto, so it is the
closest legitimate metric match. Every changing numeral sets `fontFeatureSettings = "tnum"`.

| Role | Size | Weight | Conf |
|---|---|---|---|
| `screenTitleLarge` | 34sp | 700 | low |
| `screenTitleSmall` | 18sp | 700 | low |
| `sectionHeader` | 13sp | 500, **textTertiary** | high |
| `alarmTime` | 30sp | 400, tnum | medium |
| `alarmMeridiem` | 17sp | 400 | medium |
| `alarmMeta` | 13sp | 400 | medium |
| `body` / `listTitle` | 17sp | 400 | high |
| `buttonLabel` | 15sp | 500, no caps | high |
| `caption` | 12sp | 400 | high |
| `dialogTitle` / `dialogBody` | 17sp/500, 14sp/400 | | high |

The section header is **tertiary grey, sentence case** — not accent-coloured and not all-caps.

## Shape

26dp card / dialog / sheet-top / popup (high). Pill = capsule. Row press highlight 8dp (medium).

## Spacing and size

- `screenMargin` **24dp** (high) — deliberately more generous than Material's 16dp
- Row padding 14dp vertical; 12dp for a switch row (high)
- Card gap 12dp; 24dp above a section header (medium)
- Divider 1dp, inset 24dp from the card's start edge (medium)
- Row min heights: single-line 48dp, two-line 72dp, switch row 56dp (medium)
- Toggle: track **35 × 22dp**, thumb **18dp**, inset 2dp, travel 13dp, 48dp touch target (medium)
- Collapsed header: status inset + 56dp (medium)
- Expanded header: **39.67% of screen height** per Samsung's guide — ~362dp on a Pixel 8.
  Floor at ~300dp if that reads too tall. Do **not** fall back to Material's 152dp. (medium)
- Icons 24dp in 48dp targets; ~2dp stroke, thicker in 8.5 than 8.0 (medium)

## Motion

Durations: instant 100, short 150, default 220, long 300, popup 350 (high).

Samsung's curves, as cubic beziers (medium):

| Name | Control points | Use |
|---|---|---|
| `sineInOut70` | 0.33, 0.0, 0.30, 1.0 | default state change |
| `sineInOut80` | 0.33, 0.0, 0.20, 1.0 | stronger settle, toggle thumb |
| `sineOut80` | 0.17, 0.17, 0.20, 1.0 | entrances |
| `elastic50` | 0.22, 0.25, 0.0, 1.0 | the signature snap; press release |
| `exit` | 0.4, 0.0, 1.0, 1.0 | dialog/popup exits |

**Correction to a common assumption:** none of these overshoot — no control Y exceeds 1. The
springy quality comes from `elastic50` covering ~75% of its distance in ~25% of its time. Prefer
tweens with these curves; reserve springs for gesture-driven surfaces where physics is right.

- **Press recoil** (cards, rows, buttons, tabs): scale to 0.96 over 100ms `sineOut80`, back over
  350ms `elastic50`. (high)
- Dialog in: rise ~24dp over 300ms `sineOut80`, fade over 150ms. Out: 300ms `exit`. (high)
- Popup menu: scale 0.80→1.0 over 350ms `elastic50` from the anchor corner. (high)
- Header collapse is **scroll-driven with no easing** so it tracks the finger; snap on release
  over 220ms. (medium)

## The shell

### Bottom navigation — rebuilt for 8.5

A **detached floating pill**, not a bar. (high)

- Four icon-only destinations: Alarm, World clock, Stopwatch, Timer. **No labels at all** (high)
- 16dp side margins, stretches to fill; bottom offset = nav-bar inset + 8dp (low/medium)
- 64dp tall, fully rounded capsule (low/high)
- Translucent fill with a soft drop shadow — the one place elevation is allowed (low/medium)
- Selected: a **neutral circle** (~44dp) behind a full-opacity icon. Not the Material 3 stadium
  indicator, and **not accent blue** (medium)
- Unselected icons at reduced opacity, outline variant (medium)
- Always visible; content scrolls beneath (medium)
- Scrollable content needs ~108dp bottom `contentPadding` to clear it (high)
- Older Samsung guidance says "tabs should be text only" — that describes 1.x–8.0. Ignore it
  for 8.5. (high)

### Header

- Title is the **current tab's name**, sentence case: "World clock", never "World Clock" (high)
- Expanded title **centred and low**; collapsed title centred in the toolbar row (medium)
- **Crossfade two separately-laid-out titles** rather than scaling one (medium)
- **Snap, never rest halfway** — the key mechanic. Threshold 0.5, biased by fling velocity (high)
- Collapsing scrolls up; scrolling down re-expands **from anywhere in the list** (high)
- Per-tab gradient hero lives in the expanded area (high)
- Persist expanded/collapsed per tab across restarts (high)
- Disable the expandable header when screen height < 580dp, e.g. landscape (high)

### Toolbar, overflow and select mode

- **No FAB anywhere.** Add is a "+" icon button top-right, then "⋮" outermost (medium)
- Stopwatch and Timer have no "+" — their primary action is the big circular button (medium)
- **Settings lives in the ⋮ menu**, always the last row. No gear icon, no nav entry (high)
- Select mode: entered by ⋮ → Edit *or* long-press a row. Header becomes select-all checkbox +
  live count; rows grow a leading checkbox; a full-width bottom toolbar replaces the pill (high)
- On long-press entry, the bottom toolbar appears only when the finger **lifts** (high)
- No horizontal swipe between tabs; no `HorizontalPager` (high)
- State preservation across tab switches is mandatory and rich: a running stopwatch keeps its
  laps and scroll position, each tab remembers its own header state (high)

## Material 3 defaults to actively fight

| Stock behaviour | What to do instead |
|---|---|
| Ripple from the touch point | Whole-target darken + press recoil |
| `Switch` thumb resizes; outlined off-track | Constant 18dp thumb in a solid capsule |
| `NavigationBar` stadium indicator | Neutral circle inside a floating pill |
| Elevation shadows on cards | Zero elevation; colour step + 26dp rounding |
| `LargeTopAppBar` 152dp, rests mid-collapse | ~300–360dp, snaps to one of two states |
| All-caps button labels | Sentence case, 15sp/500 |
| 16dp screen margin | 24dp |
| `MaterialTheme` as source of truth | `ClockTheme`; Material only as a fallback net |

## Open questions, worst first

1. **Does 8.5's Clock still have a collapsing header at all?** Every 8.5 write-up covers the
   tab bar, gradients and circular buttons; none describes the top of the screen. It is possible
   the redesign replaced the collapsing title with a fixed hero. We build the documented
   expandable app bar because it is the recognisable trait — but this is an assumption.
2. **Expanded title centred or leading?** Samsung's guide says centred in two places, but
   several shipping One UI apps look left-aligned. Changes the screen's whole character.
3. **Is 39.67% still right for 8.5?** That figure comes from a guide whose metadata dates it to
   2019. ~362dp on a Pixel 8 may be too tall.
4. **Selected-tab colour** — the circular indicator is well corroborated; neutral-vs-accent is
   not.
5. **Accent hex** — `#0381FE` vs `#387AFF`.
6. **Every tab-bar and title dp/sp.** Samsung publishes almost none of these.
7. **Tab-switch transition** — fade-through is inferred from swipe being disabled, not observed.
8. **Overflow contents** beyond the Alarm tab are reconstruction.
