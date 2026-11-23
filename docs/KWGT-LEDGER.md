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
| 1 | `Clean_Time_Date_Info` | pending | Time/date layer conversion not started. |
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
| 19 | `Nothing_Battery2` | pending | Implementable next; needs progress bar, charging state and remaining-time translation. |
| 20 | `Nothing_Battery_Mini` | **implemented** | Native `BatteryMiniWidgetProvider`; real system level; runtime unverified. |
| 21 | `Nothing_CPU` | dependency | Per-process CPU/system usage needs restricted or unreliable data paths. |
| 22 | `Nothing_Date_Time` | **implemented** | Native `KwgtDateTimeWidgetProvider`; live text clocks without optional exact-alarm access; runtime unverified. |
| 23 | `Nothing_Date_Time2` | **implemented** | Native `KwgtDateTime2WidgetProvider`; upper-left date stack plus lower-right hour/minute discs; live text fallback without exact-alarm access; runtime unverified. |
| 24 | `Nothing_Date_Time3` | pending | Depends on the pending Open-Meteo weather/temperature/AQI path. |
| 25 | `Nothing_Digital` | **implemented** | Native `KwgtGradientClockWidgetProvider`; right-edge gradient circles, 12-hour hh and red minute chip; live text fallback without exact-alarm access; runtime unverified. |
| 26 | `Nothing_Digital2` | pending | Digital layer conversion queued; can reuse the existing minute schedule. |
| 27 | `Nothing_Digital3` | pending | Digital layer conversion queued. |
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

`Nothing_Battery`, `Nothing_Battery_Mini`, `Nothing_Date_Time`,
`Nothing_Date_Time2`, and `Nothing_Mini_Calender` have real native providers in
this source tree. Their sanitized `preset.json` transcriptions are
`app/src/main/assets/kwgt/nothing_battery.sanitized.json`,
`app/src/main/assets/kwgt/nothing_battery_mini.sanitized.json`,
`app/src/main/assets/kwgt/nothing_date_time.sanitized.json`,
`app/src/main/assets/kwgt/nothing_date_time2.sanitized.json`, and
`app/src/main/assets/kwgt/nothing_mini_calender.sanitized.json`.

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
