# Dotwel-Widgets
Nothing OS-inspired Android home-screen widgets by Dotwel Labs

## Current checkpoint (0.1.7, versionCode 8)

- `com.dotwelwidgets.dotwellabs`, minSdk 26, compile/target SDK 36.
- Eight registered providers: dot clock, date, Analog Bold, Analog Scale, world
  clock, quotes, and native Nothing Battery plus Nothing Battery Mini
  conversions from the MIT KWGT-Widgets presets. The full 40-preset source-port
  status is in `docs/KWGT-LEDGER.md`; only those two presets are implemented, the
  rest remain pending or dependency-blocked.
- Gallery cards put Add directly below each preview. Ready cards ask the current
  launcher to pin the matching provider through the platform request-pin API;
  the launcher owns the confirmation. Unsupported pinning shows a readable
  manual HOME > Widgets path. Pending cards stay disabled and say what is
  missing.
- World Clock configuration is transferred to the actual `appWidgetId` returned
  by the launcher, is stored per instance, and uses a unique pending token so a
  cancelled pin cannot leak into another instance. Adding World from the system
  widget picker defaults to the real local timezone plus UTC, never a hardcoded
  city that was not selected.
- Dotwel Clock/Analog: when the user enables optional Alarms & reminders access,
  the placed widget draws the translated circular-dot or analog bitmap sized from
  the host's Android 12+ exact widget sizes (`OPTION_APPWIDGET_SIZES`) or, on
  older hosts, from a coherent portrait/landscape min/max pair. The bitmap is
  drawn to the ImageView content area, so widget padding is not applied twice.
  Updates use a one-minute `PendingIntent` alarm. Without that access the clock
  shows the live system `TextClock`; analog shows the platform `AnalogClock`.
- Dotwel Battery and Battery Mini read the real system battery level
  (`BATTERY_PROPERTY_CAPACITY` with a sticky `ACTION_BATTERY_CHANGED` fallback).
  They use no fake charge value,
  no new permission and no background service. The level is read when the widget
  is refreshed: on the system widget update period (best-effort, 30-minute
  minimum), resize, manual refresh and app resume. `ACTION_BATTERY_CHANGED` is
  the sticky data source; Android does not deliver it to manifest receivers, so
  per-change push updates are not claimed.
- Settings/preview activity: choose black, translucent or transparent; see a live
  preview; refresh placed widgets; open this app's Alarms & reminders settings.
- Permissions: optional `SCHEDULE_EXACT_ALARM` only. No `USE_EXACT_ALARM`,
  INTERNET, foreground service, accessibility, overlay, notification/media access,
  location, calendar, or boot permission. The app never grants alarm access
  itself.

## Limits

- 0.1.7 has a passing debug build and lint, but no runtime or screenshot
  verification in this checkpoint. Do not treat a successful build as proof of
  launcher pinning, battery-refresh timing, or pixel output.
- Minute updates are best-effort. Deep idle and doze may defer the update, and
  this is not promised as minute-accurate.
- If the user revokes Alarms & reminders access, Android stops the app and
  cancels its alarms. A placed clock falls back to the live system clock on its
  next update or resize; the fallback is not instant while the app is dead.
- Weather remains pending: only a partial, unregistered Open-Meteo domain and
  transport layer exists. No weather widget is enabled.

Build with `./gradlew assembleDebug`. See `docs/ATTRIBUTION.md` for third-party
notices, `docs/KWGT-LEDGER.md` for the 40-preset status, and
`docs/agent/STATE.md` for verified status. Artifact checksums are recorded in
`checkpoints/POLISH-SHA256SUMS`.
