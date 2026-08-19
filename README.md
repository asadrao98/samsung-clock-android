# Samsung One UI 8.5 Clock, rebuilt for Google Pixel

A native Android clock app that reproduces the **look, layout, motion and behaviour** of Samsung
Clock on One UI 8.5, written in Kotlin and Jetpack Compose for stock Android.

Fully offline and fully local. No Samsung SDKs, no Samsung services, no cloud, no account, no
analytics — and **no network permission of any kind**, verified against the built APK and against
the OS's own permission records on a device.

## What it does

| | |
|---|---|
| **Alarm** | One grouped card of alarms, custom drum time picker, repeat days, labels, ringtone picker, vibration, snooze, multi-select |
| **Ringing** | Full-screen alert over the lock screen, screen wakes itself, swipe to dismiss, snooze with a budget that survives process death |
| **World clock** | Offline city search over the platform's own time-zone database, live DST-correct offsets, day/night |
| **Stopwatch** | Monotonic timing, analog/digital hybrid dial, laps with splits |
| **Timer** | Multiple concurrent timers, quick durations ordered by recent use, presets, full-screen alert |
| **Widget** | Live analog clock for the home screen, ticked by the framework with zero wake-ups |
| **Settings** | Theme, default snooze, timer vibration — and nothing that does not change something real |

## This is a clean-room recreation

No Samsung code is decompiled or copied and no proprietary Samsung asset ships here. Every icon is
original vector artwork. Samsung's typefaces (SamsungOne, SamsungSharpSans) are proprietary, so the
app uses the platform face with One UI-like weights and metrics — the largest deliberate visual
deviation, and it is recorded as such.

Decompiled Samsung resources were available while researching and were **deliberately excluded**;
no value in this repository was transcribed from them. `docs/ONEUI_DESIGN_SPEC.md` records where
each design decision came from, and how confident it is.

## Honest status

`DEVELOPMENT_CHECKLIST.md` is the real state of the project, marked per claim:

- `[build]` compiles / packages
- `[test]` covered by a passing automated test
- `[device]` observed working on real hardware
- `[unverified]` written but not yet proven

**240 JVM tests passing, Android Lint 0 errors**, debug and release both assemble. The alarm path
is verified on a Pixel 8 (Android 17): scheduling through Doze, the full-screen alert over a locked
screen, sound, and dismissal.

Still open, and listed in the checklist rather than glossed over: reboot survival, Do Not Disturb
and battery-saver behaviour, the timer's alert firing on hardware, and a visual comparison against
a real One UI 8.5 device — which a Pixel cannot settle.

The checklist also records the bugs that only a device found, including the one-line cause of the
lock-screen alarm never appearing, and the five wrong guesses that preceded it.

## Build

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew :app:assembleDebug
```

The `JAVA_HOME` prefix is required on a machine with no system Java: `org.gradle.java.home` tells
the Gradle daemon which JDK to compile with, but the `gradlew` launcher needs a JVM of its own.

| | |
|---|---|
| JDK | 17 |
| Gradle / AGP / Kotlin | 9.3.1 / 8.12.0 / 2.1.20 |
| compileSdk / targetSdk / minSdk | 36 / 36 / 26 |

Tests: `./gradlew :app:testDebugUnitTest`. They run on the JVM under Robolectric, pinned to SDK 35
because the API 36 `android-all` jar needs Java 21 — so anything whose behaviour genuinely differs
on API 36 has to be checked on a device.

## A note on full-screen alarms

For the alarm to appear over the lock screen, Android 14+ requires the **"Allow full-screen
notifications"** permission. It is granted automatically only to apps Google Play has classified as
alarm or calling apps, so a sideloaded build must be granted it by hand in the app's notification
settings. Without it the alarm still rings and still posts a notification carrying Snooze and
Dismiss — it just cannot take over the screen.
