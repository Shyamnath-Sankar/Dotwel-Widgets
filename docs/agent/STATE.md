# Build and verification state

- Candidate: Dotwel Widgets 0.1.7 (versionCode 8). Package name is unchanged.
- Branch: `agent/gallery-refine-45125972018`, preserving the buildable
  candidate commit `6d6b40d`.
- The full catalog still contains 25 app entries (eight available providers and
  17 upcoming entries) and the separate 40-preset KWGT ledger. This run did not
  add widgets or redefine the collection as eight items only.
- This is a debug candidate, not a Play-approved or production-ready release.
  No runtime or screenshot verification is claimed for this run.
- Default main and the separate launcher repository are unchanged.

## Upstream source pins

- AwesomeAWidgets: `0fae60722764bda790f89bd94119c6dab854d90e` (MIT,
  Copyright (c) 2026 Sameer Al Sahab).
- KWGT-Widgets: `96ca27be4cbff35efec5a86c64dd39fd065c958f` (MIT,
  Copyright (c) 2024 Om Gupta).
- The eight registered providers and the two KWGT preset conversions are
  retained. See `docs/ATTRIBUTION.md` for the exact adapted classes/functions
  and `docs/KWGT-LEDGER.md` for the honest 40-preset status.

## What changed in this refinement

### Gallery front screen

- Replaced the developer-facing card stack with a compact visual gallery. The
  first available preview and its Add action now appear under a short title,
  subtitle, style chips, and one compact minute-access control.
- Removed upstream class/method names and long implementation explanations from
  the gallery cards. This provenance now lives in an in-app About & source
  credits dialog. The dialog preserves the adapted-file mapping and links to
  bundled MIT license text for both upstream projects.
- Renamed the implementation-state chip from `Ready` to `Available`; detail text
  continues to say runtime is not verified here.
- Moved the 17 pending, blocked, and unavailable catalog items into a collapsed
  Upcoming section. Expanding it shows compact rows with the group, status, and
  the actual remaining-work reason instead of large dashed preview cards.
- Kept 48dp minimum touch targets, readable muted text contrast, natural
  ScrollView scrolling, and wrap-friendly text for normal and enlarged fonts.

### Preview mode correctness

- Clock and analog previews no longer render a dotted bitmap when optional
  Alarms & reminders access is off. In that mode the gallery shows the actual
  live native `TextClock` or platform `AnalogClock` fallback used by the placed
  widget, with the same style surface and ink colour.
- With access enabled, the gallery uses the same dotted/styled bitmap renderers
  as the widget. A guard in `WidgetPreviewRenderer` also returns no bitmap for
  those kinds when access is unavailable.
- Returning from Android settings refreshes the mode immediately on
  `onResume`; the permission-change broadcast also refreshes visible previews.
- A renderer failure falls back to the live native view and reports the failure,
  rather than leaving a stale dotted bitmap or claiming dotted output is ready.
- Every preview carries a `Preview` badge that also names the current mode.

### Lifecycle and memory hazards

- Gallery previews render only when visible or near-visible, with a bounded
  preview pixel budget and a cached key so scrolling does not re-render
  unchanged cards.
- Removed the recursive `frame.post` retry path. A one-shot layout listener now
  handles a zero-size frame and checks `isFinishing`/`isDestroyed`.
- Removed bitmap recycling on replacement and destruction; a bitmap that may
  still be drawn is no longer recycled.
- Main-thread handler callbacks are removed on destruction. No runtime claim is
  made about the Pixel Launcher ANR; this correction addresses only the
  lifecycle and allocation hazards visible in this source.
- Native pin/cancel/retry, corrected host sizing, world per-instance
  configuration, and battery live-data paths were left unchanged.

## Build and lint in this run

Commands run after the final source/resource edits:

```
./gradlew assembleDebug lintDebug --no-daemon
```

Result: `BUILD SUCCESSFUL`. `lintDebug` completed with 0 errors and 49 warnings.
No lint suppression was added. The warnings are the existing SDK/AGP update
notices, API-gated widget-info attributes, unused legacy strings/colors, format
plurals suggestions, two SmallSp text-size suggestions, and the unregistered
partial weather cache's static-context warnings. No weather provider is wired.

The APK identifies itself as `versionCode=8`, `versionName=0.1.7`,
`minSdk=26`, `targetSdk=36`, `compileSdk=36`.

## Artifact

The canonical debug APK was rebuilt after the final source/resource edits and
saved as `checkpoints/dotwel-widgets-polish-debug.apk`. Its checksum is recorded
in `checkpoints/POLISH-SHA256SUMS`; this state file deliberately does not
hardcode an APK hash because the deterministic final workflow can rebuild
different bytes.

## What is verified versus not verified

Verified in this run: the debug APK builds, lint has no errors, the packaged
package/version/SDK values are as above, and the source changes above are
present.

Not verified in this run:

- any emulator or physical-device launch, screenshot, or widget-host placement;
- clock/analog preview switching after granting or revoking Alarms & reminders
  access, including the return-from-settings refresh;
- preview rendering at 130% font scale and natural scrolling on a real host;
- launcher pin confirmation, cancellation, retry, and the unsupported-pin
  fallback dialog;
- battery gauge pixel output and refresh timing across launchers/doze;
- multiple simultaneous World pin requests and the 10-minute token pruning;
- style persistence across an upgrade from an older install.

## Limits and remaining work

- The collection remains incomplete: 17 in-app entries are in Upcoming and 38
  of the 40 KWGT presets are still pending or dependency-blocked. No finished
  import is claimed.
- Weather: partial domain/transport only, no nine layouts and no registered
  provider. Keep pending.
- PHOTO and AWAY remain pending configuration/background-policy work.
- Compass and Snake remain blocked by the no-service/sensor policy.
- Media/notification, CPU, and iOS Komponent presets remain dependency-blocked.
- The OEM OnePlus entries remain unavailable for redistribution reasons.
- Play policy review, signing-key setup, repository visibility changes, launcher
  integration, and default-main promotion remain outside this checkpoint.
