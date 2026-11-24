# Dotwel-Widgets
Nothing OS-inspired Android home-screen widgets by Dotwel Labs

## Current checkpoint (0.1.8, versionCode 9)

- `com.dotwelwidgets.dotwellabs`, minSdk 26, compile/target SDK 36.
- Twenty-six registered providers: dot clock, date, Analog Bold, Analog Scale,
  world clock, quotes, and twenty native KWGT-Widgets conversions (Nothing
  Battery, Battery Mini, Battery2, Date Time, Date Time 2, Digital3, Digital4,
  Digital, Analog2, Analog3, Analog Seconds, Analog5, Analog5 Clear, and Orbit
  Clock). The full 40-preset source-port status is in `docs/KWGT-LEDGER.md`;
  twenty presets are implemented, the rest remain pending or
  dependency-blocked. `Nothing_Analog_`
  and `Nothing_Analog_2` are the exact source-seconds ports: their native second
  hands are offered only on Android 12+ (`android:hand_second`), the receivers are
  disabled below API 31, and second-tick fps is system configured and may be
  disabled by the OS.
- The front screen is a category grid: a dotted HOME heading, a short subtitle,
  an actionable dotted-update control, and rounded square category tiles with a
  real representative preview or a Dotwel motif. Each tile shows the category
  name and its honest ready count. Tapping a ready category opens a scrollable
  rounded sheet with a large preview, selected name, real provider minimum size,
  live mode label, description, red Add button, position dots, and 48dp
  Prev/Next targets; swiping the preview changes selection. Unavailable
  categories stay visible with `0 ready` and add nothing. Add asks the current
  launcher to pin the matching provider through the platform request-pin API;
  the launcher owns the confirmation. Unsupported pinning shows a readable manual
  HOME > Widgets path. The 16 not-yet-available catalog items are collapsed
  under an Upcoming section with an honest reason on expansion, so they do not
  obscure the available collection.
- Clock and analog previews follow the real current mode. With Alarms &
  reminders access off, the preview uses the same live native `TextClock` or
  platform `AnalogClock` fallback as the placed widget; no dotted bitmap is
  shown or implied. With access on, the preview calls the same dotted/styled
  renderer as the widget, and returning from Android settings refreshes it.
  Every gallery image is labeled as a preview. Source/provenance text lives in
  an About & source credits dialog with the bundled MIT license texts.
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
- KWGT Mini Calendar is a separate date-only 4x1 provider. It renders the
  source five-day strip (-2 through +2) with real calendar arithmetic, scales
  the author margin to keep all five columns on-screen, highlights today with a
  widgetWidth/5 rounded block and red dot, and does not request calendar or
  Alarms & reminders access. Refreshes use Android's inexact 30-minute
  `updatePeriodMillis` plus date/time/timezone/boot, app-refresh and resize
  callbacks; exact midnight delivery is not promised.
- Dotwel Battery, Battery Mini, KWGT Battery2 and KWGT Clean Info read the real
  system battery level (`BATTERY_PROPERTY_CAPACITY` with a sticky
  `ACTION_BATTERY_CHANGED` fallback). They use no fake charge value, no new
  permission and no background service. `BatteryRefreshScheduler` arms an
  inexact, non-wakeup `ELAPSED_REALTIME` window only while one of those four
  widgets is placed: about 15 minutes after each refresh with 10 minutes of
  delivery flex, so a delivery can arrive up to roughly 25 minutes later and
  Doze may defer it. Removing the last battery widget cancels the alarm.
- While the app process is alive a dynamic `ACTION_BATTERY_CHANGED` receiver in
  `DotwelApp` refreshes only the battery-capable providers on level/status
  changes. Android does not deliver manifest `BATTERY_CHANGED` receivers to a
  dead process, so after a process kill the recovery path is the inexact alarm
  above, not an instant push. Refresh also runs on update, resize,
  pin/app-start and the manual refresh button. The visible percentage is a
  bounded best-effort snapshot; no hard 15-minute SLA or live/instant background
  update is claimed.
- Settings/preview activity: choose black, translucent or transparent; browse
  previews; refresh placed widgets; open this app's Alarms & reminders settings.
  Previews render only when visible or near-visible, use a bounded pixel budget,
  and do not recycle a bitmap that may still be drawing.
- Permissions: optional `SCHEDULE_EXACT_ALARM` only. No `USE_EXACT_ALARM`,
  INTERNET, foreground service, accessibility, overlay, notification/media access,
  location, calendar, or boot permission. The app never grants alarm access
  itself.

## Limits

- 0.1.8 has a passing debug build and lint, but no runtime or screenshot
  verification in this checkpoint. Do not treat a successful build as proof of
  launcher pinning, battery-refresh timing, or pixel output.
- Minute updates are best-effort. Deep idle and doze may defer the update, and
  this is not promised as minute-accurate.
- Battery refreshes are best-effort too. The inexact alarm is not an exact
  15-minute guarantee, is deferred by Doze and does not wake the device; use the
  app's Refresh action when an immediate reading is needed.
- If the user revokes Alarms & reminders access, Android stops the app and
  cancels its alarms. A placed clock falls back to the live system clock on its
  next update or resize; the fallback is not instant while the app is dead.
- Weather remains pending: only a partial, unregistered Open-Meteo domain and
  transport layer exists. No weather widget is enabled.

Build with `./gradlew assembleDebug`. See `docs/ATTRIBUTION.md` for third-party
notices, `docs/KWGT-LEDGER.md` for the 40-preset status, and
`docs/agent/STATE.md` for verified status. Artifact checksums are recorded in
`checkpoints/POLISH-SHA256SUMS`.
