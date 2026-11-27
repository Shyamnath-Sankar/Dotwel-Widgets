#!/usr/bin/env bash
# Resource profile sweep on the isolated fixture host. Mirrors run_emulator.sh
# setup but runs tools/repair/profile_sweep.py instead of the ready/gallery/off
# suites. The emulator must already be reachable through adb.
set -u
PKG=com.dotwelwidgets.dotwellabs
OUT=runtime-evidence
WRAPPER_START=$(date +%s)
# The workflow wraps this script in `timeout 1250s`. Stay 90 s inside that
# ceiling and stop Python 60 s before this shell timeout so it can write partial
# results instead of being killed.
OUTER_TIMEOUT=${PROFILE_OUTER_TIMEOUT_SECONDS:-1160}
TEARDOWN_RESERVE=${PROFILE_TEARDOWN_RESERVE_SECONDS:-60}
PROFILE_SWEEP_DEADLINE_EPOCH=$((WRAPPER_START + OUTER_TIMEOUT - TEARDOWN_RESERVE))
export PROFILE_SWEEP_DEADLINE_EPOCH
mkdir -p "$OUT/profile"

adb wait-for-device
for _ in $(seq 1 180); do
  [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ] && break
  sleep 2
done
if [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]; then
  echo "emulator boot timeout" >&2
  exit 1
fi

adb install -r /tmp/dotwel-fixture.apk
adb install -r /tmp/dotwel-test.apk
adb install -r /tmp/snapshot/helper.apk
adb shell appwidget grantbind --package "$PKG" --user 0
adb shell input keyevent 82
adb shell settings put system screen_off_timeout 2147483647
adb shell cmd notification allow_listener "$PKG/.media.MediaNotificationService" || true

# Warm the fixture host process without binding a widget so the baseline is a
# real no-widget measurement. profile_sweep.py then places each widget itself.
adb shell am start -W -n "$PKG/.RepairHostActivity" >/dev/null 2>&1 || true
sleep 1

timeout --kill-after=15s "${OUTER_TIMEOUT}s" python3 tools/repair/profile_sweep.py
status=$?
printf '%s\n' \
  "Synthetic media/weather fixtures and isolated AppWidgetHost only; human pixel review and launcher placement remain required." \
  > "$OUT/profile/LIMITS.txt"
exit $status
