# Dotwel-Widgets
Nothing OS-inspired Android home-screen widgets by Dotwel Labs

## Current checkpoint (0.1.6)

- `com.dotwelwidgets.dotwellabs`, minSdk 26, compile/target SDK 36.
- The first screen has clearly labeled Add Clock and Add Date actions before the
  previews. They ask the current launcher to pin the matching widget through the
  platform request-pin API; the launcher owns the confirmation. If pinning is
  unavailable, the app shows a readable manual path: home screen, long-press an
  empty area, Widgets, Dotwel Widgets, then Clock or Date. The previews are
  labeled as previews and are not draggable. Placing either widget does not
  require Alarms & reminders access.
- Dotwel Clock: when the user enables optional Alarms & reminders access, the
  placed widget draws the circular-dot clock as a bitmap sized from the host's
  Android 12+ exact widget sizes (`OPTION_APPWIDGET_SIZES`) or, on older hosts,
  from a coherent portrait/landscape min/max pair. The bitmap is drawn to the
  ImageView content area, so the widget padding is not applied twice. Updates
  use a one-minute `PendingIntent` alarm. Without that access it shows the live
  system `TextClock` fallback.
- Dotwel Date: system-updated platform serif day number plus an abbreviated red
  weekday.
- Settings/preview activity: add either widget through the launcher prompt or
  manual fallback, choose a style, see a live preview, refresh placed widgets,
  and open this app's Alarms & reminders settings.
- Permissions: optional `SCHEDULE_EXACT_ALARM` only. No `USE_EXACT_ALARM`,
  INTERNET, foreground service, accessibility, overlay, notification access,
  location, or boot permission. The app never grants alarm access itself.

## Limits

- The 0.1.6 in-app pin path, native launcher confirmation, accept/cancel
  behavior, and the unsupported-flow manual fallback still need independent
  runtime pixel verification. A successful build does not confirm those flows.
- Minute updates are best-effort. Deep idle and doze may defer the update, and
  this is not promised as minute-accurate.
- If the user revokes Alarms & reminders access, Android stops the app and
  cancels its alarms. A placed clock falls back to the live system text clock on
  its next update or resize; the fallback is not instant while the app is dead.

Build with `./gradlew assembleDebug`. See `docs/ATTRIBUTION.md` for third-party
notices and `docs/agent/STATE.md` for verified status. Artifact checksums are
recorded in `checkpoints/POLISH-SHA256SUMS`; the deterministic rebuild recorded
in `docs/agent/STATE.md` is authoritative.
