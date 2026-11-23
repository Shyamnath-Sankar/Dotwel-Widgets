# Build and verification state

- Candidate: Dotwel Widgets 0.1.8 (versionCode 9), debug only. Package name
  `com.dotwelwidgets.dotwellabs` is unchanged.
- Branch: `agent/battery2-refresh-45251301569`, based on `5b733ea`.
- No launcher repository, visibility, workflow, signing key, permission,
  dependency or provider rendering was changed in this run.

## Battery refresh lifecycle reliability (from 5b733ea)

- Added `BatteryRefreshScheduler`: a non-wakeup `ELAPSED_REALTIME`
  `setWindow` (fallback `set`) at `now + 15 minutes` with a 10-minute delivery
  window. This is deliberately inexact, needs no exact-alarm permission and may
  be deferred by Doze; the documented bounded best effort is therefore up to
  roughly 25 minutes, not a hard 15-minute SLA.
- `BatteryRefreshReceiver` is an explicit internal receiver with
  `android:exported="false"`, `FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE` and only
  matches `com.dotwelwidgets.dotwellabs.action.BATTERY_REFRESH`. It refreshes
  only the battery-capable providers and re-arms the next window.
- Scheduling scope is `BatteryWidgetProvider`, `BatteryMiniWidgetProvider`,
  `KwgtBattery2WidgetProvider` and `KwgtCleanInfoWidgetProvider`. The alarm is
  cancelled when the last of those is removed; the other 12 providers are not
  touched and no minute polling was added.
- `DotwelApp` (registered via `android:name`) re-arms the alarm on process
  start and keeps a process-lifetime dynamic `ACTION_BATTERY_CHANGED`
  registration that refreshes only the four battery providers on level/status
  changes. It compares the percent and charge status and skips property-only
  deliveries, and never re-registers from the callback, so neither the sticky
  broadcast nor frequent temperature/voltage deliveries can loop or short-poll.
- Re-arm also runs after the refresh tick, provider update/options, pin and
  manual refresh (`WidgetUpdater.updateAll`). The existing manifest
  `BATTERY_CHANGED` entries may remain but are no longer relied on after a
  process kill.
- No foreground service, wake lock, new permission, new dependency or new
  widget was added. The percentage, 16-segment Battery2 rendering and
  charging/unknown estimate behaviour are unchanged.
- Gallery copy now says "Best-effort level..." instead of "Live level..." and
  the provider/catalog/README/ledger text states the bounded, deferrable
  refresh plus manual app refresh. No live or instant background claim remains.

### Verified in this run

```
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Result: `BUILD SUCCESSFUL`; 48 unit/layout tests passed (0 failures, 0 errors,
0 skipped), including 3 new `BatteryRefreshSchedulerTest` lifecycle assertions;
lint 125 warnings, 0 errors. XML parse: 54 files, 0 errors. Debug APK:
`app/build/outputs/apk/debug/app-debug.apk`,
sha256 `7484661ca77062a8b175ccf8a86f5c6b5c125c88a365a376f4269f078a317684`.

### Not verified in this run

- No Android emulator binary or system image is installed in this runner (only
  `adb`), and no device is attached, so app-resident `BATTERY_CHANGED`
  73 -> 42 -> 100 delivery, process-kill recovery through the real inexact
  alarm, the scheduled PendingIntent inspection, and the up-to-25-minute
  deferred delivery are **not** runtime-verified here and are not claimed.
- Do not claim immediate background refresh after an app kill. The next runtime
  check should kill the process after a known level change and wait for the
  system alarm (up to ~25 minutes) rather than triggering a manual refresh.

## KWGT Nothing_Battery2 native port

- Added the 16th provider and 10th KWGT source port,
  `KwgtBattery2WidgetProvider`, as a distinct catalog entry `KWGT Battery2`
  (`kwgt_nothing_battery2`) under the existing battery category. No alias of
  `BatteryWidgetProvider` or `BatteryMiniWidgetProvider`; their files and
  behaviour are unchanged.
- Source: pinned `Nothing_Battery2.kwgt!preset.json` at
  `96ca27be4cbff35efec5a86c64dd39fd065c958f`. Actual canvas 415x542, dark
  `#FF1B1B1D` / light white, opposite foreground, accent at 50% alpha, rounded
  corners `side / 5`, information offset `side / 8.5`, BATTERY label `side / 18`
  with 16 bottom padding, outlined body 195x78 corners 14 stroke 3.5, clip
  179x64 corners 7, 16 rectangular progress segments across 184x64, pill nub
  5x28 with a 5 margin, level `side / 4.5`, percent `side / 10`, bottom row
  `side / 15` with an 8 margin.
- Literal body/clip/progress/nub values are scaled from the author 277 design
  reference by `side / 277`; formula sizes use `side`. The source has no `h`
  global, so the background follows the host height and the whole stack is
  centred instead of reproducing the missing height.
- The source `ndot-47.ttf` is not used. Dotwel's existing original dot glyphs
  draw BATTERY and the large level; the long honest status uses platform
  sans-serif.
- Real level comes from the existing `BatteryState` path. Charging comes from
  `BatteryManager.isCharging`; a positive `computeChargeTimeRemaining()` is the
  only time value shown, labelled `TO FULL`. Unknown or discharging states show
  `--` and `ESTIMATE UNAVAILABLE`; 100% shows `FULL` only. No discharge estimate
  is fabricated.
- Updates: `APPWIDGET_UPDATE`, `BATTERY_CHANGED`, `POWER_CONNECTED`,
  `POWER_DISCONNECTED`, `BOOT_COMPLETED`, resize/options, app refresh and the
  existing 30-minute inexact widget period. No new permission, network, service,
  dependency, font or artwork.
- Tap tries `ACTION_POWER_USAGE_SUMMARY` and falls back to a known Settings
  screen when no activity resolves.
- Preview uses the same `KwgtBattery2Renderer`; Add/pin/provider-info, catalog
  count 16 and pin confirmation are wired. The sanitized asset is
  `app/src/main/assets/kwgt/nothing_battery2.sanitized.json` with email, events
  and font paths removed. Ledger now records 10 of 40 implemented and 16
  providers, all runtime-unverified.

## CleanInfo compatibility fixes

- `layout/widget_kwgt_clean_info.xml` used a raw `<View>` for
  `kwgt_clean_info_bg_fallback`, which RemoteViews does not support; a native
  host rejected the layout even when the view was `GONE`. It is now an
  `ImageView` with the same id, `match_parent` size, `fitXY` scale and
  `GONE` default. The layout no longer contains a raw `<View>`.
- The source readout is fixed 12-hour `h:mm`, but the layout's
  `format24Hour` was `H:mm`. It is now `h:mm` for both 12-hour and 24-hour
  devices, matching the source.
- All 14 existing providers, foreground conditional repair, platform font
  substitution, permissions, services and dependencies are unchanged. The
  battery `TextView` has padding only; its geometry was not altered.

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

## Verified here (Battery2)

```
python3 ElementTree parse of app/src/main/**/*.xml   -> 54 files, 0 errors
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Result: `BUILD SUCCESSFUL`; 45 unit/layout tests passed (0 failures, 0 errors),
including 5 new `KwgtBattery2RendererLayoutTest` geometry, 16-segment, level
clamp and unknown-estimate assertions; lint `0 errors, 125 warnings`. The debug
APK is `app/build/outputs/apk/debug/app-debug.apk` and is build-verified only,
not runtime-verified. No placement, resize, battery broadcast, tap destination,
segment pixels or charging-estimate runtime is claimed here.

## Verified here

```
python3 ElementTree parse of app/src/main/**/*.xml   -> 52 files, 0 errors
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Result: `BUILD SUCCESSFUL`; 40 unit/layout tests passed (0 failures, 0 errors);
lint `0 errors, 117 warnings`. The CleanInfo layout has 0 raw `<View>` elements
and both `format12Hour` and `format24Hour` are `h:mm`. The manifest registers 14
widget providers, of which 8 are KWGT source ports. The fresh debug APK is
`app/build/outputs/apk/debug/app-debug.apk`; CI uploads it on success. The APK is
build-verified only, not runtime-verified.

## Not verified

- No Android host was available in this run. Placement, resize, style switching,
  minute advance, process-kill recovery, boot/timezone refresh, tap behaviour and
  visual output for CleanInfo (and the other providers) are not runtime-verified.
  No screenshot or runtime pass is claimed.
- The KWGT collection is still incomplete: 10 of 40 presets are implemented; the
  remaining 30 stay pending or dependency-blocked and are not claimed as
  imported.
- Weather, Photo, Away, Compass, Snake, media/notification, CPU and iOS
  Komponent presets remain pending or blocked.
- Play policy review, signing-key setup, repository visibility changes, launcher
  integration and default-main promotion remain outside this checkpoint.
