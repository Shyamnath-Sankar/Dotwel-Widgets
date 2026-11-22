# Dotwel-Widgets
Nothing OS-inspired Android home-screen widgets by Dotwel Labs

## Current checkpoint (0.1.5)

- `com.dotwelwidgets.dotwellabs`, minSdk 26, compile/target SDK 36.
- Dotwel Clock: when the user enables optional Alarms & reminders access, the
  placed widget draws the circular-dot clock as a bitmap sized from the host's
  Android 12+ exact widget sizes (`OPTION_APPWIDGET_SIZES`) or, on older hosts,
  from a coherent portrait/landscape min/max pair. The bitmap is drawn to the
  ImageView content area, so the widget padding is not applied twice. Updates
  use a one-minute `PendingIntent` alarm. Without that access it shows the live
  system `TextClock` fallback.
- Dotwel Date: system-updated platform serif day number plus an abbreviated red
  weekday.
- Settings/preview activity: choose a style, see a live preview, refresh placed
  widgets, and open this app's Alarms & reminders settings.
- Permissions: optional `SCHEDULE_EXACT_ALARM` only. No `USE_EXACT_ALARM`,
  INTERNET, foreground service, accessibility, overlay, notification access,
  location, or boot permission. The app never grants alarm access itself.

## Limits

- Minute updates are best-effort. Deep idle and doze may defer the update, and
  this is not promised as minute-accurate.
- If the user revokes Alarms & reminders access, Android stops the app and
  cancels its alarms. A placed clock falls back to the live system text clock on
  its next update or resize; the fallback is not instant while the app is dead.

Build with `./gradlew assembleDebug`. See `docs/ATTRIBUTION.md` for third-party
notices and `docs/agent/STATE.md` for verified status. Artifact checksums are
recorded in `checkpoints/POLISH-SHA256SUMS`; the deterministic CI rebuild is
authoritative.
