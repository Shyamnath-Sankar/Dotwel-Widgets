# Build and verification state

- Candidate: Dotwel Widgets 0.1.7 (versionCode 8). Package name is unchanged.
- Branch: `agent/reference-ui-45135605275`, based on `3d3d637`. The separate
  launcher repository and default main remain unchanged.
- The catalog still contains the same 25 app entries: nine available providers
  and 17 upcoming entries, plus the separate 40-preset KWGT ledger. This pass
  added no provider and did not redefine the collection as eight items only.
- This is a debug candidate, not a Play-approved or production-ready release.
  No runtime, screenshot or HOME placement verification is claimed here.

## Upstream source pins

- AwesomeAWidgets: `0fae60722764bda790f89bd94119c6dab854d90e` (MIT,
  Copyright (c) 2026 Sameer Al Sahab).
- KWGT-Widgets: `96ca27be4cbff35efec5a86c64dd39fd065c958f` (MIT,
  Copyright (c) 2024 Om Gupta).
- The nine registered providers and the three KWGT preset conversions are
  retained. See `docs/ATTRIBUTION.md` for the adapted classes/functions and
  `docs/KWGT-LEDGER.md` for the 40-preset status.

## What changed in this refinement

### Gallery front screen

- Replaced the developer card stack with a compact two-column visual browser.
  Each card shows the widget name, the actual same-renderer preview, and an Add
  button directly underneath. Tapping the preview opens a full-width, larger
  preview dialog with the Add action and a short mode note.
- Added catalog-backed filters: All, Clocks, Date, Battery and Quotes. Counts
  are derived from the nine real available providers only. The collapsed
  Upcoming section still lists all 17 unfinished entries, so nothing is silently
  hidden. There is no fake active count.
- Added a small all-caps `DOTWEL` heading drawn with Dotwel's own
  `DotGlyphFont`, with a spoken content label, plus a code-drawn quietly dotted
  background. No proprietary image, font, icon or screenshot was copied.
- Added a floating rounded bottom navigation with Widgets, Style and About.
  Style opens the existing Black / Translucent / Transparent choice as a dialog.
  Scroll content has bottom inset padding so Add buttons and text are not hidden
  behind the floating navigation or system bar.
- The grid uses two columns only at normal font scale and sufficient width; at
  130% font scale or narrow width it falls back to one column. Filter chips,
  Add, secondary actions and navigation keep 48dp minimum touch targets.
- Replaced long card prose with short truthful mode labels such as
  `Live system clock`, `Dotted clock`, `Live analog clock`, `Live battery` and
  `Live battery rings`. Remaining implementation and rendering prose is in the
  About & source credits dialog, and the existing MIT provenance is retained.
- The minute-update control is a compact optional row. Available counts still
  say nine of the 25 app entries and do not imply the unfinished catalog is
  complete.

### Actual widget renderers

- Battery Gauge: the authored composition is now fitted proportionally to 92%
  of the short side, so the ring occupies about 66% of the short side instead of
  the literal `side / 2` and a 2x2 HOME surface is no longer mostly blank. The
  4/415 offset, `level * 3.6` arc, `cm(215,g,b)` colour and overture are retained.
  A faint track and disc keep the ring legible, and the real level continues to
  be drawn with Dotwel dot glyphs. No charge state or level is invented.
- Battery Mini: the author's eleven concentric rings and arc colour remain, and
  the renderer now adds a centered real-level dot readout for legibility. The
  source preset has no text layer; this is an explicit Dotwel addition, not
  fake state. Both previews and placed widgets call these same renderers.
- Digital, analog, date, world and quotes renderers were not rewritten in this
  pass. Their existing circular-dot composition, bold/scale distinction and date
  hierarchy remain. Gallery previews still use the placed-widget renderer or the
  truthful live platform fallback.

### Preserved behaviour

- Quotes per-child callback repair and the World denied-access native TextClock
  fallback are untouched.
- The digital native pin / cancel / access flow, optional Alarms & reminders
  handling, style persistence, per-instance World configuration and existing
  providers are untouched.
- No new permission, dependency, service, purchase, wallpaper feature, unlock
  banner, subscription, crown, fake system readout or background-access request
  was added.

## Build and lint in this run

Commands run after the final source, layout and resource edits:

```
./gradlew assembleDebug lintDebug --no-daemon
```

Result: `BUILD SUCCESSFUL`. `lintDebug` completed with 0 errors and 59 warnings.
No lint suppression was added. The warnings are existing SDK/AGP notices,
API-gated widget-info attributes, unused legacy strings/colors, plural
suggestions, small text-size suggestions, and the unregistered partial weather
cache's static-context warnings.

The APK identifies itself as `versionCode=8`, `versionName=0.1.7`,
`minSdk=26`, `targetSdk=36`, `compileSdk=36`.

## Artifact

The canonical debug APK was rebuilt after the final code/resource edits and
saved as `checkpoints/dotwel-widgets-polish-debug.apk`. Its SHA-256 is recorded
in `checkpoints/POLISH-SHA256SUMS`:

```
1f9100ac996f436cf1d4dc6f60f52df9ae2a197be9a6f99607ead724c9e8de5f
```

## What is verified versus not verified

Verified here: the debug APK builds, lint has no errors, and the source changes
above are present.

Not verified here:

- any emulator or physical-device launch, screenshot, or widget-host placement;
- the two-column/one-column switch at 100% and 130% font scale in actual pixels;
- bottom-navigation clearance, scrolling and contrast on a real device;
- battery gauge or Mini readout pixels on an actual HOME surface, including
  resize behaviour;
- clock/analog/world preview switching after granting or revoking Alarms &
  reminders access;
- launcher pin confirmation, cancellation, retry, and the unsupported-pin
  fallback dialog;
- multiple simultaneous World requests and token pruning;
- style persistence across an upgrade.

The independent runtime-repair run is separate; this pass makes no runtime claim.
Actual normal/130% font pixels and HOME battery placement still need independent
capture before any delivery claim.

## Limits and remaining work

- The collection remains incomplete: 17 in-app entries are in Upcoming and 37
  of the 40 KWGT presets are still pending or dependency-blocked. No finished
  import is claimed in this pass.
- Weather: partial domain/transport only, no registered provider.
- Photo and Away remain pending configuration/background-policy work.
- Compass and Snake remain blocked by the no-service/sensor policy.
- Media/notification, CPU and iOS Komponent presets remain dependency-blocked.
- The OEM OnePlus entries remain unavailable for redistribution reasons.
- Play policy review, signing-key setup, repository visibility changes, launcher
  integration and default-main promotion remain outside this checkpoint.
