# Build and verification state

## Current harness repair: gallery relaunch + profile uid

- Phase: repair harness only. Branch `agent/fix-gallery-sweep-relaunch-fl`,
  rebased onto `4fd2943` (the source tip the workflows check out as
  `agent/consolidated-repair-45440491695`). No app source, manifest, catalog,
  test, applicationId, SDK level or workflow YAML was changed.
- Root cause (gallery, run 45881551905): the sweep loaded 57 ready entries but
  wrote only 48. Entries 33-48 failed `Gallery did not load` after the
  `32-wx_circle` native pin, and the outer wrapper killed the step at 22m15s.
  The budget clock started when `gallery_sweep.py` started, after the two
  instrumentation sweeps, while the workflow's `timeout 1250s` starts at
  `run_emulator.sh`. `seek_category()` and `tree()` also had no per-step budget
  check, so one bad entry could spend up to 30s per `am instrument` call and
  consume the remaining wall clock before the between-entry check ran.
- Root cause (profile, run 45881605267): `start_host(None)` raised before the
  widget loop, so the fallback `finally` baseline was captured with no app
  process. `dumpsys package` uid parsing and `dumpsys meminfo` had no retries,
  leaving `uid: null`, `widgets: []` and an empty battery excerpt.
- Gallery fixes in `tools/repair/gallery_sweep.py`:
  - an absolute `GALLERY_SWEEP_DEADLINE_EPOCH` (exported by `run_emulator.sh`)
    is combined with the elapsed budget, so a late start still exits before the
    1250s wrapper;
  - the default budget is parsed from the ready catalog/`WidgetCatalog.kt`
    (75s + 16s/widget, capped at 1160s), not hardcoded to 57 or 61;
  - every adb/tree/scroll/page wait is budget-aware and raises
    `BudgetExhausted`; the main loop marks the current and remaining entries
    `not attempted - budget exhausted`, persists `RESULT.json`, and exits 0;
  - after a native pin the next entry uses a hard recovery: HOME, force-stop,
    `am start -W MainActivity`, then a monkey launcher intent, polling the real
    gallery marker for up to 60s;
  - three consecutive failures trigger one activity-stack reset
    (`am kill` + HOME + force-stop + relaunch) and the sweep continues;
  - entries are processed in chunks of 10 with a hard recovery at each chunk
    boundary, so one unrecoverable stretch cannot consume the whole run.
- Profile fixes in `tools/repair/profile_sweep.py`:
  - uid resolution now tries `cmd package list packages -U`, then
    `pm list packages -U`, then `dumpsys package <pkg>` `userId`;
  - `MainActivity`/monkey is started and polled until the process appears
    before the baseline meminfo; uid is re-resolved after startup;
  - `dumpsys meminfo` retries with backoff, and `dumpsys batterystats --uid`
    always uses the resolved uid;
  - an absolute `PROFILE_SWEEP_DEADLINE_EPOCH` bounds the run, partial results
    are written on exhaustion, and the script exits 1 only when the baseline has
    neither uid nor process-memory evidence.
- Budget/wrapper math:
  - `tools/repair/run_profile.sh` now uses a 1160s shell timeout (1250 - 90s),
    stops Python 60s earlier via the absolute deadline, and still writes
    `LIMITS.txt`;
  - `tools/repair/run_emulator.sh` exports a gallery deadline of
    `start + 1250 - 90` and caps the two pre-gallery instrumentation sweeps so
    at least 300s remains for the Add/pin sweep.
  - The workflow YAML `timeout 1250s` wrapper is unchanged.
- Added `tools/repair/sweep_common.py` (budget/deadline, uid parsing, dynamic
  catalog count) and `tools/repair/test_sweep_helpers.py`.
- Verified here (no emulator):
  - `python3 -m py_compile tools/repair/*.py` passes;
  - `python3 tools/repair/test_sweep_helpers.py` -> 18 tests, 0 failures;
  - `python3 tools/repair/test_fake_adb_sweeps.py` -> 3 tests, 0 failures:
    the gallery deadline marks every entry exhausted and exits 0, profile
    resolves uid 10234 and uses it for batterystats, and a no-uid/no-meminfo
    baseline exits 1;
  - `bash -n` and `shellcheck tools/repair/run_*.sh` pass;
  - fake-adb gallery run with a 2s deadline: all entries marked
    `not attempted - budget exhausted`, `RESULT.json` written, exit 0;
  - fake-adb gallery run with no gallery marker: hard recovery issued
    force-stop/am start/monkey, the third failure issued `am kill`, the tail was
    budget-exhausted, exit 0;
  - fake-adb profile run: uid `10234`, baseline PSS `12345`, all 9
    representatives measured, exit 0; a no-uid/no-meminfo run exited 1; a 3s
    deadline run kept the measured baseline and marked the rest exhausted,
    exit 0;
  - default budget math: 61 widgets -> 1051s, current 57 -> 987s, both below
    the 1160s ceiling.
- Not verified here: no emulator or device is attached, so the real
  post-`wx_circle` gallery recovery, the 61-entry sweep, real uid/meminfo
  readings and the workflow step duration are **not** runtime-verified. No APK
  was built or changed in this run, and no screenshot or Play claim is made.
- Next: run the consolidated repair/profile workflows and confirm entries after
  `wx_circle` reach the gallery again, `RESULT.json` contains a uid and widget
  rows, and each step exits before the 1250s wrapper.
- Blockers: none in the harness; emulator re-verification pending.

- Candidate: Dotwel Widgets 0.1.8 (versionCode 9), debug only. Package name
  `com.dotwelwidgets.dotwellabs` is unchanged.
- Branch: `agent/match-reference-control-tile-u`.
- Latest run restyled the Bluetooth and Wi-Fi tiles into circular control tiles,
  added cellular and battery circular tiles, and added 2x1 and 2x2 Bluetooth
  headphone battery cards (57 -> 61 registered providers). No launcher
  repository, visibility, workflow, signing key, applicationId, SDK level,
  dependency or version was changed.

## Circular status tiles and headphone battery cards (61-provider branch)

- Phase: match the reference control-tile UX with four 1x1 circular status
  tiles and add two Bluetooth headphone battery cards through the existing
  RemoteViews `BaseWidgetProvider` path. No service, polling loop, wake lock,
  WorkManager/AlarmManager beyond the existing bounded battery window, new
  dependency, proprietary font or bundled artwork.
- Status tiles: `bt_status` and `wifi_status` now render a red/off-white circle
  with an original programmatic glyph and dot-matrix ON/OFF/DATA text, then
  `cellular_status` and `battery_status` reuse the same renderer. Tapping opens
  Bluetooth settings, the Internet panel, or power usage where supported.
  `ACCESS_NETWORK_STATE` is the only new permission; no phone, location or
  READ_PHONE_STATE access. The battery tile joins the existing bounded
  `BatteryRefreshScheduler` scope.
- Headphones: `bt_headphones_wide` (2x1) and `bt_headphones_square` (2x2) draw a
  white/translucent card, original earbuds glyph, device name, dot-matrix
  percentage and level bar. `BluetoothDevice.getBatteryLevel()` and
  `ACTION_BATTERY_LEVEL_CHANGED` are hidden platform APIs, so they are accessed
  by reflection/string action and fail closed. A hidden `getBatteryUsageHint`
  fallback runs only on a short-lived, throttled background thread and never
  blocks rendering. Both entries reuse the existing optional
  `BLUETOOTH_CONNECT` opt-in dialog and open Bluetooth settings.
- Connectivity refresh: `DotwelApp` keeps a process-lifetime
  `registerDefaultNetworkCallback` for Wi-Fi/cellular updates and a hidden
  Bluetooth battery-level receiver that updates a small in-memory cache.
  Manifest receivers cover appwidget update, Bluetooth ACL/profile changes and
  the hidden battery action. No instant background update is claimed.
- Catalog, previews, mode labels, pin kinds, updater lists, manifest, layouts,
  `CatalogManifestConsistencyTest` (61) and `RemoteViewsLayoutSafetyTest` are
  wired. The original programmatic glyphs replace any font or bitmap asset.
- Verified in this run:
  `./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`
  -> `BUILD SUCCESSFUL`; 164 unit tests, 0 failures, 0 errors, 0 skipped; lint
  0 errors, 447 warnings (no new warnings in the new status/headphone files).
  Debug APK sha256
  `eda2a7b9a4f4fabd7b7ff1e2fb8831b1009a268df7f5b12655eb9bfc0bf4f1f1`.
- Not verified here: this runner has no emulator or device. Launcher placement,
  resize, style switching, real cellular/Wi-Fi/Bluetooth state changes,
  permission grant/deny, hidden headphone-battery values and tap destinations
  are not runtime-verified and are not claimed. No screenshot or Play-ready
  claim.
- Next: place all four status tiles and both headphone cards on a real launcher;
  check black/translucent/transparent, 1x1/2x1/2x2 resize, permission
  grant/deny, headphone connect/disconnect/battery changes and tap panels;
  capture screenshots and the APK checksum.
- Blockers: none in code; runtime verification pending.

## Widget resource profiling and hotspot fixes

- Phase: resource profiling harness plus three static hotspot wins. Branch
  `agent/add-widget-resource-profiling`.
- Part 1 added `tools/repair/profile_sweep.py` and `tools/repair/run_profile.sh`.
  The sweep assumes a booted emulator with the fixture host and widget app
  installed, captures a no-widget baseline (app/host `dumpsys meminfo` Total PSS,
  `dumpsys cpuinfo` top entries, `dumpsys batterystats --uid`, `dumpsys power`
  wake locks), then places digital, interstellar analog, interstellar endurance,
  battery, cache-only weather, media (fixture session), photo, quotes and away
  one at a time, settles 10 s, and captures again plus an `APPWIDGET_UPDATE`
  tick. It writes `runtime-evidence/profile/RESULT.json` and `SUMMARY.md`,
  honours `PROFILE_SWEEP_BUDGET_SECONDS` (default 900) and writes partial
  results. `run_profile.sh` waits for boot, installs the same APKs, grants
  appwidget bind and notification listener, warms the fixture host without a
  widget, and runs the sweep under a 1350 s timeout.
- Verification here: `python3 -m py_compile` and `bash -n` pass. A fake-`adb`
  simulation measured all 9 representative widgets, and a 1 s budget run wrote
  the baseline plus nine `not attempted - budget exhausted` entries and exited
  0. This is harness verification, not device runtime evidence.
- Part 2 fixes:
  - `InterstellarShapeCompiler` now compiles each variant/layer/style at the
    output raster size and reuses it through an 8 MiB byte-bounded LRU
    (`BoundedLayerCache`) keyed by variant+layer+style+pixel size. This removes
    the per-update decode/recycle of the 1108 px source layers.
  - `WidgetUpdater.updateMediaProviders` now returns immediately when no media
    widget is placed and coalesces bursts through `MediaUpdateThrottle` (500 ms)
    with one trailing run, so each placed media provider receives at most one
    update per window.
  - Lifecycle audit: no widget provider or service registers a
    receiver/handler/listener that outlives its owner. Media session callbacks
    and the active-session listener are torn down in
    `MediaNotificationService.onDestroy`/`onListenerDisconnected` through
    `MediaSessionRepository.onAccessChanged`; the only process-lifetime receiver
    is the intentional `DotwelApp` battery listener. No change was needed.
  - Wake locks: none exist in `app/src`; `NtAwayWidgetProvider` only reads
    `PowerManager.isInteractive`. Nothing was kept or removed.
- Verification: `./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug
  :app:lintDebug` -> `BUILD SUCCESSFUL`; 164 tests, 0 failures, 0 errors,
  0 skipped; lint 0 errors, 427 warnings. Debug APK sha256
  `a310975b8403d7b31408c87937a0d22634ff32053533657a90d4e414dfb22d91`.
- Not verified here: no emulator/device is attached, so the profile sweep has
  not produced real `runtime-evidence/profile` numbers, and the 500 ms media
  coalescing and layer cache were not observed on a live widget host. The next
  runtime run should execute `tools/repair/run_profile.sh` and compare the
  table before/after the fixes.
- Blockers: none in code; device profiling pending.

## Bluetooth and Wi-Fi status tiles (57-provider branch)

- Phase: add one 1x1 Bluetooth status widget and one 1x1 Wi-Fi status widget
  through the existing RemoteViews `BaseWidgetProvider` path.
  `updatePeriodMillis` stays 0; no service, polling, wake lock, exact alarm,
  new dependency, font or bundled artwork.
- Bluetooth: `BLUETOOTH` (`maxSdkVersion=30`) plus runtime `BLUETOOTH_CONNECT`.
  Live state is opt-in, explained in the Add dialog and off by default; without
  it the tile shows a neutral glyph and "Live status off" and still opens
  `Settings.ACTION_BLUETOOTH_SETTINGS`. The receiver listens only for
  `APPWIDGET_UPDATE` and the documented-exempt `ACL_CONNECTED`,
  `ACL_DISCONNECTED`, `BluetoothHeadset`/`BluetoothA2dp`
  `CONNECTION_STATE_CHANGED`; `ACTION_STATE_CHANGED` is not manifest-declared
  because it is not on Android's implicit-broadcast exemption list.
- Wi-Fi: `ACCESS_WIFI_STATE` only, no location and no manifest Wi-Fi broadcast
  (neither `WIFI_STATE_CHANGED_ACTION` nor `CONNECTIVITY_ACTION` is exempt).
  Shows Off/On/Connected/Disconnected plus the SSID when Android exposes it;
  tap opens `Settings.Panel.ACTION_INTERNET_CONNECTIVITY` on API 29+ and falls
  back to `Settings.ACTION_WIFI_SETTINGS`.
- Android 10+ blocks apps from toggling Wi-Fi and Android 13+ blocks toggling
  Bluetooth, so both tiles are status/settings shortcuts, not switches.
- Catalog, preview, mode labels, pin kinds, updater, manifest and
  `CatalogManifestConsistencyTest` are wired to 57 registered providers.
  Original programmatic glyphs replace any font or bitmap asset.
- Verified in this run:
  `./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`
  -> `BUILD SUCCESSFUL`; 156 unit tests, 0 failures; lint 0 errors.
- Not verified here: this runner has no emulator or device. Launcher placement,
  resize, style switching, real Bluetooth/Wi-Fi state changes, permission
  grant/deny and tap destinations are not runtime-verified and are not claimed.
  No screenshot or Play-ready claim.
- Next: place both tiles on a real launcher, check black/translucent/transparent,
  resize, Bluetooth grant/deny and tap panels; capture screenshots and the APK
  checksum.
- Blockers: none in code; runtime verification pending.

## Gallery sweep category navigation + graceful budget

- Phase: fix `tools/repair/gallery_sweep.py` only; no app, workflow, catalog,
  applicationId or SDK change.
- Root cause from run 45845465832 evidence: the first failure
  (`33-wx_square`) happens immediately after the `32-wx_circle` launcher pin.
  The failure XML/PNG for entries 33 onward is the launcher **app drawer**, not
  the app. The old sweep pressed `keyevent 4` (BACK) after the launcher capture;
  on this host that opens the app drawer, and the next entry's `am start` did
  not leave the gallery reliably foreground. The old `seek_category` accepted
  any `CATEGORIES` text or any `Open ` substring and then did 16 fixed swipes,
  so it reported `Category unavailable` for Weather/Photos/Live extras/Media &
  notifications even though the Weather sheet had opened for entries 31/32.
- Fixes in `tools/repair/gallery_sweep.py`:
  - `open_gallery()` now presses HOME, force-stops, starts `MainActivity`, and
    waits for a real gallery marker (`CATEGORIES` or `Open <label>, <n> ready`)
    with up to three attempts before an entry is attempted.
  - `seek_category()` scrolls the `settings_root` ScrollView forward and then
    back, re-reading the tree each step, brings the category card into the safe
    viewport above the bottom navigation, and dumps
    `category-<label>-failure.png/.xml` when a category is genuinely missing.
  - After tapping a category, the sweep waits for `Close category` plus an
    `Add ...` button before treating the sheet as open, retries the tap once,
    and otherwise reports `Category sheet did not open`.
  - Launcher-pinned entries now press HOME after capture instead of BACK, so
    the launcher app drawer is never left in front of the next entry.
  - `GALLERY_SWEEP_BUDGET_SECONDS` (default 1050) and
    `GALLERY_SWEEP_ENTRY_RESERVE_SECONDS` (default 45) bound the run. Entries
    that never start are written to `runtime-evidence/gallery/RESULT.json` as
    `not attempted - budget exhausted`; `RESULT.json` and the appwidget dump
    are always written in a `finally` block and the script exits 0.
  - Prior behaviour is retained: pin-confirmation polling, `Add live/plain`
    access-dialog recognition, top-of-card taps, and bounded per-entry waits.
- Verification in this run: `python3 -m py_compile` passes. A fake-`adb`
  harness (19-entry catalog covering the four failing categories) reached
  native pin confirmation for 19/19, including the `wx_circle`,
  `photo_circle` and `media_mini` launcher detours. A 7-entry budget run with
  `GALLERY_SWEEP_BUDGET_SECONDS=18` ran 4 entries, marked the other 3
  `not attempted - budget exhausted`, wrote all 7 to RESULT.json and exited 0.
  A missing-category run wrote `category-nonexistent-failure.xml/.png`,
  recorded the error, and exited 0. The harness is a simulation, not device
  runtime evidence.
- Not verified here: this runner has no emulator or device, so the real 55/55
  sweep, the four previously failing category sheets and launcher screenshots
  are not runtime-verified in this run.
- Next: run the emulator workflow and confirm 55/55 entries (or a clean
  budget-exhausted tail) with the four category sheets opening.
- Blockers: none in code; runtime re-verification pending.

## Mini media gallery final fixes (55-provider branch)

- Phase: three targeted repairs on the repaired 55-provider collection, no
  refactor outside the named files.
- FIX 1 (visible clipping): `media_empty_access_off`, `media_empty_no_notification`,
  `media_empty_no_metadata` and `media_empty_no_session` are now the short
  sentences requested in the task. `widget_media.xml` keeps `maxLines="3"` and
  adds `android:ellipsize="end"` to `@+id/media_message`. This addresses the
  clipped sentence in `media_mini-BLACK-180.png`; the 180dp screenshot is not
  recaptured here because this runner has no emulator.
- FIX 2 (synthetic media not registering): `RepairMediaActivity` now sets the
  metadata (title/artist/duration) and `STATE_PLAYING` transport state with
  PLAY_PAUSE/SKIP_NEXT/SKIP_PREVIOUS actions and a position **before**
  `isActive = true`, and logs the state line after activation. Album art is
  256x256 so the metadata binder transaction stays well below its limit. The
  ready instrumentation now starts the fixture itself after notification access
  is granted, because `am instrument` starts a fresh target process and destroys
  any session created before it. It polls the fixture event file for `state:`
  before the first host read, and polls up to 2.5 s for each transport callback
  instead of using a fixed 350 ms sleep. Fixtures remain behind
  `-PrepairFixtures` only.
- FIX 3 (gallery sweep): `tools/repair/gallery_sweep.py` now polls for the native
  pin confirmation instead of reading one immediate tree. It recognises the
  actual `Add to home screen` confirmation and the clock access dialog's
  `Add live ...`/`Add plain ...` positive buttons (the old code only looked for
  the word `plain`, which missed the analog/KWGT dialogs). Category cards are
  tapped near their top instead of at the centre, because the Weather/Photos
  cards sit under the app's bottom navigation at the centre point. Within a
  category the sweep fast-forwards the sheet with fixed Next taps and one tree
  read instead of a tree round-trip per page. Fixed sleeps around the Add flow
  are replaced with condition waits, and the 66 s/3 s launcher kill waits are
  reduced to a short bounded capture so all 55 entries fit the 1250 s budget.
  Full minute-tick kill evidence is still a separate focused check.
- Verification in this run:
  `./gradlew --no-daemon testDebugUnitTest` -> `BUILD SUCCESSFUL`, 154 tests,
  0 failures, 0 errors, 0 skipped. `./gradlew --no-daemon assembleDebug` ->
  `BUILD SUCCESSFUL`. `./gradlew --no-daemon -PrepairFixtures assembleDebug
  assembleDebugAndroidTest` -> `BUILD SUCCESSFUL`, so the fixture and ready
  instrumentation compile. The ordinary merged manifest and the built
  `app-debug.apk` contain no RepairHost/RepairMedia/RepairWeather/BIND_APPWIDGET
  entries (checked with aapt2). Ordinary debug APK sha256:
  `f9f2d3affe8528cb422e888d60325acaadd94d2cba61572a2ceb7977ede62c6d`.
- Not verified here: no emulator or device is available in this runner, so the
  ready-suite media callbacks, the 55/55 gallery Add sweep, the 180dp clipping
  screenshot and any launcher placement are **not** runtime-verified. The next
  emulator run should confirm `media_master` transport events include `state:`,
  that all 55 entries reach native pin confirmation, and recapture
  `media_mini-BLACK-180.png`.
- Blockers: none in code; runtime re-verification pending.

## 26th provider build repair (Orbit native preview)

- Candidate branch `agent/orbit-finish-45378636903`, source commit
  `57152a06a60678ea761c46f1ffd7ff34a6db801f` plus the compile repair below.
  This remains the existing debug checkpoint; no version bump, provider addition
  beyond the already-source Orbit entry, permission, service, dependency,
  launcher change or workflow edit was made.
- Root cause of the failed build: `WidgetPreviewRenderer.render()` switches on
  `PreviewKind` without an `else`, and the newly added `KWGT_ORBIT` enum value
  had no branch. The repair is the single native-preview branch
  `PreviewKind.KWGT_ORBIT -> null`, matching Analog2/Future/Analog3/Seconds/
  Analog5/Clear: Orbit is inflated from its native RemoteViews layout by
  `WidgetPreviewBinder`, not rendered as a bitmap. A full recompile confirmed no
  other missing exhaustive `when` branch; lint suppressions were not added and
  no test assertion was weakened.
- Orbit source review found the implementation already uses the actual
  `Nothing_Digital_Clock` relationships: 277 outer circle, 164 inner disc,
  205/245 orbits at 0.5 stroke, 12-diameter hour/minute dots at radii 102/122.5,
  hour red `#D71921`, minute white/dark per style, and 277 viewport vectors.
  The central time is a live platform `TextClock` (`hh:mm`/`HH:mm`), and the
  platform `AnalogClock` rotates the dot hand vectors; no baked or fake time is
  used. The source Time module has no `text_expression`; the sanitized JSON,
  ledger and attribution record this as a preview-led `hh:mm` repair based on the
  embedded 02:11 preview, not as a literal source expression. The stored
  `CLOCK_SECOND` formula values carry inactive toggle 0 and remain unused, so no
  seconds hand or API 31 gate was added.
- The three Orbit layouts keep the native clock and TextClock centered in the
  actual host frame (`match_parent` clock with `center` gravity, `wrap_content`
  live text); no fixed preview-only host size or bitmap is used. Runtime
  non-square host scaling, resize, style switching and dot movement remain
  unverified here. Gallery copy is unchanged and at most 45 words.
- iOS Battery pin path was rechecked without changing the battery widget:
  catalog `kwgt_ios_battery` has `IosBatteryWidgetProvider` and
  `PIN_KIND_KWGT_IOS_BATTERY`; the manifest registers `.IosBatteryWidgetProvider`
  with `@xml/ios_battery_widget_info`, and `widget_ios_battery.xml` keeps
  `@+id/widget_ios_battery_bitmap`. `PinResultReceiver` is registered and maps
  the catalog pin kind to its confirmation. The later isolated run that showed a
  native pin prompt but no HOME widget is not evidence of an app-side receiver
  mismatch because the launcher owns placement after the prompt; no battery code
  was mutated without a proven app defect.
- Verification in this run:
  `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` finished
  `BUILD SUCCESSFUL`. Unit/layout tests: 122, 0 skipped, 0 failures, 0 errors.
  Lint: 0 errors, 313 warnings, no added suppressions. Debug APK:
  `app/build/outputs/apk/debug/app-debug.apk`, sha256
  `40bedfe0da0d6a116098ad08845bd000ac35284a437bc816fc4d9a4380323aa8`.
  This is build-verification only; it is not a runtime pass, screenshot,
  completion or Play acceptance claim. The source is 26 providers / 20 of 40
  presets pending native runtime verification.

## iOS Battery fit and gallery copy correction (25 providers)

- `IosBatteryRenderer` now normalises to the visible Komponent card basis 290
  (`bgwidth`/`bgheight`), not the 416 preview canvas that includes transparent
  margins. The ring 92, stroke 10, padding 32, percentage 92 with left 32 /
  bottom 16 and corners 69 stay in those source relationships. Each bitmap uses
  one uniform `min(rasterWidth, rasterHeight) / 290` scale from the raster
  dimensions it actually received; no host coordinate or second scale is mixed
  in. The ring uses ROUND caps to match the source preview; the sanitized preset
  has no serialised cap toggle.
- The renderer still uses original programmatic battery vectors, platform
  sans-serif and the existing real level/charging path. The charging glyph is
  white and drawn last, above the background and ring. No provider, permission,
  service, network feature, dependency or battery-threshold behaviour changed;
  the 14/15/24/25 red/orange/green thresholds remain as before.
- The gallery/system widget descriptions for all 25 real providers were
  shortened to plain user-facing copy (catalog entries are at most 45 words).
  Technical formulas, provenance and adaptations remain in the sanitized JSON,
  `docs/ATTRIBUTION.md`, this file and the ledger, not in the gallery cards.
- Weather disclosure now states the verified lifecycle: off by default, nothing
  requested before consent, manual city with no GPS, and after consent plus a
  selected city an app resume may refresh a stale cache at most hourly with no
  background polling. `maybeAutoRefreshWeather()` checks enabled and location
  before any request; widget updates read only the cache.
- Ledger truth: 19 of 40 presets and 25 registered providers, pending native
  runtime verification. This checkpoint does **not** claim a runtime pass,
  screenshots or completion.

## KWGT Nothing_Analog_2 native port

- Added the 21st provider and 15th KWGT source port, `KwgtAnalog2WidgetProvider`,
  as a distinct catalog entry `KWGT Analog2` (`kwgt_analog2_clock`) in the
  existing Clock category. The existing 20 providers, layouts, vectors and
  descriptions are unchanged.
- Source: `/tmp/analog2-preset/preset.json` (NothingAnalog2 v13, canvas
  415x542), sanitized to
  `app/src/main/assets/kwgt/nothing_analog2.sanitized.json` with the author
  email, launch-app tap intent, font payloads and assets removed; formulas and
  active toggles retained.
- Actual source geometry: filled 277 circle dial; sharp hour rect 6 x 74 at
  centre Y `138.5 - 64/2`; sharp minute rect 6 x 100 at centre Y
  `138.5 - 112/2`; red seconds rect 130 x 3 with right padding 100 and the
  active `CLOCK_SECOND` 90 offset, represented as a full-viewport vertical rect
  x 137..140, centre Y 88.5, extent 23.5..153.5; 12 hour ticks 8 x 16.5 on the
  `style_size 250` ring; 60 minute ticks 11 x 8 on the `style_size 258` ring;
  red centre circle diameter 16. The actual source uses 277, not 279.
- The source paints the ticks and centre after the hands. The layouts keep that
  z-order with a transparent full-viewport `fitCenter` ImageView overlay
  (`kwgt_analog2_overlay`) above the native `android.widget.AnalogClock`; the
  overlay vectors contain the 12 + 60 ticks and the red centre. Both clock and
  overlay are `match_parent` and centered.
- Three styles: black `#1B1B1D` dial / white ink; translucent `#A6FFFFFF` /
  dark `#1B1B1D` ink; transparent no filled dial / white ink. Red seconds and
  centre stay `#D71921` in all styles. The source minute global formula is
  active, so the raw gray minute value is not used; the minute raw width 6 is
  retained because the active width toggle has no formula.
- The centre ShapeModule has no explicit type; Kustom's CIRCLE default is
  documented and used (diameter 16), not a new invented geometry.
- API gate: minSdk stays 26. Only `layout-v31` wires `android:hand_second`; the
  receiver is disabled below API 31 with the existing
  `kwgt_analog_seconds_available` false/true bool, and the catalog entry is
  UNAVAILABLE there with no silently degraded seconds. No new bool, permission,
  dependency, alarm, service, font, artwork or network path was added.
- Catalog/Add/manifest/updater/pin are wired: id `kwgt_analog2_clock`, pin kind
  `kwgt_analog_2`, provider list entry, Clock category, and gallery preview
  inflating the same native layout. The widget picker description is 18 words:
  "A Nothing-style analog clock with a red seconds hand. Android 12 or newer.
  Seconds follow your device settings."
- The prior native `android:hand_second` mechanism was independently reported to
  move the red seconds after an app kill. This Analog2 build was not runtime
  tested in this runner; seconds movement, non-square host alignment, resize and
  style switching remain runtime-unverified here.
- Tests: `KwgtAnalog2LayoutTest` covers the API gate, all three base and v31
  layouts' RemoteViews tags, the exact 72 ticks and red centre, overlay-above-
  clock z-order, `match_parent`/`fitCenter` alignment, source hand/dial vectors,
  and the sanitized source formulas/toggles. `RemoteViewsLayoutSafetyTest` now
  also parses the three Analog2 base layouts.
- Attribution/ledger updated to 15 of 40 presets and 21 registered providers;
  rights and the MIT notices are unchanged. The existing 20-provider checkpoint
  artifacts remain in `checkpoints/`.

## KWGT Nothing_Analog_3 native port

- Added the 17th provider and 11th KWGT source port, `KwgtAnalog3WidgetProvider`,
  as a distinct catalog entry `KWGT Analog3` (`kwgt_nothing_analog3`) in the
  existing Clock category. No alias of `AnalogBoldWidgetProvider` or
  `AnalogScaleWidgetProvider`; their files and behaviour are unchanged.
- Source: `Nothing_Analog_3.kwgt!preset.json` at
  `96ca27be4cbff35efec5a86c64dd39fd065c958f`. Actual canvas 415x542,
  `side = min(width, height)`, 277 square design viewport with a 138.5 dial
  radius. The source has a circle plus two hollow rounded capsule hands and no
  seconds layer. Sanitized actual JSON is
  `app/src/main/assets/kwgt/nothing_analog3.sanitized.json`; author email and the
  launch-app tap intent are removed.
- Hands retain the source formulas: width `side / 13.85` (20), stroke
  `side / 138.5` (2), minute height `side / 2.475` centred at
  `138.5 - (side / 2.988) / 2`, hour height `side / 3.3765` centred at
  `138.5 - (side / 4.4675) / 2`, capsule corner radius 10, transparent fill and
  foreground stroke.
- Three layouts ship: black with a filled `#FF1B1B1D` vector dial and white
  hands, translucent with a filled `#A6FFFFFF` vector dial and dark `#FF1B1B1D`
  hands, transparent with a transparent vector dial and white hands. Each layout
  embeds one native `android.widget.AnalogClock`
  (`@id/kwgt_analog3_clock`) using public `android:dial`, `android:hand_hour` and
  `android:hand_minute` vector resources. The platform clock rotates the hands
  automatically; no runtime AnalogClock setter, bitmap time, minute alarm,
  exact-alarm access, service, dependency or new permission is used.
- `KwgtAnalog3WidgetProvider` overrides `buildViews` instead of using the base
  card: it selects the style layout and sets the root click
  `PendingIntent` to `AlarmClock.ACTION_SHOW_ALARMS` without calling
  `setBackgroundResource`. The gallery preview inflates the same native layout
  so it shares the vectors and current time.
- Catalog, manifest receiver (`kwgt_analog3_widget_info`), `PinResultReceiver`
  kind `kwgt_analog_3`, `WidgetUpdater` provider list, strings, attribution and
  ledger are updated to 11 of 40 presets / 17 registered providers. All 16
  earlier renderers and the Battery2 refresh lifecycle are preserved.

### Verified here (Analog3)

```
./gradlew assembleDebug -Pkotlin.compiler.execution.strategy=in-process -Pkotlin.incremental=false --no-daemon --max-workers=1
./gradlew :app:testDebugUnitTest :app:lintDebug -Pkotlin.compiler.execution.strategy=in-process -Pkotlin.incremental=false --no-daemon --max-workers=1
```

Result: first command `BUILD SUCCESSFUL`. Second command `BUILD SUCCESSFUL`;
51 unit/layout tests passed (0 failures, 0 errors, 0 skipped), including the new
`KwgtAnalog3LayoutTest` style/dial/hand/vector assertions and the existing
RemoteViews tag allowlist now covering the three analog3 layouts; lint 135
warnings, 0 errors. Debug APK:
`app/build/outputs/apk/debug/app-debug.apk`,
sha256 `8df01a3db9d46b86acb025b79edbd4cae6bc7ae10c4b3c6e39406570f86375ba`.

### Not verified here (Analog3)

- No Android host or emulator is available in this runner. RemoteViews inflation
  of the native `AnalogClock` with the three vector drawables, placement on a
  launcher, resize behaviour, style switching, tap opening Alarms, and actual
  hour/minute hand movement are **not** runtime-verified and are not claimed.
- The source has no seconds layer and none was added; no future-seconds
  interpretation was introduced.

## Battery refresh lifecycle reliability (from 5b733ea)

- Added `BatteryRefreshScheduler`: a non-wakeup `ELAPSED_REALTIME`
  `setWindow` (fallback `set`) at `now + 15 minutes` with a 10-minute delivery
  window. This is deliberately inexact, needs no exact-alarm permission and may
  be deferred by Doze; the documented bounded best effort is therefore up to
  roughly 25 minutes, not a hard 15-minute SLA.
- `BatteryRefreshReceiver` is an explicit internal receiver with
  `android:exported="false"`, `FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE` and only
  matches `com.dotwelwidgets.dotwellabs.action.BATTERY_REFRESH`. It refreshes
  only the battery-capable providers and re-arms the next window.
- Scheduling scope is `BatteryWidgetProvider`, `BatteryMiniWidgetProvider`,
  `KwgtBattery2WidgetProvider` and `KwgtCleanInfoWidgetProvider`. The alarm is
  cancelled when the last of those is removed; the other 12 providers are not
  touched and no minute polling was added.
- `DotwelApp` (registered via `android:name`) re-arms the alarm on process
  start and keeps a process-lifetime dynamic `ACTION_BATTERY_CHANGED`
  registration that refreshes only the four battery providers on level/status
  changes. It compares the percent and charge status and skips property-only
  deliveries, and never re-registers from the callback, so neither the sticky
  broadcast nor frequent temperature/voltage deliveries can loop or short-poll.
- Re-arm also runs after the refresh tick, provider update/options, pin and
  manual refresh (`WidgetUpdater.updateAll`). The existing manifest
  `BATTERY_CHANGED` entries may remain but are no longer relied on after a
  process kill.
- No foreground service, wake lock, new permission, new dependency or new
  widget was added. The percentage, 16-segment Battery2 rendering and
  charging/unknown estimate behaviour are unchanged.
- Gallery copy now says "Best-effort level..." instead of "Live level..." and
  the provider/catalog/README/ledger text states the bounded, deferrable
  refresh plus manual app refresh. No live or instant background claim remains.

### Verified in this run

```
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Result: `BUILD SUCCESSFUL`; 48 unit/layout tests passed (0 failures, 0 errors,
0 skipped), including 3 new `BatteryRefreshSchedulerTest` lifecycle assertions;
lint 125 warnings, 0 errors. XML parse: 54 files, 0 errors. Debug APK:
`app/build/outputs/apk/debug/app-debug.apk`,
sha256 `7484661ca77062a8b175ccf8a86f5c6b5c125c88a365a376f4269f078a317684`.

### Not verified in this run

- No Android emulator binary or system image is installed in this runner (only
  `adb`), and no device is attached, so app-resident `BATTERY_CHANGED`
  73 -> 42 -> 100 delivery, process-kill recovery through the real inexact
  alarm, the scheduled PendingIntent inspection, and the up-to-25-minute
  deferred delivery are **not** runtime-verified here and are not claimed.
- Do not claim immediate background refresh after an app kill. The next runtime
  check should kill the process after a known level change and wait for the
  system alarm (up to ~25 minutes) rather than triggering a manual refresh.

## KWGT Nothing_Battery2 native port

- Added the 16th provider and 10th KWGT source port,
  `KwgtBattery2WidgetProvider`, as a distinct catalog entry `KWGT Battery2`
  (`kwgt_nothing_battery2`) under the existing battery category. No alias of
  `BatteryWidgetProvider` or `BatteryMiniWidgetProvider`; their files and
  behaviour are unchanged.
- Source: pinned `Nothing_Battery2.kwgt!preset.json` at
  `96ca27be4cbff35efec5a86c64dd39fd065c958f`. Actual canvas 415x542, dark
  `#FF1B1B1D` / light white, opposite foreground, accent at 50% alpha, rounded
  corners `side / 5`, information offset `side / 8.5`, BATTERY label `side / 18`
  with 16 bottom padding, outlined body 195x78 corners 14 stroke 3.5, clip
  179x64 corners 7, 16 rectangular progress segments across 184x64, pill nub
  5x28 with a 5 margin, level `side / 4.5`, percent `side / 10`, bottom row
  `side / 15` with an 8 margin.
- Literal body/clip/progress/nub values are scaled from the author 277 design
  reference by `side / 277`; formula sizes use `side`. The source has no `h`
  global, so the background follows the host height and the whole stack is
  centred instead of reproducing the missing height.
- The source `ndot-47.ttf` is not used. Dotwel's existing original dot glyphs
  draw BATTERY and the large level; the long honest status uses platform
  sans-serif.
- Real level comes from the existing `BatteryState` path. Charging comes from
  `BatteryManager.isCharging`; a positive `computeChargeTimeRemaining()` is the
  only time value shown, labelled `TO FULL`. Unknown or discharging states show
  `--` and `ESTIMATE UNAVAILABLE`; 100% shows `FULL` only. No discharge estimate
  is fabricated.
- Updates: `APPWIDGET_UPDATE`, `BATTERY_CHANGED`, `POWER_CONNECTED`,
  `POWER_DISCONNECTED`, `BOOT_COMPLETED`, resize/options, app refresh and the
  existing 30-minute inexact widget period. No new permission, network, service,
  dependency, font or artwork.
- Tap tries `ACTION_POWER_USAGE_SUMMARY` and falls back to a known Settings
  screen when no activity resolves.
- Preview uses the same `KwgtBattery2Renderer`; Add/pin/provider-info, catalog
  count 16 and pin confirmation are wired. The sanitized asset is
  `app/src/main/assets/kwgt/nothing_battery2.sanitized.json` with email, events
  and font paths removed. Ledger now records 10 of 40 implemented and 16
  providers, all runtime-unverified.

## CleanInfo compatibility fixes

- `layout/widget_kwgt_clean_info.xml` used a raw `<View>` for
  `kwgt_clean_info_bg_fallback`, which RemoteViews does not support; a native
  host rejected the layout even when the view was `GONE`. It is now an
  `ImageView` with the same id, `match_parent` size, `fitXY` scale and
  `GONE` default. The layout no longer contains a raw `<View>`.
- The source readout is fixed 12-hour `h:mm`, but the layout's
  `format24Hour` was `H:mm`. It is now `h:mm` for both 12-hour and 24-hour
  devices, matching the source.
- All 14 existing providers, foreground conditional repair, platform font
  substitution, permissions, services and dependencies are unchanged. The
  battery `TextView` has padding only; its geometry was not altered.

## Compiler fix run (ce464a0)

- Restored the original `BaseWidgetProvider.buildBitmapViews` signature
  (`render` then a trailing/default `decorate: (RemoteViews, Boolean) -> Unit`).
  The target-aware `decorateWithHost` optional Function3 parameter that broke
  trailing-lambda binding in the existing providers was removed.
- Added a distinct `buildTargetBitmapViews(..., decorateTarget)` helper used
  only by `KwgtDigital3WidgetProvider`; it is not an overload of
  `buildBitmapViews`, so no ambiguity. The other 13 provider files were not
  edited.
- Moved `KwgtDigital3Renderer`'s `MinuteRule` type alias from a nested class
  member to file top level; no experimental compiler flag added.
- `KwgtDigital3RendererLayoutTest` (including the source `joint` NEVER hierarchy
  and minute-threshold assertions) and the diagnosed real-host vs scanned-raster
  test remain unchanged and pass.
- Scope of this run: repair the incomplete KWGT `Nothing_Digital3` import
  without restarting the implementation or rewriting existing providers,
  renderers, layouts or tests.

## Repaired

- `app/src/main/AndroidManifest.xml` line 179 was malformed: the
  `android.intent.action.DATE_CHANGED` action on the Digital3 receiver was
  missing `android:name="`. Fixed; all 50 `app/src/main/**/*.xml` files now
  parse with Python `xml.etree.ElementTree`.
- `catalog_kwgt_digital3_desc` in `res/values/strings.xml` was interpreted by
  lint as a format string because of its literal percentages. Marked
  `formatted="false"` so the displayed copy is unchanged.
- `KwgtDigital3Renderer` used `android.graphics.Color` static helpers, which
  local JVM tests cannot mock. Replaced them with equivalent ARGB bit
  operations; colours and alphas are unchanged. The Digital3 layout test now
  reads alpha with bit operations too.

## Digital3 wiring confirmed

- Real 14th provider and 8th KWGT source port. Manifest receiver
  `KwgtDigital3WidgetProvider` registers the appwidget update, time, timezone,
  date and boot actions plus `@xml/kwgt_digital3_widget_info`.
- Catalog entry `kwgt_nothing_digital3` is READY with `PreviewKind.KWGT_DIGITAL_3`,
  `KwgtDigital3WidgetProvider` and `PIN_KIND_KWGT_DIGITAL_3`; it is one of the
  seven CLOCK variants. The other 13 providers were left in place.
- Preview: `WidgetPreviewRenderer` and `WidgetPreviewBinder` call the Digital3
  renderer; without access the preview uses the time-free dot matrix under live
  `TextClock` labels.
- Add/native pin: `MainActivity` routes the entry through the existing
  request-pin flow and the Digital3 access dialog; `PinResultReceiver` shows the
  Digital3 confirmation string.
- Updates: `WidgetUpdater.providers` and
  `MinuteTickScheduler.MINUTE_PROVIDERS` both include Digital3. The provider
  reschedules on update, resize, enable/disable/delete, minute tick,
  time/timezone/date change and boot. Tap uses
  `AlarmClock.ACTION_SHOW_ALARMS`.
- Access on draws the full bitmap from the existing minute scheduler. Access off
  draws `renderDots()` (surface plus minute-lit dots, no baked-in time text)
  behind three live platform `TextClock` views for hh, mm and aa, so no frozen
  bitmap time is presented as live. The minute grid itself is a static fallback
  between widget updates and is described that way in the UI copy.
- The renderer follows `nothing_digital3.sanitized.json`: `side / 5` corners,
  4x4 `side * 48 / 277` circles at `side * 12 / 277` margins, `side / 8.5`
  right and `side / 21.875` up offset, `#FFD71921` primary hh/mm cells,
  uppercase aa, accent at 35% alpha, accent2 at 15% alpha, and minute
  thresholds `mm > 9`, `> 29`, `> 39`, `> 49`. App-owned `DotGlyphFont` dots
  replace `ndot-47.ttf`.
- No new permission, service, dependency or font payload. The existing
  `SCHEDULE_EXACT_ALARM` permission is unchanged. The sanitized Digital3 JSON
  and MIT attribution remain, with the ledger/attribution rows updated.

## Verified here (Battery2)

```
python3 ElementTree parse of app/src/main/**/*.xml   -> 54 files, 0 errors
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Result: `BUILD SUCCESSFUL`; 45 unit/layout tests passed (0 failures, 0 errors),
including 5 new `KwgtBattery2RendererLayoutTest` geometry, 16-segment, level
clamp and unknown-estimate assertions; lint `0 errors, 125 warnings`. The debug
APK is `app/build/outputs/apk/debug/app-debug.apk` and is build-verified only,
not runtime-verified. No placement, resize, battery broadcast, tap destination,
segment pixels or charging-estimate runtime is claimed here.

## Verified here

```
python3 ElementTree parse of app/src/main/**/*.xml   -> 52 files, 0 errors
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Result: `BUILD SUCCESSFUL`; 40 unit/layout tests passed (0 failures, 0 errors);
lint `0 errors, 117 warnings`. The CleanInfo layout has 0 raw `<View>` elements
and both `format12Hour` and `format24Hour` are `h:mm`. The manifest registers 14
widget providers, of which 8 are KWGT source ports. The fresh debug APK is
`app/build/outputs/apk/debug/app-debug.apk`; CI uploads it on success. The APK is
build-verified only, not runtime-verified.

## Not verified

- No Android host was available in this run. Placement, resize, style switching,
  minute advance, process-kill recovery, boot/timezone refresh, tap behaviour and
  visual output for CleanInfo (and the other providers) are not runtime-verified.
  No screenshot or runtime pass is claimed.
- The KWGT collection is still incomplete: 11 of 40 presets are implemented; the
  remaining 29 stay pending or dependency-blocked and are not claimed as
  imported.
- Weather, Photo, Away, Compass, Snake, media/notification, CPU and iOS
  Komponent presets remain pending or blocked.
- Play policy review, signing-key setup, repository visibility changes, launcher
  integration and default-main promotion remain outside this checkpoint.

