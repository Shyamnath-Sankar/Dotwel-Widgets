# Build and verification state

- Candidate: Dotwel Widgets 0.1.5 (versionCode 6).
- Tested application source: c46d54754af46cd833975d1f80f74e337e11b7cd.
- Candidate branch: agent/final-host-sizing-45059644402.
- This is a polished Clock/Date debug test candidate, not a Play-approved release.
- Default main remains unchanged. Launcher integration remains deferred.

## Exact tested download

Use only `checkpoints/dotwel-widgets-polish-debug.apk` on this candidate branch.
It is 910811 bytes. `checkpoints/POLISH-SHA256SUMS` records its final checksum.
The verified SHA-256 is:

`9edb8b744810eaa40856eb47d4b505eb50a57e55f6ee25b4d21154523ba53df3`

Download page:
https://github.com/Shyamnath-Sankar/Dotwel-Widgets/blob/agent/final-host-sizing-45059644402/checkpoints/dotwel-widgets-polish-debug.apk

The extra version-named APK was an earlier build with different bytes. It was
removed to prevent installing the wrong artifact. The final POLISH APK and
checksum were not changed or rebuilt during this documentation cleanup.

## Build and implementation

Coding run:
https://github.com/Shyamnath-Sankar/Dotwel-Widgets/actions/runs/45059644402

The coding step reached its time limit. Subsequent deterministic assembleDebug
and lintDebug both succeeded, and source plus the final APK were preserved.
The run's overall failure status reflects that coding timeout, not a failed
assemble or lint result.

The actual home-screen clock uses circular-dot bitmap RemoteViews when optional
Alarms & reminders access is enabled. Android 12+ host sizes select size-keyed
layouts. Older hosts use coherent portrait/landscape size estimates. Combined
bitmap pixels are bounded. Native TextClock remains the live denial fallback.
The serif Date and red abbreviated weekday use the native live Date path.

The sizing correction did not change the scheduler, permissions, package,
SDK, signing or services. Settings text no longer contains the malformed
apostrophe escape and explains that deep idle can delay updates.

## Independent actual HOME verification

Exact candidate runtime:
https://github.com/Shyamnath-Sankar/Dotwel-Widgets/actions/runs/45061373320

Saved original captures:
https://github.com/Shyamnath-Sankar/Dotwel-Widgets/tree/checkpoint/runtime-45061373320/runtime-evidence

Original screenshots and recorded state were inspected. Verified:

- Default placed clock has large, round, unclipped dotted digits beside Date.
- Actual HOME bitmap advances 07:38 to 07:39 after the background app process
  was killed and its PID was confirmed absent.
- Access denial uses the native live clock. Android access was revoked using
  its actual toggle; native fallback was confirmed after explicit app resume.
  Regrant restored the actual HOME bitmap.
- Black, translucent and transparent backgrounds work.
- 12-hour and 24-hour display work. Both widgets fit at 130% system font scale.
- Corrected settings copy is readable and includes the idle-delay caveat.

That runtime stopped after a test-dismissal tap opened Google Search following
a successful vertical resize. This was a harness interruption, not an app crash.
Its remaining stages were completed separately on the same exact APK:

https://github.com/Shyamnath-Sankar/Dotwel-Widgets/actions/runs/45063341690

Original remaining-stage evidence:
https://github.com/Shyamnath-Sankar/Dotwel-Widgets/tree/checkpoint/runtime-45063341690/runtime-evidence

The remaining-stage run succeeded in 9m8s. Original screenshots were inspected:

- Host bounds changed from [67,546,519,1134] to [67,546,519,1449], then returned
  to [67,546,519,1134]. Dots remained round, centered and unclipped.
- After 75 seconds of emulator-forced deep idle, returning HOME without opening
  the app showed bitmap 20:01 matching the device's 20:01.
- Reboot restored the actual HOME bitmap at 20:02 with Date still visible.

## Limits and remaining work

- Resize verification covers growth and shrink-back to the default size.
  A smaller-than-default compact host was not verified. The runtime report
  explicitly retains that caveat.
- The idle check is a bounded emulator observation. It does not promise exact
  minute updates during long Doze or reliability across every OEM launcher.
- Android stops the app and cancels alarms when optional access is revoked.
  Fallback is confirmed on the next app resume/update/resize, not instantly
  while the app is stopped.
- Current exact-candidate tests were on an Android 15 Pixel emulator. Physical
  device and other launcher testing remain useful before release.
- Prior native Date day-transition behavior was tested on the preserved native
  baseline; this exact candidate's runtime did not wait through another day.
- Play policy requirements were considered, but no Play submission or approval
  occurred. Do not label this artifact Play-approved or production-ready.
- Source promotion to default main and launcher integration are not part of
  this candidate-only housekeeping.

This cleanup changes documentation and removes the misleading duplicate APK
only. It does not alter tested application source, workflows, the final POLISH
APK or its checksum, and does not dispatch or require a rebuild.
