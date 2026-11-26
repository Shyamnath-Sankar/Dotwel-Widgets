# Bounded combined repair verification

Fixtures compile only with `-PrepairFixtures`. They are not in normal debug or release artifacts. A fixture APK must never be delivered to the owner. Rebuild normal debug after verification and inspect its merged manifest for fixture names and BIND_APPWIDGET before copying it to checkpoints.

1. Build ordinary `assembleDebug testDebugUnitTest lintDebug`, retain ordinary APK separately.
2. Build `-PrepairFixtures assembleDebug assembleDebugAndroidTest`, install both fixture APKs on the isolated emulator. Grant BIND_APPWIDGET to the target package through adb; no owner account or data is present.
3. First run RepairInstrumentation without any permission grant. This sequentially inflates all registered providers at 180x180 and 360x180, styles black/translucent/transparent, and captures full-screen pixels in no_backup/repair-evidence. Extract through run-as. Isolated AppWidgetHost proof only.
4. Allow the test listener only in the emulator using cmd notification allow_listener. Start RepairMediaActivity to publish synthetic metadata and transport callbacks with no audio/network. Start RepairWeatherActivity for synthetic replay, not live network proof. Run RepairInstrumentation again into a separate evidence directory. Verify events record actual callbacks, then revoke listener and clear synthetic weather, inspect data-off states. No owner notifications.
5. Actual gallery Add and launcher placement must be tested separately using MainActivity and a real launcher. Do not substitute fixture host screenshots. Clocks with access off should choose the plain live option in the app's explanation. World uses Save and add. Verify the Add content description matches the selected provider, native confirmation, actual launcher pixels and dumpsys widget binding. Check all57Add flows, final representative launcher pins, resize, kill/reboot clock movement, deletion, system settings tap destinations and weather live network separately.
6. Record every failure before collective repair. Keep source identity and APK SHA256. No per-provider workflow loops, no repeated force-stop/style UI for native inflation. Inspect decisive full-resolution screenshots before pass claims.

Local tests and fixtures do not settle Endurance source fidelity. That remains rejected until source geometry matches. Real API weather result must be distinguished from synthetic data. Media progress is sampled, not a continuously ticking bitmap. Inexact Away samples can be delayed by Android. Compass and Snake remain optional and need the owner's OK for a live service, under the current service-free scope.

## Sweep budgets and recovery controls

- `GALLERY_SWEEP_DEADLINE_EPOCH` and `PROFILE_SWEEP_DEADLINE_EPOCH` are absolute
  wall-clock deadlines exported by `run_emulator.sh` / `run_profile.sh` from the
  workflow's 1250 s wrapper. Each Python sweep uses the smaller of its elapsed
  budget and that deadline, so a late start cannot overrun the wrapper.
- Default budgets scale with the ready catalog count parsed from
  `WidgetCatalog.kt` (gallery: 75 s + 16 s/widget; profile: 90 s + 14 s/widget)
  and are capped at 1160 s, leaving at least 90 s of the 1250 s wrapper for
  teardown and evidence push. Override with `GALLERY_SWEEP_BUDGET_SECONDS` or
  `PROFILE_SWEEP_BUDGET_SECONDS`.
- After any launcher pin the gallery sweep force-stops the app, relaunches
  `MainActivity` with `am start` and then `monkey`, and waits up to 60 s for the
  gallery root. Three consecutive failures trigger one activity-stack reset
  (`am kill` + HOME + relaunch). Results are persisted after every entry and the
  sweep exits 0 with partial results when the budget is exhausted.
- The profile sweep resolves the uid with `cmd package list packages -U`, then
  `pm list packages -U`, then `dumpsys package <pkg>` `userId`, starts
  `MainActivity` before reading `dumpsys meminfo`, retries meminfo with backoff,
  and uses the resolved uid for `dumpsys batterystats --uid`. It exits non-zero
  only when the baseline has neither a uid nor process-memory evidence.
- Emulator-free checks: `python3 tools/repair/test_sweep_helpers.py`,
  `python3 tools/repair/test_fake_adb_sweeps.py`,
  `python3 -m py_compile tools/repair/*.py`, and
  `shellcheck tools/repair/run_*.sh`.
