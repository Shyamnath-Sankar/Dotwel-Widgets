# Build state

- phase: 2 - placed bitmap clock + optional exact-alarm update path
- branch: agent/live-dot-clock-45053646259
- version: 0.1.4 (versionCode 5)
- baseline preserved: 0.1.2 reliability at commit
  `0bc4988452e170683583efc3a262c872e3388650`

## Done in this step

- Connected the saved circular-dot renderer to the actual placed Clock
  `RemoteViews`. When access is allowed, `ClockWidgetProvider` renders an
  `ImageView` bitmap from the widget's own option dimensions and hides the
  `TextClock`; otherwise it hides the bitmap and leaves the live system
  `TextClock` visible.
- Added `ClockWidgetRenderer` with a `480 * 480` pixel cap (about 0.9 MB for
  ARGB_8888) to stay under the RemoteViews/Binder bitmap budget. Actual widget
  aspect ratio is preserved when clamping.
- Added `ClockTickScheduler`, which uses a durable broadcast `PendingIntent`
  with `AlarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, ...)` aligned to
  the next minute boundary. It checks `canScheduleExactAlarms()` on API 31+,
  recalculates the next boundary from the current time on every run, cancels
  when access is denied or no placed clocks remain, and catches
  `SecurityException`.
- Added `SCHEDULE_EXACT_ALARM` only. No `USE_EXACT_ALARM`, service, boot
  permission, notification, accessibility, overlay, Internet, billing, or
  signing change.
- Re-renders and reschedules on widget update, resize, enable, delete, the
  minute tick, time/time-zone/date changes (manifest filters on the provider
  receiver), and package/device restart through the system app-widget update.
- Added settings UI that explains the optional access, shows enabled/denied
  state, and launches `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM` for this
  package. The app never grants access itself and never blocks the app or the
  native Date widget on denial. A dynamically registered receiver plus
  `onResume` refreshes widgets after a grant.
- Set a localized current-time `contentDescription` on the bitmap clock.
- Kept the native live serif Date `TextClock` work and existing tap/style
  behavior.
- Bumped to versionCode 5 / versionName 0.1.4. Updated README, ATTRIBUTION, this
  state file, checkpoint notes, APK, and checksum. Removed stale DotGothic16
  references.

## Checks (actual this run)

- `./gradlew assembleDebug` (JDK 21, Gradle 9.9.1): BUILD SUCCESSFUL.
- `aapt2 dump badging` on `app/build/outputs/apk/debug/app-debug.apk`: package
  `com.dotwelwidgets.dotwellabs`, versionCode 5, versionName 0.1.4, minSdk 26,
  targetSdk 36.
- `aapt2 dump permissions`: includes `android.permission.SCHEDULE_EXACT_ALARM`.
- `./gradlew lintDebug` (JDK 21): BUILD SUCCESSFUL; 0 errors, 10 warnings
  (OldTargetApi, API-31 widget metadata UnusedAttribute, Gradle/plugin update
  notices, unused upstream MIT raw resource). No exact-alarm lint error.
- Final APK SHA-256:
  `d8b9c732389516c5e892b250a6f58aeb6bc78627dc9c5bfd7e685f319a43a8f2`
  (`checkpoints/POLISH-SHA256SUMS`).

## Not done / remaining gaps

- No emulator or device was run in this coding step. The actual HOME bitmap
  path, denied-then-granted Alarms & reminders flow, real pixel change across a
  minute boundary while the app is not foreground, resize/style, 12/24-hour,
  restart/process lifetime, and revocation were NOT runtime-verified here.
- Known honest limits: exact alarms can be deferred in deep idle/doze; after
  external revocation Android stops the app and cancels alarms, so fallback
  happens on the next update or resize, not instantly.
- Not polished and not Play-approved. Acceptance requires the runtime checks
  above against the actual HOME widget, not the in-app preview.

## Blockers

- None for the build. Runtime verification remains outstanding.
