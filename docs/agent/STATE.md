# Build and verification state

- Candidate: Dotwel Widgets 0.1.6 (versionCode 7).
- Tested application source: e79951cd1726a511fe05b308d23d146b4d15c1dd.
- Candidate branch: agent/add-widget-flow-45113975964.
- This is a debug test candidate, not a Play-approved release.
- Default main remains unchanged. Launcher integration remains deferred.

## Exact candidate download

Use only `checkpoints/dotwel-widgets-polish-debug.apk` on this candidate branch.
It is 915019 bytes. `checkpoints/POLISH-SHA256SUMS` records its final checksum.
The verified SHA-256 is:

`1ef42595c5fb6eab84f0b6318d593e647a66194dca63c7c18083eac559bd13ee`

Download page:
https://github.com/Shyamnath-Sankar/Dotwel-Widgets/blob/agent/add-widget-flow-45113975964/checkpoints/dotwel-widgets-polish-debug.apk

The POLISH APK and POLISH-SHA256SUMS are the authoritative 0.1.6 artifacts.
No extra version-named APK with different bytes is kept in `checkpoints/`.

## What changed in 0.1.6

- The first screen now shows Add Clock and Add Date before the previews and the
  longer technical copy.
- Each action checks `AppWidgetManager.isRequestPinAppWidgetSupported` at click
  time and calls `requestPinAppWidget` with the exact `ClockWidgetProvider` or
  `DateWidgetProvider` component.
- A `true` return only shows a neutral request-sent message. The success
  callback is used only to show a placed confirmation after the platform
  reports widget creation.
- Cancellation produces no callback and no success message. The screen remains
  usable for another attempt.
- Unsupported, false-return, and foreground/security-error paths show a
  readable fallback dialog with HOME > long-press empty area > Widgets >
  Dotwel Widgets > Clock or Date. Cancel and Back dismiss it.
- Previews are labeled as previews and explicitly not draggable. Placement does
  not require Alarms & reminders access.
- The renderer, scheduler, widget providers, sizing/styles, optional alarm UX,
  permissions, minSdk, and targetSdk are unchanged. No new permission, service,
  dependency, or exported test component was added.

## Build and implementation

`./gradlew assembleDebug lintDebug` succeeded in this coding step. Lint reported
0 errors and 10 pre-existing warnings: target/compile SDK update notices, API-31
widget attributes, Gradle/AGP update notices, and the bundled MIT text resource.
No suppressions were added.

Deterministic rebuild check: two clean `./gradlew clean assembleDebug` runs
produced the identical SHA-256 shown above. The final POLISH APK was copied from
that deterministic build and `checkpoints/POLISH-SHA256SUMS` was regenerated.

The actual home-screen clock uses circular-dot bitmap RemoteViews when optional
Alarms & reminders access is enabled. Android 12+ host sizes select size-keyed
layouts. Older hosts use coherent portrait/landscape size estimates. Combined
bitmap pixels are bounded. Native TextClock remains the live denial fallback.
The serif Date and red abbreviated weekday use the native live Date path.

The sizing correction did not change the scheduler, permissions, package,
SDK, signing or services. Settings text no longer contains the malformed
apostrophe escape and explains that deep idle can delay updates.

## Developer verification note: DEBUG-only unsupported-flow override

This is not an in-app setting and cannot affect a release build. The extra is
honored only when the installed app is debuggable and the value is explicitly
true:

`adb shell am start -n com.dotwelwidgets.dotwellabs/.MainActivity --ez com.dotwelwidgets.dotwellabs.extra.DEBUG_FORCE_PIN_UNSUPPORTED true`

It makes the Add actions show the manual fallback dialog so the unsupported UI
can be checked deterministically. Normal launches do not set it. A separate
runtime pass is still required for the real supported pin request, native
launcher confirmation, accept, cancel, and the actual HOME fallback path.

## Earlier 0.1.5 verified checkpoint (retained)

The earlier exact 0.1.5 candidate and its independently verified HOME results
remain on their own branch. They are historical and are not this 0.1.6 build.

- 0.1.5 APK:
  https://github.com/Shyamnath-Sankar/Dotwel-Widgets/blob/agent/final-host-sizing-45059644402/checkpoints/dotwel-widgets-polish-debug.apk
- 0.1.5 SHA-256:
  `9edb8b744810eaa40856eb47d4b505eb50a57e55f6ee25b4d21154523ba53df3`
- Coding run: https://github.com/Shyamnath-Sankar/Dotwel-Widgets/actions/runs/45059644402
- Runtime run: https://github.com/Shyamnath-Sankar/Dotwel-Widgets/actions/runs/45061373320
- Original captures:
  https://github.com/Shyamnath-Sankar/Dotwel-Widgets/tree/checkpoint/runtime-45061373320/runtime-evidence
- Remaining-stage run:
  https://github.com/Shyamnath-Sankar/Dotwel-Widgets/actions/runs/45063341690
- Remaining-stage captures:
  https://github.com/Shyamnath-Sankar/Dotwel-Widgets/tree/checkpoint/runtime-45063341690/runtime-evidence

Verified on 0.1.5:

- Default placed clock has large, round, unclipped dotted digits beside Date.
- Actual HOME bitmap advances 07:38 to 07:39 after the background app process
  was killed and its PID was confirmed absent.
- Access denial uses the native live clock. Android access was revoked using
  its actual toggle; native fallback was confirmed after explicit app resume.
  Regrant restored the actual HOME bitmap.
- Black, translucent and transparent backgrounds work.
- 12-hour and 24-hour display work. Both widgets fit at 130% system font scale.
- Corrected settings copy is readable and includes the idle-delay caveat.
- Host bounds changed from [67,546,519,1134] to [67,546,519,1449], then returned
  to [67,546,519,1134]. Dots remained round, centered and unclipped.
- After 75 seconds of emulator-forced deep idle, returning HOME without opening
  the app showed bitmap 20:01 matching the device's 20:01.
- Reboot restored the actual HOME bitmap at 20:02 with Date still visible.

## Limits and remaining work

- The 0.1.6 Add Clock/Add Date buttons, native launcher confirmation,
  accept/cancel behavior, and unsupported-flow HOME fallback still need
  independent runtime pixel verification. Do not mark them verified from a
  successful build.
- 0.1.5 resize verification covers growth and shrink-back to the default size.
  A smaller-than-default compact host was not verified.
- The idle check is a bounded emulator observation. It does not promise exact
  minute updates during long Doze or reliability across every OEM launcher.
- Android stops the app and cancels alarms when optional access is revoked.
  Fallback is confirmed on the next app resume/update/resize, not instantly
  while the app is stopped.
- Current exact-candidate tests were on an Android 15 Pixel emulator. Physical
  device and other launcher testing remain useful before release.
- Prior native Date day-transition behavior was tested on the preserved native
  baseline; the 0.1.5 runtime did not wait through another day.
- Play policy requirements were considered, but no Play submission or approval
  occurred. Do not label this artifact Play-approved or production-ready.
- Source promotion to default main and launcher integration are not part of
  this candidate-only work.
