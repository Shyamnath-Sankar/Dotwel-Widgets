# Build and verification state

- Candidate: Dotwel Widgets 0.1.8 (versionCode 9), debug only. Package name
  `com.dotwelwidgets.dotwellabs` is unchanged.
- Branch: `agent/digital3-helper-45217649073`, based on `ce464a0`.

## Compiler fix run (ce464a0)

- Restored the original `BaseWidgetProvider.buildBitmapViews` signature
  (`render` then a trailing/default `decorate: (RemoteViews, Boolean) -> Unit`).
  The target-aware `decorateWithHost` optional Function3 parameter that broke
  trailing-lambda binding in the existing providers was removed.
- Added a distinct `buildTargetBitmapViews(..., decorateTarget)` helper used
  only by `KwgtDigital3WidgetProvider`; it is not an overload of
  `buildBitmapViews`, so no ambiguity. The other 13 provider files were not
  edited.
- Moved `KwgtDigital3Renderer`'s `MinuteRule` type alias from a nested class
  member to file top level; no experimental compiler flag added.
- `KwgtDigital3RendererLayoutTest` (including the source `joint` NEVER hierarchy
  and minute-threshold assertions) and the diagnosed real-host vs scanned-raster
  test remain unchanged and pass.
- Scope of this run: repair the incomplete KWGT `Nothing_Digital3` import
  without restarting the implementation or rewriting existing providers,
  renderers, layouts or tests.

## Repaired

- `app/src/main/AndroidManifest.xml` line 179 was malformed: the
  `android.intent.action.DATE_CHANGED` action on the Digital3 receiver was
  missing `android:name="`. Fixed; all 50 `app/src/main/**/*.xml` files now
  parse with Python `xml.etree.ElementTree`.
- `catalog_kwgt_digital3_desc` in `res/values/strings.xml` was interpreted by
  lint as a format string because of its literal percentages. Marked
  `formatted="false"` so the displayed copy is unchanged.
- `KwgtDigital3Renderer` used `android.graphics.Color` static helpers, which
  local JVM tests cannot mock. Replaced them with equivalent ARGB bit
  operations; colours and alphas are unchanged. The Digital3 layout test now
  reads alpha with bit operations too.

## Digital3 wiring confirmed

- Real 14th provider and 8th KWGT source port. Manifest receiver
  `KwgtDigital3WidgetProvider` registers the appwidget update, time, timezone,
  date and boot actions plus `@xml/kwgt_digital3_widget_info`.
- Catalog entry `kwgt_nothing_digital3` is READY with `PreviewKind.KWGT_DIGITAL_3`,
  `KwgtDigital3WidgetProvider` and `PIN_KIND_KWGT_DIGITAL_3`; it is one of the
  seven CLOCK variants. The other 13 providers were left in place.
- Preview: `WidgetPreviewRenderer` and `WidgetPreviewBinder` call the Digital3
  renderer; without access the preview uses the time-free dot matrix under live
  `TextClock` labels.
- Add/native pin: `MainActivity` routes the entry through the existing
  request-pin flow and the Digital3 access dialog; `PinResultReceiver` shows the
  Digital3 confirmation string.
- Updates: `WidgetUpdater.providers` and
  `MinuteTickScheduler.MINUTE_PROVIDERS` both include Digital3. The provider
  reschedules on update, resize, enable/disable/delete, minute tick,
  time/timezone/date change and boot. Tap uses
  `AlarmClock.ACTION_SHOW_ALARMS`.
- Access on draws the full bitmap from the existing minute scheduler. Access off
  draws `renderDots()` (surface plus minute-lit dots, no baked-in time text)
  behind three live platform `TextClock` views for hh, mm and aa, so no frozen
  bitmap time is presented as live. The minute grid itself is a static fallback
  between widget updates and is described that way in the UI copy.
- The renderer follows `nothing_digital3.sanitized.json`: `side / 5` corners,
  4x4 `side * 48 / 277` circles at `side * 12 / 277` margins, `side / 8.5`
  right and `side / 21.875` up offset, `#FFD71921` primary hh/mm cells,
  uppercase aa, accent at 35% alpha, accent2 at 15% alpha, and minute
  thresholds `mm > 9`, `> 29`, `> 39`, `> 49`. App-owned `DotGlyphFont` dots
  replace `ndot-47.ttf`.
- No new permission, service, dependency or font payload. The existing
  `SCHEDULE_EXACT_ALARM` permission is unchanged. The sanitized Digital3 JSON
  and MIT attribution remain, with the ledger/attribution rows updated.

## Verified here

```
python3 ElementTree parse of app/src/main/**/*.xml   -> 50 files, 0 errors
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

Result: `BUILD SUCCESSFUL`; 33 unit/layout tests passed (0 failures); lint
`0 errors, 112 warnings`. The manifest registers 14 widget providers, of which
8 are KWGT source ports. The fresh debug APK is
`app/build/outputs/apk/debug/app-debug.apk`; CI uploads it on success. The stale
build output APK was deleted before this build; older checkpoint APKs were left
untouched. The APK is build-verified only, not runtime-verified.

## Not verified

- No Android host was available in this run. Placement, resize, style switching,
  minute advance, process-kill recovery, boot/timezone refresh, tap behaviour and
  visual output for Digital3 are not runtime-verified. No screenshot or runtime
  pass is claimed.
- The KWGT collection is still incomplete: 8 of 40 presets are implemented; the
  remaining 32 stay pending or dependency-blocked and are not claimed as
  imported.
- Weather, Photo, Away, Compass, Snake, media/notification, CPU and iOS
  Komponent presets remain pending or blocked.
- Play policy review, signing-key setup, repository visibility changes, launcher
  integration and default-main promotion remain outside this checkpoint.
