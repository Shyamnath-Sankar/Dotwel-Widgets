# Third-party attribution

Dotwel Widgets is an independent app. It is not affiliated with, endorsed by, or
sponsored by Nothing Technology Limited or OnePlus.

## AwesomeAWidgets (MIT)

Primary design and implementation reference:
https://github.com/SameerAlSahab/AwesomeAWidgets

Copyright (c) 2026 Sameer Al Sahab. Licensed under the MIT License. The full MIT
text is bundled in `app/src/main/res/raw/awesomeawidgets_mit.txt`.

### Reused / adapted

No smali bytecode, APK resources, fonts, or artwork from AwesomeAWidgets are
included in this project. The following author-written classes were inspected and
their behavior was reimplemented in Kotlin using standard Android widget APIs:

- `NtDigital.smali`: 24-hour versus 12-hour clock selection and zero-padded
  `HH:mm` presentation. Replaced with a system-updated `TextClock`; no bitmap
  rendering or tick service.
- `NtDate.smali`: day-number and weekday/month date presentation, plus opening
  the system calendar on tap. Replaced with system-updated `TextClock` views and
  a calendar `PendingIntent`.
- `NtProvider.smali`: `AppWidgetProvider` update loop, resize callback, and
  clock-tap behavior. Reimplemented as `BaseWidgetProvider`.
- `NtStyle.smali`: a persisted integer style setting read by the widget provider.
  Reimplemented as a three-value `WidgetStyle` enum in `WidgetPrefs`.
- `NtDraw.smali`: reviewed for its black / translucent / transparent style idea
  and contrast choices. No drawing code was copied; backgrounds are Android shape
  drawables and text is rendered by the host with `TextClock`.

## DotGothic16 (SIL Open Font License 1.1)

`app/src/main/res/font/dotgothic16_regular.ttf` is DotGothic16 Regular from the
Google Fonts repository (`ofl/dotgothic16`), Copyright 2020 The DotGothic16
Project Authors (https://github.com/fontworks-fonts/DotGothic16).

Licensed under the SIL Open Font License, Version 1.1. The full OFL text is
bundled in `app/src/main/res/raw/dotgothic16_ofl.txt`. The font is unmodified.

## Not included

- The upstream AwesomeAWidgets APK, decompiled OEM `net.oneplus.*` classes, and
  OEM resources.
- Upstream fonts such as `nt_ndot77jp.ttf`, `nt_ntype82.otf`, and
  `nt_lettera_mono.otf`.
- Upstream screenshots, logos, preview images, and other artwork.
