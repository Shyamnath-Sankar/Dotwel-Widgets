# Build and verification state

- Candidate: Dotwel Widgets 0.1.7 (versionCode 8). Package name is unchanged.
- Branch: `agent/collection-repair-45121252124`, based on the saved
  upstream-render commit `237d043`.
- This is a debug candidate, not a Play-approved or production-ready release.
- Default main and the separate launcher repository are unchanged.

## Upstream source pins

- AwesomeAWidgets: `0fae60722764bda790f89bd94119c6dab854d90e` (MIT,
  Copyright (c) 2026 Sameer Al Sahab).
- KWGT-Widgets: `96ca27be4cbff35efec5a86c64dd39fd065c958f` (MIT,
  Copyright (c) 2024 Om Gupta).
- See `docs/ATTRIBUTION.md` for the precise adapted classes/functions and
  `docs/KWGT-LEDGER.md` for the honest 40-preset status.

## Build and lint in this run

Commands run in this coding step:

```
./gradlew assembleDebug lintDebug --no-daemon
```

Result: `BUILD SUCCESSFUL`. `lintDebug` completed with 0 errors and 41 warnings.
No lint suppression was added. The warnings are the existing SDK/AGP update
notices, unused attributes/resources, two SmallSp item-card/clock text sizes,
one plurals suggestion, and two StaticFieldLeak warnings in the unregistered
partial weather cache. The weather code is not wired to any provider.

The APK identifies itself as `versionCode=8`, `versionName=0.1.7`,
`minSdk=26`, `targetSdk=36`, `compileSdk=36`.

## Artifact

Canonical checkpoint on this branch:
`checkpoints/dotwel-widgets-polish-debug.apk`

- Size: 1063607 bytes.
- SHA-256:
  `661d00912860526c5d970132f4d47e2beb26f24d1e5ad911fb09158f390de077`
- Recorded in `checkpoints/POLISH-SHA256SUMS`.

The inherited 0.1.6 APK and its checksum file were deleted from this candidate
checkpoint before the new APK was saved. Git history was not rewritten, so the
old artifact is not present in the working checkpoint directory.

## What changed in 0.1.7

### Build repair

All `buildBitmapViews` call sites passed two trailing lambdas, which is invalid
Kotlin. `render` is now named inside the parentheses at every call site in
`BaseWidgetProvider`, `WorldWidgetProvider` and `QuotesWidgetProvider`;
`decorate` remains the sole trailing lambda. No renderer logic was redesigned.

### World configuration correctness

- Pre-pin zones are stored under a unique request token and are applied only in
  the pin-success callback using the real `EXTRA_APPWIDGET_ID`.
- Zones remain stored per `appWidgetId`, so multiple instances are independent.
- A cancelled or failed pin consumes no token; abandoned tokens are cleared on
  known failure paths and pruned after 10 minutes. A stale token cannot be
  applied to a different instance.
- Adding World from the system widget picker now initializes to the device's
  actual local timezone plus UTC. The former hardcoded London/Tokyo default that
  looked user-selected was removed.

### KWGT conversion

- Added `Nothing_Battery` as a real native provider converted from the KWGT
  author layer geometry and formulas. The sanitized source transcription is
  `app/src/main/assets/kwgt/nothing_battery.sanitized.json`; the conversion is
  `KwgtBatteryRenderer.kt` plus `BatteryWidgetProvider.kt`.
- Battery level is read from the real system battery state, with no fake charge
  value, no added permission and no service.
- The gallery preview and the placed widget call the same renderer.
- Only 1 of 40 presets is implemented. The other 39 remain pending or
  dependency-blocked in `docs/KWGT-LEDGER.md`; none is claimed as imported.

### Permissions

- Removed the unused `INTERNET` permission. The offline checkpoint now declares
  only optional `SCHEDULE_EXACT_ALARM`. The partial weather transport is
  unregistered; implementing weather later will require a documented permission
  decision and Open-Meteo attribution.

## What is READY versus what is tested

`READY` in the gallery means the provider is registered and buildable in this
candidate. It does not mean runtime-tested. Seven providers are registered:
dot clock, date, Analog Bold, Analog Scale, world clock, quotes, battery.

No emulator or physical-device run happened for 0.1.7. No screenshot is claimed
as runtime evidence. In particular these remain unverified:

- native launcher pin confirmation and cancel behavior for the new battery card;
- battery refresh timing across launchers/doze. The level is read from the real
  system state; refresh uses the standard `updatePeriodMillis` (30-minute
  minimum, best-effort) plus resize/manual app refresh/app resume, not a
  manifest `ACTION_BATTERY_CHANGED` receiver;
- the truly-unsupported-pin fallback dialog;
- multiple simultaneous World pin requests and the 10-minute token pruning;
- pixel output of the battery gauge and world default on a real host;
- style persistence across an upgrade from an older install.

## Limits and remaining work

- Weather: partial domain/transport only, no nine layouts and no registered
  provider. Keep pending.
- KWGT: `Nothing_Battery2`, `Nothing_Battery_Mini`, `Nothing_Digital2`,
  `Nothing_Date_Time`, `Nothing_Mini_Calender` are the next implementable ports.
  Media/notification, CPU and iOS Komponent presets remain dependency-blocked.
- Compass and Snake remain blocked by the no-service/sensor policy.
- PHOTO and AWAY remain pending configuration/background-policy work.
- The OEM OnePlus entries remain unavailable for redistribution reasons.
- Play policy review, signing-key setup, repository visibility changes, launcher
  integration and default-main promotion are outside this checkpoint.
