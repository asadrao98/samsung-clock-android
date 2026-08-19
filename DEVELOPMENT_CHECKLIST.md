# Development Checklist — Samsung One UI 8.5 Clock, rebuilt for Google Pixel

A native Android clock app that replicates the **look, layout, motion and behaviour** of
Samsung Clock on One UI 8.5, built with Kotlin + Jetpack Compose for stock Android.
Fully offline, fully local. No Samsung SDKs, no Samsung services, no cloud, no account,
no analytics, no network permission.

**This is a clean-room recreation of a user experience.** No Samsung code is decompiled or
copied, and no proprietary Samsung asset — fonts, icons, sounds — is shipped. Where a Samsung
asset is central to the look (notably the SamsungOne / SamsungSharpSans typefaces) a
legitimate substitute is used and the substitution is recorded below.

## Why this repo exists separately

The Samsung Clock brief was issued while the working directory was `~/apple-notes-android`,
which holds a finished, device-verified Apple Notes replica (`com.asadrao.notes`). Building a
clock app there would have overwritten that project's `DEVELOPMENT_CHECKLIST.md` — its live
state file, which had uncommitted edits at the time — and its Gradle/namespace configuration.
On 19 Aug 2026 the owner chose to put the clock app in this new sibling repo instead.
**`~/apple-notes-android` is untouched by this project.**

## Build & install (this Mac)

```bash
cd ~/samsung-clock-android
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew :app:assembleDebug
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The `JAVA_HOME` prefix is **required**, not optional. This Mac has no system Java at all
(`java -version` fails), so the `gradlew` launcher cannot start without it.
`org.gradle.java.home` in `gradle.properties` only tells the Gradle daemon which JDK to
compile with; it does not help the launcher.

### Verified toolchain (19 Aug 2026)

| Piece | Version | Note |
|---|---|---|
| JDK | Homebrew `openjdk@17` 17.0.20 | the only JDK installed on this machine |
| Gradle | 9.3.1 (wrapper) | distribution already cached locally |
| AGP | 8.12.0 | |
| Kotlin | 2.1.20, KSP 2.1.20-2.0.1 | |
| Compose BOM | 2025.06.01 | |
| compileSdk / targetSdk | 36 | `android-36` is the only platform installed |
| minSdk | 26 | gives `java.time` without desugaring |
| Build tools | 36.0.0 (35.0.0 also present) | |

Versions were deliberately matched to the Apple Notes project rather than bumped to latest,
because that exact combination is already proven to build on this machine.

### Testing constraints — read before trusting any "verified" claim

**Nothing in this repo has been seen running.** There is no device and no emulator on this machine.
Read that alongside every tick below.


- **JVM unit tests + Robolectric work.** `./gradlew :app:testDebugUnitTest`
- **Robolectric is pinned to SDK 35** in `app/src/test/resources/robolectric.properties`.
  Reason: Robolectric defaults to the app's `targetSdk` (36), and the API 36 `android-all`
  jar requires **Java 21**, which this machine does not have. Tests therefore exercise API 35
  shadows. Anything whose behaviour genuinely differs on API 36 must be checked on a device.
  (Installing JDK 21 would remove this gap — not done, as it is a machine-wide change.)
- **There is no connected device and no emulator installed** (`adb devices` empty,
  no AVDs, no `emulator` package in the SDK). So nothing in this repo has been
  observed running yet. Every claim below is marked with how it was verified.
- Robolectric downloads `android-all` jars from Maven on first run, so the first test
  invocation needs network on the dev machine. The app itself never needs network.

**Do not run `connectedAndroidTest` against a phone whose clock app data matters:** Gradle
uninstalls the app afterwards, wiping its data. Install manually and use
`adb shell am instrument` instead. (Carried over from the Notes project, where this cost data.)

## Verification legend

Used throughout this file, because "it compiles" is not "it works":

- `[build]` — compiles / packages successfully
- `[test]` — covered by an automated test that passes
- `[device]` — observed working on real hardware
- `[unverified]` — written, but not yet proven by any of the above

## Phase status

| Phase | Scope | State |
|---|---|---|
| 1 | Project foundation, design system, navigation shell | built, `[test]` |
| 2 | Alarm: list, editor, persistence, AlarmManager scheduling | built, `[test]` |
| 3 | Alarm ringing: notification, full-screen, snooze, dismiss | built, `[test]`, ring path `[unverified]` on hardware |
| 4 | World clock: search, add/remove, timezone maths, day/night | built, `[test]`; reorder is model-only |
| 5 | Stopwatch: monotonic timing, laps, background accuracy | built, `[test]` |
| 6 | Timer: entry, presets, multiple timers, background, ringing | built, `[test]` |
| 7 | Settings | built, `[test]` |
| 8 | Visual matching audit against One UI 8.5 | `[device]` on Pixel 8; Samsung comparison still open |
| 9 | Behavioural testing: reboot, DND, timezone, DST, process death | scheduling + ring `[device]`; reboot/DND still open |

**Whole project, from a clean build:** 240 JVM tests passing, Android Lint 0 errors, debug and
release APKs both assemble, release APK 12 MB, and **zero network permissions in either**.

## Phase 1 — Project foundation

### Build & packaging

- [x] `[build]` Gradle project created: `rootProject.name = "SamsungClock"`, single `:app`
      module, `applicationId com.asadrao.clock`
- [x] `[build]` Gradle wrapper copied from the Notes project and **hash-verified**
      (`acb0e15f…`) rather than re-downloaded
- [x] `[build]` Debug **and release** APKs both assemble
- [x] `[build]` **No network permission, in either build type.** `aapt2 dump permissions`
      reports exactly one entry on each APK: Compose's internal
      `com.asadrao.clock.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. No `INTERNET`, no
      `ACCESS_NETWORK_STATE`. Re-check after **every** dependency change — in the Notes
      project ML Kit silently added both to the *merged* manifest.
- [x] `[build]` Adaptive launcher icon drawn as an original vector (analog dial at 10:10),
      with a `monochrome` layer so Android 13+ themed icons work
- [x] `[build]` XML window background matches the Compose page background in light and dark,
      so launch shows no colour flash
- [x] `[test]` All five hand-authored vector drawables actually inflate at runtime with a
      non-zero intrinsic size. Worth a test: malformed vector XML compiles fine and only
      fails when something tries to draw it.

### Design system — `ui/theme/`

- [x] `[build]` `Color.kt` — semantic roles only, full light and dark palettes. No screen
      names a raw colour.
- [x] `[build]` `Type.kt` — named type roles; `tnum` tabular figures on every readout that
      changes while you watch it
- [x] `[build]` `Shape.kt`, `Dimensions.kt` — radii, spacing grid, component heights.
      Heights are minimums applied with `defaultMinSize`, so rows still grow at large font
      scales.
- [x] `[build]` `Animation.kt` — One UI-style ease-out plus under-damped springs, with
      per-unit spec helpers (a `Dp` animation needs a different visibility threshold from an
      `IntOffset` one)
- [x] `[build]` `Theme.kt` — tokens exposed through `ClockTheme` over `staticCompositionLocalOf`;
      a `MaterialTheme` is installed underneath mapped to the same palette, purely so any
      Material component used for its behaviour does not show Material purple
- [x] `[build]` Dynamic colour (Material You) deliberately **not** used — it would pull the
      app away from the One UI look this project exists to reproduce
- [x] `[build]` `Indication.kt` — Material's ripple replaced app-wide via `LocalIndication`
      with a One UI-style whole-target darken/lighten. A ripple is one of the loudest tells
      that an app is Material underneath.
- [x] `[test]` Shell composes in **both** light and dark, so no palette role is
      accidentally defined for only one theme

### Navigation shell

- [x] `[build]` Four tab icons drawn as original vectors on a shared 24dp/1.8-stroke grid
- [x] `[test]` Bottom navigation with all four destinations in Samsung's order; selection
      shown by colour and label weight, with **no Material 3 pill indicator**
- [x] `[test]` Tapping a tab swaps content; returning to a tab restores its screen
- [x] `[build]` Tab state preserved by `rememberSaveableStateHolder`, so a tab's scroll
      position and collapsed-header state survive switching away without keeping all four
      screens composed and ticking
- [x] `[build]`/`[test]` Collapsing large-title header: scroll is consumed by the header
      first, so the list only begins to move once the header has closed. Its arithmetic is
      covered by 9 unit tests including the awkward cases — bottoming out mid-gesture,
      re-expanding only as far as it collapsed, a font-scale change while part-closed, and a
      zero-height range.
- [x] `[test]` **Accessibility fix found by testing:** the header cross-fades two copies of
      the title, and both sit in the semantics tree regardless of opacity, so TalkBack
      announced the screen title twice. The app-bar copy is now hidden with
      `clearAndSetSemantics`, the large one marked as the `heading`. A test asserts exactly
      one heading.

### Not done in Phase 1, deliberately

- [ ] **Header snap.** One UI snaps the header fully open or closed when you release it
      part-way. It interacts with the list's own fling and there is no device here to tune it
      against, so it is left for Phase 8 rather than guessed at.
- [ ] **`AppContainer` / DI wiring.** Nothing to contain yet. Added in Phase 2 with the
      first repository, rather than committing an empty class now.
- [ ] **`NavHost`.** There is still only one destination. Added in Phase 2, when the alarm
      editor becomes a second one.

### Test totals

17 JVM tests, all passing (`./gradlew :app:testDebugUnitTest`).

**What "passing" does and does not mean here.** Compose UI tests run under Robolectric, which
measures layout but never puts a pixel on a screen. They prove the shell composes, wires its
clicks and swaps its content. They prove nothing about whether it *looks* like One UI, and
nothing about gesture feel. Visual fidelity is unverified until Phase 8 on real hardware.

## Phases 2-7 — what was built

Details of the alarm domain, scheduling arithmetic and persistence are in the git history; this is
the state of each area now.

### Phase 2 — Alarm

- [x] `[test]` One grouped card holding every alarm, hairline-divided — the signature One UI 8.5
      container change. Not a card per alarm.
- [x] `[test]` Alarm row: time with baseline-aligned AM/PM, seven locale-ordered repeat letters,
      optional name, toggle. A disabled alarm changes **alpha only** — never layout, weight or hue —
      so the list does not shift as alarms are toggled.
- [x] `[test]` Custom drum time picker: three columns in 12-hour, two in 24-hour, looping hours and
      minutes, snap-to-item, per-item haptic tick, continuous scale/alpha falloff applied in the
      **draw** phase rather than by animating `fontSize`.
- [x] `[test]` Full-screen editor with a pinned picker region — a drum inside a scrollable parent
      has its flings stolen, so it is deliberately outside the scroll.
- [x] `[test]` Day chips, inline name field, system ringtone picker, vibration, snooze sheet, and a
      bottom Cancel/Save bar of plain text buttons.
- [x] `[test]` Selection mode from long-press **or** ⋮ → Edit, with the two behaving differently:
      long-press selects the row you pressed, the menu selects nothing. Pre-selecting from the menu
      would be surprising next to a Delete button.
- [x] `[test]` 20 scheduling tests including both DST directions, midnight crossings, and an alarm
      one minute away.
- [x] `[test]` The `PendingIntent` equality trap closed — ids live in the data URI, so ten alarms
      coexist rather than overwriting each other.

### Phase 3 — Ringing

- [x] `[test]` Foreground service owns the sound and vibration, so the ringing outlives the UI. The
      screen can be destroyed without silencing an alarm that should still be going.
- [x] `[test]` **The alarm channel is deliberately silent and non-vibrating.** The service plays the
      alarm with `USAGE_ALARM` audio itself. If the channel also made the sound, whether an alarm is
      audible would become a notification setting the user can switch off — which is not an alarm
      clock.
- [x] `[test]` Full-screen intent over the lockscreen via `setShowWhenLocked` / `setTurnScreenOn`,
      with the notification carrying working Snooze and Dismiss actions as the fallback for a revoked
      full-screen-intent permission.
- [x] `[build]` Sound fallback chain: chosen URI → system alarm sound → system default. A deleted
      file or lapsed permission degrades instead of ringing silently.
- [x] `[test]` Snooze budget persisted, so it survives the process being killed between snoozes
      rather than silently resetting to unlimited.
- [x] `[build]` Volume and power keys snooze; Back is deliberately inert, so the alarm cannot be
      hidden while still ringing.

### Phase 4 — World clock

- [x] `[test]` **No bundled city list.** The catalogue is derived from the platform's own tz
      database, which is already on the device, updates with the OS and carries the DST rules.
      Technical zones (`Etc/`, `SystemV/`, bare aliases) are filtered out.
- [x] `[test]` Search by city, country or zone id, ranking prefix matches first so "lon" finds
      London before Colombo.
- [x] `[test]` Offsets computed **live**, never stored — a stored "+4" is wrong for half the year.
      Half- and quarter-hour zones reported precisely.
- [x] `[test]` Yesterday/Tomorrow by calendar date, and a day/night glyph.
- [x] `[build]` The list ticks once a minute **on the minute**, so rows roll over together instead
      of at sixty different moments.
- [ ] Reorder exists in the view model and repository but has no drag handle in the UI yet.

### Phase 5 — Stopwatch

- [x] `[test]` 17 tests. Elapsed time is **derived from a monotonic clock**, never accumulated by
      ticking, so eight hours in the background is still accurate to the millisecond.
- [x] `[test]` A reboot mid-run keeps the time banked before the restart and stops, rather than
      inventing a duration from an incomparable clock reading.
- [x] `[test]` Laps stored cumulatively and differenced for display, so a split can never drift out
      of step with the readout above it.
- [x] `[build]` Analog/digital hybrid dial drawn on a Canvas, hand sweeping continuously.
- [x] `[build]` Recomputed once per frame **only while running**; costs nothing when paused.

### Phase 6 — Timer

- [x] `[test]` 14 tests. Remaining time derived from a monotonic deadline, so it survives the wall
      clock being changed, timezone changes and DST.
- [x] `[build]` Completion scheduled with `ELAPSED_REALTIME_WAKEUP`, an exact match for how a timer
      is modelled — and `setExactAndAllowWhileIdle` rather than `setAlarmClock`, because a timer
      should not claim the system's upcoming-alarm indicator.
- [x] `[build]` Multiple concurrent timers as a stacked scrollable list, with presets.
- [x] `[test]` "+1 min" extends both deadline and total, so the ring does not jump backwards.
- [x] `[build]` Its own channel and ring service, separate from alarms.

### Phase 7 — Settings

- [x] `[test]` Theme (light/dark/system) that genuinely repaints the app, default snooze that seeds
      new alarms, and timer vibration that the ring service actually reads.
- [x] `[test]` **No 12/24-hour toggle** — that belongs to the system, and the app follows it live
      via a `ContentObserver`. A test asserts the setting is absent.
- [x] `[build]` Notification settings hand off to the platform rather than pretending to own it.
- [x] `[test]` Room migrations 1→2 and 2→3 written by hand, contiguous, with **no**
      `fallbackToDestructiveMigration` — alarms someone relies on must not be dropped by a schema bump.

## Bugs found by the tests and tooling, not by inspection

Worth recording, because each one would have shipped:

1. **The header announced its title to TalkBack twice.** Two cross-faded copies both sit in the
   semantics tree regardless of opacity. Found by writing an accessibility assertion.
2. **No heading at all on short screens.** When the expandable header is disabled the large copy is
   not composed, so nothing carried the heading role.
3. **`Duration.toMinutesPart()` requires API 31** but `minSdk` is 26 — a guaranteed crash on
   Android 8 to 11 in the World clock. Found by lint.
4. **Ringing-screen state created without `remember`** — rebuilt on every recomposition, so the
   clock reset each time it ticked. Found by lint.
5. **An NPE in the alarm receiver.** `goAsync()` returns null when `onReceive` runs outside a real
   broadcast dispatch, and `finish()` then threw on a background thread — in the alarm path, where a
   crash is most costly. It surfaced as a *flaky unrelated test*, which took several wrong guesses to
   trace back.
6. **A leaked SQLite handle** in tests, tripping Robolectric's CloseGuard and failing whichever test
   ran next.
7. **⋮ → Edit pre-selected the first alarm**, putting a selected row next to a Delete button.

## Phase 8/9 — on a real Pixel 8, 19 Aug 2026

Installed on a **Pixel 8, Android 17 (API 37), 1080x2400 @ 420dpi (411x914dp)**. Note the platform
gap: the app is `targetSdk 36` on API 37, so it runs under compatibility behaviours.

### Confirmed on hardware

- [x] `[device]` Clean install; cold launch 88 ms; no crash
- [x] `[device]` **No network permission, confirmed by the OS itself.** `dumpsys package` records
      exactly eight requested permissions — POST_NOTIFICATIONS, FOREGROUND_SERVICE,
      FOREGROUND_SERVICE_MEDIA_PLAYBACK, RECEIVE_BOOT_COMPLETED, USE_EXACT_ALARM,
      USE_FULL_SCREEN_INTENT, VIBRATE, and Compose's internal one. No INTERNET.
      `SCHEDULE_EXACT_ALARM` correctly absent, being capped at API 32.
- [x] `[device]` `USE_EXACT_ALARM: granted=true` — the install-time grant works, so exact alarms
      need no user toggle
- [x] `[device]` **`setAlarmClock` verified through `dumpsys alarm`**: `RTC_WAKEUP`, `window=0`
      (not batched), `exactAllowReason=policy_permission`, an `Alarm clock:` block carrying
      `triggerTime` and `showIntent`, and listed as the device's **`Next wake from idle`** — so it
      fires through Doze.
- [x] `[device]` The alarm fired at exactly its scheduled second and the foreground service
      started from the background: `Background started FGS: Allowed ... code:ALARM_MANAGER_ALARM_CLOCK`
- [x] `[device]` Ring service runs as `isForeground=true types=0x00000002` (mediaPlayback) with the
      notification on the `alarm_ringing` channel showing `sound=null vibrate=null ... category=alarm
      actions=2` — the deliberately-silent channel and its two fallback actions, exactly as designed
- [x] `[device]` Alarm audio started with `usage=USAGE_ALARM ... event:started`
- [x] `[device]` All four tabs render: expanded header, per-tab gradient hero, centred large title,
      floating icon-only pill with the neutral circular indicator, `+` / `⋮` actions
- [x] `[device]` Alarm editor: three-column drum picker opening at the current time, day chips,
      grouped control card, bottom Cancel/Save bar

### Bugs the device found that no test had

1. **The drum picker was off by one.** Every wheel displayed the item *before* its value — the
   Timer opened on `23h 00m 59s` instead of `0h 01m 00s`, and a new alarm would have opened a
   minute behind. Cause: the list is padded by exactly one slot, so the item at
   `firstVisibleItemIndex` *is* the centred one; subtracting `slotsAboveCentre` moved everything
   down one. The centred value is now derived from measured layout rather than index arithmetic,
   so it cannot drift out of step with the padding again.
2. **The full-screen alarm never appeared over the lockscreen.**
   `appops get USE_FULL_SCREEN_INTENT` showed `default; rejectTime=` stamped at the exact second
   the alarm fired: on Android 14+ that permission is only auto-granted to apps Google Play has
   classified as alarm or calling apps, so a sideloaded build is refused and the system silently
   declines to launch the activity. Fixed by launching `AlarmRingingActivity` **directly from the
   receiver** — an alarm scheduled with `setAlarmClock` arrives with a background-activity-launch
   grant (`backgroundActivityAllowed=2` in `dumpsys alarm`), which makes that legal. The
   full-screen intent remains as a second route and the notification actions as a third.
   **Still to verify on hardware.**
3. **Turning the timer's wheel scrolled the page.** The collapsing header sits above the drums, and
   Compose consults the *outermost* nested-scroll connection first, so the header claimed the
   drag. A child cannot pre-empt an ancestor, so Timer and Stopwatch now opt out of
   collapse-on-scroll entirely — which also matches Samsung, where those screens are fixed.
4. **No quick-duration buttons.** Samsung offers one-tap durations ordered by what you have
   actually used. Added, with a recents store that moves a reused duration to the front.

### Also observed, and worth knowing

- Android 17 logs `AudioHardening background playback would be muted for com.asadrao.clock ...
  usage: USAGE_ALARM`. The player did start, so on this build it appears to be advisory rather
  than enforced — but background-started alarm audio is clearly on a path to being restricted.
  Getting the ringing activity to the foreground first (fix 2 above) is the durable answer.
- `attributionTag not declared in manifest` is logged by AppOps for the media player. Harmless,
  but declaring one would quieten it.

### The lock-screen alarm: solved, and what it took

**Working.** The ringing screen comes up over the locked screen, the display lights itself, sound
plays, and swipe-to-dismiss stops it — confirmed on the Pixel 8 and by the owner hearing it.

The cause was one line, and five wrong guesses preceded it. Recorded because the mistakes are the
useful part:

1. Tried launching the activity straight from the receiver. Refused: `BAL_BLOCK`.
2. Read `balAllowedByPiCreator: BSP.ALLOW_BAL` as permission and switched to sending the activity's
   PendingIntent. Still refused — the next field says `resultIfPiCreatorAllowsBal: BAL_BLOCK`,
   i.e. blocked *even with* the opt-in. **A background receiver cannot start an activity at all.**
   Only the system can, which is what a full-screen intent asks it to do.
3. Suspected the full-screen intent was ignored because the notification was handed to
   `startForeground`, so posted it via `notify()` first. No change.
4. Gave the full-screen notification its own id. No change, and it produced a visible duplicate
   notification — spotted by the owner, reverted.
5. Simplified the launch flags and `launchMode`. No change.

The answer came from the live notification record:

```
fullscreenIntent=PendingIntent{... (allowlist: +30s/NOTIFICATION_SERVICE/NotificationManagerService)}
flags=ONGOING_EVENT|NO_CLEAR|FOREGROUND_SERVICE|HIGH_PRIORITY|SILENT
```

The intent was attached and **already allowlisted for a background launch**. The blocker was
`SILENT` — our own `setSilent(true)`. A silent notification is not interruptive, and full-screen
intents are only launched from the interruptive path. The flag was redundant anyway, since the
channel already sets sound and vibration to null. Removing it produced
`BAL_ALLOW_NON_APP_VISIBLE_WINDOW [realCaller] ... result code=0` and the screen appeared.

**Silence belongs to the channel, never to the notification flag.**

### A misread that nearly became a bug

`dumpsys audio` logs `event:muted updated source:none` after every player starts, and
`AudioHardening background playback would be muted ... usage: USAGE_ALARM`. Both look like the alarm
being silenced. They are not: `source:none` is the mute-*source* set being updated to empty, and the
hardening line is advisory about a future policy. `STREAM_ALARM` reported `Muted: false` and the
owner confirmed the alarm was audible.

A workaround had already been written to restart the player once the screen was visible. It was
reverted — it fixed nothing and would have caused an audible stutter half a second into every alarm.
**Do not re-add it.**

### Other device findings

- **`setBypassDnd(true)` silently does nothing.** The channel reports `mBypassDnd=false`: bypassing
  Do Not Disturb needs notification-policy access, which an app cannot grant itself.
- **`USE_FULL_SCREEN_INTENT` is reset by `adb install -r`** and must be re-granted between test
  installs. On a real install the user grants it in app settings.
- This device hides notification content on the lock screen, so the notification fallback shows
  nothing there. Another reason the full-screen intent is the path that matters.
- A **debug-only** `DebugAlarmReceiver` (in `src/debug`, absent from release — verified) schedules an
  alarm over adb. Testing the ringing path needs a locked screen, and a locked screen cannot be
  driven over adb to set up the next attempt.

### Layout fixes found by using it

- **Timer and Stopwatch now use a compact header.** The hero band is ~40% of the screen, which left
  the dial fitting but its controls under the floating pill. Samsung keeps those two screens on a
  small title too.
- A first attempt blamed `fillMaxSize()` in the header's content slot. That was wrong — `Column`
  already measures later children against the remaining space, proven by reintroducing it and
  watching the test still pass. The comment was corrected rather than left to mislead.
- **The fixed header lost its title**, because the crossfade formula holds the small title at alpha 0
  when the collapse fraction is always 0.
- The stopwatch reserved no room for the pill when no laps existed.

### Icon and widget

- The launcher icon was a thin-stroke outline, which reads as dated once masked and scaled down.
  Redrawn twice: first as a filled face, then again after the owner pointed out the hands were
  misaligned. The first attempt hand-drew each slanted hand as a bespoke outline, and they came out
  asymmetric about their own axis — bent-looking, not meeting at the centre. **Hands are now
  symmetric vertical bars rotated into place by a group transform**, so the angles are arithmetic
  ((10 + 10/60) x 30 = 305 deg for the hour hand, 10 x 6 = 60 deg for the minute) and they cannot
  drift off centre. Proportions matter at launcher size too: the hour hand is deliberately much
  shorter and thicker than the minute hand, or the icon reads as a smudge once masked.
- Verified by rendering it on the device and looking at it, via the system App info page, rather
  than by trusting the path arithmetic. Worth doing — the misalignment was invisible in the source.
- **A live app icon is not possible.** Launcher icons are static drawables and no API lets an app
  animate one or feed it the time. Samsung's live clock icon is a feature of Samsung's *launcher*
  applied to its own built-in clock; Pixel Launcher does the same for Google Clock.
- Instead there is now a **live analog clock widget**, registered and confirmed present in the
  system's provider list. It uses the platform `AnalogClock`, ticked by the framework, so it stays
  accurate with **no periodic wake-ups at all** — `updatePeriodMillis=0`, no alarms, no jobs.
- It carries a **sweeping second hand** on API 31+, from `res/layout-v31`, which is what makes it
  visibly live. The base layout omits `android:hand_second` so an older device still inflates a
  correct hour/minute clock instead of failing on an attribute it does not know.
- Widget hand drawables are square, and a test asserts it: `AnalogClock` spins each one about the
  drawable's own centre, so a non-square hand would wobble as it went round.
- [ ] **Not yet seen on a home screen.** A widget cannot be placed over adb, so this needs the
      owner to add it once.

### Still unverified on hardware

- [x] `[device]` The ringing screen over a locked screen, with the display lighting itself
- [x] `[device]` Dismiss by swipe: activity closes, service stops, player released
- [ ] Snooze from the ringing screen, and both actions from the notification
- [ ] The timer's full-screen alert (built, mirrors the alarm's, not yet seen fire)
- [ ] Alarms surviving a real reboot
- [ ] Behaviour under Do Not Disturb and battery saver
- [ ] A timer finishing while the app is closed
- [ ] Process death with a stopwatch running

### Visual questions the device settled

- The expanded header, gradient hero and floating pill all read correctly at 411dp wide.
- **Still open:** whether One UI 8.5's Clock really has a collapsing header, centred-vs-leading
  title, the 39.67% proportion, and the accent hex. Those need a Samsung device to compare
  against, not a Pixel.

## Design decisions

### Typography substitution

One UI's real typefaces (SamsungOne, and the geometric numerals of SamsungSharpSans) are
proprietary and cannot be shipped. The app therefore uses the platform sans (Roboto on Pixel)
routed through a single `ClockFontFamily` declaration, with One UI-like weights, sizes and
letter spacing, and `fontFeatureSettings = "tnum"` on every numeric readout so digits do not
jitter as they change. Swapping in a bundled open font later is a one-line change.
**This is the largest deliberate visual deviation in the project.**

### No DI framework

The object graph is small, so it is wired by hand in a single container rather than with Hilt.
Fewer moving parts, no annotation processing beyond Room, and the graph stays readable.

