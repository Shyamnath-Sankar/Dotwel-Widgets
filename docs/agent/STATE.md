# Build and verification state

- Candidate: Dotwel Widgets 0.1.6 (versionCode 7).
- Tested candidate commit: e6d01e1edf483ba004ab47d3a90f952a0d02bb5e. Application implementation was written at e79951cd1726a511fe05b308d23d146b4d15c1dd; final APK preservation followed at e6d01e1e.
- Candidate branch: agent/add-widget-flow-45113975964.
- This is a debug test candidate, not a Play-approved release.
- Default main remains unchanged. Launcher integration remains deferred.

## Exact candidate download

Use only `checkpoints/dotwel-widgets-polish-debug.apk` on this candidate branch.
It is 915019 bytes. `checkpoints/POLISH-SHA256SUMS` records its final checksum.
The verified SHA-256 is:

`9b8e2a4d5b27c391fe7d8f8cbd38851fc3376c15846dc1381864b661bfefa6be`

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

Final artifact verification: the final saved POLISH APK is 915019 bytes and
matches its committed POLISH-SHA256SUMS and the independent runtime's installed
artifact hash above. An earlier revision of this STATE file reported an
intermediate build hash; runtime 45115605374 rejected that mismatch before
installation. This corrected record describes the final saved APK actually
tested by runtime 45117118514. No byte-identical clean-rebuild claim is made
for this final saved artifact.

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
can be checked deterministically. Normal launches do not set it. The independent runtime below exercised real supported pin requests and
native launcher confirmation. The unsupported dialog was exercised through
this DEBUG-only override, not through a genuinely unsupported OEM launcher.

## Independent 0.1.6 runtime verification

Run: https://github.com/Shyamnath-Sankar/Dotwel-Widgets/actions/runs/45117118514

Original captures:
https://github.com/Shyamnath-Sankar/Dotwel-Widgets/tree/checkpoint/runtime-45117118514/runtime-evidence

The run installed the exact final APK hash above on an Android 15 Pixel 7
emulator. Original screenshot pixels were independently inspected, including:

- Add Clock and Add Date visible on the first screen without scrolling at
  normal and 130% system font scale. Previews clearly labeled non-draggable.
- The real native launcher confirmation for both Clock and Date, opened by
  tapping the in-app Add buttons. No shell widget binding was used.
- Clock cancellation left no Clock view on HOME. A retry then accepted and
  placed Clock. Accepting Date placed the native Date beside Clock.
- DEBUG-only unsupported override showed readable manual HOME / Widgets
  instructions for Clock and Date. Back, retry, and Got it worked. This is
  dialog-path coverage, not genuine unsupported-launcher compatibility proof.
- Placement worked with optional alarm access still off, showing the live
  native Clock fallback. The native Android access toggle was then used to
  enable dotted minute updates.
- Actual HOME circular-dot bitmap advanced from 05:26 to 05:27 after the
  background process was killed and its PID was confirmed absent. Device
  time was 05:27. Clock dots and serif Date were round/unclipped and readable.

Placement is established by the real HOME views and appwidget diagnostic
captures, not by requestPinAppWidget returning true or by a success toast.
The callback toast itself was not independently verified. The user device's
unknown launcher remains untested; launchers own support and confirmation.
No source rebuild or main-branch application promotion followed this test.

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

- Native in-app placement is verified on the Android 15 Pixel emulator only.
  Other launchers and the user device remain untested. Unsupported fallback
  dialog coverage used the explicit DEBUG-only override.
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
