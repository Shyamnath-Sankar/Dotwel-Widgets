# Third-party attribution

Dotwel Widgets is an independent app. It is not affiliated with, endorsed by, or
sponsored by Nothing Technology Limited or OnePlus.

## AwesomeAWidgets (MIT)

Primary design and implementation reference:
https://github.com/SameerAlSahab/AwesomeAWidgets

Copyright (c) 2026 Sameer Al Sahab. Licensed under the MIT License. The full MIT
text is bundled in `app/src/main/res/raw/awesomeawidgets_mit.txt`.

### Inspected / adapted behaviour

No smali bytecode, APK resources, fonts, or artwork from AwesomeAWidgets are
included in this project. The following author-written classes were inspected and
their behaviour was reimplemented in Kotlin using standard Android widget APIs:

- `NtDigital.smali`: 24-hour versus 12-hour clock selection and zero-padded
  `HH:mm` presentation. Reimplemented as a system-updated `TextClock` fallback
  and as the time formatting used by the new circular-dot bitmap renderer.
- `NtDate.smali`: day-number and weekday date presentation, plus opening the
  system calendar on tap. Reimplemented with system-updated platform-serif
  `TextClock` views and a calendar `PendingIntent`.
- `NtProvider.smali`: `AppWidgetProvider` update loop, resize callback, and
  clock-tap behaviour. Reimplemented as `BaseWidgetProvider` plus the new
  `ClockWidgetProvider` bitmap and scheduling path.
- `NtStyle.smali`: a persisted integer style setting read by the widget
  provider. Reimplemented as a three-value `WidgetStyle` enum in `WidgetPrefs`.
- `NtDraw.smali`: reviewed for its black / translucent / transparent style idea
  and contrast choices. No drawing code was copied; backgrounds are Android
  shape drawables and the digit geometry below was written for this app.
- `NtTick.smali`: reviewed for the idea of minute updates; no service or
  broadcast code was copied. The update path is a `PendingIntent`-based exact
  alarm scheduled only when the user enables Alarms & reminders access.

## Original assets in this app

- `DotMatrixRenderer.kt` contains original programmatic circular-dot digit
  geometry written for Dotwel Widgets. No third-party font file is bundled or
  loaded.
- `SerifDateRenderer.kt` and the date widget use the Android platform `serif`
  family; no font file is bundled.
- Launcher icon, backgrounds, previews, and screenshots in this repository are
  original to Dotwel Widgets.

## Not included

- The upstream AwesomeAWidgets APK, decompiled OEM `net.oneplus.*` classes, and
  OEM resources.
- Upstream or third-party font files such as `nt_ndot77jp.ttf`, `nt_ntype82.otf`,
  `nt_lettera_mono.otf`, and the previously removed DotGothic16 files.
- Upstream screenshots, logos, preview images, and other artwork.
- `nothing-kde-widgets` and `nothing_archive` source, archives, or assets; those
  repositories were inspected as references only.
