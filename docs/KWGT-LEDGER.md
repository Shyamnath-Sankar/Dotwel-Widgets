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
| 1 | `Clean_Time_Date_Info` | **implemented** | Native `KwgtCleanInfoWidgetProvider`; wide card with live TextClock h:mm / EEEE / uppercase MMMM d, oversized 19% ghost time, and real BatteryManager percentage refreshed on the system battery broadcast; no exact-alarm access required; runtime unverified. |
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
| 13 | `Nothing_Analog_` | pending | Analog layer conversion queued behind the battery/time ports. |
| 14 | `Nothing_Analog_2` | pending | Analog layer conversion queued. |
| 15 | `Nothing_Analog_3` | pending | Analog layer conversion queued. |
| 16 | `Nothing_Analog_5` | pending | Analog layer conversion queued. |
| 17 | `Nothing_Analog_5_Transparent` | pending | Analog layer conversion queued. |
| 18 | `Nothing_Battery` | **implemented** | Native `BatteryWidgetProvider`; real system level; runtime unverified. |
| 19 | `Nothing_Battery2` | **implemented** | Native `KwgtBattery2WidgetProvider`; rounded surface, outlined 16-segment body with clip and nub, real level, system charging state and only real `computeChargeTimeRemaining` values; unknown estimates show `--` / `ESTIMATE UNAVAILABLE`; runtime unverified. |
| 20 | `Nothing_Battery_Mini` | **implemented** | Native `BatteryMiniWidgetProvider`; real system level; runtime unverified. |
| 21 | `Nothing_CPU` | dependency | Per-process CPU/system usage needs restricted or unreliable data paths. |
| 22 | `Nothing_Date_Time` | **implemented** | Native `KwgtDateTimeWidgetProvider`; live text clocks without optional exact-alarm access; runtime unverified. |
| 23 | `Nothing_Date_Time2` | **implemented** | Native `KwgtDateTime2WidgetProvider`; upper-left date stack plus lower-right hour/minute discs; live text fallback without exact-alarm access; runtime unverified. |
| 24 | `Nothing_Date_Time3` | pending | Depends on the pending Open-Meteo weather/temperature/AQI path. |
| 25 | `Nothing_Digital` | **implemented** | Native `KwgtGradientClockWidgetProvider`; right-edge gradient circles, 12-hour hh and red minute chip; live text fallback without exact-alarm access; runtime unverified. |
| 26 | `Nothing_Digital2` | pending | Digital layer conversion queued; can reuse the existing minute schedule. |
| 27 | `Nothing_Digital3` | **implemented** | Native `KwgtDigital3WidgetProvider`; 4x4 minute-lit dot matrix with red hh/mm, uppercase aa and source thresholds; live TextClock fallback without exact-alarm access; runtime unverified. |
| 28 | `Nothing_Digital4` | **implemented** | Native `KwgtDigital4WidgetProvider`; red hh / centre divider / ink mm split; live text fallback without exact-alarm access; runtime unverified. |
| 29 | `Nothing_Digital_Clock` | pending | Digital layer conversion queued. |
| 30 | `Nothing_Future_Clock` | pending | Layer conversion queued. |
| 31 | `Nothing_Master_Widget` | dependency | Combines media/notification data with weather; media access is prohibited. |
| 32 | `Nothing_Media_Player` | dependency | Media/notification access is prohibited for this app. |
| 33 | `Nothing_Media_Player2` | dependency | Media/notification access is prohibited for this app. |
| 34 | `Nothing_Media_Player3` | dependency | Media/notification access is prohibited for this app. |
| 35 | `Nothing_Mini_Calender` | **implemented** | Native `KwgtMiniCalendarWidgetProvider`; five-day date-only strip, no calendar or exact-alarm permission; runtime unverified. |
| 36 | `Nothing_Sun_Info` | pending | Depends on the pending Open-Meteo location/weather path. |
| 37 | `Nothing_Temp` | pending | Depends on the pending Open-Meteo location/weather path. |
| 38 | `Universal_Media_Player` | dependency | Media/notification access is prohibited for this app. |
| 39 | `iOS_Battery_Widget` | dependency | Uses a KomponentModule plus launcher media globals and a separately licensed icon font. |
| 40 | `material_notification_widget` | dependency | Notification access is prohibited for this app. |

## Implemented in this checkpoint

Ten of the 40 presets now have native providers: `Clean_Time_Date_Info`,
`Nothing_Battery`, `Nothing_Battery2`, `Nothing_Battery_Mini`,
`Nothing_Date_Time`, `Nothing_Date_Time2`, `Nothing_Digital`, `Nothing_Digital3`,
`Nothing_Digital4` and `Nothing_Mini_Calender`. All 16 registered providers are
build-verified only and remain runtime-unverified in this checkpoint. The
sanitized transcriptions are under `app/src/main/assets/kwgt/`, including
`clean_time_date_info.sanitized.json`,
`nothing_battery.sanitized.json`, `nothing_battery2.sanitized.json`,
`nothing_battery_mini.sanitized.json`,
`nothing_date_time.sanitized.json`, `nothing_date_time2.sanitized.json`,
`nothing_digital.sanitized.json`, `nothing_digital3.sanitized.json`,
`nothing_digital4.sanitized.json`, and
`nothing_mini_calender.sanitized.json`.

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

Sanitized out of the assets and implementations: the author email, the background
`SINGLE_TAP` intent string, and the bundled `ndot-47.ttf`. Dotwel's original
circular-dot glyphs replace the font where needed. No preset archive, APK,
preview JPEG, font or icon is bundled. The gallery preview calls the same
renderer as the provider.

## Not counted as presets

`docs/previews/*.png` light/dark images in the upstream repository are preview
renders, not separate widgets. They are neither copied nor counted in the 40.

## Weather note

Weather-related presets above remain pending. The partial Open-Meteo domain and
transport source in `weather/` is not registered as a provider and is isolated
from the current widget set. Personal, noncommercial use is within Open-Meteo
terms with attribution; any separate commercial launcher use must be reviewed
before shipping.
