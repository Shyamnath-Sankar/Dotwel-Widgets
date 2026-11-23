# Build and verification state

- Candidate: Dotwel Widgets 0.1.8 (versionCode 9), debug only. Package name
  `com.dotwelwidgets.dotwellabs` is unchanged.
- Branch: `agent/gradient-wire-45185550298`, based on `130e1dd`.
- Scope of this run: finish the incomplete KWGT `Nothing_Digital` gradient-clock
  import without rewriting the existing renderer, layouts, XML, or tests.
  Preserve every previously wired provider.

## Wiring added

- New `KwgtGradientClockWidgetProvider` modelled on the Digital4 provider:
  `KwgtGradientClockRenderer.render()` for the styled bitmap and
  `renderStaticBackground()` behind live `TextClock` hh/mm when optional
  exact-alarm access is absent. It uses the existing
  `widget_kwgt_gradient_clock.xml` ids and host-size geometry, supports resize,
  shares `MinuteTickScheduler`, handles minute ticks plus
  time/timezone/date/boot broadcasts, and opens `ACTION_SHOW_ALARMS` on tap.
- Added the `kwgt_gradient_clock` catalog entry as the sixth CLOCK variant and
  seventh KWGT provider, with Add/pin wiring, `WidgetUpdater`, and
  `PinResultReceiver` confirmation text.
- Added the manifest receiver and the missing strings referenced by the existing
  layout and widget-info XML (`kwgt_gradient_preview_description`,
  `widget_kwgt_gradient_description`, plus content descriptions, label, catalog,
  pin and access-dialog copy).
- `MainActivity` now recognises `PreviewKind.KWGT_GRADIENT_CLOCK` for the alarm
  access dialog, so its wording matches a clock rather than a date/time widget.
- Category previews now render the actual `KwgtGradientClockRenderer` output.
  Without optional exact-alarm access the preview fallback draws the same static
  gradient surface under live `TextClock` hh/mm, matching the placed widget.
- Added `app/src/main/assets/kwgt/nothing_digital.sanitized.json` and the
  Nothing_Digital MIT provenance in `docs/ATTRIBUTION.md`. Updated the
  `docs/KWGT-LEDGER.md` rows for `Nothing_Digital` and `Nothing_Digital4`.

## Verified here

```
./gradlew assembleDebug testDebugUnitTest --no-daemon
./gradlew lintDebug --no-daemon
```

Result: `BUILD SUCCESSFUL`; all unit tests pass; lint `0 errors, 106 warnings`.
The manifest now registers 13 widget providers, of which 7 are KWGT source
ports: Battery, Battery Mini, Date Time, Date Time 2, Split Clock, Gradient
Clock, and Mini Calendar. The existing renderer, layouts, XML and renderer tests
were not rewritten.

Debug APK: `checkpoints/dotwel-widgets-gradient-clock-debug.apk`.
SHA-256 in `checkpoints/GRADIENT-CLOCK-SHA256SUMS`:

```
41f269bd829110b01fe464983bafb0f784763179fb42652d83a3fa416dc784d8  checkpoints/dotwel-widgets-gradient-clock-debug.apk
```

Older checkpoint APKs were left untouched.

## Not verified

- No Android host was available in this run. Native pin preview, HOME placement,
  resize, style switching, minute advance, boot/timezone refresh, and tap
  behaviour for the gradient clock are not runtime-verified. No screenshot or
  runtime pass is claimed.
- The KWGT collection is still incomplete: 7 of 40 presets are implemented; the
  rest remain pending or dependency-blocked and are not claimed as imported.
- Weather, Photo, Away, Compass, Snake, media/notification, CPU and iOS
  Komponent presets remain pending or blocked.
- Play policy review, signing-key setup, repository visibility changes, launcher
  integration and default-main promotion remain outside this checkpoint.
