# Build state

- phase: 1 - first widget checkpoint
- branch: agent/compat-polish-45047829143
- version: 0.1.2 (versionCode 3)

## Done
- Removed the redundant `android:windowLightNavigationBar` `false` item from
  `values/themes.xml`. It is the dark default, so dropping it preserves behavior;
  the attribute does not exist on API 26. No lint suppression, minSdk change, or
  lint failure relaxation was used.
- The settings live full-date preview now requests the `yyyyMMMMEEEEd` skeleton,
  so the `TextClock` renders a four-digit year (for example 2026) instead of a
  two-digit year. Locale-specific field order, weekday, month and day-of-month
  still come from `DateFormat.getBestDateTimePattern`, and the preview remains a
  live `TextClock`.
- Bumped to versionCode 3 / versionName 0.1.2.
- Replaced the checkpoint debug APK and checksum with the 0.1.2 build.

## Checks (actual)
- `./gradlew assembleDebug` (JDK 21, Gradle 9.9.1): BUILD SUCCESSFUL;
  `app/build/outputs/apk/debug/app-debug.apk` was produced.
- `aapt2 dump badging` on the APK: package `com.dotwelwidgets.dotwellabs`,
  versionCode 3, versionName 0.1.2, minSdk 26, targetSdk 36.
- `./gradlew lintDebug` (JDK 21): BUILD SUCCESSFUL; 0 errors, 15 warnings. The
  prior `NewApi` error for `windowLightNavigationBar` is gone. Remaining
  warnings are existing categories: OldTargetApi (targetSdk 36 while 37 is
  available), UnusedAttribute for API 31 widget metadata, Gradle/plugin update
  notices, root overdraw, unused raw license texts, and UselessParent.
- Checkpoint APK SHA-256:
  `a3aaab4efd18a575813fd4a068c2e6975986650e794bc946cadb18b622d45e80`.

## Next / remaining gaps
- Runtime validation of the four-digit year on an API 35 host and home-widget
  placement, update, resize and style behavior remain unverified in this slice.
  No emulator work or runtime screenshot was done here.
- Not Play-ready. The host typography design decision is still pending.

## Blockers
- None for this coding step.
