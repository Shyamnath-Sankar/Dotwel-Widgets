# Build and verification state

- Candidate: Dotwel Widgets 0.1.7 (versionCode 8), debug only. Package name
  `com.dotwelwidgets.dotwellabs` is unchanged.
- Branch: `agent/kwgt-date2-45154407260`, based on `76cf4fa`. The separate
  launcher repository, default main, repository visibility and signing setup
  are unchanged.
- Scope of this run: add one real native provider for the upstream
  `Nothing_Date_Time2.kwgt` preset. The nine previously registered providers,
  category grid, sheets, selected-Add flow and positive four-row spacing were
  not redesigned or rewritten.

## What changed

New files:

- `KwgtDateTime2Renderer.kt` converts the source layer composition: rounded
  side/5 surface, upper-left EEE weekday over the side/4 red day, lower-right
  hour and minute rows, side/5 discs, contrasting radial notches, adjacent
  `hh`/`mm` text, smooth hour rotation and minute rotation.
- `KwgtDateTime2WidgetProvider.kt` registers the native provider, uses the
  shared `MinuteTickScheduler` only on the bitmap path, handles time/date/
  timezone/resize refresh, and routes date-area taps to the calendar, time-area
  taps to the clock, with the whole widget opening the app as fallback.
- `widget_kwgt_datetime2.xml` and `kwgt_datetime2_widget_info.xml` provide the
  bitmap view, live access-off composition, click regions and 2x2 resizable
  provider metadata.
- `assets/kwgt/nothing_date_time2.sanitized.json` retains sanitized geometry and
  formulas only. Source font payloads and tap intents are removed; MIT
  provenance is retained.

Wiring updates:

- `WidgetCatalog.kt` adds `KWGT Date Time 2` to the Calendar category as a
  separate ready variant and adds `PreviewKind.KWGT_DATE_TIME_2`.
- `WidgetPreviewRenderer.kt` and `WidgetPreviewBinder.kt` render the actual
  bitmap when optional exact-alarm access is on, and a live `TextClock`
  composition without analog discs when access is off.
- `MinuteTickScheduler.kt`, `WidgetUpdater.kt`, `PinResultReceiver.kt`,
  `AndroidManifest.xml`, `MainActivity.kt` and `strings.xml` register the
  provider, pin result, preview/add path and access-off dialog.

Provider counts are now 10 ready in-app entries and 4 implemented of the 40
KWGT presets. The remaining 36 stay pending or dependency-blocked. README,
`docs/ATTRIBUTION.md` and `docs/KWGT-LEDGER.md` were updated to match.

## Preserved source geometry

Retained from the preset: `side = min(width, height)`, corners `side / 5`,
left offset `side / 8.5`, weekday/hour/minute text `side / 10`, red day
`side / 4`, clock group left padding `side / 2.5`, disc diameter `side / 5`,
hour rotation `(hour + minute / 60) * 30`, minute rotation `minute * 6`, notch
width `side / 69.25`, hour notch length `side / 28` with `side / 6.1` offset,
minute notch length `side / 12` with `side / 8.5` offset, and primary red
`#FFD71921`.

Font substitution: the source NType82 regular/headline files are not copied.
The renderer uses Android platform serif regular and bold with a width fit
guard, documented in the renderer and attribution.

## Access behavior

- Exact-alarm access on: the provider draws the styled minute bitmap and the
  shared scheduler refreshes it once a minute, including after boot/app update,
  timezone/date changes and resize callbacks.
- Exact-alarm access off: the placed widget shows live platform `TextClock`
  values for weekday, day, hour and minute in the matching regions. It does not
  draw analog discs at all, so no frozen hand/minute can be mistaken for live
  time. The app's existing access dialog is shown when adding the widget.
- No new permission, service, dependency, launcher component or paid path was
  added. No `NothingWidgetPort` code was used.

## Build and lint in this run

```
./gradlew assembleDebug lintDebug --no-daemon
./gradlew testDebugUnitTest lintDebug --no-daemon
```

Both builds completed `BUILD SUCCESSFUL`. Final lint result: `0 errors,
95 warnings`, with no lint suppression added or changed. Warnings are the
pre-existing SDK/AGP, API-gate, legacy-resource, plural, small-text and
static-context notices plus the same API-gate warnings for the new widget info
XML.

The merged manifest contains
`com.dotwelwidgets.dotwellabs.KwgtDateTime2WidgetProvider`, and the APK contains
the new layout, provider metadata and sanitized asset.

## Artifact

Final debug APK: `checkpoints/dotwel-widgets-datetime2-debug.apk`.
SHA-256 in `checkpoints/DATETIME2-SHA256SUMS`:

```
28bb07666f05eb7f229f589c7ffdac81a72365020f72b63afe810dfe8a1fe54e  checkpoints/dotwel-widgets-datetime2-debug.apk
```

Older checkpoint APKs and checksum files were left untouched.

## Verified versus not verified

Verified here: source compiles, debug APK assembles, unit tests pass, lint has
no errors, the new receiver and resources are packaged, and source inspection
confirms access-on bitmap updates, access-off live TextClock fallback, click
regions and scheduler/updater registration.

Not verified here: actual Android-host placement, screenshots, resize, style
switching, calendar/clock tap targets, or minute advance for this new provider.
No runtime or screenshot pass is claimed for `KWGT Date Time 2`.

## Limits and remaining work

- The collection remains incomplete: 36 of the 40 KWGT presets are still
  pending or dependency-blocked.
- Weather, Photo, Away, Compass, Snake, media/notification, CPU and iOS
  Komponent presets remain in their prior blocked or pending states.
- OEM OnePlus entries and the NothingWidgetPort remain excluded.
- Play policy review, signing-key setup, repository visibility changes, launcher
  integration and default-main promotion remain outside this checkpoint.
