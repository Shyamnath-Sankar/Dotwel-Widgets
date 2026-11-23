# Build and verification state

- Candidate: Dotwel Widgets 0.1.7 (versionCode 8), debug only. Package name
  `com.dotwelwidgets.dotwellabs` is unchanged.
- Branch: `agent/kwgt-spacing-45147146140`, based on `7e14ff2`. The separate
  launcher repository, default main, repository visibility and signing setup
  are unchanged.
- Scope of this run: the KWGT Date/Time stack-collision repair only. The
  existing category grid/sheet/swipe wiring, the nine registered providers
  (including KWGT Date/Time) and the earlier DateTime copy fix are preserved.

## Upstream source pins

- AwesomeAWidgets: `0fae60722764bda790f89bd94119c6dab854d90e` (MIT,
  Copyright (c) 2026 Sameer Al Sahab).
- KWGT-Widgets: `96ca27be4cbff35efec5a86c64dd39fd065c958f` (MIT,
  Copyright (c) 2024 Om Gupta).
- Adapted classes/functions and the 40-preset ledger remain documented in
  `docs/ATTRIBUTION.md` and `docs/KWGT-LEDGER.md`.

## What changed in this repair

`KwgtDateTimeRenderer.kt` was the only production file changed.

- Root cause: the source preset stack margin `-4/415 * side` was copied into a
  renderer whose Dotwel 5x7 glyphs visibly fill the line box. The real drawn
  dot extent is `6.8 * pitch` (six row-centre pitches plus a `0.4 * pitch` dot
  radius at each end), not `7 * pitch`. The negative margin therefore made
  styled four-row output physically overlap.
- Fix: rows are now laid out from the actual `6.8 * pitch` drawn bound with a
  positive clear gap. The gap is `max(side * 0.045, 1.5 * min(pitch_i,
  pitch_i+1))`, so it scales with the smaller row instead of collapsing.
- The complete stack is fitted inside the host height with top/bottom padding
  (`side * 0.04`). If the natural stack is too tall, all pitches are scaled
  uniformly; width fitting is retained, so long `NOVEMBER`/`SEPTEMBER` rows
  still fit.
- Left alignment for weekday, red time, month and ordinal day is unchanged.
- No glyph metrics or other renderers changed. No source font was copied.
- The provenance comment now states explicitly that the positive gap is a
  deliberate legibility adaptation for Dotwel's dot glyphs, not pixel-exact
  upstream KWGT geometry.

No permission, provider registration, UI flow or runtime dependency changed.
The only new build dependency is test-only JUnit 4.13.2 for the layout math
assertions.

## Unit tests

Added `app/src/test/java/com/dotwelwidgets/dotwellabs/KwgtDateTimeRendererLayoutTest.kt`.
It calls the renderer's pure `fitRows` math and asserts:

- at 200x200, 300x400 and 400x200 every row has positive pitch, positive
  inter-row gap, stays within the widget height, and does not overlap the next
  row;
- long month names `NOVEMBER` and `SEPTEMBER` fit inside the widget width at
  those same sizes.

`./gradlew testDebugUnitTest --no-daemon` passed.

## Build and lint in this run

Commands run after the final edits:

```
./gradlew testDebugUnitTest --no-daemon
./gradlew assembleDebug lintDebug --no-daemon
```

Both were `BUILD SUCCESSFUL`. `lintDebug` completed with `0 errors, 93 warnings`
and no lint suppression was added or changed. The warnings are existing
SDK/AGP, API-gate, legacy-resource, plural, small-text and static-context
notices.

The APK identifies itself as `versionCode=8`, `versionName=0.1.7`,
`minSdk=26`, `targetSdk=36`, `compileSdk=36`.

## Artifact

Final debug APK: `checkpoints/dotwel-widgets-datetime-layout-debug.apk`.
SHA-256 recorded in `checkpoints/DATETIME-LAYOUT-SHA256SUMS`:

```
9b7333eb2463fe5396362455c39bd1285d175333ce8d68dcfdbf91990b1be5e8  checkpoints/dotwel-widgets-datetime-layout-debug.apk
```

The older `checkpoints/dotwel-widgets-polish-debug.apk` was left untouched.

## Verified versus not verified

Verified here: the source compiles, the debug APK assembles, lint has no errors,
and the pure layout assertions pass for non-overlap, height containment and
long-month width fit at the stated sizes.

Not verified here:

- actual pixels on an Android host after this change, including the styled
  four-row date/time widget at the real 2x2 tall host size;
- screenshot or HOME-placement evidence for this repair.

An independent prior run proved the live/styled minute advance after process
kill, but it also observed the collision this edit targets. This pass makes no
runtime or screenshot claim. The repaired spacing still needs independent
actual-device or emulator pixel verification before it is called complete.

## Limits and remaining work

- The collection remains incomplete: 17 in-app entries are Upcoming and 37 of
  the 40 KWGT presets are still pending or dependency-blocked.
- Weather, Photo, Away, Compass, Snake, media/notification, CPU and iOS
  Komponent presets remain in their prior blocked or pending states.
- The OEM OnePlus entries remain excluded for redistribution reasons; the
  NothingWidgetPort remains excluded.
- Play policy review, signing-key setup, repository visibility changes, launcher
  integration and default-main promotion remain outside this checkpoint.
