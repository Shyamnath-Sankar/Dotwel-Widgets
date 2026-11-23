# Third-party attribution

Dotwel Widgets is an independent app. It is not affiliated with, endorsed by, or
sponsored by Nothing Technology Limited, OnePlus, Kustom, or the referenced
authors. This is a debug candidate; it is not Play-approved or runtime-verified
in this checkpoint.

## AwesomeAWidgets (MIT)

Primary design and implementation reference:
https://github.com/SameerAlSahab/AwesomeAWidgets

Pinned source commit: `0fae60722764bda790f89bd94119c6dab854d90e`

Copyright (c) 2026 Sameer Al Sahab. Licensed under the MIT License. The full MIT
text is bundled in `app/src/main/res/raw/awesomeawidgets_mit.txt`.

### Adapted author-written behaviour (precise mapping)

No smali bytecode, APK resources, fonts, or artwork from AwesomeAWidgets are
included. The following author-written classes were read and their behaviour was
translated into Kotlin using standard Android widget APIs:

- `NtDraw.SCHEME`, `NtDraw.color`, `NtDraw.surface`, `NtDraw.fit`, `NtDraw.center`,
  `NtDraw.paint`, `NtDraw.shadow` -> `UpstreamNtDraw.kt`. The black,
  translucent and transparent palettes are retained. `NtDraw.size` is
  deliberately not translated; Dotwel keeps its own host-size correction in
  `WidgetSizing.kt`.
- `NtDigital.build` -> `NtDigitalRenderer.kt`. Hour/minute row placement at
  0.27/0.73 of height, the `"00"` fit to 0.76 width and 0.37 height, and the
  centred reference baseline are retained. Ndot is replaced with Dotwel's
  original circular-dot glyphs.
- `NtDate.build` -> `NtDateRenderer.kt`. The day-number fit and 0.6-height
  centre baseline, the right-aligned abbreviated weekday at 0.15 of the minimum
  side, and the top-margin offset are retained. NType/Lettera files are replaced
  with the Android platform `serif` family.
- `NtAnalogBold.build` / `NtAnalogScale.build` provider layouts ->
  `NtAnalogRenderer.kt` and the two analog providers in `BaseWidgetProvider.kt`.
  The provider wiring, palette and bold/thin hands are retained as original
  programmatic Canvas drawing; the upstream dial/hand drawables are not copied.
- `NtWorld.build` -> `NtWorldRenderer.kt`. The 0.12 minimum-side margin, two
  0.22-label rows, half-row-height time fit and 0.88-row-height baseline are
  retained. Ndot/Ndot77 references are replaced with platform condensed/sans
  families.
- `NtQuotes.build` drawing block -> `NtQuotesRenderer.kt`. The 0.11 margin,
  0.26 accent quote mark, 0.13 body text and 6% shrink-to-fit step are retained.
  Quote text comes from the upstream author data file `assets/nt_quotes.csv`.
- `NtProvider` update loop, resize callback and clock-tap behaviour ->
  `BaseWidgetProvider.kt` plus the concrete widget providers.
- `NtStyle` persisted style integer -> the three-value `WidgetStyle` enum in
  `WidgetPrefs.kt`.
- `NtTick` minute-update idea -> `MinuteTickScheduler.kt`. No upstream service or
  broadcast code is copied. The update path is a `PendingIntent` exact alarm,
  scheduled only when the user enables Alarms & reminders access.

### Original substitutions

- `DotGlyphFont.kt` contains Dotwel's original 5x7 circular-dot digit, colon and
  percent geometry. No third-party font file is bundled or loaded.
- `NtDateRenderer.kt` and the native date widget use the Android platform
  `serif` family; `NtWorldRenderer.kt` uses `sans-serif-condensed`; no font file
  is bundled.
- Launcher icon, backgrounds, previews, and screenshots in this repository are
  original to Dotwel Widgets unless noted below.

## KWGT-Widgets (MIT)

Secondary source for preset logic:
https://github.com/AumGupta/KWGT-Widgets

Pinned source commit: `96ca27be4cbff35efec5a86c64dd39fd065c958f`

Copyright (c) 2024 Om Gupta. Licensed under the MIT License.

### Adapted preset

- `widgets/Nothing_Battery.kwgt!preset.json` -> `KwgtBatteryRenderer.kt`,
  `BatteryWidgetProvider.kt`, and the sanitized data model
  `app/src/main/assets/kwgt/nothing_battery.sanitized.json`. Retained: the
  `side` global, `side / 2` circle diameter, 4-pixel vertical offset,
  `level * 3.6` rotation, `cm(215, max(level,25)*2.55, max(level,33)*2.55)`
  colour formula, and `side / 9` text size. Battery level is read from the real
  system battery state, not a placeholder value.
- The author's sweep-gradient disc is rendered as a level-scaled arc plus a faint
  disc so the gauge remains readable at small widget sizes. This is a documented
  rendering substitution, not a claim of pixel-identical output.
- Sanitized out: the author email in `preset_info`, the background `SINGLE_TAP`
  intent string, and the bundled `ndot-47.ttf` font reference. Dotwel's original
  dot glyphs replace the font.
- The full 40-preset source-port ledger is `docs/KWGT-LEDGER.md`. Only
  `Nothing_Battery` is implemented in this checkpoint; the rest remain pending or
  dependency-blocked and are not claimed as imported.

## Not included

- The upstream AwesomeAWidgets APK, decompiled OEM `net.oneplus.*` classes, and
  OEM resources.
- Any `.kwgt` preset archive, KWGT APK, upstream preview JPEG/PNG, icon font, or
  bitmap artwork from KWGT-Widgets.
- Upstream or third-party font files such as `nt_ndot77jp.ttf`, `nt_ntype82.otf`,
  `nt_lettera_mono.otf`, `ndot-47.ttf`, `ProductSans*.ttf`, and `material.ttf`.
  These have separate or unresolved redistribution rights.
- Upstream screenshots, logos, preview images, and other artwork.
- `nothing-kde-widgets` and `nothing_archive` source, archives, or assets; those
  repositories were inspected as references only.
- Media/notification, location, broad-storage, accessibility, overlay, or
  foreground-service capabilities. Weather data remains a pending, isolated
  domain/transport layer; no weather provider is registered in this checkpoint.
