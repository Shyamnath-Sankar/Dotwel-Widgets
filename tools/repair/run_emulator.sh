#!/usr/bin/env bash
set -u
PKG=com.dotwelwidgets.dotwellabs
OUT=runtime-evidence
WRAPPER_START=$(date +%s)
# The workflow wraps this script in `timeout 1250s`. Reserve 90 s for the
# post-gallery teardown and evidence push, and hand the gallery an absolute
# deadline so its own budget cannot start late and overrun the wrapper.
OUTER_WRAPPER=${GALLERY_OUTER_TIMEOUT_SECONDS:-1250}
TEARDOWN_MARGIN=${GALLERY_SWEEP_TEARDOWN_RESERVE_SECONDS:-90}
GALLERY_MIN_SECONDS=${GALLERY_SWEEP_MIN_SECONDS:-300}
GALLERY_DEADLINE=$((WRAPPER_START + OUTER_WRAPPER - TEARDOWN_MARGIN))
export GALLERY_SWEEP_DEADLINE_EPOCH=$GALLERY_DEADLINE
mkdir -p "$OUT"
adb install -r /tmp/dotwel-fixture.apk
adb install -r /tmp/dotwel-test.apk
adb install -r /tmp/snapshot/helper.apk
adb shell appwidget grantbind --package "$PKG" --user 0
adb shell input keyevent 82
adb shell settings put system screen_off_timeout 2147483647
function sweep() {
  local name=$1
  local now limit setup_left
  now=$(date +%s)
  # Do not let the two pre-gallery instrumentation sweeps consume the whole
  # wrapper: keep at least GALLERY_MIN_SECONDS for the Add/pin sweep.
  setup_left=$((GALLERY_DEADLINE - now - GALLERY_MIN_SECONDS))
  limit=420
  [ "$setup_left" -lt "$limit" ] && limit=$setup_left
  [ "$limit" -lt 60 ] && limit=60
  adb shell run-as "$PKG" rm -rf no_backup/repair-evidence
  timeout "${limit}s" adb shell am instrument -w "$PKG.test/com.dotwelwidgets.dotwellabs.RepairInstrumentation" | tee "$OUT/instrument-$name.txt"
  mkdir -p "$OUT/$name"
  adb exec-out run-as "$PKG" sh -c 'cd no_backup/repair-evidence && tar cf - .' > "/tmp/evidence-$name.tar"
  tar xf "/tmp/evidence-$name.tar" -C "$OUT/$name"
}
sweep off
adb shell cmd notification allow_listener "$PKG/.media.MediaNotificationService"
adb shell am start -W -n "$PKG/.RepairMediaActivity"
adb shell am start -W -n "$PKG/.RepairWeatherActivity"
sleep 2
sweep ready
python3 tools/repair/gallery_sweep.py
adb exec-out run-as "$PKG" cat no_backup/repair-media-events.txt > "$OUT/media-events.txt"
adb shell cmd notification disallow_listener "$PKG/.media.MediaNotificationService"
adb shell am start -W -n "$PKG/.RepairWeatherActivity" --ez clear true
adb shell am start -W -n "$PKG/.RepairMediaActivity" --ez stop true
sleep 2
# One focused revoke view, not another full sweep.
adb shell am start -W -n "$PKG/.RepairHostActivity" --es provider "$PKG.MiniMediaPlayerProvider" --ei width 320 --ei height 260
sleep 1
adb exec-out screencap -p > "$OUT/revoked-media.png"
adb shell dumpsys appwidget > "$OUT/appwidget-dump.txt"
adb logcat -d -t 6000 > "$OUT/logcat.txt"
# Do not call synthetic fixture playback or forecast a live network check.
printf '%s\n' 'Synthetic replay and real Android host; human pixel review, clock movement, live weather and final ordinary-APK launcher verification remain required.' > "$OUT/LIMITS.txt"
