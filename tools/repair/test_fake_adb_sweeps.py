#!/usr/bin/env python3
"""Emulator-free fake-adb checks for the two sweep scripts.

These are deliberately small simulations, not device evidence.  They prove the
budget deadline fires before an outer wrapper and that profile uid resolution
survives the package-manager formats without an emulator.

Run with: python3 tools/repair/test_fake_adb_sweeps.py
"""

import json
import os
import pathlib
import subprocess
import sys
import tempfile
import textwrap
import time
import unittest

REPO = pathlib.Path(__file__).resolve().parents[2]
GALLERY = REPO / "tools" / "repair" / "gallery_sweep.py"
PROFILE = REPO / "tools" / "repair" / "profile_sweep.py"

FAKE_ADB = textwrap.dedent(
    """\
    #!/usr/bin/env python3
    import os
    import pathlib
    import sys

    args = sys.argv[1:]
    state = pathlib.Path(os.environ.get("FAKE_STATE", "state"))
    scenario = os.environ.get("FAKE_SCENARIO", "gallery")

    def out(text):
        sys.stdout.write(text)

    crash = pathlib.Path(str(state) + ".crash")
    closed = pathlib.Path(str(state) + ".crash-closed")

    if scenario in ("gallery", "gallery-crash"):
        if args[:3] == ["shell", "wm", "size"]:
            out("Physical size: 1080x2340\\n")
        elif "instrument" in args:
            if (scenario == "gallery-crash" and crash.exists()
                    and not closed.exists()):
                out('<hierarchy>'
                    '<node resource-id="android:id/aerr_close" text="Close app" '
                    'bounds="[100,100][300,200]"/>'
                    '<node text="Dotwel Widgets keeps stopping"/>'
                    '</hierarchy>\\n')
            else:
                out('<hierarchy><node text="CATEGORIES"/>'
                    '<node content-desc="Open Clock, 3 ready" '
                    'bounds="[100,300][900,500]"/></hierarchy>\\n')
        elif args[:2] == ["shell", "input"] and "tap" in args:
            if scenario == "gallery-crash":
                if crash.exists() and not closed.exists():
                    closed.write_text("closed")
                else:
                    crash.write_text("crashed")
                    if closed.exists():
                        closed.unlink()
        elif "screencap" in args:
            out("PNG")
        elif "dumpsys" in args and "appwidget" in args:
            out("appwidget dump\\n")
        sys.exit(0)

    if "cmd" in args and "list" in args and "-U" in args:
        if os.environ.get("FAKE_NO_UID") != "1":
            out("package:com.other uid:1000\\n"
                "package:com.dotwelwidgets.dotwellabs uid:10234\\n")
    elif "pm" in args and "list" in args and "-U" in args:
        if os.environ.get("FAKE_NO_UID") != "1":
            out("package:com.dotwelwidgets.dotwellabs uid:10234\\n")
    elif "dumpsys" in args and "package" in args:
        if os.environ.get("FAKE_NO_UID") != "1":
            out("userId=10234\\n")
    elif "am" in args and "start" in args and any(
            ".MainActivity" in arg for arg in args):
        state.write_text("started")
    elif "monkey" in args:
        state.write_text("started")
    elif "pidof" in args:
        if state.exists():
            out("1234\\n")
    elif "dumpsys" in args and "meminfo" in args:
        if os.environ.get("FAKE_NO_MEMINFO") != "1" and state.exists():
            if os.environ.get("FAKE_MEMINFO_COMMA") == "1":
                out("TOTAL PSS:   18,732\\n")
            else:
                out("TOTAL PSS: 12345\\n")
        else:
            out("No process found\\n")
    elif "dumpsys" in args and "cpuinfo" in args:
        out(" 1.0% 1234/com.dotwelwidgets.dotwellabs\\n")
    elif "dumpsys" in args and "power" in args:
        out("Wake Locks: size=0\\n")
    elif "dumpsys" in args and "batterystats" in args:
        out("Battery Stats\\n")
    elif "instrument" in args:
        out('<hierarchy><node text="RepairHostActivity"/>'
            '<node text="id=42"/></hierarchy>\\n')
    sys.exit(0)
    """
)


def write_catalog(root, count=3):
    ready = root / "runtime-evidence" / "ready"
    ready.mkdir(parents=True, exist_ok=True)
    entries = [
        {
            "id": f"w{index}",
            "name": f"Widget {index}",
            "provider": "P",
            "category": "Clock",
            "action": "A",
        }
        for index in range(count)
    ]
    (ready / "catalog.json").write_text(json.dumps(entries))


class FakeAdbSweepsTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = pathlib.Path(self.temp.name)
        self.bin = self.root / "bin"
        self.bin.mkdir()
        adb = self.bin / "adb"
        adb.write_text(FAKE_ADB)
        adb.chmod(0o755)
        self.env = os.environ.copy()
        self.env["PATH"] = str(self.bin) + os.pathsep + self.env["PATH"]
        self.env["FAKE_STATE"] = str(self.root / "state")

    def tearDown(self):
        self.temp.cleanup()

    def run_script(self, script, extra_env):
        env = self.env.copy()
        env.update(extra_env)
        return subprocess.run(
            [sys.executable, str(script)],
            cwd=self.root,
            env=env,
            capture_output=True,
            text=True,
            timeout=60,
        )

    def result(self, path):
        return json.loads((self.root / path).read_text())

    def test_gallery_deadline_marks_all_entries_exhausted(self):
        write_catalog(self.root)
        result = self.run_script(GALLERY, {
            "FAKE_SCENARIO": "gallery",
            "GALLERY_SWEEP_DEADLINE_EPOCH": str(int(time.time()) + 2),
            "GALLERY_SWEEP_ENTRY_RESERVE_SECONDS": "45",
            "GALLERY_SWEEP_OPEN_TIMEOUT_SECONDS": "0.25",
        })
        self.assertEqual(result.returncode, 0, result.stderr)
        entries = self.result("runtime-evidence/gallery/RESULT.json")
        self.assertEqual(len(entries), 3)
        self.assertTrue(all(
            entry.get("gallery_add") == "not attempted - budget exhausted"
            for entry in entries
        ))

    def test_gallery_dismisses_crash_dialog_and_marks_crashed_entry(self):
        write_catalog(self.root)
        result = self.run_script(GALLERY, {
            "FAKE_SCENARIO": "gallery-crash",
            "GALLERY_SWEEP_OPEN_TIMEOUT_SECONDS": "0.5",
            "GALLERY_SWEEP_ENTRY_RESERVE_SECONDS": "5",
        })
        self.assertEqual(result.returncode, 0, result.stderr)
        entries = self.result("runtime-evidence/gallery/RESULT.json")
        self.assertEqual(len(entries), 3)
        self.assertTrue(all(
            entry.get("gallery_add") == "app crashed" for entry in entries
        ), entries)
        self.assertTrue(
            (self.root / "state.crash-closed").exists(),
            "the crash dialog close button must be tapped",
        )

    def test_profile_resolves_uid_and_uses_it_for_batterystats(self):
        result = self.run_script(PROFILE, {
            "FAKE_SCENARIO": "profile",
            "PROFILE_SETTLE_SECONDS": "0",
            "PROFILE_SWEEP_DEADLINE_EPOCH": str(int(time.time()) + 2),
        })
        self.assertEqual(result.returncode, 0, result.stderr)
        data = self.result("runtime-evidence/profile/RESULT.json")
        self.assertEqual(data["uid"], 10234)
        self.assertEqual(data["baseline"]["battery"]["uid"], 10234)
        self.assertEqual(
            data["baseline"]["app"]["totalPssKb"], 12345)

    def test_profile_parses_comma_grouped_meminfo(self):
        result = self.run_script(PROFILE, {
            "FAKE_SCENARIO": "profile",
            "FAKE_MEMINFO_COMMA": "1",
            "PROFILE_SETTLE_SECONDS": "0",
            "PROFILE_SWEEP_DEADLINE_EPOCH": str(int(time.time()) + 2),
        })
        self.assertEqual(result.returncode, 0, result.stderr)
        data = self.result("runtime-evidence/profile/RESULT.json")
        self.assertEqual(data["baseline"]["app"]["totalPssKb"], 18732)

    def test_profile_saves_raw_meminfo_when_parsing_fails(self):
        result = self.run_script(PROFILE, {
            "FAKE_SCENARIO": "profile",
            "FAKE_NO_MEMINFO": "1",
            "PROFILE_SETTLE_SECONDS": "0",
            "PROFILE_SWEEP_DEADLINE_EPOCH": str(int(time.time()) + 5),
        })
        self.assertEqual(result.returncode, 0, result.stderr)
        raw = (self.root / "runtime-evidence" / "profile" / "raw"
               / "baseline-meminfo.txt")
        self.assertTrue(raw.exists(), "failed meminfo must leave a raw dump")
        self.assertIn("No process found", raw.read_text())

    def test_profile_exits_nonzero_when_baseline_has_no_evidence(self):
        result = self.run_script(PROFILE, {
            "FAKE_SCENARIO": "profile",
            "FAKE_NO_UID": "1",
            "FAKE_NO_MEMINFO": "1",
            "PROFILE_SETTLE_SECONDS": "0",
            "PROFILE_SWEEP_DEADLINE_EPOCH": str(int(time.time()) + 2),
        })
        self.assertEqual(result.returncode, 1, result.stderr)
        data = self.result("runtime-evidence/profile/RESULT.json")
        self.assertIsNone(data["uid"])
        self.assertIsNone(data["baseline"]["app"]["totalPssKb"])


if __name__ == "__main__":
    unittest.main(verbosity=2)
