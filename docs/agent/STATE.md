# Build state

- phase: 1 - first widget checkpoint
- branch: agent/live-date-45044779859
- version: 0.1.1 (versionCode 2)

## Done
- Dotwel Clock and Dotwel Date widgets, three styles, and the settings/preview
  activity are implemented.
- The settings preview date is now a live `TextClock` showing weekday, month,
  day-of-month and year. The pattern comes from
  `DateFormat.getBestDateTimePattern(Locale.getDefault(), "yMMMMEEEEd")` so
  ordering follows the device locale; the system `TextClock` handles time-zone
  changes without alarm or service permissions.
- Removed the stale hardcoded `preview_date_caption` string.
- The preview card can grow beyond its 220dp minimum, so the date wraps without
  clipping at larger font scales.

## Checks (actual)
- `./gradlew assembleDebug` (JDK 21): BUILD SUCCESSFUL, debug APK produced.
- `./gradlew lintDebug`: 1 pre-existing `NewApi` error in `values/themes.xml`
  (`windowLightNavigationBar`, API 27 vs minSdk 26) plus existing warnings. No
  new issue comes from this change; the lint error was left alone as out of scope.

## Next / remaining gaps
- Home-widget placement, update, resize and style behavior on an Android host are
  still under separate runtime validation. No emulator work was done in this step.
- Not Play-ready.

## Blockers
- None for this coding step.
