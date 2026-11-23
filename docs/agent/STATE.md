# Build and verification state

- Candidate: Dotwel Widgets 0.1.7 (versionCode 8), debug only. Package name
  `com.dotwelwidgets.dotwellabs` is unchanged.
- Branch: `agent/category-label-45151068025`, based on `8c898a3`. The separate
  launcher repository, default main, repository visibility and signing setup
  are unchanged.
- Scope of this run: the blank visible Add label in the MainActivity category
  sheet only. The previously verified category preview Next/Previous/swipe,
  selected Calendar second-variant pinning, live-minute advance (09:46 to
  09:47) and dotted-style advance (09:48 to 09:49) after process kill are
  preserved; no provider, renderer, asset, dependency or permission code was
  touched.

## Upstream source pins

- AwesomeAWidgets: `0fae60722764bda790f89bd94119c6dab854d90e` (MIT,
  Copyright (c) 2026 Sameer Al Sahab).
- KWGT-Widgets: `96ca27be4cbff35efec5a86c64dd39fd065c958f` (MIT,
  Copyright (c) 2024 Om Gupta).
- Adapted classes/functions and the 40-preset ledger remain documented in
  `docs/ATTRIBUTION.md` and `docs/KWGT-LEDGER.md`.

## What changed in this repair

`MainActivity.kt` is the only production file changed.

- Root cause: the category-sheet `rebind()` assigned
  `add.contentDescription = getString(R.string.sheet_add_widget_format,
  entry.name)` but never assigned `add.text`, so the red Add pill rendered
  blank despite its existing size, colour and click wiring.
- Fix: `rebind()` now sets `add.text = getString(R.string.sheet_add_widget)`
  ("Add widget", already localized in `strings.xml`).
- The existing dynamic content description (`Add <selected widget>`) and
  `onAdd(entries[selected])` click behaviour are unchanged.
- The button keeps its fixed 52 dp height and full-width layout. Text size is
  still 15 sp, colour is still white, `maxLines` is 1 and default Button
  padding is cleared to `0`, so the label is not clipped by framework insets
  at large font scale.
- Row spacing and all other sheet views are untouched.
- A prior blank 130% font-scale capture was attributed to a harness recreation
  screenshot race. This edit targets the statically missing text and makes no
  claim that screenshot race is resolved.

## Build and lint in this run

Commands run after the final edits:

```
./gradlew assembleDebug lintDebug --no-daemon
```

Result: `BUILD SUCCESSFUL`. `lintDebug` completed with `0 errors, 92 warnings`
and no lint suppression was added or changed. The warnings are pre-existing
SDK/AGP, API-gate, legacy-resource, plural, small-text and static-context
notices.

The APK identifies itself as `versionCode=8`, `versionName=0.1.7`,
`minSdk=26`, `targetSdk=36`, `compileSdk=36`.

## Artifact

Final debug APK: `checkpoints/dotwel-widgets-category-label-debug.apk`.
SHA-256 recorded in `checkpoints/CATEGORY-LABEL-SHA256SUMS`:

```
76112273f1d0399d1240c568ead83bc2a473a4e5091dba9b40571b0d53692c43  checkpoints/dotwel-widgets-category-label-debug.apk
```

The older `checkpoints/dotwel-widgets-polish-debug.apk` and
`checkpoints/dotwel-widgets-datetime-layout-debug.apk` were left untouched.

## Verified versus not verified

Verified here: the source compiles, the debug APK assembles and lint has no
errors. Source inspection confirms the Add pill now receives a non-empty
localized text value on every rebind while retaining its content description
and add action.

Not verified here:

- actual pixels of the Add pill after this revision on an Android host,
  including at 130% font scale;
- runtime placement, swipe, style, resize or post-kill update evidence for this
  revision.

No runtime or screenshot pass is claimed for this revision. An independent
actual-device or emulator capture is still required before the label repair is
called complete.

## Limits and remaining work

- The collection remains incomplete: 17 in-app entries are Upcoming and 37 of
  the 40 KWGT presets are still pending or dependency-blocked.
- Weather, Photo, Away, Compass, Snake, media/notification, CPU and iOS
  Komponent presets remain in their prior blocked or pending states.
- The OEM OnePlus entries remain excluded for redistribution reasons; the
  NothingWidgetPort remains excluded.
- Play policy review, signing-key setup, repository visibility changes, launcher
  integration and default-main promotion remain outside this checkpoint.
