# KWGT-Widgets source-port ledger

Source: https://github.com/AumGupta/KWGT-Widgets
Pinned commit: `96ca27be4cbff35efec5a86c64dd39fd065c958f`
License: MIT, Copyright (c) 2024 Om Gupta. The full notice is retained in
`docs/ATTRIBUTION.md`.

The 40 `widgets/*.kwgt` files are ZIP archives whose `preset.json` contains Kustom
layer geometry and formulas. They are not Android `AppWidgetProvider`s. Each row
below is one exact preset file, not a light/dark preview image. A preset is only
marked **implemented** when this app has a real registered provider that uses its
converted layer geometry/formulas and real system data.

Status legend: **implemented** = native provider in this build; **pending** =
explicitly assigned, not yet converted; **dependency** = cannot be implemented
without a prohibited capability or unresolved third-party rights.

| # | Exact preset name | Status | Dependency / remaining work |
|---:|---|---|---|
| 1 | `Clean_Time_Date_Info` | **implemented** | Native `KwgtCleanInfoWidgetProvider`; wide card with live TextClock h:mm / EEEE / uppercase MMMM d, oversized 19% ghost time, and real BatteryManager percentage. Battery refresh is bounded best-effort: dynamic battery broadcast while the process is alive, otherwise an inexact non-wakeup ~15 min alarm with 10 min flex that Doze may defer; no exact-alarm access required; runtime unverified. |
| 2 | `Interstellar_Analog` | pending | Custom dial artwork has no separate redistribution grant; would require original redraw. |
| 3 | `Interstellar_Analog_Khaki_Field_Murph_Auto` | pending | Branded watch-face artwork and font rights unresolved; original redraw required. |
| 4 | `Interstellar_Endurance` | pending | Spacecraft artwork rights unresolved; original redraw required. |
| 5 | `Interstellar_Endurance_Analog` | pending | Spacecraft artwork rights unresolved; original redraw required. |
| 6 | `Interstellar_Endurance_Analog_2` | pending | Spacecraft artwork rights unresolved; original redraw required. |
| 7 | `Material3_Expanded_Media_Player` | dependency | Media/notification access is prohibited for this app. |
| 8 | `Material3_Media_Player_Output` | dependency | Media/notification access is prohibited for this app. |
| 9 | `Mini_Media_Player` | dependency | Media/notification access is prohibited for this app. |
| 10 | `Minimal_Media_Player` | dependency | Media/notification access is prohibited for this app. |
| 11 | `Minimal_Media_Player_2` | dependency | Media/notification access is prohibited for this app. |
| 12 | `Minimal_Media_Player_3` | dependency | Media/notification access is prohibited for this app. |
| 13 | `Nothing_Analog_` | **implemented** | Native `KwgtAnalogSecondsWidgetProvider`; exact source-seconds port. 277 viewport, filled `#1B1B1D` dial, filled hour capsule 277/11.54 x 277/3.378 at `138.5 - (277/4.616)/2`, constant `#6C696E` minute capsule 277/27.7 x 277/2.473 at `138.5 - (277/2.7426)/2`, and the source `#D71921` second circle (diameter 277/23.08) at radius `(277/1.145)/2`. Android 12+ native `android:hand_second` rotates the full-viewport second drawable from the 12 o'clock zero position, absorbing the source `CLOCK_SECOND` 90 offset; the source second toggle is active, not frozen or omitted. Receiver is disabled below API 31 via a `false`/`true` `values`/`values-v31` bool and the catalog marks it unavailable there with no no-seconds fallback. Black/translucent/transparent retain the three documented hour/dial styles with constant minute and second colours. Second-tick fps is system configured and may be disabled. No bitmap time, alarm, service, permission or new dependency; runtime unverified. |
| 14 | `Nothing_Analog_2` | **implemented** | Native `KwgtAnalog2WidgetProvider`; exact source port. 277 filled circular dial, sharp hour rect 6 x 74 centred at `138.5 - 64/2`, sharp minute rect 6 x 100 centred at `138.5 - 112/2`, and the `#D71921` seconds rect 130 x 3 with right padding 100 and the active `CLOCK_SECOND` 90 offset (native zero vertical at x 138.5, centre Y 88.5, extent 23.5..153.5). 12 hour ticks 8 x 16.5 on the `style_size 250` ring and 60 minute ticks 11 x 8 on the `style_size 258` ring are drawn in a transparent full-viewport `fitCenter` overlay above the native `android.widget.AnalogClock`, preserving the source tick/centre z-order above the hands; the red centre circle diameter 16 is last. Only `layout-v31` wires `android:hand_second`; the receiver is disabled below API 31 by `kwgt_analog_seconds_available` and the catalog marks it unavailable with no silently degraded seconds. The minute raw width 6 is retained because its active toggle has no formula; the centre's missing type is documented as Kustom's CIRCLE default. Black `#1B1B1D` / white ink, translucent `#A6FFFFFF` / dark ink, transparent no dial / white ink; red seconds and centre constant. No bitmap time, alarm, service, permission or new dependency; non-square host scaling and runtime unverified. |
| 15 | `Nothing_Analog_3` | **implemented** | Native `KwgtAnalog3WidgetProvider`; 277 vector dial with two hollow rounded capsule hands in a platform `android.widget.AnalogClock`, which rotates the hands automatically. Black `#1B1B1D` / white hands, translucent `#A6FFFFFF` / dark hands, transparent no dial / white hands. No seconds, bitmap time, minute alarm, permission or service; runtime unverified. |
| 16 | `Nothing_Analog_5` | **implemented** | Native `KwgtAnalog5WidgetProvider`; 277 rounded-square vector dial with a clipped 60-tick minute ring and 12-tick hour ring, two square rounded overlays, a red `#D71921` filled hour pill and foreground minute pill in a platform `android.widget.AnalogClock`. Black `#1B1B1D` / white ticks, translucent `#A6FFFFFF` / dark ticks, transparent no filled dial with ticks and hands. Uses the source `CLOCK_HOUR_SMOOTH` / `CLOCK_MINUTE` semantics through the platform clock; no seconds, bitmap time, minute alarm, permission or service; runtime unverified. |
| 17 | `Nothing_Analog_5_Transparent` | **implemented** | Native `KwgtAnalog5TransparentWidgetProvider`; actual transparent source port with the source `style_size = side * 1.05` minute ring, `side / 5` outer clip corners, `side / 6.5` first CLEAR corners, and real even-odd CLEAR cut-outs rather than a painted background. All three Dotwel styles are transparent with no filled dial card; white ticks/minute hand on black/transparent and dark `#1B1B1D` on translucent are a documented manual wallpaper-contrast choice. Red `#D71921` hour pill and foreground minute pill in a platform `android.widget.AnalogClock`; no bitmap time, minute alarm, permission or service; runtime unverified. |
| 18 | `Nothing_Battery` | **implemented** | Native `BatteryWidgetProvider`; real system level with bounded best-effort `BatteryRefreshScheduler` (inexact ELAPSED_REALTIME ~15 min + 10 min flex, does not wake the device); runtime unverified. |
| 19 | `Nothing_Battery2` | **implemented** | Native `KwgtBattery2WidgetProvider`; rounded surface, outlined 16-segment body with clip and nub, real level, system charging state and only real `computeChargeTimeRemaining` values; unknown estimates show `--` / `ESTIMATE UNAVAILABLE`. Lifecycle: dynamic `BATTERY_CHANGED` only while the process lives, plus an inexact non-wakeup `BatteryRefreshScheduler` window (~15 min + 10 min flex, Doze-deferrable) and manual app refresh; not a live/instant claim; runtime unverified. |
| 20 | `Nothing_Battery_Mini` | **implemented** | Native `BatteryMiniWidgetProvider`; real system level with bounded best-effort `BatteryRefreshScheduler` (inexact ELAPSED_REALTIME ~15 min + 10 min flex, does not wake the device); runtime unverified. |
| 21 | `Nothing_CPU` | dependency | Per-process CPU/system usage needs restricted or unreliable data paths. |
| 22 | `Nothing_Date_Time` | **implemented** | Native `KwgtDateTimeWidgetProvider`; live text clocks without optional exact-alarm access; runtime unverified. |
| 23 | `Nothing_Date_Time2` | **implemented** | Native `KwgtDateTime2WidgetProvider`; upper-left date stack plus lower-right hour/minute discs; live text fallback without exact-alarm access; runtime unverified. |
| 24 | `Nothing_Date_Time3` | pending | Depends on the pending Open-Meteo weather/temperature/AQI path. |
| 25 | `Nothing_Digital` | **implemented** | Native `KwgtGradientClockWidgetProvider`; right-edge gradient circles, 12-hour hh and red minute chip; live text fallback without exact-alarm access; runtime unverified. |
| 26 | `Nothing_Digital2` | pending | Digital layer conversion queued; can reuse the existing minute schedule. |
| 27 | `Nothing_Digital3` | **implemented** | Native `KwgtDigital3WidgetProvider`; 4x4 minute-lit dot matrix with red hh/mm, uppercase aa and source thresholds; live TextClock fallback without exact-alarm access; runtime unverified. |
| 28 | `Nothing_Digital4` | **implemented** | Native `KwgtDigital4WidgetProvider`; red hh / centre divider / ink mm split; live text fallback without exact-alarm access; runtime unverified. |
| 29 | `Nothing_Digital_Clock` | pending | Digital layer conversion queued. |
| 30 | `Nothing_Future_Clock` | **implemented** | Native `KwgtFutureWidgetProvider`; three sweeping gradient discs for hour, minute and second with platform-clock rotation and live labels. Android 12+ for the seconds disc; runtime unverified. |
| 31 | `Nothing_Master_Widget` | dependency | Combines media/notification data with weather; media access is prohibited. |
| 32 | `Nothing_Media_Player` | dependency | Media/notification access is prohibited for this app. |
| 33 | `Nothing_Media_Player2` | dependency | Media/notification access is prohibited for this app. |
| 34 | `Nothing_Media_Player3` | dependency | Media/notification access is prohibited for this app. |
| 35 | `Nothing_Mini_Calender` | **implemented** | Native `KwgtMiniCalendarWidgetProvider`; five-day date-only strip, no calendar or exact-alarm permission; runtime unverified. |
| 36 | `Nothing_Sun_Info` | **implemented** | Native `NothingSunInfoWidgetProvider`; next cached sunrise/sunset for a manually chosen city, optional Open-Meteo opt-in, polar or unknown data shows Unavailable. Runtime unverified. |
| 37 | `Nothing_Temp` | **implemented** | Native `NothingTempWidgetProvider`; today's forecast low and day high/low for a manually chosen city from cached data, optional Open-Meteo opt-in, no matching day shows `--`. Runtime unverified. |
| 38 | `Universal_Media_Player` | dependency | Media/notification access is prohibited for this app. |
| 39 | `iOS_Battery_Widget` | **implemented** | Native `IosBatteryWidgetProvider`; real level and charging state, 92 ring / 10 stroke / 32 padding / 69 corners normalised to the visible 290 Komponent card (not the 416 preview canvas), platform sans-serif and original battery vectors. The stale media/cover formula is inactive. Bounded Doze-deferrable battery refresh; runtime unverified. |
| 40 | `material_notification_widget` | dependency | Notification access is prohibited for this app. |

## Implemented in this checkpoint

Nineteen of the 40 presets now have native providers: `Clean_Time_Date_Info`,
`iOS_Battery_Widget`, `Nothing_Analog_`, `Nothing_Analog_2`,
`Nothing_Analog_3`, `Nothing_Analog_5`, `Nothing_Analog_5_Transparent`,
`Nothing_Battery`, `Nothing_Battery2`, `Nothing_Battery_Mini`,
`Nothing_Date_Time`, `Nothing_Date_Time2`, `Nothing_Digital`,
`Nothing_Digital3`, `Nothing_Digital4`, `Nothing_Future_Clock`,
`Nothing_Mini_Calender`, `Nothing_Sun_Info` and `Nothing_Temp`. The app also
ships six upstream-derived native providers (digital, bold analog, scale analog,
date, world and quotes), for 25 registered providers in total. All 25 are
build-verified only and remain runtime-unverified in this checkpoint; this is
not a claim of completion or runtime acceptance. The sanitized transcriptions
are under `app/src/main/assets/kwgt/`, including
`clean_time_date_info.sanitized.json`, `ios_battery.sanitized.json`,
`nothing_analog2.sanitized.json`, `nothing_analog3.sanitized.json`,
`nothing_analog_seconds.sanitized.json`, `nothing_analog5.sanitized.json`,
`nothing_analog5_transparent.sanitized.json`, `nothing_battery.sanitized.json`,
`nothing_battery2.sanitized.json`, `nothing_battery_mini.sanitized.json`,
`nothing_date_time.sanitized.json`, `nothing_date_time2.sanitized.json`,
`nothing_digital.sanitized.json`, `nothing_digital3.sanitized.json`,
`nothing_digital4.sanitized.json`, `nothing_future_clock.sanitized.json`,
`nothing_mini_calender.sanitized.json`, `nothing_sun_info.sanitized.json` and
`nothing_temp.sanitized.json`.

Clean_Time_Date_Info keeps these author relationships:

- 553 x 248 wide card, rounded corners 38, dark `#FF16161D` / light `#FFE4E4FF`
- bottom-left stack at left 28 / bottom 28, weekday size 69, uppercase month/day
  size 24 and the source -15 stack margin
- CENTERRIGHT `h:mm` at size 196 in foreground ink at 19% alpha, retained as the
  intentional oversized ghost back layer behind the readable text
- top-left battery body 59 x 28 rounded 6, nub 10.5 x 16 rounded 4 overlapped by
  the source -5 margin, and the true bilevel percentage at 16 bold in card ink

The source `fgcolor` formula is malformed (`$if(si(darkmode)#FFFFFF,#000000)$`,
missing the comma after `si(darkmode)`). It is repaired to the intended
`$if(si(darkmode),#FFFFFF,#000000)$`: dark surfaces use white ink and light
surfaces use black ink. ProductSans-Bold/Medium are not redistributed; the
platform `sans-serif` family is substituted. Geometry is derived from the JSON
and each text size is fitted to the host bounds instead of reproducing the
cropped source preview. Time, weekday and date are live platform TextClocks, so
no frozen bitmap time is shown and no exact-alarm access is required. Only the
static card/battery background is a bitmap. The percentage reads the real
`BatteryManager` level through the existing `BatteryState` path and refreshes on
`ACTION_BATTERY_CHANGED`, date/time/time-zone/boot and resize updates. The source
time readout is fixed 12-hour `h:mm`, so the TextClock uses `h:mm` for both
`format12Hour` and `format24Hour` and 24-hour devices still match the source;
the fallback background slot is a supported `ImageView` rather than a raw `View`.

Nothing_Analog_5 keeps these author relationships:

- source canvas 415 x 542; `side = min(width, height)`; 277 square design viewport
- outer rounded square corner `side / 5` (55.4 at 277); dark `#FF1B1B1D` / light
  white surface with the opposite `fg` ink; `primary` red `#FFD71921`
- clip rounded rectangle width `side / 1.08`, height `side`, corners `side / 6`
- 60 minute rect ticks width 4, height `side / 5`, on the `style_size = side`
  circle; 12 hour rect ticks width 6, height `side / 4`; both rings rotated
  radially and clipped by the rounded clip path
- square rounded background overlays `side / 1.15` (corners `side / 6.8`) and
  `side / 1.3` (corners `side / 7`)
- filled rounded hour pill width `side / 13.19`, height `side / 4.134`, centre Y
  `138.5 - (side / 6.155) / 2`, `primary` red
- filled rounded minute pill width `side / 25.18` (the source raw width 10 is
  overridden by the formula), height `side / 2.77`, centre Y
  `138.5 - (side / 3.112) / 2`, `fg`
- rotation `CLOCK_HOUR_SMOOTH` and `CLOCK_MINUTE` are provided by the platform
  `AnalogClock`; the source has no seconds layer

The source references an `h` global for the background, clip and overlay
heights but does not define it. The native port uses the 277 square side for the
background and clip heights, and keeps the two background overlays square on
their own source widths so the 60/12 tick ring stays visible as in the source
preview. This is the documented missing-height adaptation, not a new height
meaning. The source has no tap intent, font or raster asset; the native provider
taps `AlarmClock.ACTION_SHOW_ALARMS`. Black uses the dark dial with white ticks
and minute hand, translucent uses `#A6FFFFFF` with dark ticks and minute hand,
transparent keeps the ticks and hands with no filled dial. The hour pill stays
`#D71921` in every style. Runtime inflation on a launcher and actual hand
movement are not verified in this checkpoint.

Nothing_Analog_5_Transparent keeps these actual-source relationships:

- source canvas 415 x 542; `side = min(width, height)`; source
  `config_scale_value = 157.74908`; 277 square design viewport chosen for the
  native vectors (the scale is recorded, not applied at runtime)
- source globals are `side`, wallpaper-based `fg` and `primary` red `#FFD71921`;
  the source has no `bg` and no `h` global, so no filled dial card is drawn
- outer clip rounded rectangle width `side / 1.08`, height normalised to the 277
  side, corners `side / 5` (55.4); this differs from the Analog5 `/ 6` corner
- 60 minute rect ticks width 4, height `side / 5`, on the source
  `style_size = side * 1.05` ring (centre radius 145.425, outer 173.125); this
  is the actual transparent source value, not the Analog5 `style_size = side`
- 12 hour rect ticks width 6, height `side / 4`, centred on the 277 ring
- first CLEAR square rounded overlay width `side / 1.15`, corner `side / 6.5`
  (42.6154), applied after the minute ticks and before the hour ticks
- last CLEAR square rounded overlay width `side / 1.3`, corner `side / 7`
  (39.5714), applied after the hour ticks and clearing all earlier static ticks
- both CLEAR shapes are rendered as real even-odd transparent holes in the
  static tick vector; there is no background-coloured approximation. The
  minute tick path subtracts the CLEAR union (the nested second CLEAR is inside
  the first), and the hour tick path subtracts the last CLEAR only
- filled rounded hour pill and minute pill reuse the source-identical Analog5
  formulas; the hour pill is always red `#FFD71921`
- rotation `CLOCK_HOUR_SMOOTH` and `CLOCK_MINUTE` are provided by the platform
  `AnalogClock`; the source has no seconds layer

The source `fg` formula depends on `darkwp` (wallpaper). RemoteViews cannot
safely sample the wallpaper, so all three layouts keep a transparent dial and
choose contrast manually: black and transparent styles use white ticks/minute
hand, translucent uses dark `#1B1B1D` ticks/minute hand. The missing `h` global
is a documented port choice: CLEAR heights use each CLEAR width (square) and the
clip height uses the 277 design side. Runtime inflation on a launcher, resize,
style switching and actual hand movement are not verified in this checkpoint.

Nothing_Battery keeps these author relationships:

- `side = if(rwidth > rheight, rheight, rwidth)`
- level rotation `level * 3.6`
- colour `cm(215, max(level,25)*2.55, max(level,33)*2.55)`
- the 4/415 vertical offset as a fraction of the fitted gauge
- the numeric readout, fitted to the ring

Dotwel proportionally scales the whole gauge to 92% of the short side (ring
diameter about 66% of the short side) instead of the literal `side / 2`, so a
2x2 host surface is filled without stretching. The gallery preview calls the
same fitted renderer as the provider.

Nothing_Battery_Mini keeps the author's eleven `ProgressModule` sizes
`w / {1.38, 1.23, 1.1, 1.1, 1.005, 1.005, 1.005, 1.1, 1.1, 1.23, 1.38}`,
`progress_level = bi(level)`, primary `#FFD71921` arc and translucent accent
track. It has no text layer in the source preset; Dotwel adds a centered
real-level dot readout for legibility without inventing charge state.


Nothing_Battery2 keeps these author relationships:

- source canvas 415 x 542; `side = min(width, height)` and rounded background
  corners `side / 5`
- dark `#FF1B1B1D` / light white surface with the opposite foreground; accent is
  foreground at 50% alpha
- information stack offset `side / 8.5`; BATTERY label `side / 18` with 16 bottom
  padding
- outlined body 195 x 78, corners 14, stroke 3.5; clip 179 x 64, corners 7; 16
  rectangular progress segments across 184 x 64; pill nub 5 x 28 with a 5 margin
- large level `side / 4.5`, smaller percent `side / 10`, bottom row `side / 15`
  with an 8 margin
- the `primary` global has no value, so the explicit REMAINING paint
  `#FFD71921` is retained for the red status

The source has no `h` global, so the background follows the host height and the
whole stack is centred rather than reproducing the missing height. Body, clip,
progress and nub literals are scaled from the author 277 design reference by
`side / 277`; formula sizes use `side` directly. The source `ndot-47.ttf` is
replaced by Dotwel's original dot glyphs for BATTERY and the large level, with
the Android platform sans-serif family for the long honest status copy. Android
does not expose a reliable discharge-time estimate. When charging, a positive
`BatteryManager.computeChargeTimeRemaining()` result is shown as `H` / `M` with
`TO FULL`; otherwise the row shows `--` and `ESTIMATE UNAVAILABLE`. At 100% only
`FULL` is shown, with no invented time. The tap tries
`ACTION_POWER_USAGE_SUMMARY` and falls back to a known Settings screen when no
activity resolves. Runtime is not claimed here.

Nothing_Date_Time2 keeps these author relationships:

- `side = if(rwidth > rheight, rheight, rwidth)`
- weekday `EEE` at `side / 10`, huge `d` at `side / 4`
- left information offset `side / 8.5` and rounded corners `side / 5`
- lower-right clock group padding left `side / 2.5`
- hour and minute discs `side / 5` diameter
- hour smooth rotation `(hour + minute / 60) * 30`, minute rotation `minute * 6`
- notch width `side / 69.25`; hour notch length `side / 28` with `side / 6.1`
  offset; minute notch length `side / 12` with `side / 8.5` offset
- primary red `#FFD71921`

The source NType82 regular/headline fonts are replaced by the Android platform
serif regular and bold families. The source tap intents and font payloads are
removed from the sanitized asset. Without optional exact-alarm access the
provider uses live platform TextClocks in the matching regions and does not
draw any analog disc, so no frozen minute is presented.

Nothing_Mini_Calender keeps these author relationships:

- horizontal five-column strip at relative days -2, -1, today, +1, +2, resolved
  with calendar date arithmetic rather than 86,400,000 ms additions
- uppercase `EEE` weekday over two-digit `dd` in every column
- weekday size `side / 6.291`, date size `side / 3.282`
- outer columns use `accent` = ink at 50% alpha; inner columns and today use full
  ink; today uses a rounded `widgetWidth / 5` highlight and the primary
  `#FFD71921` dot below the date
- source horizontal margin 58/916 of width, scaled with the host and fitted with
  a small positive gap so all five columns remain on-screen

The source `ndot-47.ttf` is replaced by Dotwel's own dot glyphs. The source
highlight colour is wallpaper-conditional; Dotwel uses a low-alpha ink wash so it
stays visible over the app's black, translucent and transparent surfaces. The
916x226 landscape preview was adapted to a legible 4x1 strip: the source itself
is five days, but the narrow centre day in the original preview came from that
host's aspect ratio. This is a documented resize adaptation, not pixel-exact
upstream output. The provider is date-only and does not request calendar or
exact-alarm permission. Android's inexact `updatePeriodMillis` is backed up by
DATE/TIME/TIMEZONE/BOOT broadcasts, app refresh and resize updates; it is not
promised as an exact midnight alarm.

Nothing_Analog_3 keeps these author relationships:

- source canvas 415 x 542; `side = min(width, height)`; 277 square design viewport
- dial radius 138.5 in the 277 viewport; the native port uses a filled vector
  dial in the source `bg` colour rather than the source 3-unit stroke
- minute hand width `side / 13.85` (20), stroke `side / 138.5` (2), height
  `side / 2.475`, capsule corner radius 10, centred at
  `138.5 - (side / 2.988) / 2`
- hour hand same width/stroke/radius, height `side / 3.3765`, centred at
  `138.5 - (side / 4.4675) / 2`
- minute rotation `CLOCK_MINUTE`, smooth hour rotation `CLOCK_HOUR_SMOOTH` are
  provided by the platform `AnalogClock`; the source has no seconds layer

Black uses the source dark `#FF1B1B1D` dial with white foreground hands,
translucent uses a `#A6FFFFFF` dial with the dark `#FF1B1B1D` hands, and
transparent uses no dial with white hands. The three RemoteViews layouts embed a
native `android.widget.AnalogClock` with vector `android:dial`,
`android:hand_hour` and `android:hand_minute` resources; no runtime AnalogClock
setter, bitmap time, minute alarm, permission, service or dependency is used.
The preview inflates the same native layout so it shows the same vectors and the
current time. Runtime inflation on a launcher and actual minute-hand movement
are not verified in this checkpoint.

Sanitized out of the assets and implementations: the author email, the background
`SINGLE_TAP` intent string, and the bundled `ndot-47.ttf`. Dotwel's original
circular-dot glyphs replace the font where needed. No preset archive, APK,
preview JPEG, font or icon is bundled. The gallery preview calls the same
renderer as the provider.

## Not counted as presets

`docs/previews/*.png` light/dark images in the upstream repository are preview
renders, not separate widgets. They are neither copied nor counted in the 40.

## Weather note

`Nothing_Temp` and `Nothing_Sun_Info` are implemented optional Open-Meteo
providers. Weather is off by default and nothing is requested before consent;
the city is chosen manually with no GPS or device-location access. After consent
and a selected city, opening or resuming the app may refresh a stale cached
forecast at most once an hour; there is no background network polling. Widget
updates read only the private cache. The other weather variants remain pending.
Personal, noncommercial use is within Open-Meteo terms with attribution; any
separate commercial launcher use must be reviewed before shipping.

Analog2 overlay uses centerInside to match native AnalogClock downscale-only intrinsic size behavior, including large host cells.

Future Clock: native three equal columns now have 0.1-column responsive gaps, preserving source18 gap relative to h/2 discs at its916x542 reference. On narrow/tall hosts discs fit columns and cannot touch. Actual runtime pending for this repair.
