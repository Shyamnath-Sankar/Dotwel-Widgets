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
text is bundled in `app/src/main/res/raw/awesomeawidgets_mit.txt` and is
reachable in the app from About & source credits > View MIT license texts.

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

- `DotGlyphFont.kt` contains Dotwel's original 5x7 circular-dot digit, colon,
  percent and DOTWEL heading glyphs. No third-party font file is bundled or
  loaded.
- `NtDateRenderer.kt` and the native date widget use the Android platform
  `serif` family; `NtWorldRenderer.kt` uses `sans-serif-condensed`; no font file
  is bundled.
- Launcher icon, backgrounds, previews, and screenshots in this repository are
  original to Dotwel Widgets unless noted below.

## KWGT-Widgets (MIT)

Secondary source for preset logic:
https://github.com/AumGupta/KWGT-Widgets

Pinned source commit: `96ca27be4cbff35efec5a86c64dd39fd065c958f`

Copyright (c) 2024 Om Gupta. Licensed under the MIT License. The full MIT text
is bundled in `app/src/main/res/raw/kwgt_widgets_mit.txt` and is reachable in
the app from About & source credits > View MIT license texts.

### Adapted presets

- `widgets/Nothing_Battery.kwgt!preset.json` -> `KwgtBatteryRenderer.kt`,
  `BatteryWidgetProvider.kt`, and the sanitized data model
  `app/src/main/assets/kwgt/nothing_battery.sanitized.json`. Retained: the
  `side`-relative composition, 4/415 vertical offset, `level * 3.6` rotation and
  `cm(215, max(level,25)*2.55, max(level,33)*2.55)` colour formula. The whole
  gauge is proportionally fitted to 92% of the short side (ring diameter about
  66% of the short side) instead of the literal `side / 2`, and the percent text
  is fitted to that ring, so a 2x2 host is not mostly empty. Battery level is
  read from the real system battery state, not a placeholder value.
- `widgets/Nothing_Battery_Mini.kwgt!preset.json` -> `KwgtBatteryMiniRenderer.kt`,
  `BatteryMiniWidgetProvider.kt`, and the sanitized data model
  `app/src/main/assets/kwgt/nothing_battery_mini.sanitized.json`. Retained: the
  eleven `w / {1.38, 1.23, 1.1, 1.1, 1.005, 1.005, 1.005, 1.1, 1.1, 1.23, 1.38}`
  concentric rings, `progress_level = bi(level)`, primary `#FFD71921` foreground
  and translucent accent track. The source preset has no text layer; Dotwel adds
  a centered real-level dot readout for legibility, so no charge state or number
  is invented.
- `widgets/Nothing_Date_Time.kwgt!preset.json` -> `KwgtDateTimeRenderer.kt` and
  `KwgtDateTimeWidgetProvider.kt`. Retained: the left-aligned four-line stack
  (weekday, red time, full month, ordinal day), the author side/8.5 offset and
  side/7 and side/6 text sizes, and the rounded-square composition. The source
  `ndot-47.ttf` font is replaced by Dotwel's original dot glyphs and platform
  text clocks; no upstream font or preset archive is bundled. The source
  `-4/415` stack margin is deliberately not reproduced: because Dotwel's dot
  glyphs visibly fill their line box, the renderer uses a positive clear gap
  and fits the stack inside the host height. That is a documented legibility
  adaptation, not pixel-exact upstream geometry. Runtime is not claimed here.
- `widgets/Nothing_Date_Time2.kwgt!preset.json` -> `KwgtDateTime2Renderer.kt`,
  `KwgtDateTime2WidgetProvider.kt`, and the sanitized data model
  `app/src/main/assets/kwgt/nothing_date_time2.sanitized.json`. Retained: the
  upper-left weekday over the huge red day, the lower-right hour and minute
  rows, the author side/8.5 offset, side/10 weekday and clock text, side/4 day
  text, side/5 discs, side/2.5 clock-group padding, smooth hour rotation
  `(hour + minute / 60) * 30`, minute rotation `minute * 6`, and the side/69.25
  notch width with side/28 hour and side/12 minute lengths and their source
  offsets. The NType82 regular/headline fonts are replaced by the Android
  platform serif regular and bold families; no upstream font file is bundled.
  The source tap intents and font payloads are omitted from the sanitized asset.
  Runtime is not claimed here.
- `widgets/Nothing_Digital.kwgt!preset.json` -> `KwgtGradientClockRenderer.kt`,
  `KwgtGradientClockWidgetProvider.kt`, and the sanitized data model
  `app/src/main/assets/kwgt/nothing_digital.sanitized.json`. Retained: the
  rounded `side / 5` surface, the three right-edge circles at `side / 1.1`,
  `side / 1.4` and `side / 2` with source gradient offsets 69/61/64 and widths
  45/71/54, the centred 12-hour `hh` at `side / 3` in foreground ink, and the
  `side / 6` primary minute chip with `mm` at `side / 12`. The bundled
  `ndot-47.ttf` is replaced by Dotwel's original dot glyphs. The source rotates
  each gradient circle to 170 degrees; Android's `LinearGradient` is drawn on a
  horizontal right-to-left axis while keeping the source offsets and widths, a
  documented legibility approximation rather than pixel-identical output. The
  minute chip is kept more opaque than the 45% source so `mm` stays legible on
  all three app surfaces. Without optional exact-alarm access the provider uses
  live platform TextClocks over a time-free static gradient surface. Runtime is
  not claimed here.
- `widgets/Nothing_Mini_Calender.kwgt!preset.json` -> `KwgtMiniCalendarRenderer.kt`,
  `KwgtMiniCalendarDates.kt`, `KwgtMiniCalendarWidgetProvider.kt`, and the
  sanitized data model
  `app/src/main/assets/kwgt/nothing_mini_calender.sanitized.json`. Retained: the
  horizontal five-day strip at offsets -2, -1, today, +1, +2; uppercase EEE over
  two-digit dd; weekday `side / 6.291` and date `side / 3.282`; outer-column
  accent at 50% ink; full ink for inner columns and today; the
  `widgetWidth / 5` rounded today highlight; the primary `#FFD71921` dot; and the
  scaled source horizontal margin. Relative dates use `Calendar.add`, never a
  86,400,000 ms constant. The bundled ndot-47.ttf is replaced by Dotwel's own dot
  glyphs, and the wallpaper-conditional highlight is replaced by a low-alpha ink
  wash so it stays visible on all three app surfaces. The 916x226 landscape
  preview is deliberately adapted to a legible 4x1 five-column strip with resize
  fit; that is a documented host-aspect adaptation, not pixel-exact output. The
  provider is date-only and requests no calendar, exact-alarm or boot permission.
  Android's inexact periodic refresh is supplemented by date/time/timezone/boot,
  app-refresh and resize callbacks; exact midnight delivery is not promised.
  Runtime is not claimed here.
- The author's sweep-gradient disc is rendered as a level-scaled arc plus a faint
  disc and track so the gauge remains readable at small widget sizes. This is a
  documented rendering substitution, not a claim of pixel-identical output.
- Sanitized out: author email fields, source `SINGLE_TAP` intent strings, font
  family payloads, and bundled font references including `ndot-47.ttf`.
  Dotwel's original dot glyphs or the documented platform font substitution
  replace the fonts.
- The full 40-preset source-port ledger is `docs/KWGT-LEDGER.md`. Only
  `Nothing_Battery`, `Nothing_Battery_Mini`, `Nothing_Date_Time`,
  `Nothing_Date_Time2`, `Nothing_Digital4`, `Nothing_Digital`, and
  `Nothing_Mini_Calender` are implemented in this checkpoint; the rest remain
  pending or dependency-blocked and are not claimed as imported.

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
