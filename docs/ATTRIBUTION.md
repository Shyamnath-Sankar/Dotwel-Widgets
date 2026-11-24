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
- `widgets/Nothing_Battery2.kwgt!preset.json` -> `KwgtBattery2Renderer.kt`,
  `KwgtBattery2WidgetProvider.kt`, `Battery2State`, and the sanitized data model
  `app/src/main/assets/kwgt/nothing_battery2.sanitized.json`. Retained: the
  `side = min(width,height)` composition, rounded `side / 5` surface, dark
  `#FF1B1B1D` / white surface with opposite foreground, accent at 50% alpha, the
  `side / 8.5` information offset, `side / 18` BATTERY label with 16 bottom
  padding, the outlined 195 x 78 body (corners 14, stroke 3.5), the 179 x 64
  rounded clip, 16 rectangular progress segments across 184 x 64, the pill nub
  5 x 28 with a 5 margin, `side / 4.5` level, `side / 10` percent, `side / 15`
  bottom row and the explicit red `#FFD71921` REMAINING paint. The missing `h`
  global is fitted to the host height with the whole stack centred. Body, clip,
  progress and nub literals are scaled from the author 277 reference by
  `side / 277`. The banned `ndot-47.ttf` is replaced by Dotwel's original dot
  glyphs, with platform sans-serif for the long honest status. Android has no
  reliable discharge-time source: only a real
  `BatteryManager.computeChargeTimeRemaining()` value while charging is shown,
  labelled `TO FULL`; otherwise the widget shows `--` and `ESTIMATE UNAVAILABLE`.
  At 100% only `FULL` is shown, with no invented time. Runtime is not claimed
  here.
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
- `widgets/Nothing_Digital3.kwgt!preset.json` -> `KwgtDigital3Renderer.kt`,
  `KwgtDigital3WidgetProvider.kt`, and the sanitized data model
  `app/src/main/assets/kwgt/nothing_digital3.sanitized.json`. Retained: the
  rounded `side / 5` surface, the 4 by 4 grid of `side * 48 / 277` circles at
  `side * 12 / 277` margins, the `side / 8.5` right and `side / 21.875` up
  Pattern offset, the primary `#FFD71921` hh/mm cells, the uppercase aa cell,
  accent ink at 35% alpha and accent2 at 15% alpha, and the source minute
  thresholds `mm > 9`, `mm > 29`, `mm > 39` and `mm > 49`. The author's
  `ndot-47.ttf` is replaced by Dotwel's original dot glyphs, and the source
  helper joint layers are reproduced from the explicit ancestor/child
  visibility tables as cut-out bars so translucent joints never double-paint
  over translucent dots. Without optional exact-alarm access the provider draws
  the same time-free dot matrix under three live platform TextClocks for hh, mm
  and aa, positioned in true host content pixels, so no frozen bitmap time is
  presented as live. Runtime is not claimed here.
- `widgets/Nothing_Analog_3.kwgt!preset.json` -> `KwgtAnalog3WidgetProvider.kt`,
  the three `widget_kwgt_analog3_{black,translucent,transparent}.xml` layouts,
  the dial/hand vector resources and the sanitized data model
  `app/src/main/assets/kwgt/nothing_analog3.sanitized.json`. Retained: the 277
  square design viewport, the `side = min(width,height)` composition, the
  `side / 13.85` hand width (20), `side / 138.5` stroke (2), `side / 2.475` and
  `side / 3.3765` hand heights with their exact
  `138.5 - (side / 2.988) / 2` and `138.5 - (side / 4.4675) / 2` centred-Y
  formulas, and the 10-unit capsule corner radius. The source has no seconds
  layer. Black uses `#1B1B1D` with white hands, translucent `#A6FFFFFF` with dark
  hands, transparent no dial with white hands. The RemoteViews layouts embed a
  native `android.widget.AnalogClock` with vector `android:dial`,
  `android:hand_hour` and `android:hand_minute`, so the platform rotates the
  hands automatically. No runtime AnalogClock setter, bitmap time, minute alarm,
  service, permission or new dependency is used. The gallery preview inflates
  the same native layout. Runtime inflation on a launcher and actual hand
  movement are not claimed here.
- `widgets/Nothing_Analog_5.kwgt!preset.json` -> `KwgtAnalog5WidgetProvider.kt`,
  the three `widget_kwgt_analog5_{black,translucent,transparent}.xml` layouts,
  the `dw_analog5_dial_*`, `dw_analog5_hour` and `dw_analog5_minute_*` vector
  resources and the sanitized data model
  `app/src/main/assets/kwgt/nothing_analog5.sanitized.json`. Retained: the 277
  square design viewport, `side = min(width,height)`, outer corner `side / 5`,
  the clipped 60-tick minute ring (4 x `side / 5`) and 12-tick hour ring
  (6 x `side / 4`) on the `style_size = side` circle, the `side / 1.15` and
  `side / 1.3` rounded overlays, the filled hour pill `side / 13.19` by
  `side / 4.134` in `#D71921` with centre Y `138.5 - (side / 6.155) / 2`, and
  the filled minute pill `side / 25.18` by `side / 2.77` in `fg` with centre Y
  `138.5 - (side / 3.112) / 2` (the source raw width 10 is overridden by the
  formula). The source has no seconds layer. The missing `h` global is adapted
  by using the 277 square side for the background and clip heights and keeping
  the overlays square on their own source widths, so the source tick ring stays
  visible; that adaptation is documented and does not invent a new height
  meaning. Black uses `#1B1B1D` with white ticks and minute hand, translucent
  uses `#A6FFFFFF` with dark ticks and minute hand, transparent keeps ticks and
  hands with no filled dial. The RemoteViews layouts embed a native
  `android.widget.AnalogClock` with vector `android:dial`, `android:hand_hour`
  and `android:hand_minute`, so the platform rotates the hands automatically.
  No runtime AnalogClock setter, bitmap time, minute alarm, service, permission,
  font, raster asset or new dependency is used. The gallery preview inflates the
  same native layout. Runtime inflation and actual hand movement are not claimed.
- `widgets/Nothing_Analog_5_Transparent.kwgt!preset.json` ->
  `KwgtAnalog5TransparentWidgetProvider.kt`, the three
  `widget_kwgt_analog5clear_{black,translucent,transparent}.xml` layouts, the
  new `dw_analog5clear_dial_white` / `dw_analog5clear_dial_dark` cut-out vector
  dials, the reused source-identical Analog5 hand vectors, and the sanitized
  data model `app/src/main/assets/kwgt/nothing_analog5_transparent.sanitized.json`.
  Retained from the actual transparent preset: `config_scale_value`
  157.74908, the 277 square design viewport, `side = min(width,height)`, the
  wallpaper `fg` formula, primary red `#FFD71921`, outer clip `side / 1.08` with
  `side / 5` corners, 60 minute ticks 4 x `side / 5` on the
  `style_size = side * 1.05` ring, 12 hour ticks 6 x `side / 4` on the 277 ring,
  first CLEAR overlay `side / 1.15` with `side / 6.5` corners after the minute
  ticks, last CLEAR overlay `side / 1.3` with `side / 7` corners after the hour
  ticks, and the source hand formulas. The CLEAR overlays are real even-odd
  transparent holes in the static tick vector, not a background-coloured fill;
  the nested second CLEAR is inside the first, so the minute ticks subtract the
  CLEAR union and the hour ticks subtract only the last CLEAR. The source has no
  `bg` or `h` global: no filled dial card is drawn, each CLEAR height is
  normalised to its width, and the clip height uses the 277 design side. Because
  RemoteViews cannot safely sample the wallpaper, all three layouts are
  transparent with a manual contrast choice: white ticks/minute hand on black
  and transparent styles, dark `#1B1B1D` on translucent. The layouts embed a
  native `android.widget.AnalogClock` with vector `android:dial`,
  `android:hand_hour` and `android:hand_minute`; no runtime setter, bitmap time,
  minute alarm, font, raster asset, permission, service or dependency is used.
  The gallery preview inflates the same native layout. Runtime inflation, resize,
  style switching and actual hand movement are not claimed.
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
- `widgets/Clean_Time_Date_Info.kwgt!preset.json` -> `KwgtCleanInfoRenderer.kt`,
  `KwgtCleanInfoWidgetProvider.kt`, and the sanitized data model
  `app/src/main/assets/kwgt/clean_time_date_info.sanitized.json`. Retained: the
  553 x 248 wide card, corners 38, dark `#FF16161D` / light `#FFE4E4FF` surface,
  the bottom-left weekday/uppercase-month-day stack at left 28 / bottom 28 with
  sizes 69 and 24 and the -15 stack margin, the CENTERRIGHT `h:mm` at 196 in
  foreground ink at 19% alpha as the intentional ghost back layer, and the
  top-left 59 x 28 battery body plus 10.5 x 16 nub with the true bilevel
  percentage at 16 bold in card ink. The source `fgcolor` formula is malformed
  (`$if(si(darkmode)#FFFFFF,#000000)$`, missing the comma after `si(darkmode)`);
  it is repaired to the intended `$if(si(darkmode),#FFFFFF,#000000)$`, so dark
  surfaces use white ink and light surfaces use black ink. ProductSans-Bold and
  ProductSans-Medium are replaced by the platform `sans-serif` family, and no
  font payload is bundled. Geometry is derived from the JSON and each text size
  is fitted to the host instead of reproducing the cropped source preview. Time,
  weekday and date are live platform TextClocks and the percentage reads the
  real system battery level on the existing battery broadcast path, so no frozen
  time bitmap is presented and no exact-alarm access is required. Runtime is not
  claimed here.
- The author's sweep-gradient disc is rendered as a level-scaled arc plus a faint
  disc and track so the gauge remains readable at small widget sizes. This is a
  documented rendering substitution, not a claim of pixel-identical output.
- Sanitized out: author email fields, source `SINGLE_TAP` intent strings, font
  family payloads, and bundled font references including `ndot-47.ttf`.
  Dotwel's original dot glyphs or the documented platform font substitution
  replace the fonts.
- The full 40-preset source-port ledger is `docs/KWGT-LEDGER.md`. Only
  `Clean_Time_Date_Info`, `Nothing_Analog_3`, `Nothing_Analog_5`,
  `Nothing_Analog_5_Transparent`, `Nothing_Battery`, `Nothing_Battery2`,
  `Nothing_Battery_Mini`, `Nothing_Date_Time`,
  `Nothing_Date_Time2`, `Nothing_Digital4`, `Nothing_Digital3`,
  `Nothing_Digital`, and `Nothing_Mini_Calender` are implemented in this
  checkpoint (13 of 40); the rest remain pending or dependency-blocked and are
  not claimed as imported. All 19 registered providers are build-verified only
  and remain runtime-unverified.

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
