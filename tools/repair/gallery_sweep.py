#!/usr/bin/env python3
"""Real gallery UI/Add and selected launcher pins. No source mutations or owner data."""
import json
import os
import pathlib
import re
import subprocess as sp
import sys
import time
import xml.etree.ElementTree as ET

from sweep_common import (
    BudgetExhausted,
    SweepBudget,
    compute_graceful_budget,
    ready_catalog_count,
    widget_catalog_count,
    widget_catalog_ready_count,
)

P = 'com.dotwelwidgets.dotwellabs'
OUT = pathlib.Path('runtime-evidence/gallery')
OUTER_WRAPPER_SECONDS = float(os.environ.get('GALLERY_OUTER_TIMEOUT_SECONDS', '1250'))
TEARDOWN_MARGIN = float(os.environ.get('GALLERY_SWEEP_TEARDOWN_RESERVE_SECONDS', '90'))
PER_ENTRY_SECONDS = float(os.environ.get('GALLERY_SWEEP_PER_ENTRY_SECONDS', '16'))
OVERHEAD_SECONDS = float(os.environ.get('GALLERY_SWEEP_OVERHEAD_SECONDS', '75'))
ENTRY_RESERVE = float(os.environ.get('GALLERY_SWEEP_ENTRY_RESERVE_SECONDS', '45'))
CHUNK_SIZE = max(1, int(os.environ.get('GALLERY_SWEEP_CHUNK_SIZE', '10')))
HARD_RECOVERY_SECONDS = float(os.environ.get('GALLERY_SWEEP_HARD_RECOVERY_SECONDS', '60'))
OPEN_TIMEOUT_SECONDS = float(os.environ.get('GALLERY_SWEEP_OPEN_TIMEOUT_SECONDS', '8'))
MAX_CONSECUTIVE_FAILURES = int(
    os.environ.get('GALLERY_SWEEP_MAX_CONSECUTIVE_FAILURES', '3')
)

PINNED_IDS = [
    'kwgt_interstellar_endurance', 'kwgt_nothing_digital2', 'media_mini',
    'wx_circle', 'photo_circle',
]

entries = []
positions = {}
SIZE = (1080, 2340)
budget = None


def remaining():
    return budget.remaining() if budget is not None else float('inf')


def guard(reserve=0.0):
    if budget is not None:
        budget.ensure(reserve)


def adb(*args, check=True, timeout=30):
    limit = budget.timeout(timeout) if budget is not None else timeout
    try:
        result = sp.run(['adb', *args], capture_output=True, timeout=limit)
    except sp.TimeoutExpired as error:
        if remaining() <= 1:
            raise BudgetExhausted('adb timed out at the sweep budget: '
                                  + ' '.join(args)) from error
        raise RuntimeError('adb timed out: ' + ' '.join(args)) from error
    if check and result.returncode:
        raise RuntimeError(result.stderr.decode(errors='replace'))
    return result.stdout


def tree():
    raw = adb('shell', 'am', 'instrument', '-w', 'org.dotwel.snapshot/.Snapshot').decode(errors='replace')
    a = raw.find('<hierarchy')
    b = raw.find('</hierarchy>')
    if a < 0 or b < 0:
        raise RuntimeError('fresh accessibility tree missing')
    return ET.fromstring(raw[a:b + 12])


def find(root, text, exact=True):
    return next((n for n in root.iter('node')
                 if any((text.casefold() == n.get(k, '').casefold() if exact
                         else text.casefold() in n.get(k, '').casefold())
                        for k in ('text', 'content-desc'))), None)


def click(node, top=False):
    if node is None:
        raise RuntimeError('UI target missing')
    b = list(map(int, re.findall(r'\d+', node.get('bounds', ''))))
    x = (b[0] + b[2]) // 2
    y = (b[1] + b[3]) // 2 if not top else b[1] + min(48, max(8, (b[3] - b[1]) // 4))
    adb('shell', 'input', 'tap', str(x), str(y))


def capture(name, root=None):
    (OUT / (name + '.png')).write_bytes(adb('exec-out', 'screencap', '-p'))
    try:
        (OUT / (name + '.xml')).write_bytes(
            ET.tostring(root if root is not None else tree()))
    except BudgetExhausted:
        raise
    except Exception:
        pass


def wait(predicate, timeout=6.0):
    end = time.time() + max(0.0, min(timeout, remaining()))
    last = None
    while True:
        try:
            last = tree()
            value = predicate(last)
            if value is not None:
                return last, value
        except BudgetExhausted:
            raise
        except Exception:
            last = None
        if time.time() >= end:
            return last, None
        time.sleep(.15)


CATEGORY_RE = re.compile(r'^Open .+, \d+ ready$', re.I)


def gallery_marker(root):
    return find(root, 'CATEGORIES') is not None or any(
        CATEGORY_RE.match((n.get('content-desc') or '').strip())
        for n in root.iter('node'))


def screen():
    try:
        m = re.search(r'(\d+)x(\d+)',
                      adb('shell', 'wm', 'size').decode(errors='replace'))
        if m:
            return int(m.group(1)), int(m.group(2))
    except BudgetExhausted:
        raise
    except Exception:
        pass
    return 1080, 2340


def swipe_content(direction):
    w, h = SIZE
    x = w // 2
    y1, y2 = ((int(h * .62), int(h * .30)) if direction == 'forward'
              else (int(h * .30), int(h * .62)))
    adb('shell', 'input', 'swipe', str(x), str(y1), str(x), str(y2), '220')


def launch_gallery():
    adb('shell', 'am', 'force-stop', P, check=False)
    adb('shell', 'am', 'start', '-W', '-n', P + '/.MainActivity', check=False)
    root, _ = wait(lambda r: r if gallery_marker(r) else None,
                   timeout=OPEN_TIMEOUT_SECONDS)
    return root if root is not None and gallery_marker(root) else None


def monkey_launch_gallery():
    adb('shell', 'monkey', '-p', P, '-c',
        'android.intent.category.LAUNCHER', '1', check=False)
    root, _ = wait(lambda r: r if gallery_marker(r) else None,
                   timeout=OPEN_TIMEOUT_SECONDS)
    return root if root is not None and gallery_marker(root) else None


def hard_recover_gallery(timeout=None):
    """Force-stop and relaunch until the gallery root is actually visible.

    This is deliberately not the old quick ``am start`` path: a native launcher
    pin can leave a system dialog or a different task in front, so retry both
    ``am start`` and a monkey launcher intent for up to ``timeout`` seconds.
    """
    timeout = HARD_RECOVERY_SECONDS if timeout is None else timeout
    deadline = time.time() + max(0.0, min(timeout, max(0.0, remaining() - 1)))
    while time.time() < deadline:
        adb('shell', 'input', 'keyevent', '3', check=False)
        adb('shell', 'am', 'force-stop', P, check=False)
        root = launch_gallery()
        if root is not None:
            return root
        if time.time() >= deadline:
            break
        root = monkey_launch_gallery()
        if root is not None:
            return root
        time.sleep(.5)
    return None


def open_gallery(hard=False):
    if hard:
        return hard_recover_gallery()
    for _ in range(3):
        adb('shell', 'input', 'keyevent', '3', check=False)
        adb('shell', 'am', 'force-stop', P, check=False)
        adb('shell', 'am', 'start', '-W', '-n', P + '/.MainActivity', check=False)
        root, _ = wait(lambda r: r if gallery_marker(r) else None,
                       timeout=OPEN_TIMEOUT_SECONDS)
        if root is not None and gallery_marker(root):
            return root
    return None


def reset_activity_stack():
    """Restart the sweep task stack once after a run of failed relaunches."""
    adb('shell', 'am', 'kill', P, check=False)
    adb('shell', 'input', 'keyevent', '3', check=False)
    adb('shell', 'am', 'force-stop', P, check=False)
    adb('shell', 'am', 'start', '-W', '-n', P + '/.MainActivity', check=False)
    wait(lambda r: r if gallery_marker(r) else None, timeout=10)


def in_view(node):
    lo = 120
    hi = SIZE[1] - int(SIZE[1] * .16)
    b = list(map(int, re.findall(r'\d+', node.get('bounds', ''))))
    return bool(b) and b[1] >= lo and b[1] <= hi


def bring_into_view(label, root):
    for _ in range(8):
        guard(ENTRY_RESERVE)
        node = find(root, 'Open ' + label + ',', False)
        if node is None:
            return None
        if in_view(node):
            return node
        b = list(map(int, re.findall(r'\d+', node.get('bounds', ''))))
        swipe_content('forward' if b[1] > SIZE[1] - int(SIZE[1] * .16) else 'back')
        time.sleep(.15)
        try:
            root = tree()
        except BudgetExhausted:
            raise
        except Exception:
            pass
    return find(root, 'Open ' + label + ',', False)


def seek_category(label):
    root, _ = wait(lambda r: r if gallery_marker(r) else None,
                   timeout=OPEN_TIMEOUT_SECONDS)
    if root is None:
        raise RuntimeError('Gallery did not load')
    for direction in ('forward', 'back'):
        for _ in range(14):
            guard(ENTRY_RESERVE)
            node = find(root, 'Open ' + label + ',', False)
            if node is not None:
                node = bring_into_view(label, root)
                if node is not None:
                    return node
            swipe_content(direction)
            time.sleep(.15)
            try:
                root = tree()
            except BudgetExhausted:
                raise
            except Exception:
                pass
    try:
        capture('category-' + re.sub(r'\W+', '-', label).strip('-').lower()
                + '-failure', root)
    except BudgetExhausted:
        raise
    except Exception:
        pass
    raise RuntimeError('Category unavailable: ' + label)


def sheet_marker(root):
    if find(root, 'Close category') is None:
        return None
    return next((n for n in root.iter('node')
                 if (n.get('content-desc') or '').startswith('Add ')), None)


def confirmation(root):
    # Ignore the still-dismissing gallery sheet, whose button also says "Add widget".
    if find(root, 'Close category') is not None:
        return None
    for text in ('Add to home screen', 'Add automatically', 'Add widget', 'Add'):
        node = find(root, text)
        if node is not None:
            return node
    return None


def access_fallback(root):
    for node in root.iter('node'):
        text = (node.get('text') or '').casefold()
        if text.startswith('add ') and any(
                word in text for word in ('plain', 'live', 'fallback')):
            return node
    return None


def wait_pin(timeout=10.0):
    end = time.time() + max(0.0, min(timeout, remaining()))
    plain_clicks = 0
    last = None
    while True:
        try:
            last = tree()
        except BudgetExhausted:
            raise
        except Exception:
            last = None
        if last is not None:
            confirm = confirmation(last)
            if confirm is not None:
                return last, confirm
            save = find(last, 'Save and add')
            if save is not None:
                click(save)
                time.sleep(.4)
                continue
            plain = access_fallback(last)
            if plain is not None and plain_clicks < 2:
                click(plain)
                plain_clicks += 1
                time.sleep(.4)
                continue
        if time.time() >= end:
            return last, None
        time.sleep(.15)


def mark_not_attempted(results, start, reason='not attempted - budget exhausted'):
    for rest in entries[start:]:
        skipped = dict(rest)
        skipped['gallery_add'] = reason
        results.append(skipped)


def persist(results):
    (OUT / 'RESULT.json').write_text(json.dumps(results, indent=2))


def main():
    global entries, positions, SIZE, budget
    OUT.mkdir(parents=True, exist_ok=True)
    entries = json.loads(
        pathlib.Path('runtime-evidence/ready/catalog.json').read_text())
    count = len(entries) or ready_catalog_count() or widget_catalog_ready_count() \
        or widget_catalog_count()
    default_budget = compute_graceful_budget(
        count, per_entry=PER_ENTRY_SECONDS, overhead=OVERHEAD_SECONDS,
        outer=OUTER_WRAPPER_SECONDS, margin=TEARDOWN_MARGIN)
    budget = SweepBudget(
        budget_seconds=float(os.environ.get('GALLERY_SWEEP_BUDGET_SECONDS',
                                            default_budget)),
        deadline_epoch=(float(os.environ['GALLERY_SWEEP_DEADLINE_EPOCH'])
                        if os.environ.get('GALLERY_SWEEP_DEADLINE_EPOCH') else None),
    )
    # catalog.json is grouped by category; this is the sheet index for each entry.
    positions = {}
    seen = {}
    for ix, entry in enumerate(entries):
        positions[ix] = seen.get(entry['category'], 0)
        seen[entry['category']] = positions[ix] + 1
    try:
        SIZE = screen()
    except BudgetExhausted:
        SIZE = (1080, 2340)
    print('catalog entries: {} default budget: {:.0f}s'.format(
        count, default_budget), flush=True)

    results = []
    consecutive_failures = 0
    stack_reset_done = False
    needs_hard_recovery = False
    try:
        for ix, entry in enumerate(entries):
            name = '{:02d}-{}'.format(ix + 1, entry['id'])
            result = dict(entry)
            results.append(result)
            if remaining() <= ENTRY_RESERVE:
                result['gallery_add'] = 'not attempted - budget exhausted'
                mark_not_attempted(results, ix + 1)
                print(name + ': not attempted - budget exhausted', flush=True)
                break
            hard = (needs_hard_recovery or consecutive_failures > 0
                    or (ix > 0 and ix % CHUNK_SIZE == 0))
            try:
                guard(ENTRY_RESERVE)
                root = open_gallery(hard=hard)
                if root is None:
                    root = hard_recover_gallery()
                if root is None:
                    raise RuntimeError('Gallery did not load')
                needs_hard_recovery = False
                click(seek_category(entry['category']), top=True)
                root, sheet = wait(
                    lambda r: r if sheet_marker(r) is not None else None,
                    timeout=8)
                if sheet is None:
                    click(seek_category(entry['category']), top=True)
                    root, sheet = wait(
                        lambda r: r if sheet_marker(r) is not None else None,
                        timeout=8)
                if sheet is None:
                    raise RuntimeError('Category sheet did not open: '
                                       + entry['category'])
                next_button = find(root, 'Next')
                position = positions[ix]
                if position and next_button is not None:
                    # The sheet controls are fixed; fast-forward without a tree
                    # round-trip per page.
                    b = list(map(int, re.findall(r'\d+',
                                                 next_button.get('bounds', ''))))
                    x = (b[0] + b[2]) // 2
                    y = (b[1] + b[3]) // 2
                    adb('shell', ';'.join([f'input tap {x} {y}'] * position))
                root, button = wait(lambda r: find(r, 'Add ' + entry['name']),
                                    timeout=6)
                if button is None:
                    for _page in range(30):
                        guard(ENTRY_RESERVE)
                        try:
                            root = tree()
                        except BudgetExhausted:
                            raise
                        except Exception:
                            continue
                        button = find(root, 'Add ' + entry['name'])
                        if button is not None:
                            break
                        nxt = find(root, 'Next')
                        if nxt is None:
                            break
                        click(nxt)
                    if button is None:
                        raise RuntimeError('Selected gallery identity not reached')
                capture(name + '-gallery', root)
                click(button)
                root, confirm = wait_pin()
                if confirm is None:
                    raise RuntimeError(
                        'No native pin confirmation; explanation captured')
                capture(name + '-pin-confirmation', root)
                result['gallery_add'] = 'native confirmation reached'
                if entry['id'] in PINNED_IDS:
                    click(confirm)
                    wait(lambda r: True if confirmation(r) is None else None,
                         timeout=4)
                    adb('shell', 'input', 'keyevent', '3')
                    time.sleep(.4)
                    capture(name + '-launcher')
                    result['launcher'] = (
                        'pinned; actual pixels and dumpsys require review')
                    if entry['id'] in ['kwgt_interstellar_endurance',
                                       'kwgt_nothing_digital2']:
                        adb('shell', 'am', 'kill', P)
                        capture(name + '-launcher-killed-before')
                        time.sleep(3)
                        capture(name + '-launcher-killed-after')
                    # A native pin can leave the launcher task in front; force a
                    # full relaunch before the next gallery entry.
                    needs_hard_recovery = True
                else:
                    adb('shell', 'input', 'keyevent', '4')
                consecutive_failures = 0
            except BudgetExhausted:
                result.pop('error', None)
                result['gallery_add'] = 'not attempted - budget exhausted'
                mark_not_attempted(results, ix + 1)
                print(name + ': not attempted - budget exhausted', flush=True)
                break
            except Exception as error:
                result['error'] = str(error)
                consecutive_failures += 1
                if (consecutive_failures >= MAX_CONSECUTIVE_FAILURES
                        and not stack_reset_done):
                    stack_reset_done = True
                    print('{} consecutive gallery failures; resetting the '
                          'sweep activity stack once'.format(consecutive_failures),
                          flush=True)
                    try:
                        reset_activity_stack()
                    except BudgetExhausted:
                        pass
                    except Exception as reset_error:
                        print('stack reset failed: ' + str(reset_error),
                              flush=True)
                    needs_hard_recovery = True
                    consecutive_failures = 0
                try:
                    capture(name + '-failure')
                except BudgetExhausted:
                    pass
                except Exception:
                    pass
            finally:
                persist(results)
                print('{}: {}'.format(
                    name, result.get('gallery_add') or result.get('error')),
                    flush=True)
    finally:
        persist(results)
        try:
            (OUT / 'appwidget-dump.txt').write_bytes(
                adb('shell', 'dumpsys', 'appwidget', timeout=15))
        except BudgetExhausted:
            pass
        except Exception:
            pass
        print(json.dumps(results, indent=2))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
