# Build state

- phase: 3 - final host sizing and copy polish
- branch: agent/final-host-sizing-45059644402
- version: 0.1.5 (versionCode 6)
- baseline preserved: 0.1.4 at commit
  `5ebaae967efb6110c1d11a5e43130ee3457dd7ac` and on
  `checkpoint/dot-core-45058243427`

## Done in this step

- Replaced the legacy `OPTION_APPWIDGET_MIN_WIDTH` + `MIN_HEIGHT` bitmap target
  with the documented Android 12+ exact-size route. `ClockWidgetRenderer` reads
  `OPTION_APPWIDGET_SIZES` from the host options and the provider builds a
  `RemoteViews` map keyed by those `SizeF` values, so the launcher selects the
  bitmap for the actual size and orientation.
- When a host does not provide exact sizes (or on API < 31), the renderer uses
  a coherent legacy pair: portrait is min width x max height; landscape is max
  width x min height. It no longer pairs two range minima from different
  orientations.
- Bitmaps are rendered to the ImageView content area (host size minus the
  `widget_clock.xml` 8dp root padding), so the layout padding is not applied a
  second time by `fitCenter`. The existing dot margin is unchanged.
- Kept total bitmap pixels across all size variants at `480 * 480` (about
  0.9 MB ARGB_8888) to stay under the RemoteViews/Binder budget; at most four
  distinct size variants are rendered.
- Preserved the native `TextClock` fallback, the existing tap action, the
  background styles, and the proven `ClockTickScheduler` next-minute path. No
  scheduler, permission, SDK, package, signing, or workflow changes.
- Rewrote `exact_alarm_explainer` in plain words. Removed the literal
  `\x2019` escapes that rendered as `appx2019s`, and added an honest note that
  deep idle or Doze can delay updates. Softened the enabled-status and widget
  description copy from unconditional "each minute" wording.
- Bumped to versionCode 6 / versionName 0.1.5. Updated README, this state file,
  and checkpoint notes. Removed fixed APK hash claims from prose; the artifact
  checksum reference is `checkpoints/POLISH-SHA256SUMS`.

## Checks (actual this run)

- `./gradlew assembleDebug` (JDK 21, Gradle 9.9.1): BUILD SUCCESSFUL.
- `aapt2 dump badging` on `app/build/outputs/apk/debug/app-debug.apk`: package
  `com.dotwelwidgets.dotwellabs`, versionCode 6, versionName 0.1.5, minSdk 26,
  targetSdk 36.
- `aapt2 dump permissions`: only `android.permission.SCHEDULE_EXACT_ALARM`.
- `aapt2 dump resources` confirms `exact_alarm_explainer` no longer contains the
  literal `x2019` text and the new deep-idle wording is compiled in.
- `./gradlew lintDebug` (JDK 21): BUILD SUCCESSFUL; 0 errors, 10 warnings
  (the same dependency/plugin update notices and unused upstream MIT raw
  resource as before; no new sizing or string lint issues). No lint baseline or
  global suppressions were added.
- Debug APK saved as `checkpoints/dotwel-widgets-0.1.5-debug.apk`; checksums in
  `checkpoints/POLISH-SHA256SUMS`.

## Not done / remaining gaps

- No emulator or device was run in this coding step. The actual HOME widget was
  not re-captured, so the final visual result of the sizing change is still
  pending independent verification. The in-app preview is not a substitute.
- Still to verify on a real HOME host: default 2-cell dot fill, resize/style,
  12/24-hour, minute-boundary bitmap change while not foreground, denied-then-
  granted access, revocation fallback, and process-restart behavior.
- Known honest limits: updates can be deferred in deep idle/doze; after external
  revocation Android stops the app and cancels alarms, so fallback happens on
  the next update or resize, not instantly.
- Not polished and not Play-approved. Acceptance requires the runtime checks
  above against the actual HOME widget.

## Blockers

- None for the build. Actual-home final visual verification remains outstanding.
