#!/usr/bin/env python3
"""Bounded widget resource profile sweep for the isolated fixture host.

The emulator is assumed to be booted with the fixture host (the debug app built
with -PrepairFixtures) and the accessibility snapshot helper installed. This
script places one representative widget at a time through RepairHostActivity,
lets it settle, then records memory, CPU, battery and wake-lock state before and
after an update tick. It never changes app source, permissions or owner data.
"""
import json
import os
import pathlib
import re
import subprocess as sp
import time
import xml.etree.ElementTree as ET

P = "com.dotwelwidgets.dotwellabs"
HOST_PACKAGE = os.environ.get("PROFILE_HOST_PACKAGE", P)
OUT = pathlib.Path(os.environ.get("PROFILE_OUTPUT_DIR", "runtime-evidence/profile"))
RAW = OUT / "raw"
BUDGET = float(os.environ.get("PROFILE_SWEEP_BUDGET_SECONDS", "900"))
SETTLE = float(os.environ.get("PROFILE_SETTLE_SECONDS", "10"))
WIDGET_WIDTH = int(os.environ.get("PROFILE_WIDGET_WIDTH", "320"))
WIDGET_HEIGHT = int(os.environ.get("PROFILE_WIDGET_HEIGHT", "260"))
START = time.monotonic()

REPRESENTATIVE = [
    {"id": "digital", "name": "Digital Clock",
     "provider": f"{P}.ClockWidgetProvider", "category": "clock"},
    {"id": "kwgt_interstellar_endurance_analog", "name": "Interstellar Analog",
     "provider": f"{P}.KwgtInterstellarEnduranceAnalogProvider", "category": "clock"},
    {"id": "kwgt_interstellar_endurance", "name": "Interstellar Endurance",
     "provider": f"{P}.KwgtInterstellarEnduranceProvider", "category": "clock"},
    {"id": "kwgt_nothing_battery", "name": "Battery Gauge",
     "provider": f"{P}.BatteryWidgetProvider", "category": "battery"},
    {"id": "wx_circle", "name": "Weather Circle",
     "provider": f"{P}.NtWxCircleProvider", "category": "weather",
     "fixture": "weather"},
    {"id": "media_mini", "name": "Mini Media",
     "provider": f"{P}.MiniMediaPlayerProvider", "category": "media",
     "fixture": "media"},
    {"id": "photo_circle", "name": "Photo Circle",
     "provider": f"{P}.NtPhotoCircleProvider", "category": "photo"},
    {"id": "quotes", "name": "Quotes",
     "provider": f"{P}.QuotesWidgetProvider", "category": "quotes"},
    {"id": "away", "name": "Away Time",
     "provider": f"{P}.NtAwayWidgetProvider", "category": "away"},
]


def remaining():
    return BUDGET - (time.monotonic() - START)


def adb(*args, check=True, timeout=60):
    result = sp.run(["adb", *args], capture_output=True, timeout=timeout)
    if check and result.returncode:
        raise RuntimeError(result.stderr.decode(errors="replace").strip())
    return result.stdout


def adb_text(*args, **kwargs):
    return adb(*args, **kwargs).decode(errors="replace")


def tree():
    raw = adb_text(
        "shell", "am", "instrument", "-w", "org.dotwel.snapshot/.Snapshot"
    )
    a = raw.find("<hierarchy")
    b = raw.find("</hierarchy>")
    if a < 0 or b < 0:
        raise RuntimeError("fresh accessibility tree missing")
    return ET.fromstring(raw[a:b + len("</hierarchy>")])


def node_text(node):
    return " ".join(
        filter(None, (node.get("text") or "", node.get("content-desc") or ""))
    )


def wait_for_host(timeout=8.0):
    end = time.time() + max(0.0, min(timeout, remaining()))
    last = None
    while True:
        try:
            last = tree()
            if any("RepairHostActivity" in node_text(n) or "id=" in node_text(n)
                   for n in last.iter("node")):
                return last
        except Exception:
            last = None
        if time.time() >= end:
            return last
        time.sleep(0.25)


def placed_widget_id(root):
    if root is None:
        return None
    for node in root.iter("node"):
        match = re.search(r"\bid=(\d+)\b", node_text(node))
        if match:
            return int(match.group(1))
    return None


def start_host(provider, style=0):
    args = [
        "shell", "am", "start", "-W", "-n", f"{P}/.RepairHostActivity",
        "--ei", "width", str(WIDGET_WIDTH),
        "--ei", "height", str(WIDGET_HEIGHT),
        "--ei", "style", str(style),
    ]
    if provider:
        args += ["--es", "provider", provider]
    adb(*args, timeout=30)
    return wait_for_host()


def start_fixture(activity, extra=None):
    args = ["shell", "am", "start", "-W", "-n", f"{P}/.{activity}"]
    if extra:
        args += extra
    try:
        adb(*args, timeout=30)
    except Exception as error:
        print(f"fixture {activity}: {error}", flush=True)
        return False
    return True


def ensure_media_fixture():
    start_fixture("RepairMediaActivity")
    end = time.time() + 8.0
    while time.time() < end and remaining() > 5:
        try:
            events = adb_text(
                "exec-out", "run-as", P, "cat", "no_backup/repair-media-events.txt",
                check=False,
            )
            if "state:" in events:
                return True
        except Exception:
            pass
        time.sleep(0.5)
    return False


def total_pss_kb(text):
    patterns = (
        r"TOTAL PSS:\s*(\d+)",
        r"^\s*TOTAL:\s+(\d+)",
        r"^\s*TOTAL\s+(\d+)",
    )
    for pattern in patterns:
        match = re.search(pattern, text, re.MULTILINE)
        if match:
            return int(match.group(1))
    return None


def meminfo(package):
    text = adb_text("shell", "dumpsys", "meminfo", package, check=False)
    return {"package": package, "totalPssKb": total_pss_kb(text)}


def package_uid():
    text = adb_text("shell", "dumpsys", "package", P, check=False)
    match = re.search(r"userId=(\d+)", text)
    return int(match.group(1)) if match else None


def cpu_percent(text, package):
    for line in text.splitlines():
        match = re.match(r"\s*([\d.]+)%\s+\d+/([\w.:]+)", line)
        if match and (match.group(2) == package
                      or match.group(2).startswith(package + ":")):
            return float(match.group(1))
    return None


def cpu_snapshot():
    text = adb_text("shell", "dumpsys", "cpuinfo", check=False)
    top = [line for line in text.splitlines() if line.strip()][:18]
    return {"appPercent": cpu_percent(text, P), "top": top}


def wake_locks():
    text = adb_text("shell", "dumpsys", "power", check=False)
    lines = text.splitlines()
    held = []
    for index, line in enumerate(lines):
        if line.strip().startswith("Wake Locks:"):
            for extra in lines[index + 1:]:
                if not extra.strip():
                    break
                held.append(extra.strip())
            break
    count_match = re.search(r"Wake Locks:\s*size=(\d+)", text)
    count = int(count_match.group(1)) if count_match else len(held)
    return {"count": count, "held": held}


def battery_stats(uid):
    if uid is None:
        return {"uid": None, "excerpt": []}
    text = adb_text("shell", "dumpsys", "batterystats", "--uid", str(uid),
                    check=False)
    if not text.strip():
        text = adb_text("shell", "dumpsys", "batterystats", P, check=False)
    return {"uid": uid, "excerpt": [line for line in text.splitlines()
                                    if line.strip()][:30]}


def snapshot(label):
    cpu = cpu_snapshot()
    return {
        "label": label,
        "atMillis": int(time.time() * 1000),
        "app": meminfo(P),
        "host": meminfo(HOST_PACKAGE),
        "cpu": cpu,
        "wakeLocks": wake_locks(),
        "battery": battery_stats(UID),
    }


def save_raw(label, data):
    RAW.mkdir(parents=True, exist_ok=True)
    (RAW / f"{label}.json").write_text(json.dumps(data, indent=2))
    (RAW / f"{label}-batterystats.txt").write_text(
        "\n".join(data["battery"]["excerpt"])
    )


def delta(value, base):
    if value is None or base is None:
        return None
    return value - base


def trigger_tick(provider, widget_id):
    if widget_id is not None:
        try:
            adb(
                "shell", "am", "broadcast",
                "-a", "android.appwidget.action.APPWIDGET_UPDATE",
                "-n", f"{P}/{provider}",
                "--eia", "appWidgetIds", str(widget_id),
                timeout=30,
            )
            return "APPWIDGET_UPDATE"
        except Exception as error:
            print(f"tick broadcast failed: {error}", flush=True)
    start_host(provider)
    return "host restart"


def summarize(results):
    baseline = results.get("baseline") or {}
    base_app = (baseline.get("app") or {}).get("totalPssKb")
    base_host = (baseline.get("host") or {}).get("totalPssKb")
    lines = [
        "# Widget resource profile",
        "",
        f"- Package: `{P}`",
        f"- Fixture host process: `{HOST_PACKAGE}`",
        f"- Budget: {int(BUDGET)} s",
        f"- Settle after placement: {int(SETTLE)} s",
        "",
        "| Widget | App PSS kB | dApp | Host PSS kB | dHost | CPU % | Wake locks | Tick growth app/host kB | Status |",
        "| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |",
        "| baseline | {} | - | {} | - | {} | {} | - | measured |".format(
            base_app, base_host,
            (baseline.get("cpu") or {}).get("appPercent"),
            (baseline.get("wakeLocks") or {}).get("count"),
        ),
    ]
    for item in results.get("widgets", []):
        settled = item.get("settled") or {}
        after = item.get("afterTick") or {}
        app = (settled.get("app") or {}).get("totalPssKb")
        host = (settled.get("host") or {}).get("totalPssKb")
        growth_app = delta((after.get("app") or {}).get("totalPssKb"), app)
        growth_host = delta((after.get("host") or {}).get("totalPssKb"), host)
        lines.append("| {} | {} | {} | {} | {} | {} | {} | {}/{} | {} |".format(
            item.get("id"),
            app, item.get("appDeltaKb"),
            host, item.get("hostDeltaKb"),
            (settled.get("cpu") or {}).get("appPercent"),
            (settled.get("wakeLocks") or {}).get("count"),
            growth_app, growth_host,
            item.get("status", "measured"),
        ))
    lines += [
        "",
        "Deltas are relative to the no-widget baseline in the same boot.",
        "A positive tick growth means the update pass increased process PSS.",
        "The fixture host runs in the widget app process unless",
        "`PROFILE_HOST_PACKAGE` overrides it, so app and host PSS can be equal.",
        "This harness records measurements only; it does not claim a performance",
        "pass or a launcher placement.",
        "",
    ]
    return "\n".join(lines)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    RAW.mkdir(parents=True, exist_ok=True)
    global UID
    results = {
        "package": P,
        "hostPackage": HOST_PACKAGE,
        "budgetSeconds": BUDGET,
        "settleSeconds": SETTLE,
        "startedAtMillis": int(time.time() * 1000),
        "baseline": None,
        "widgets": [],
    }
    try:
        UID = package_uid()
        results["uid"] = UID
        adb("shell", "am", "force-stop", P, check=False)
        start_host(None)
        baseline = snapshot("baseline")
        save_raw("baseline", baseline)
        results["baseline"] = baseline
        base_app = baseline["app"]["totalPssKb"]
        base_host = baseline["host"]["totalPssKb"]
        for entry in REPRESENTATIVE:
            item = {
                "id": entry["id"],
                "name": entry["name"],
                "provider": entry["provider"],
                "category": entry["category"],
            }
            results["widgets"].append(item)
            if remaining() < SETTLE + 25:
                item["status"] = "not attempted - budget exhausted"
                print(f"{entry['id']}: budget exhausted", flush=True)
                continue
            try:
                if entry.get("fixture") == "media":
                    item["mediaFixture"] = ensure_media_fixture()
                elif entry.get("fixture") == "weather":
                    item["weatherFixture"] = start_fixture("RepairWeatherActivity")
                    time.sleep(1.0)
                root = start_host(entry["provider"])
                item["widgetId"] = placed_widget_id(root)
                time.sleep(max(0.0, min(SETTLE, remaining() - 20)))
                settled = snapshot(entry["id"] + "-settled")
                save_raw(entry["id"] + "-settled", settled)
                item["settled"] = settled
                item["appTotalPssKb"] = settled["app"]["totalPssKb"]
                item["hostTotalPssKb"] = settled["host"]["totalPssKb"]
                item["appDeltaKb"] = delta(item["appTotalPssKb"], base_app)
                item["hostDeltaKb"] = delta(item["hostTotalPssKb"], base_host)
                item["tickTrigger"] = trigger_tick(
                    entry["provider"], item.get("widgetId")
                )
                time.sleep(2.0)
                after = snapshot(entry["id"] + "-after-tick")
                save_raw(entry["id"] + "-after-tick", after)
                item["afterTick"] = after
                item["tickGrowthAppKb"] = delta(
                    after["app"]["totalPssKb"], item["appTotalPssKb"]
                )
                item["tickGrowthHostKb"] = delta(
                    after["host"]["totalPssKb"], item["hostTotalPssKb"]
                )
                item["status"] = "measured"
                print(f"{entry['id']}: measured", flush=True)
            except Exception as error:
                item["status"] = "error"
                item["error"] = str(error)
                print(f"{entry['id']}: {error}", flush=True)
            finally:
                (OUT / "RESULT.json").write_text(json.dumps(results, indent=2))
                (OUT / "SUMMARY.md").write_text(summarize(results))
    finally:
        if results.get("baseline") is None:
            try:
                results["baseline"] = snapshot("baseline")
            except Exception as error:
                results["baselineError"] = str(error)
        results["finishedAtMillis"] = int(time.time() * 1000)
        results["limits"] = (
            "Isolated fixture AppWidgetHost measurements only; not a launcher "
            "placement, performance pass or Play claim. Synthetic media/weather "
            "fixtures are labelled where used."
        )
        (OUT / "RESULT.json").write_text(json.dumps(results, indent=2))
        (OUT / "SUMMARY.md").write_text(summarize(results))
        print(json.dumps(results, indent=2))
    return 0


UID = None

if __name__ == "__main__":
    raise SystemExit(main())
