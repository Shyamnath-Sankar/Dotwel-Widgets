# Batch native sweep plan

This plan covers every registered provider in the build. It is a sequential
manual/emulator checklist, not evidence that any row has passed. This
implementation job is build/test/lint only; native placement, resize, style and
data checks remain pending.

## Per-provider checks (run in this order)

For each row below:

1. Build installs and the provider appears in the system widget picker.
2. Add/pin through the app's individual Add button and through the system picker.
3. Place on a real Android host, confirm no "Couldn't add widget" or RemoteViews
   inflation error.
4. Resize smaller and larger; confirm content stays inside the widget and does
   not crash.
5. Switch black, translucent and transparent; confirm contrast and that no stale
   bitmap remains.
6. Confirm the data source is genuine (system clock, battery, cache, session or
   notification) and that missing data shows the documented empty/unavailable
   state.
7. Force-stop/revoke relevant access; confirm fallback and no fabricated data.
8. Remove the widget; confirm any scheduler cancels when the last instance goes.

## Registered providers

| # | Catalog id | Name | Group |
|---:|---|---|---|
| 1 | `digital` | Digital Clock | CLOCKS |
| 2 | `analog_bold` | Analog Bold | CLOCKS |
| 3 | `analog_scale` | Analog Scale | CLOCKS |
| 4 | `date` | Date | TIME |
| 5 | `world` | World Clock | TIME |
| 6 | `wx_nothing_temp` | Nothing Temp | WEATHER |
| 7 | `wx_nothing_sun` | Nothing Sun Info | WEATHER |
| 8 | `quotes` | Quotes | MEDIA |
| 9 | `kwgt_nothing_battery` | Battery Gauge | KWGT |
| 10 | `kwgt_nothing_battery_mini` | Battery Mini | KWGT |
| 11 | `kwgt_nothing_battery2` | KWGT Battery2 | KWGT |
| 12 | `kwgt_nothing_date_time` | KWGT Date Time | KWGT |
| 13 | `kwgt_nothing_date_time2` | KWGT Date Time 2 | KWGT |
| 14 | `kwgt_nothing_digital4` | KWGT Split Clock | KWGT |
| 15 | `kwgt_nothing_digital3` | KWGT Digital3 Dot Clock | KWGT |
| 16 | `kwgt_gradient_clock` | KWGT Gradient Clock | KWGT |
| 17 | `kwgt_analog2_clock` | KWGT Analog2 | KWGT |
| 18 | `kwgt_future_clock` | KWGT Future Clock | KWGT |
| 19 | `kwgt_nothing_analog3` | KWGT Analog3 | KWGT |
| 20 | `kwgt_analog_seconds_clock` | KWGT Analog Seconds | KWGT |
| 21 | `kwgt_nothing_analog5` | KWGT Analog5 | KWGT |
| 22 | `kwgt_nothing_analog5_transparent` | KWGT Analog5 Clear | KWGT |
| 23 | `kwgt_orbit_clock` | KWGT Orbit Clock | KWGT |
| 24 | `kwgt_mini_calendar` | KWGT Mini Calendar | KWGT |
| 25 | `kwgt_clean_info` | KWGT Clean Info | KWGT |
| 26 | `kwgt_ios_battery` | KWGT iOS Battery | KWGT |
| 27 | `kwgt_nothing_digital2` | KWGT Digital2 Minutes | KWGT |
| 28 | `kwgt_nothing_date_time3` | KWGT Date Time 3 Weather | KWGT |
| 29 | `kwgt_interstellar_endurance` | KWGT Endurance | KWGT |
| 30 | `kwgt_interstellar_endurance_analog` | KWGT Endurance Analog | KWGT |
| 31 | `kwgt_interstellar_endurance_analog_2` | KWGT Endurance Analog 2 | KWGT |
| 32 | `media_m3_expanded` | Material3 Media Expanded | MEDIA_PLAYER |
| 33 | `media_m3_output` | Material3 Media Output | MEDIA_PLAYER |
| 34 | `media_mini` | Mini Media Player | MEDIA_PLAYER |
| 35 | `media_minimal` | Minimal Media Player | MEDIA_PLAYER |
| 36 | `media_minimal2` | Minimal Media Player 2 | MEDIA_PLAYER |
| 37 | `media_minimal3` | Minimal Media Player 3 | MEDIA_PLAYER |
| 38 | `media_master` | Nothing Master Widget | MEDIA_PLAYER |
| 39 | `media_nothing_player` | Nothing Media Player | MEDIA_PLAYER |
| 40 | `media_nothing_player2` | Nothing Media Player2 | MEDIA_PLAYER |
| 41 | `media_nothing_player3` | Nothing Media Player3 | MEDIA_PLAYER |
| 42 | `media_universal` | Universal Media Player | MEDIA_PLAYER |
| 43 | `media_notification` | Notification Widget | MEDIA_PLAYER |

## Special checks

- `kwgt_nothing_digital2`: live central TextClock; relative labels refresh only
  with minute scheduler access.
- `kwgt_nothing_date_time3`: weather off shows setup text; cached weather shows
  current temperature/condition/US AQI; missing AQI shows `AQI --`.
- `kwgt_interstellar_*`: Android 12+ only; verify sampled seconds are not
  presented as live; verify style contrast and resize.
- `media_*` and `kwgt_master`/notification rows: verify access-off empty state,
  system-settings opt-in, masked text default, text opt-in, transport guards,
  revoke clears cached notification, and no background network.
- Weather rows: verify off-by-default, manual city only, hourly app-resume
  refresh cap, cache-only widget updates, and AQI failure isolation.

## Unregistered audit rows (not counted as providers)

- `Interstellar_Analog` — blocked on `IMG718befb261acf17a52fc205d18d0efe8`.
- `Interstellar_Analog_Khaki_Field_Murph_Auto` — blocked on five bitmap modules
  and `Roboto-Regular.ttf`.
- `Nothing_CPU` — unavailable; needs whole-device CPU counters Android does not
  expose to this app.
